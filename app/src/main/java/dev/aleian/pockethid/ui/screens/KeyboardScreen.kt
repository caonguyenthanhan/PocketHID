package dev.aleian.pockethid.ui.screens

import android.content.Context
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by dev.aleian.pockethid.model.SettingsRepository.settings.collectAsState()

    var ctrlState by remember { mutableStateOf(ModifierState.OFF) }
    var shiftState by remember { mutableStateOf(ModifierState.OFF) }
    var altState by remember { mutableStateOf(ModifierState.OFF) }
    var guiState by remember { mutableStateOf(ModifierState.OFF) } // Win / Cmd

    var hiddenEditText by remember { mutableStateOf<EditText?>(null) }
    var lastSentCharInfo by remember { mutableStateOf("Tap keyboard area to type") }
    var terminalStreamText by remember { mutableStateOf("ready>") }

    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(dev.aleian.pockethid.model.ConnectionState.Disconnected) })
    var lastWarnTime by remember { mutableStateOf(0L) }

    fun checkConnectionWarn() {
        if (connState !is dev.aleian.pockethid.model.ConnectionState.Connected) {
            val now = System.currentTimeMillis()
            if (now - lastWarnTime > 3000) {
                lastWarnTime = now
                android.widget.Toast.makeText(context, "Chưa kết nối máy tính! Vui lòng kết nối Bluetooth trước.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
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
        checkConnectionWarn()
        val totalMods = (getActiveModifiers().toInt() or extraModifier.toInt()).toByte()
        triggerHaptic()
        scope.launch {
            transport?.sendKeyClick(keyCode, totalMods)
            consumeStickyModifiers()
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
        hiddenEditText?.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(hiddenEditText, InputMethodManager.SHOW_IMPLICIT)
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
            .padding(16.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

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

        Spacer(modifier = Modifier.height(10.dp))

        // Live Keystroke Terminal & Input Monitor (from mockups)
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

        Spacer(modifier = Modifier.height(10.dp))

        // IME Input Area (Transparent EditText inside)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .clickable { focusHiddenInput() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap to open phone keyboard",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lastSentCharInfo,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                // Invisible Android EditText for capturing system IME
                AndroidView(
                    factory = { ctx ->
                        EditText(ctx).apply {
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setTextColor(android.graphics.Color.TRANSPARENT)
                            alpha = 0.01f

                            setOnKeyListener { _, keyCode, event ->
                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    val stroke = KeyMapper.mapAndroidKeyEvent(keyCode)
                                    if (stroke != null) {
                                        sendKey(stroke.keyCode, stroke.modifiers)
                                        lastSentCharInfo = "Key code: $keyCode"
                                        return@setOnKeyListener true
                                    }
                                }
                                false
                            }

                            doAfterTextChanged { editable ->
                                val text = editable?.toString() ?: ""
                                if (text.isNotEmpty()) {
                                    for (char in text) {
                                        val stroke = KeyMapper.mapCharToStroke(char)
                                        if (stroke != null) {
                                            sendKey(stroke.keyCode, stroke.modifiers)
                                            lastSentCharInfo = "Typed: '$char'"
                                        }
                                    }
                                    editable?.clear()
                                }
                            }

                            hiddenEditText = this
                        }
                    },
                    modifier = Modifier.size(1.dp)
                )
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
