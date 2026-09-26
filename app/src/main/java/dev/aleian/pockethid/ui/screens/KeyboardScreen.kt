package dev.aleian.pockethid.ui.screens

import android.content.Context
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import dev.aleian.pockethid.action.ActionDispatcher
import dev.aleian.pockethid.action.ActionExecutionPlan
import dev.aleian.pockethid.mapping.TextInputResolver
import dev.aleian.pockethid.ui.components.ImeDiagnosticsHub
import dev.aleian.pockethid.ui.components.PocketImeInputView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.aleian.pockethid.ui.components.DedicatedNumberRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import dev.aleian.pockethid.mapping.KeyMapper
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.DeckKey
import dev.aleian.pockethid.ui.components.SubModeSelector
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class ModifierState {
    OFF,
    STICKY, // active for the next key, then turns off
    LOCKED  // stays active until tapped again
}

data class SpecialKey(
    val label: String,
    val keyCode: Byte,
    val baseModifier: Byte = 0
)

@Composable
fun KeyboardScreen(
    transport: InputTransport?,
    connectionState: dev.aleian.pockethid.model.ConnectionState = dev.aleian.pockethid.model.ConnectionState.Disconnected,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by dev.aleian.pockethid.model.SettingsRepository.settings.collectAsState()

    var selectedLayer by remember { mutableIntStateOf(0) } // 0: Type, 1: Shortcuts, 2: Media, 3: System, 4: F-Keys, 5: Numpad

    var ctrlState by remember { mutableStateOf(ModifierState.OFF) }
    var shiftState by remember { mutableStateOf(ModifierState.OFF) }
    var altState by remember { mutableStateOf(ModifierState.OFF) }
    var guiState by remember { mutableStateOf(ModifierState.OFF) } // Win / Cmd

    var imeInputView by remember { mutableStateOf<PocketImeInputView?>(null) }
    var lastSentCharInfo by remember { mutableStateOf("Ready to type") }
    var terminalStreamText by remember { mutableStateOf("ready>") }

    var lastWarnTime by remember { mutableStateOf(0L) }

    fun canSendInput(): Boolean {
        if (connectionState is dev.aleian.pockethid.model.ConnectionState.Connected || transport?.isConnected == true) {
            return true
        }
        if (connectionState is dev.aleian.pockethid.model.ConnectionState.Connecting) {
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastWarnTime > 3000) {
            lastWarnTime = now
            android.widget.Toast.makeText(context, "Connect to a host first.", android.widget.Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun triggerHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun getActiveModifiers(): Byte {
        var mods: Byte = 0
        if (ctrlState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
        if (shiftState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
        if (altState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
        if (guiState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
        return mods
    }

    fun consumeStickyModifiers() {
        if (ctrlState == ModifierState.STICKY) ctrlState = ModifierState.OFF
        if (shiftState == ModifierState.STICKY) shiftState = ModifierState.OFF
        if (altState == ModifierState.STICKY) altState = ModifierState.OFF
        if (guiState == ModifierState.STICKY) guiState = ModifierState.OFF
    }

    fun sendKey(keyCode: Byte, extraModifier: Byte = 0) {
        if (!canSendInput()) return
        val totalMods = (getActiveModifiers().toInt() or extraModifier.toInt()).toByte()
        triggerHaptic()
        scope.launch {
            if (transport != null) {
                ActionDispatcher.execute(ActionExecutionPlan.KeyStroke(keyCode, totalMods), transport)
            }
            consumeStickyModifiers()
        }
    }

    fun sendConsumerKey(usageCode: Int, label: String = "") {
        if (!canSendInput()) return
        triggerHaptic()
        lastSentCharInfo = label
        scope.launch {
            if (transport != null) {
                ActionDispatcher.execute(ActionExecutionPlan.ConsumerKey(usageCode), transport)
            }
        }
    }

    fun cycleModifier(currentState: ModifierState): ModifierState {
        triggerHaptic()
        return when (currentState) {
            ModifierState.OFF -> ModifierState.STICKY
            ModifierState.STICKY -> ModifierState.LOCKED
            ModifierState.LOCKED -> ModifierState.OFF
        }
    }

    fun focusHiddenInput() {
        if (!canSendInput()) return
        imeInputView?.requestKeyboard()
    }

    val specialKeys = remember {
        listOf(
            SpecialKey("Esc", HidConstants.KEY_ESC),
            SpecialKey("Tab", HidConstants.KEY_TAB),
            SpecialKey("Enter", HidConstants.KEY_ENTER),
            SpecialKey("Bksp", HidConstants.KEY_BACKSPACE),
            SpecialKey("Del", HidConstants.KEY_DELETE),
            SpecialKey("Space", HidConstants.KEY_SPACE),
            SpecialKey("←", HidConstants.KEY_LEFT),
            SpecialKey("↑", HidConstants.KEY_UP),
            SpecialKey("↓", HidConstants.KEY_DOWN),
            SpecialKey("→", HidConstants.KEY_RIGHT),
            SpecialKey("Home", HidConstants.KEY_HOME),
            SpecialKey("End", HidConstants.KEY_END),
            SpecialKey("PgUp", HidConstants.KEY_PAGEUP),
            SpecialKey("PgDn", HidConstants.KEY_PAGEDOWN),
            SpecialKey("Ins", HidConstants.KEY_INSERT),
            SpecialKey("PrtSc", HidConstants.KEY_PRINTSCREEN),
            SpecialKey("F1", HidConstants.KEY_F1),
            SpecialKey("F2", HidConstants.KEY_F2),
            SpecialKey("F3", HidConstants.KEY_F3),
            SpecialKey("F4", HidConstants.KEY_F4),
            SpecialKey("F5", HidConstants.KEY_F5),
            SpecialKey("F6", HidConstants.KEY_F6),
            SpecialKey("F7", HidConstants.KEY_F7),
            SpecialKey("F8", HidConstants.KEY_F8),
            SpecialKey("F9", HidConstants.KEY_F9),
            SpecialKey("F10", HidConstants.KEY_F10),
            SpecialKey("F11", HidConstants.KEY_F11),
            SpecialKey("F12", HidConstants.KEY_F12)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sticky Modifiers Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModifierButton("Ctrl", ctrlState, Modifier.weight(1f)) { ctrlState = cycleModifier(ctrlState) }
            ModifierButton("Shift", shiftState, Modifier.weight(1f)) { shiftState = cycleModifier(shiftState) }
            ModifierButton("Alt", altState, Modifier.weight(1f)) { altState = cycleModifier(altState) }
            ModifierButton("Win/Cmd", guiState, Modifier.weight(1.2f)) { guiState = cycleModifier(guiState) }
        }

        // SubMode Layer Selector (TYPE | SHORTCUTS | MEDIA | SYSTEM | F-KEYS | NUMPAD)
        SubModeSelector(
            selectedSubMode = selectedLayer,
            onSelectSubMode = { selectedLayer = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Dynamic Active Layer View
        when (selectedLayer) {
            0 -> {
                // LAYER 0: TYPE (Dedicated Number Row + Special Keys + Quick Shortcuts + Keystroke Monitor + Tap to Type)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Permanent Dedicated Number Row (` 1 2 3 4 5 6 7 8 9 0 - = BKSP)
                    DedicatedNumberRow(
                        isShiftActive = shiftState != ModifierState.OFF,
                        onSendKey = { keyCode, extraMod, label ->
                            lastSentCharInfo = "Sent: $label"
                            terminalStreamText = label
                            sendKey(keyCode, extraMod)
                        },
                        onBackspaceRepeat = {
                            lastSentCharInfo = "Sent: ⌫"
                            terminalStreamText = "BKSP"
                            sendKey(HidConstants.KEY_BACKSPACE)
                        },
                        hapticsEnabled = settings.keyboardHaptics,
                        fontSize = 11.sp,
                        keyHeight = 34.dp
                    )

                    // Special Keys Row (Horizontal Scroll)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(specialKeys) { key ->
                            SpecialKeyButton(label = key.label) {
                                lastSentCharInfo = "Sent key: ${key.label}"
                                sendKey(key.keyCode, key.baseModifier)
                            }
                        }
                    }

                    // Quick Shortcuts Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickShortcutButton("Ctrl+C", Modifier.weight(1f)) {
                            sendKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL)
                            lastSentCharInfo = "Sent Ctrl+C"
                            terminalStreamText = "^C (SIGINT)"
                        }
                        QuickShortcutButton("Ctrl+V", Modifier.weight(1f)) {
                            sendKey(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL)
                            lastSentCharInfo = "Sent Ctrl+V"
                            terminalStreamText = "^V (PASTE)"
                        }
                        QuickShortcutButton("Ctrl+Z", Modifier.weight(1f)) {
                            sendKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL)
                            lastSentCharInfo = "Sent Ctrl+Z"
                            terminalStreamText = "^Z (TSTP)"
                        }
                        QuickShortcutButton("Alt+Tab", Modifier.weight(1f)) {
                            sendKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT)
                            lastSentCharInfo = "Sent Alt+Tab"
                            terminalStreamText = "Alt+Tab (SWITCH)"
                        }
                    }

                    // Live Keystroke Terminal & Input Monitor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(AccentGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "HID STREAM INJECTION",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "rate: ${settings.pasteDelayMs}ms",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PrimaryBlue
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "host> ",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                    Text(
                                        text = terminalStreamText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable { terminalStreamText = "ready>" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✕", fontSize = 10.sp, color = TextMuted)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimaryBlue.copy(alpha = 0.2f))
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrEmpty()) {
                                                    terminalStreamText = clip.take(24) + "..."
                                                    scope.launch {
                                                        for (char in clip) {
                                                            val stroke = KeyMapper.mapCharToStroke(char)
                                                            if (stroke != null) {
                                                                transport?.sendKeyClick(stroke.keyCode, stroke.modifiers)
                                                                kotlinx.coroutines.delay(settings.pasteDelayMs)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "PASTE",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // IME Input Area (High-Reliability PocketImeInputView surface)
                    val isConnected = connectionState is dev.aleian.pockethid.model.ConnectionState.Connected || transport?.isConnected == true
                    val isConnecting = connectionState is dev.aleian.pockethid.model.ConnectionState.Connecting
                    val hostName = if (connectionState is dev.aleian.pockethid.model.ConnectionState.Connected) {
                        connectionState.device.name ?: connectionState.device.address
                    } else "Host"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(
                                1.dp,
                                if (isConnected) PrimaryBlue.copy(alpha = 0.5f) else DarkBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { focusHiddenInput() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Status Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isConnected -> AccentGreen
                                                isConnecting -> PrimaryBlue
                                                else -> Color(0xFFEF4444)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isConnected -> "HID Connected • Typing to $hostName"
                                        isConnecting -> "Connecting to host..."
                                        else -> "Not Connected"
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isConnected -> AccentGreen
                                        isConnecting -> PrimaryBlue
                                        else -> Color(0xFFEF4444)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = if (isConnected) PrimaryBlue else TextMuted,
                                modifier = Modifier.size(38.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isConnected) "PHONE KEYBOARD READY" else "KEYBOARD OFFLINE",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isConnected) {
                                    "Tap to open phone keyboard → Characters stream to PC"
                                } else {
                                    "Connect a host to start typing"
                                },
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = lastSentCharInfo,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = PrimaryBlue
                            )

                            // Dedicated IME Input View for reliable Android InputConnection
                            AndroidView(
                                factory = { ctx ->
                                    PocketImeInputView(ctx).apply {
                                        onCommitText = { text ->
                                            if (canSendInput() && transport != null) {
                                                triggerHaptic()
                                                scope.launch {
                                                    val strokes = TextInputResolver.resolveText(text)
                                                    for (stroke in strokes) {
                                                        ActionDispatcher.execute(
                                                            ActionExecutionPlan.KeyStroke(stroke.keyCode, stroke.modifiers),
                                                            transport
                                                        )
                                                        ImeDiagnosticsHub.record(
                                                            "commitText",
                                                            text,
                                                            "KEY 0x${stroke.keyCode.toString(16)} (mod=0x${stroke.modifiers.toString(16)})",
                                                            "Sent"
                                                        )
                                                        lastSentCharInfo = "Typed: '$text'"
                                                        terminalStreamText = text
                                                        if (settings.pasteDelayMs > 0) {
                                                            kotlinx.coroutines.delay(settings.pasteDelayMs)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        onDeleteBack = { count ->
                                            if (canSendInput() && transport != null) {
                                                triggerHaptic()
                                                scope.launch {
                                                    repeat(count) {
                                                        ActionDispatcher.execute(
                                                            ActionExecutionPlan.KeyStroke(HidConstants.KEY_BACKSPACE),
                                                            transport
                                                        )
                                                        lastSentCharInfo = "Sent: ⌫"
                                                        terminalStreamText = "BKSP"
                                                        if (it < count - 1) kotlinx.coroutines.delay(10)
                                                    }
                                                }
                                            }
                                        }

                                        onDeleteForward = { count ->
                                            if (canSendInput() && transport != null) {
                                                triggerHaptic()
                                                scope.launch {
                                                    repeat(count) {
                                                        ActionDispatcher.execute(
                                                            ActionExecutionPlan.KeyStroke(HidConstants.KEY_DELETE),
                                                            transport
                                                        )
                                                        lastSentCharInfo = "Sent: Del"
                                                        terminalStreamText = "DEL"
                                                        if (it < count - 1) kotlinx.coroutines.delay(10)
                                                    }
                                                }
                                            }
                                        }

                                        onEditorAction = { actionCode ->
                                            if (canSendInput() && transport != null) {
                                                triggerHaptic()
                                                scope.launch {
                                                    ActionDispatcher.execute(
                                                        ActionExecutionPlan.KeyStroke(HidConstants.KEY_ENTER),
                                                        transport
                                                    )
                                                    lastSentCharInfo = "Sent: Enter"
                                                    terminalStreamText = "ENTER"
                                                }
                                            }
                                        }

                                        onKeyEvent = { event ->
                                            if (canSendInput() && transport != null) {
                                                val stroke = TextInputResolver.resolveKeyEvent(event)
                                                if (stroke != null) {
                                                    triggerHaptic()
                                                    scope.launch {
                                                        ActionDispatcher.execute(
                                                            ActionExecutionPlan.KeyStroke(stroke.keyCode, stroke.modifiers),
                                                            transport
                                                        )
                                                        lastSentCharInfo = "Key: ${event.keyCode}"
                                                        terminalStreamText = "KEY_${event.keyCode}"
                                                    }
                                                }
                                            }
                                        }

                                        imeInputView = this
                                    }
                                },
                                modifier = Modifier.size(1.dp)
                            )
                        }
                    }
                }
            }

            1 -> {
                // LAYER 1: SHORTCUTS (Matrix Grid of High-Frequency Workflow Actions)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Clipboard
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "COPY (^C)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "PASTE (^V)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "CUT (^X)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_X, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "ALL (^A)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_A, HidConstants.MOD_LEFT_CTRL)
                        }
                    }

                    // Row 2: History & Actions
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "UNDO (^Z)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                            sendKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "REDO (^Y)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                            sendKey(HidConstants.KEY_Y, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "SAVE (^S)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFF10B981)) {
                            sendKey(HidConstants.KEY_S, HidConstants.MOD_LEFT_CTRL)
                        }
                        DeckKey(text = "FIND (^F)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_F, HidConstants.MOD_LEFT_CTRL)
                        }
                    }

                    // Row 3: Windows Management
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "ALT+TAB", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT)
                        }
                        DeckKey(text = "DESKTOP (Win+D)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "TASK MGR", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(
                                HidConstants.KEY_ESC,
                                (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "LOCK (Win+L)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            sendKey(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI)
                        }
                    }

                    // Row 4: Instant Navigation Keys
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "SNIP TOOL (Win+⇧+S)", modifier = Modifier.weight(1.5f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                            sendKey(
                                HidConstants.KEY_S,
                                (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "ENTER", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_ENTER)
                        }
                        DeckKey(text = "ESC", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_ESC)
                        }
                        DeckKey(text = "TAB", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_TAB)
                        }
                    }
                }
            }

            2 -> {
                // LAYER 2: MEDIA (Media Remote Deck)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Playback Controls
                    Row(modifier = Modifier.fillMaxWidth().weight(1.2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "⏮ PREV", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendConsumerKey(HidConstants.CONSUMER_SCAN_PREV, "PrevTrack")
                        }
                        DeckKey(
                            text = "⏯ PLAY / PAUSE",
                            modifier = Modifier.weight(2f).fillMaxHeight(),
                            containerColor = SurfaceCard,
                            textColor = Color(0xFF10B981),
                            fontSize = 15.sp
                        ) {
                            sendConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause")
                        }
                        DeckKey(text = "NEXT ⏭", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendConsumerKey(HidConstants.CONSUMER_SCAN_NEXT, "NextTrack")
                        }
                    }

                    // Row 2: Volume & Mute
                    Row(modifier = Modifier.fillMaxWidth().weight(1.2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "VOL −", modifier = Modifier.weight(1.2f).fillMaxHeight(), fontSize = 14.sp) {
                            sendConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN, "Vol-")
                        }
                        DeckKey(
                            text = "🔇 MUTE AUDIO",
                            modifier = Modifier.weight(1.6f).fillMaxHeight(),
                            containerColor = SurfaceCard,
                            textColor = Color(0xFFF59E0B),
                            fontSize = 14.sp
                        ) {
                            sendConsumerKey(HidConstants.CONSUMER_MUTE, "Mute")
                        }
                        DeckKey(text = "VOL ＋", modifier = Modifier.weight(1.2f).fillMaxHeight(), fontSize = 14.sp) {
                            sendConsumerKey(HidConstants.CONSUMER_VOLUME_UP, "Vol+")
                        }
                    }

                    // Row 3: Video Seek & Fullscreen
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "SEEK −5s (◀)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_LEFT)
                        }
                        DeckKey(text = "SPACE (Pause)", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_SPACE)
                        }
                        DeckKey(text = "FULLSCREEN (F)", modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_F)
                        }
                        DeckKey(text = "SEEK +5s (▶)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_RIGHT)
                        }
                    }
                }
            }

            3 -> {
                // LAYER 3: SYSTEM (Windows & Virtual Desktops Controller)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Virtual Desktops
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "◀ DESKTOP", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(
                                HidConstants.KEY_LEFT,
                                (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "＋ NEW", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFF10B981)) {
                            sendKey(
                                HidConstants.KEY_D,
                                (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "✕ CLOSE", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            sendKey(
                                HidConstants.KEY_F4,
                                (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "DESKTOP ▶", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(
                                HidConstants.KEY_RIGHT,
                                (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            )
                        }
                    }

                    // Row 2: Windows System Shell
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "TASK VIEW (Win+Tab)", modifier = Modifier.weight(1.3f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "SNIP TOOL (Win+⇧+S)", modifier = Modifier.weight(1.3f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                            sendKey(
                                HidConstants.KEY_S,
                                (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                            )
                        }
                        DeckKey(text = "EXPLORER (Win+E)", modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_E, HidConstants.MOD_LEFT_GUI)
                        }
                    }

                    // Row 3: Window Management & Run
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "SNAP ◀", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_LEFT, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "MAXIMIZE ▲", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            sendKey(HidConstants.KEY_UP, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "SNAP ▶", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_RIGHT, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "RUN (Win+R)", modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                            sendKey(HidConstants.KEY_R, HidConstants.MOD_LEFT_GUI)
                        }
                        DeckKey(text = "CLOSE (Alt+F4)", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            sendKey(HidConstants.KEY_F4, HidConstants.MOD_LEFT_ALT)
                        }
                    }
                }
            }

            4 -> {
                // LAYER 4: F-KEYS (F1 - F12 + Extended Keys)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("F1" to HidConstants.KEY_F1, "F2" to HidConstants.KEY_F2, "F3" to HidConstants.KEY_F3, "F4" to HidConstants.KEY_F4, "F5" to HidConstants.KEY_F5, "F6" to HidConstants.KEY_F6).forEach { (lbl, code) ->
                            DeckKey(text = lbl, modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(code) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("F7" to HidConstants.KEY_F7, "F8" to HidConstants.KEY_F8, "F9" to HidConstants.KEY_F9, "F10" to HidConstants.KEY_F10, "F11" to HidConstants.KEY_F11, "F12" to HidConstants.KEY_F12).forEach { (lbl, code) ->
                            DeckKey(text = lbl, modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(code) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeckKey(text = "PRTSC", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) { sendKey(HidConstants.KEY_PRINTSCREEN) }
                        DeckKey(text = "HOME", modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(HidConstants.KEY_HOME) }
                        DeckKey(text = "END", modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(HidConstants.KEY_END) }
                        DeckKey(text = "PGUP", modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(HidConstants.KEY_PAGEUP) }
                        DeckKey(text = "PGDN", modifier = Modifier.weight(1f).fillMaxHeight()) { sendKey(HidConstants.KEY_PAGEDOWN) }
                    }
                }
            }

            5 -> {
                // LAYER 5: NUMPAD (Full Numeric Keypad)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "7", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP7) }
                        DeckKey(text = "8", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP8) }
                        DeckKey(text = "9", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP9) }
                        DeckKey(text = "/", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp, textColor = PrimaryBlue) { sendKey(HidConstants.KEY_KPSLASH) }
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "4", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP4) }
                        DeckKey(text = "5", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP5) }
                        DeckKey(text = "6", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP6) }
                        DeckKey(text = "*", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp, textColor = PrimaryBlue) { sendKey(HidConstants.KEY_KPASTERISK) }
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "1", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP1) }
                        DeckKey(text = "2", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP2) }
                        DeckKey(text = "3", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP3) }
                        DeckKey(text = "-", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp, textColor = PrimaryBlue) { sendKey(HidConstants.KEY_KPMINUS) }
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckKey(text = "0", modifier = Modifier.weight(2f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KP0) }
                        DeckKey(text = ".", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp) { sendKey(HidConstants.KEY_KPDOT) }
                        DeckKey(text = "+", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 16.sp, textColor = PrimaryBlue) { sendKey(HidConstants.KEY_KPPLUS) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierButton(
    label: String,
    state: ModifierState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when (state) {
        ModifierState.OFF -> Pair(DarkSurfaceVariant, TextSecondary)
        ModifierState.STICKY -> Pair(PrimaryBlue, TextPrimary)
        ModifierState.LOCKED -> Pair(AccentGreen, DarkBg)
    }

    val stateIndicator = when (state) {
        ModifierState.OFF -> ""
        ModifierState.STICKY -> " •"
        ModifierState.LOCKED -> " 🔒"
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label$stateIndicator",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun QuickShortcutButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
