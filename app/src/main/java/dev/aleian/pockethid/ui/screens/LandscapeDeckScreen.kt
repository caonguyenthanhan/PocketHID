package dev.aleian.pockethid.ui.screens

import android.content.ClipData
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.mapping.GestureInterpreter
import dev.aleian.pockethid.mapping.KeyMapper
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.ErrorContainer
import dev.aleian.pockethid.ui.theme.OnPrimaryContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.PrimaryContainer
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.SurfaceActive
import dev.aleian.pockethid.ui.theme.SurfaceBase
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainer
import dev.aleian.pockethid.ui.theme.SurfaceContainerHigh
import dev.aleian.pockethid.ui.theme.SurfaceContainerLow
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import dev.aleian.pockethid.ui.theme.SurfaceRaised
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LandscapeDeckScreen(
    transport: InputTransport?,
    onExitLandscape: (() -> Unit)? = null,
    onToggleOrientation: () -> Unit = onExitLandscape ?: {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()
    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) })

    var commandBuffer by remember { mutableStateOf("ready>") }
    var activeHex by remember { mutableStateOf("0x00") }
    var rightDeckTab by remember { mutableIntStateOf(0) } // 0: Numpad, 1: Trackpad

    var ctrlState by remember { mutableStateOf(ModifierState.OFF) }
    var altState by remember { mutableStateOf(ModifierState.OFF) }
    var superState by remember { mutableStateOf(ModifierState.OFF) }
    var shiftState by remember { mutableStateOf(ModifierState.OFF) }

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
        if (ctrlState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
        if (shiftState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
        if (altState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
        if (superState != ModifierState.OFF) mods = (mods.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
        return mods
    }

    fun consumeStickyModifiers() {
        if (ctrlState == ModifierState.STICKY) ctrlState = ModifierState.OFF
        if (shiftState == ModifierState.STICKY) shiftState = ModifierState.OFF
        if (altState == ModifierState.STICKY) altState = ModifierState.OFF
        if (superState == ModifierState.STICKY) superState = ModifierState.OFF
    }

    fun sendRawKey(keyCode: Byte, extraModifier: Byte = 0, label: String = "") {
        val totalMods = (getActiveModifiers().toInt() or extraModifier.toInt()).toByte()
        triggerHaptic()
        activeHex = "0x" + Integer.toHexString(keyCode.toInt() and 0xFF).uppercase()
        commandBuffer = if (label.isNotEmpty()) "key: $label" else "code: $activeHex"
        scope.launch {
            transport?.sendKeyClick(keyCode, totalMods)
            consumeStickyModifiers()
        }
    }

    fun cycleMod(current: ModifierState): ModifierState {
        triggerHaptic()
        return when (current) {
            ModifierState.OFF -> ModifierState.STICKY
            ModifierState.STICKY -> ModifierState.LOCKED
            ModifierState.LOCKED -> ModifierState.OFF
        }
    }

    fun sendStringSafe(text: String) {
        scope.launch {
            commandBuffer = "pasting: ${text.take(20)}..."
            for (char in text) {
                val stroke = KeyMapper.mapCharToStroke(char)
                if (stroke != null) {
                    transport?.sendKeyClick(stroke.keyCode, stroke.modifiers)
                    delay(settings.pasteDelayMs)
                }
            }
            commandBuffer = "pasted ${text.length} chars"
        }
    }

    val hostName = when (connState) {
        is ConnectionState.Connected -> (connState as ConnectionState.Connected).device.name ?: "Connected Host"
        else -> "Host Disconnected"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBase)
            .systemBarsPadding()
            .displayCutoutPadding()
    ) {
        // TOP HUD STRIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(SurfaceContainerLowest)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại màn hình dọc",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onToggleOrientation() }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryBlue.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(4.dp))
                        .clickable { onToggleOrientation() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Xoay màn hình",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("XOAY DỌC", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusConnected)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(hostName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Text(
                    text = "8ms / ${settings.pollingRate.toInt()}Hz",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (settings.keyboardHaptics) "HAPTIC: ON" else "HAPTIC: OFF",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryBlue
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = StatusConnected,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("98%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                }
            }
        }

        // REALTIME COMMAND BUFFER / SCANCODE MONITOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(SurfaceContainerLow)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceBase)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("host>", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = commandBuffer,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "HEX: $activeHex",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DeckMiniButton(label = "CLEAR", icon = Icons.Default.Backspace) {
                    commandBuffer = "ready>"
                    activeHex = "0x00"
                }

                DeckMiniButton(label = "PASTE", icon = Icons.Default.ContentPaste) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrEmpty()) {
                        sendStringSafe(clip)
                    } else {
                        Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ERGONOMIC 3-ZONE HARDWARE DECK
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // ZONE 1: LEFT THUMB DECK
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerLowest)
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Zone Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LEFT DECK", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Text("Z1", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = PrimaryBlue)
                }

                // Macro Matrix (2x2)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MacroDeckButton("^C", "SIGINT", Color(0xFFEF4444), Modifier.weight(1f)) {
                            sendRawKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL, "Ctrl+C")
                        }
                        MacroDeckButton("^Z", "TSTP", Color(0xFFF59E0B), Modifier.weight(1f)) {
                            sendRawKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL, "Ctrl+Z")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MacroDeckButton("Alt+Tab", "SW", PrimaryBlue, Modifier.weight(1f)) {
                            sendRawKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT, "Alt+Tab")
                        }
                        MacroDeckButton("Win+D", "DESK", TextPrimary, Modifier.weight(1f)) {
                            sendRawKey(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI, "Win+D")
                        }
                    }
                }

                // Modifiers
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    KeycapButton(text = "ESC", modifier = Modifier.fillMaxWidth().height(22.dp), color = Color(0xFFEF4444), fontSize = 9.sp) {
                        sendRawKey(HidConstants.KEY_ESC, 0, "ESC")
                    }
                    ModifierDeckButton("CTRL", ctrlState) { ctrlState = cycleMod(ctrlState) }
                    ModifierDeckButton("ALT", altState) { altState = cycleMod(altState) }
                    ModifierDeckButton("SUPER", superState) { superState = cycleMod(superState) }
                    ModifierDeckButton("SHIFT", shiftState) { shiftState = cycleMod(shiftState) }
                    KeycapButton(text = "TAB", modifier = Modifier.fillMaxWidth().height(20.dp), fontSize = 9.sp) {
                        sendRawKey(HidConstants.KEY_TAB, 0, "TAB")
                    }
                }

                // Micro D-Pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DPadButton(icon = Icons.Default.ArrowDropUp) {
                            sendRawKey(HidConstants.KEY_UP, 0, "UP")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DPadButton(icon = Icons.Default.ArrowLeft) {
                                sendRawKey(HidConstants.KEY_LEFT, 0, "LEFT")
                            }
                            DPadButton(icon = Icons.Default.ArrowRight) {
                                sendRawKey(HidConstants.KEY_RIGHT, 0, "RIGHT")
                            }
                        }
                        DPadButton(icon = Icons.Default.ArrowDropDown) {
                            sendRawKey(HidConstants.KEY_DOWN, 0, "DOWN")
                        }
                    }
                }
            }

            // ZONE 2: MAIN 75%/TKL MECHANICAL KEYBOARD
            Column(
                modifier = Modifier
                    .weight(7.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainer)
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // F-Row
                Row(
                    modifier = Modifier.fillMaxWidth().weight(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val fKeys = listOf(
                        "F1" to HidConstants.KEY_F1, "F2" to HidConstants.KEY_F2,
                        "F3" to HidConstants.KEY_F3, "F4" to HidConstants.KEY_F4,
                        "F5" to HidConstants.KEY_F5, "F6" to HidConstants.KEY_F6,
                        "F7" to HidConstants.KEY_F7, "F8" to HidConstants.KEY_F8,
                        "F9" to HidConstants.KEY_F9, "F10" to HidConstants.KEY_F10,
                        "F11" to HidConstants.KEY_F11, "F12" to HidConstants.KEY_F12
                    )
                    fKeys.forEach { (lbl, code) ->
                        KeycapButton(
                            text = lbl,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 10.sp
                        ) {
                            sendRawKey(code, 0, lbl)
                        }
                    }
                    KeycapButton(
                        text = "Prt",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_PRINTSCREEN, 0, "PrtSc")
                    }
                }

                // Row 1: Numbers
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.05f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val numRow = listOf(
                        "`" to HidConstants.KEY_GRAVE, "1" to HidConstants.KEY_1,
                        "2" to HidConstants.KEY_2, "3" to HidConstants.KEY_3,
                        "4" to HidConstants.KEY_4, "5" to HidConstants.KEY_5,
                        "6" to HidConstants.KEY_6, "7" to HidConstants.KEY_7,
                        "8" to HidConstants.KEY_8, "9" to HidConstants.KEY_9,
                        "0" to HidConstants.KEY_0, "-" to HidConstants.KEY_MINUS,
                        "=" to HidConstants.KEY_EQUAL
                    )
                    numRow.forEach { (lbl, code) ->
                        KeycapButton(
                            text = lbl,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 11.sp
                        ) {
                            sendRawKey(code, 0, lbl)
                        }
                    }
                    KeycapButton(
                        text = "Bksp",
                        modifier = Modifier.weight(1.5f).fillMaxHeight(),
                        fontSize = 10.sp,
                        color = Color(0xFFEF4444)
                    ) {
                        sendRawKey(HidConstants.KEY_BACKSPACE, 0, "BKSP")
                    }
                }

                // Row 2: QWERTY
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.05f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val qRow = listOf(
                        "Q" to HidConstants.KEY_Q, "W" to HidConstants.KEY_W,
                        "E" to HidConstants.KEY_E, "R" to HidConstants.KEY_R,
                        "T" to HidConstants.KEY_T, "Y" to HidConstants.KEY_Y,
                        "U" to HidConstants.KEY_U, "I" to HidConstants.KEY_I,
                        "O" to HidConstants.KEY_O, "P" to HidConstants.KEY_P,
                        "[" to HidConstants.KEY_LEFTBRACE, "]" to HidConstants.KEY_RIGHTBRACE,
                        "\\" to HidConstants.KEY_BACKSLASH
                    )
                    qRow.forEach { (lbl, code) ->
                        KeycapButton(
                            text = lbl,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 11.sp
                        ) {
                            sendRawKey(code, 0, lbl)
                        }
                    }
                }

                // Row 3: ASDFGHJKL
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.05f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    KeycapButton(
                        text = "Caps",
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_CAPSLOCK, 0, "CAPS")
                    }
                    val aRow = listOf(
                        "A" to HidConstants.KEY_A, "S" to HidConstants.KEY_S,
                        "D" to HidConstants.KEY_D, "F" to HidConstants.KEY_F,
                        "G" to HidConstants.KEY_G, "H" to HidConstants.KEY_H,
                        "J" to HidConstants.KEY_J, "K" to HidConstants.KEY_K,
                        "L" to HidConstants.KEY_L, ";" to HidConstants.KEY_SEMICOLON,
                        "'" to HidConstants.KEY_APOSTROPHE
                    )
                    aRow.forEach { (lbl, code) ->
                        KeycapButton(
                            text = lbl,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 11.sp
                        ) {
                            sendRawKey(code, 0, lbl)
                        }
                    }
                    KeycapButton(
                        text = "Enter",
                        modifier = Modifier.weight(1.8f).fillMaxHeight(),
                        fontSize = 11.sp,
                        containerColor = PrimaryContainer,
                        color = OnPrimaryContainer
                    ) {
                        sendRawKey(HidConstants.KEY_ENTER, 0, "ENTER")
                    }
                }

                // Row 4: ZXCVBNM
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.05f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    KeycapButton(
                        text = "Shift",
                        modifier = Modifier.weight(1.5f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_LEFTSHIFT, 0, "SHIFT")
                    }
                    val zRow = listOf(
                        "Z" to HidConstants.KEY_Z, "X" to HidConstants.KEY_X,
                        "C" to HidConstants.KEY_C, "V" to HidConstants.KEY_V,
                        "B" to HidConstants.KEY_B, "N" to HidConstants.KEY_N,
                        "M" to HidConstants.KEY_M, "," to HidConstants.KEY_COMMA,
                        "." to HidConstants.KEY_DOT, "/" to HidConstants.KEY_SLASH
                    )
                    zRow.forEach { (lbl, code) ->
                        KeycapButton(
                            text = lbl,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 11.sp
                        ) {
                            sendRawKey(code, 0, lbl)
                        }
                    }
                    KeycapButton(
                        text = "Shift",
                        modifier = Modifier.weight(1.5f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_RIGHTSHIFT, 0, "SHIFT")
                    }
                }

                // Row 5: Spacebar row
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1.05f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    KeycapButton(
                        text = "Fn",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {}
                    KeycapButton(
                        text = "Ctrl",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_LEFTCTRL, 0, "Ctrl")
                    }
                    KeycapButton(
                        text = "Alt",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_LEFTALT, 0, "Alt")
                    }
                    // Spacebar
                    Box(
                        modifier = Modifier
                            .weight(5f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .clickable { sendRawKey(HidConstants.KEY_SPACE, 0, "SPACE") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PrimaryBlue.copy(alpha = 0.6f))
                        )
                    }
                    KeycapButton(
                        text = "AltGr",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_RIGHTALT, 0, "AltGr")
                    }
                    KeycapButton(
                        text = "Menu",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_MENU, 0, "Menu")
                    }
                    KeycapButton(
                        text = "Ctrl",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        fontSize = 10.sp
                    ) {
                        sendRawKey(HidConstants.KEY_RIGHTCTRL, 0, "Ctrl")
                    }
                }
            }

            // ZONE 3: RIGHT THUMB DECK (Numpad or Trackpad)
            Column(
                modifier = Modifier
                    .weight(2.6f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerLowest)
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Header with Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RIGHT DECK", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceCard)
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (rightDeckTab == 0) SurfaceActive else Color.Transparent)
                                .clickable { rightDeckTab = 0 }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("NUM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (rightDeckTab == 0) PrimaryBlue else TextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (rightDeckTab == 1) SurfaceActive else Color.Transparent)
                                .clickable { rightDeckTab = 1 }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("PAD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (rightDeckTab == 1) PrimaryBlue else TextMuted)
                        }
                    }
                }

                // Nav 6-Key Island
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        KeycapButton("INS", Modifier.weight(1f).height(22.dp), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_INSERT, 0, "INS") }
                        KeycapButton("HOME", Modifier.weight(1f).height(22.dp), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_HOME, 0, "HOME") }
                        KeycapButton("PGUP", Modifier.weight(1f).height(22.dp), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_PAGEUP, 0, "PGUP") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        KeycapButton("DEL", Modifier.weight(1f).height(22.dp), color = Color(0xFFEF4444), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_DELETE, 0, "DEL") }
                        KeycapButton("END", Modifier.weight(1f).height(22.dp), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_END, 0, "END") }
                        KeycapButton("PGDN", Modifier.weight(1f).height(22.dp), fontSize = 9.sp) { sendRawKey(HidConstants.KEY_PAGEDOWN, 0, "PGDN") }
                    }
                }

                // Dynamic Mode: Numpad or Trackpad
                if (rightDeckTab == 0) {
                    // NUMPAD VIEW (Takes full remaining vertical space)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            KeycapButton("Num", Modifier.weight(1f).fillMaxHeight(), fontSize = 11.sp) { sendRawKey(HidConstants.KEY_NUMLOCK, 0, "NUM") }
                            KeycapButton("/", Modifier.weight(1f).fillMaxHeight(), fontSize = 11.sp) { sendRawKey(HidConstants.KEY_KPSLASH, 0, "/") }
                            KeycapButton("*", Modifier.weight(1f).fillMaxHeight(), fontSize = 11.sp) { sendRawKey(HidConstants.KEY_KPASTERISK, 0, "*") }
                        }
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            KeycapButton("7", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP7, 0, "7") }
                            KeycapButton("8", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP8, 0, "8") }
                            KeycapButton("9", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP9, 0, "9") }
                        }
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            KeycapButton("4", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP4, 0, "4") }
                            KeycapButton("5", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP5, 0, "5") }
                            KeycapButton("6", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP6, 0, "6") }
                        }
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            KeycapButton("1", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP1, 0, "1") }
                            KeycapButton("2", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP2, 0, "2") }
                            KeycapButton("3", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP3, 0, "3") }
                        }
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            KeycapButton("0", Modifier.weight(2f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KP0, 0, "0") }
                            KeycapButton(".", Modifier.weight(1f).fillMaxHeight(), fontSize = 12.sp) { sendRawKey(HidConstants.KEY_KPDOT, 0, ".") }
                        }
                    }
                } else {
                    // TRACKPAD VIEW
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerLow)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val action = gestureInterpreter.processOneFingerMove(dragAmount.x, dragAmount.y, false)
                                        transport?.sendMouseMove(action.dx, action.dy, action.buttons, 0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TRACKPAD", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceCard)
                                    .clickable {
                                        scope.launch {
                                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_LEFT, 0)
                                            delay(16)
                                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("L-CLICK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceCard)
                                    .clickable {
                                        scope.launch {
                                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_RIGHT, 0)
                                            delay(16)
                                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("R-CLICK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeycapButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    color: Color = TextPrimary,
    fontSize: TextUnit = 11.sp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ModifierDeckButton(
    label: String,
    state: ModifierState,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when (state) {
        ModifierState.OFF -> Pair(SurfaceCard, TextSecondary)
        ModifierState.STICKY -> Pair(PrimaryBlue, OnPrimaryContainer)
        ModifierState.LOCKED -> Pair(AccentGreen, Color.Black)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(1.dp, DarkBorder, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor)
            Text(
                text = when (state) {
                    ModifierState.OFF -> "OFF"
                    ModifierState.STICKY -> "•"
                    ModifierState.LOCKED -> "LK"
                },
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}

@Composable
private fun MacroDeckButton(
    title: String,
    subtitle: String,
    titleColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = titleColor)
            Text(subtitle, fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
        }
    }
}

@Composable
private fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun DeckMiniButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
