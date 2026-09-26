package dev.aleian.pockethid.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.mapping.GestureInterpreter
import dev.aleian.pockethid.mapping.KeyMapper
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.DeckKey
import dev.aleian.pockethid.ui.components.DeckModeSwitcher
import dev.aleian.pockethid.ui.components.ModifierToggleState
import dev.aleian.pockethid.ui.components.NaturalThumbArrowPad
import dev.aleian.pockethid.ui.components.SubModeSelector
import dev.aleian.pockethid.ui.components.ThumbModifierKey
import dev.aleian.pockethid.ui.components.TopCommandBar
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.OnPrimaryContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.PrimaryContainer
import dev.aleian.pockethid.ui.theme.SurfaceBase
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainer
import dev.aleian.pockethid.ui.theme.SurfaceContainerLow
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LandscapeDeckScreen(
    transport: InputTransport?,
    pairedDevices: List<BluetoothDevice> = emptyList(),
    onConnectToDevice: (BluetoothDevice) -> Unit = {},
    onMakeDiscoverable: () -> Unit = {},
    onExitLandscape: (() -> Unit)? = null,
    onToggleOrientation: () -> Unit = onExitLandscape ?: {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()
    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) })

    // Mode States
    var selectedTopMode by remember { mutableIntStateOf(0) } // 0: Keyboard, 1: Mouse, 2: Presenter
    var selectedKeyboardSubMode by remember { mutableIntStateOf(0) } // 0: Type, 1: Shortcuts, 2: F-Keys, 3: Numpad

    // Modifier Key States
    var ctrlState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var altState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var superState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var shiftState by remember { mutableStateOf(ModifierToggleState.OFF) }

    // Telemetry & Diagnostics info
    var lastScancode by remember { mutableStateOf("0x00") }
    var lastInputLabel by remember { mutableStateOf("IDLE") }

    // Modals
    var showDiagnostics by remember { mutableStateOf(false) }
    var showPairingSheet by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Drag lock for Mouse mode
    var isDragLocked by remember { mutableStateOf(false) }

    val gestureInterpreter = remember(settings) {
        GestureInterpreter(
            accelerationFactor = settings.accelerationFactor,
            naturalScroll = settings.naturalScroll
        )
    }

    fun triggerHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun getActiveModifiers(): Byte {
        var mods: Byte = 0
        if (ctrlState != ModifierToggleState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
        if (shiftState != ModifierToggleState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
        if (altState != ModifierToggleState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
        if (superState != ModifierToggleState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
        return mods
    }

    fun consumeStickyModifiers() {
        if (ctrlState == ModifierToggleState.STICKY) ctrlState = ModifierToggleState.OFF
        if (shiftState == ModifierToggleState.STICKY) shiftState = ModifierToggleState.OFF
        if (altState == ModifierToggleState.STICKY) altState = ModifierToggleState.OFF
        if (superState == ModifierToggleState.STICKY) superState = ModifierToggleState.OFF
    }

    fun sendRawKey(keyCode: Byte, extraModifier: Byte = 0, label: String = "") {
        val totalMods = (getActiveModifiers().toInt() or extraModifier.toInt()).toByte()
        triggerHaptic()
        lastScancode = "0x" + Integer.toHexString(keyCode.toInt() and 0xFF).uppercase()
        lastInputLabel = label.ifEmpty { lastScancode }
        scope.launch {
            transport?.sendKeyClick(keyCode, totalMods)
            consumeStickyModifiers()
        }
    }

    fun cycleModifier(current: ModifierToggleState): ModifierToggleState {
        triggerHaptic()
        return when (current) {
            ModifierToggleState.OFF -> ModifierToggleState.STICKY
            ModifierToggleState.STICKY -> ModifierToggleState.LOCKED
            ModifierToggleState.LOCKED -> ModifierToggleState.OFF
        }
    }

    fun sendStringSafe(text: String) {
        scope.launch {
            lastInputLabel = "PASTE"
            for (char in text) {
                val stroke = KeyMapper.mapCharToStroke(char)
                if (stroke != null) {
                    transport?.sendKeyClick(stroke.keyCode, stroke.modifiers)
                    delay(settings.pasteDelayMs)
                }
            }
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            transport = transport,
            onBack = { showSettingsScreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBase)
            .systemBarsPadding()
            .displayCutoutPadding()
    ) {
        // TOP COMMAND BAR (Global Shell)
        TopCommandBar(
            connectionState = connState,
            hapticEnabled = settings.keyboardHaptics,
            onRotateClick = onToggleOrientation,
            onHostClick = { showPairingSheet = true },
            onToggleHaptic = {
                SettingsRepository.updateSettings(
                    settings.copy(keyboardHaptics = !settings.keyboardHaptics)
                )
            },
            onDiagnosticsClick = { showDiagnostics = true },
            onSettingsClick = { showSettingsScreen = true }
        )

        // GLOBAL 3-MODE SWITCHER (KEYBOARD | MOUSE | PRESENTER)
        DeckModeSwitcher(
            selectedMode = selectedTopMode,
            onSelectMode = { selectedTopMode = it }
        )

        // ACTIVE DECK CONTENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceContainerLowest)
                .padding(3.dp)
        ) {
            when (selectedTopMode) {
                0 -> {
                    // KEYBOARD COMMAND DECK (Thumb-first 3-Zone Architecture)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // ZONE 1: LEFT THUMB CLUSTER (Modifiers & Primary Execution)
                        Column(
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "MODIFIERS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )

                            DeckKey(
                                text = "ESC",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textColor = Color(0xFFEF4444),
                                fontSize = 11.sp
                            ) {
                                sendRawKey(HidConstants.KEY_ESC, 0, "ESC")
                            }

                            DeckKey(
                                text = "TAB",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                fontSize = 10.sp
                            ) {
                                sendRawKey(HidConstants.KEY_TAB, 0, "TAB")
                            }

                            ThumbModifierKey(
                                label = "CTRL",
                                state = ctrlState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                ctrlState = cycleModifier(ctrlState)
                            }

                            ThumbModifierKey(
                                label = "ALT",
                                state = altState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                altState = cycleModifier(altState)
                            }

                            ThumbModifierKey(
                                label = "SUPER",
                                state = superState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                superState = cycleModifier(superState)
                            }

                            ThumbModifierKey(
                                label = "SHIFT",
                                state = shiftState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                shiftState = cycleModifier(shiftState)
                            }

                            // Safe Paste Quick Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(5.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrEmpty()) {
                                            sendStringSafe(clip)
                                        } else {
                                            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PASTE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        // ZONE 2: CENTER ADAPTIVE KEYBED (Type / Shortcuts / F-Keys / Numpad)
                        Column(
                            modifier = Modifier
                                .weight(6.4f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Sub-mode Header Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubModeSelector(
                                    selectedSubMode = selectedKeyboardSubMode,
                                    onSelectSubMode = { selectedKeyboardSubMode = it }
                                )
                                Text(
                                    text = "TOUCH OPTIMIZED",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }

                            // Dynamic Center Content based on Sub-mode
                            when (selectedKeyboardSubMode) {
                                0 -> {
                                    // TYPE MODE: Generous Touch QWERTY (No cluttered F-keys or Numpad)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // QWERTY ROW 1
                                        val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                                        val codes1 = listOf(
                                            HidConstants.KEY_Q, HidConstants.KEY_W, HidConstants.KEY_E,
                                            HidConstants.KEY_R, HidConstants.KEY_T, HidConstants.KEY_Y,
                                            HidConstants.KEY_U, HidConstants.KEY_I, HidConstants.KEY_O, HidConstants.KEY_P
                                        )
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            row1.forEachIndexed { i, l ->
                                                DeckKey(text = l, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 13.sp) {
                                                    sendRawKey(codes1[i], 0, l)
                                                }
                                            }
                                        }

                                        // QWERTY ROW 2
                                        val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                                        val codes2 = listOf(
                                            HidConstants.KEY_A, HidConstants.KEY_S, HidConstants.KEY_D,
                                            HidConstants.KEY_F, HidConstants.KEY_G, HidConstants.KEY_H,
                                            HidConstants.KEY_J, HidConstants.KEY_K, HidConstants.KEY_L
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            row2.forEachIndexed { i, l ->
                                                DeckKey(text = l, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 13.sp) {
                                                    sendRawKey(codes2[i], 0, l)
                                                }
                                            }
                                        }

                                        // QWERTY ROW 3
                                        val row3 = listOf("Z", "X", "C", "V", "B", "N", "M", ",", ".")
                                        val codes3 = listOf(
                                            HidConstants.KEY_Z, HidConstants.KEY_X, HidConstants.KEY_C,
                                            HidConstants.KEY_V, HidConstants.KEY_B, HidConstants.KEY_N,
                                            HidConstants.KEY_M, HidConstants.KEY_COMMA, HidConstants.KEY_DOT
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            row3.forEachIndexed { i, l ->
                                                DeckKey(text = l, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 13.sp) {
                                                    sendRawKey(codes3[i], 0, l)
                                                }
                                            }
                                        }

                                        // QWERTY ROW 4: SPACEBAR DECK
                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            DeckKey(text = "`", modifier = Modifier.weight(1f).fillMaxHeight()) { sendRawKey(HidConstants.KEY_GRAVE, 0, "`") }
                                            DeckKey(text = "/", modifier = Modifier.weight(1f).fillMaxHeight()) { sendRawKey(HidConstants.KEY_SLASH, 0, "/") }
                                            DeckKey(
                                                text = "SPACE",
                                                modifier = Modifier.weight(5f).fillMaxHeight(),
                                                containerColor = SurfaceCard,
                                                textColor = PrimaryBlue
                                            ) {
                                                sendRawKey(HidConstants.KEY_SPACE, 0, "SPACE")
                                            }
                                            DeckKey(text = "-", modifier = Modifier.weight(1f).fillMaxHeight()) { sendRawKey(HidConstants.KEY_MINUS, 0, "-") }
                                            DeckKey(text = "=", modifier = Modifier.weight(1f).fillMaxHeight()) { sendRawKey(HidConstants.KEY_EQUAL, 0, "=") }
                                        }
                                    }
                                }

                                1 -> {
                                    // SHORTCUTS MODE: Large Instant Workflow Macro Grid
                                    Column(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Row 1: Clipboard
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            DeckKey(text = "COPY  (^C)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                                                sendRawKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL, "Copy")
                                            }
                                            DeckKey(text = "PASTE (^V)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                                                sendRawKey(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL, "Paste")
                                            }
                                            DeckKey(text = "CUT   (^X)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_X, HidConstants.MOD_LEFT_CTRL, "Cut")
                                            }
                                            DeckKey(text = "SELECT ALL (^A)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_A, HidConstants.MOD_LEFT_CTRL, "SelectAll")
                                            }
                                        }

                                        // Row 2: History & Actions
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            DeckKey(text = "UNDO  (^Z)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                                                sendRawKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL, "Undo")
                                            }
                                            DeckKey(text = "REDO  (^Y)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                                                sendRawKey(HidConstants.KEY_Y, HidConstants.MOD_LEFT_CTRL, "Redo")
                                            }
                                            DeckKey(text = "SAVE  (^S)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFF10B981)) {
                                                sendRawKey(HidConstants.KEY_S, HidConstants.MOD_LEFT_CTRL, "Save")
                                            }
                                            DeckKey(text = "FIND  (^F)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_F, HidConstants.MOD_LEFT_CTRL, "Find")
                                            }
                                        }

                                        // Row 3: Windows & System Management
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            DeckKey(text = "ALT + TAB", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                                                sendRawKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT, "Alt+Tab")
                                            }
                                            DeckKey(text = "SHOW DESKTOP (Win+D)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI, "Win+D")
                                            }
                                            DeckKey(text = "TASK MGR (^⇧Esc)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_ESC, (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte(), "TaskMgr")
                                            }
                                            DeckKey(text = "LOCK (Win+L)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                                                sendRawKey(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI, "Lock")
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    // F-KEYS MODE: Clean 2-Row Layout + System Function Keys
                                    Column(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // F1 - F6
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            val f1to6 = listOf(
                                                "F1" to HidConstants.KEY_F1, "F2" to HidConstants.KEY_F2,
                                                "F3" to HidConstants.KEY_F3, "F4" to HidConstants.KEY_F4,
                                                "F5" to HidConstants.KEY_F5, "F6" to HidConstants.KEY_F6
                                            )
                                            f1to6.forEach { (lbl, code) ->
                                                DeckKey(text = lbl, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 13.sp) {
                                                    sendRawKey(code, 0, lbl)
                                                }
                                            }
                                        }

                                        // F7 - F12
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            val f7to12 = listOf(
                                                "F7" to HidConstants.KEY_F7, "F8" to HidConstants.KEY_F8,
                                                "F9" to HidConstants.KEY_F9, "F10" to HidConstants.KEY_F10,
                                                "F11" to HidConstants.KEY_F11, "F12" to HidConstants.KEY_F12
                                            )
                                            f7to12.forEach { (lbl, code) ->
                                                DeckKey(text = lbl, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 13.sp) {
                                                    sendRawKey(code, 0, lbl)
                                                }
                                            }
                                        }

                                        // Extended System Keys
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            DeckKey(text = "PRTSC", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                                                sendRawKey(HidConstants.KEY_PRINTSCREEN, 0, "PrtSc")
                                            }
                                            DeckKey(text = "HOME", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_HOME, 0, "Home")
                                            }
                                            DeckKey(text = "END", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_END, 0, "End")
                                            }
                                            DeckKey(text = "PGUP", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_PAGEUP, 0, "PgUp")
                                            }
                                            DeckKey(text = "PGDN", modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                sendRawKey(HidConstants.KEY_PAGEDOWN, 0, "PgDn")
                                            }
                                        }
                                    }
                                }

                                3 -> {
                                    // NUMPAD MODE: Generous 4x4 Numeric Touchpad
                                    Column(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            DeckKey(text = "7", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP7, 0, "7") }
                                            DeckKey(text = "8", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP8, 0, "8") }
                                            DeckKey(text = "9", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP9, 0, "9") }
                                            DeckKey(text = "/", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp, textColor = PrimaryBlue) { sendRawKey(HidConstants.KEY_KPSLASH, 0, "/") }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            DeckKey(text = "4", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP4, 0, "4") }
                                            DeckKey(text = "5", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP5, 0, "5") }
                                            DeckKey(text = "6", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP6, 0, "6") }
                                            DeckKey(text = "*", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp, textColor = PrimaryBlue) { sendRawKey(HidConstants.KEY_KPASTERISK, 0, "*") }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            DeckKey(text = "1", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP1, 0, "1") }
                                            DeckKey(text = "2", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP2, 0, "2") }
                                            DeckKey(text = "3", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP3, 0, "3") }
                                            DeckKey(text = "-", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp, textColor = PrimaryBlue) { sendRawKey(HidConstants.KEY_KPMINUS, 0, "-") }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            DeckKey(text = "0", modifier = Modifier.weight(2f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KP0, 0, "0") }
                                            DeckKey(text = ".", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp) { sendRawKey(HidConstants.KEY_KPDOT, 0, ".") }
                                            DeckKey(text = "+", modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 14.sp, textColor = PrimaryBlue) { sendRawKey(HidConstants.KEY_KPPLUS, 0, "+") }
                                        }
                                    }
                                }
                            }
                        }

                        // ZONE 3: RIGHT THUMB CLUSTER (Execution & Natural Directional Navigation)
                        Column(
                            modifier = Modifier
                                .weight(2.0f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "ACTIONS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )

                            // Backspace
                            DeckKey(
                                text = "BKSP",
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                textColor = Color(0xFFEF4444),
                                fontSize = 11.sp
                            ) {
                                sendRawKey(HidConstants.KEY_BACKSPACE, 0, "BKSP")
                            }

                            // Enter (Prominent Action Key)
                            DeckKey(
                                text = "ENTER",
                                modifier = Modifier.fillMaxWidth().weight(1.3f),
                                containerColor = PrimaryBlue.copy(alpha = 0.2f),
                                textColor = PrimaryBlue,
                                fontSize = 12.sp
                            ) {
                                sendRawKey(HidConstants.KEY_ENTER, 0, "ENTER")
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Natural 4-Way Thumb Arrow Cluster
                            NaturalThumbArrowPad(
                                modifier = Modifier.fillMaxWidth().weight(1.8f),
                                hapticFeedback = settings.keyboardHaptics,
                                onUp = { sendRawKey(HidConstants.KEY_UP, 0, "UP") },
                                onDown = { sendRawKey(HidConstants.KEY_DOWN, 0, "DOWN") },
                                onLeft = { sendRawKey(HidConstants.KEY_LEFT, 0, "LEFT") },
                                onRight = { sendRawKey(HidConstants.KEY_RIGHT, 0, "RIGHT") }
                            )
                        }
                    }
                }

                1 -> {
                    // MOUSE COMMAND DECK (Dominant Touchpad + Thumb Bottom Controls)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Dominant Central Trackpad
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val action = gestureInterpreter.processOneFingerMove(dragAmount.x, dragAmount.y, isDragLocked)
                                        transport?.sendMouseMove(action.dx, action.dy, action.buttons, 0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PRECISION TRACKPAD",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        // Bottom 3 Action Buttons + Drag Lock
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Drag Lock Toggle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDragLocked) PrimaryBlue.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .border(1.dp, if (isDragLocked) PrimaryBlue else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        isDragLocked = !isDragLocked
                                        triggerHaptic()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isDragLocked) PrimaryBlue else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDragLocked) PrimaryBlue else TextSecondary)
                                }
                            }

                            // Left Click Button
                            DeckKey(
                                text = "LEFT",
                                modifier = Modifier.weight(2f).fillMaxHeight(),
                                containerColor = SurfaceCard,
                                textColor = TextPrimary,
                                fontSize = 12.sp
                            ) {
                                scope.launch {
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_LEFT, 0)
                                    delay(16)
                                    if (!isDragLocked) transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                                }
                            }

                            // Middle Click Button
                            DeckKey(
                                text = "MIDDLE",
                                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                                containerColor = SurfaceCard,
                                textColor = TextSecondary,
                                fontSize = 11.sp
                            ) {
                                scope.launch {
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_MIDDLE, 0)
                                    delay(16)
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                                }
                            }

                            // Right Click Button
                            DeckKey(
                                text = "RIGHT",
                                modifier = Modifier.weight(2f).fillMaxHeight(),
                                containerColor = SurfaceCard,
                                textColor = TextPrimary,
                                fontSize = 12.sp
                            ) {
                                scope.launch {
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_RIGHT, 0)
                                    delay(16)
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // PRESENTER COMMAND DECK
                    PresenterScreen(
                        transport = transport,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Diagnostics / Developer HUD Modal
    if (showDiagnostics) {
        DiagnosticsSheet(
            connectionState = connState,
            lastScancode = lastScancode,
            lastInputLabel = lastInputLabel,
            onDismiss = { showDiagnostics = false }
        )
    }

    // Host Manager / Pairing Modal
    if (showPairingSheet) {
        PairingSheet(
            transport = transport,
            onDismiss = { showPairingSheet = false },
            pairedDevices = pairedDevices,
            onSelectDevice = { device ->
                showPairingSheet = false
                onConnectToDevice(device)
            },
            onMakeDiscoverable = onMakeDiscoverable,
            onOpenSettings = {
                showPairingSheet = false
                showSettingsScreen = true
            }
        )
    }
}
