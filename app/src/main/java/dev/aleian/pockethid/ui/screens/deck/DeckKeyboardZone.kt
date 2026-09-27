package dev.aleian.pockethid.ui.screens.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.aleian.pockethid.action.PocketAction
import dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.KeyLegend
import dev.aleian.pockethid.model.KeyLegends
import dev.aleian.pockethid.ui.components.DeckKey
import dev.aleian.pockethid.ui.components.DeckLegendKey
import dev.aleian.pockethid.ui.components.DeckRepeatKey
import dev.aleian.pockethid.ui.components.DedicatedNumberRow
import dev.aleian.pockethid.ui.components.ModifierToggleState
import dev.aleian.pockethid.ui.components.SubModeSelector
import dev.aleian.pockethid.ui.theme.*

@Composable
fun DeckKeyboardZone(
    selectedSubMode: Int,
    onSelectSubMode: (Int) -> Unit,
    ctrlState: ModifierToggleState,
    altState: ModifierToggleState,
    shiftState: ModifierToggleState,
    superState: ModifierToggleState = ModifierToggleState.OFF,
    isCapsLockActive: Boolean = false,
    onCycleCtrl: () -> Unit,
    onCycleAlt: () -> Unit,
    onCycleShift: () -> Unit,
    onCycleSuper: () -> Unit = {},
    onSendRawKey: (keyCode: Byte, extraMod: Byte, label: String) -> Unit,
    onSendConsumerKey: (usageCode: Int, label: String) -> Unit,
    onDispatchAction: (action: PocketAction) -> Unit = {},
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isShiftActive = shiftState != ModifierToggleState.OFF

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Sub-mode Header Selector
        SubModeSelector(
            selectedSubMode = selectedSubMode,
            onSelectSubMode = onSelectSubMode,
            modifier = Modifier.fillMaxWidth()
        )

        // Dynamic Center Content based on Sub-mode
        when (selectedSubMode) {
            0 -> {
                // TYPE MODE: Dedicated Number Row + Maximum Touch Width QWERTY
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // ROW 1: PERMANENT DEDICATED NUMBER ROW (` 1 2 3 4 5 6 7 8 9 0 - = BKSP)
                    DedicatedNumberRow(
                        isShiftActive = isShiftActive,
                        onSendKey = { keyCode, extraMod, label ->
                            onSendRawKey(keyCode, extraMod, label)
                        },
                        onBackspaceRepeat = {
                            onSendRawKey(HidConstants.KEY_BACKSPACE, 0, "BKSP")
                        },
                        hapticsEnabled = hapticsEnabled,
                        fontSize = 12.sp,
                        keyHeight = 36.dp
                    )

                    // ROW 2: Q W E R T Y U I O P [ ] \ (with Dual Legend on brackets/backslash)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        KeyLegends.ROW_2.forEach { legend ->
                            DeckLegendKey(
                                legend = legend,
                                isShiftActive = isShiftActive,
                                isCapsLockActive = isCapsLockActive,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                val activeLabel = legend.resolveActiveLabel(isShiftActive, isCapsLockActive)
                                val extraMod = if (isShiftActive && legend.shifted != null) HidConstants.MOD_LEFT_SHIFT else 0.toByte()
                                onSendRawKey(legend.keyCode, extraMod, activeLabel)
                            }
                        }
                    }

                    // ROW 3: A S D F G H J K L ; ' (Maximized horizontal touch target with Dual Legend on ; and ')
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        KeyLegends.ROW_3.forEach { legend ->
                            DeckLegendKey(
                                legend = legend,
                                isShiftActive = isShiftActive,
                                isCapsLockActive = isCapsLockActive,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                val activeLabel = legend.resolveActiveLabel(isShiftActive, isCapsLockActive)
                                val extraMod = if (isShiftActive && legend.shifted != null) HidConstants.MOD_LEFT_SHIFT else 0.toByte()
                                onSendRawKey(legend.keyCode, extraMod, activeLabel)
                            }
                        }
                    }

                    // ROW 4: Z X C V B N M , . / (Maximized horizontal touch target with Dual Legend on , . /)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        KeyLegends.ROW_4.forEach { legend ->
                            DeckLegendKey(
                                legend = legend,
                                isShiftActive = isShiftActive,
                                isCapsLockActive = isCapsLockActive,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                val activeLabel = legend.resolveActiveLabel(isShiftActive, isCapsLockActive)
                                val extraMod = if (isShiftActive && legend.shifted != null) HidConstants.MOD_LEFT_SHIFT else 0.toByte()
                                onSendRawKey(legend.keyCode, extraMod, activeLabel)
                            }
                        }
                    }

                    // ROW 5: BOTTOM MODIFIER + SPACEBAR DECK (CTRL ⊞ WIN ALT SPACE SHIFT ENTER)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DeckKey(
                            text = "CTRL",
                            modifier = Modifier.weight(1.0f).fillMaxHeight(),
                            containerColor = if (ctrlState != ModifierToggleState.OFF) PrimaryBlue.copy(alpha = 0.3f) else DarkSurfaceVariant,
                            textColor = if (ctrlState != ModifierToggleState.OFF) PrimaryBlue else TextPrimary,
                            fontSize = 11.sp
                        ) {
                            onCycleCtrl()
                        }

                        DeckKey(
                            text = "⊞ WIN",
                            modifier = Modifier.weight(1.0f).fillMaxHeight(),
                            containerColor = if (superState != ModifierToggleState.OFF) PrimaryBlue.copy(alpha = 0.3f) else DarkSurfaceVariant,
                            textColor = if (superState != ModifierToggleState.OFF) PrimaryBlue else TextPrimary,
                            fontSize = 11.sp,
                            onLongClick = {
                                onSendRawKey(HidConstants.KEY_NONE, HidConstants.MOD_LEFT_GUI, "Win")
                            }
                        ) {
                            onCycleSuper()
                        }

                        DeckKey(
                            text = "ALT",
                            modifier = Modifier.weight(1.0f).fillMaxHeight(),
                            containerColor = if (altState != ModifierToggleState.OFF) PrimaryBlue.copy(alpha = 0.3f) else DarkSurfaceVariant,
                            textColor = if (altState != ModifierToggleState.OFF) PrimaryBlue else TextPrimary,
                            fontSize = 11.sp
                        ) {
                            onCycleAlt()
                        }

                        DeckKey(
                            text = "SPACE",
                            modifier = Modifier.weight(3.6f).fillMaxHeight(),
                            containerColor = SurfaceCard,
                            textColor = PrimaryBlue,
                            fontSize = 12.sp
                        ) {
                            onSendRawKey(HidConstants.KEY_SPACE, 0, "SPACE")
                        }

                        DeckKey(
                            text = if (shiftState == ModifierToggleState.LOCKED) "SHIFT 🔒" else "SHIFT",
                            modifier = Modifier.weight(1.1f).fillMaxHeight(),
                            containerColor = if (isShiftActive) PrimaryBlue.copy(alpha = 0.35f) else DarkSurfaceVariant,
                            textColor = if (isShiftActive) PrimaryBlue else TextPrimary,
                            fontSize = 11.sp
                        ) {
                            onCycleShift()
                        }

                        DeckKey(
                            text = "ENTER",
                            modifier = Modifier.weight(1.7f).fillMaxHeight(),
                            containerColor = PrimaryBlue.copy(alpha = 0.25f),
                            textColor = PrimaryBlue,
                            fontSize = 12.sp
                        ) {
                            onSendRawKey(HidConstants.KEY_ENTER, 0, "ENTER")
                        }
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
                            onSendRawKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL, "Copy")
                        }
                        DeckKey(text = "PASTE (^V)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            onSendRawKey(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL, "Paste")
                        }
                        DeckKey(text = "CUT   (^X)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_X, HidConstants.MOD_LEFT_CTRL, "Cut")
                        }
                        DeckKey(text = "UNDO  (^Z)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = AccentAmber) {
                            onSendRawKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL, "Undo")
                        }
                        DeckKey(text = "REDO  (^Y)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = AccentAmber) {
                            onSendRawKey(HidConstants.KEY_Y, HidConstants.MOD_LEFT_CTRL, "Redo")
                        }
                    }

                    // Row 2: Navigation & Search
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeckKey(text = "SELECT ALL (^A)", modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_A, HidConstants.MOD_LEFT_CTRL, "SelectAll")
                        }
                        DeckKey(text = "FIND (^F)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_F, HidConstants.MOD_LEFT_CTRL, "Find")
                        }
                        DeckKey(text = "SAVE (^S)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFF10B981)) {
                            onSendRawKey(HidConstants.KEY_S, HidConstants.MOD_LEFT_CTRL, "Save")
                        }
                        DeckKey(text = "SWITCH (Alt+Tab)", modifier = Modifier.weight(1.3f).fillMaxHeight(), textColor = PrimaryBlue) {
                            onSendRawKey(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT, "Alt+Tab")
                        }
                        DeckKey(text = "SHOW DESKTOP (Win+D)", modifier = Modifier.weight(1.5f).fillMaxHeight(), textColor = PrimaryBlue) {
                            onDispatchAction(PocketAction.SystemAction.ShowDesktop)
                        }
                    }

                    // Row 3: Terminal & Dev Power Tools
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeckKey(text = "SIGINT (^C)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            onSendRawKey(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL, "^C")
                        }
                        DeckKey(text = "TSTP (^Z)", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = AccentAmber) {
                            onSendRawKey(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL, "^Z")
                        }
                        DeckKey(text = "CLEAR (^L)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_L, HidConstants.MOD_LEFT_CTRL, "^L")
                        }
                        DeckKey(text = "EOF (^D)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_D, HidConstants.MOD_LEFT_CTRL, "^D")
                        }
                        DeckKey(text = "LOCK (Win+L)", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            onSendRawKey(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI, "Win+L")
                        }
                    }
                }
            }

            2 -> {
                // MEDIA MODE: Consumer Report Controller (Preferred 2-Row Compact Control Deck)
                val consumerTelemetry by ConsumerDiagnosticsHub.telemetry.collectAsState()

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Track Playback (PREV | PLAY/PAUSE | NEXT)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeckKey(
                            text = "⏮  PREV",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 13.sp
                        ) {
                            onDispatchAction(PocketAction.MediaAction.PrevTrack)
                        }

                        DeckKey(
                            text = "⏯  PLAY / PAUSE",
                            modifier = Modifier.weight(1.4f).fillMaxHeight(),
                            containerColor = PrimaryBlue.copy(alpha = 0.2f),
                            textColor = PrimaryBlue,
                            fontSize = 14.sp
                        ) {
                            onDispatchAction(PocketAction.MediaAction.PlayPause)
                        }

                        DeckKey(
                            text = "NEXT  ⏭",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            fontSize = 13.sp
                        ) {
                            onDispatchAction(PocketAction.MediaAction.NextTrack)
                        }
                    }

                    // Row 2: Volume & Mute (VOL- | MUTE | VOL+)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeckRepeatKey(
                            text = "VOL −",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textColor = PrimaryBlue,
                            fontSize = 14.sp,
                            hapticFeedback = hapticsEnabled,
                            enableRepeat = true,
                            repeatIntervalMs = 100L,
                            onTrigger = {
                                onDispatchAction(PocketAction.MediaAction.VolumeDown)
                            }
                        )

                        DeckRepeatKey(
                            text = "MUTE 🔇",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                            textColor = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            hapticFeedback = hapticsEnabled,
                            enableRepeat = false, // Single tap only, no hold repeat!
                            onTrigger = {
                                onDispatchAction(PocketAction.MediaAction.Mute)
                            }
                        )

                        DeckRepeatKey(
                            text = "VOL ＋",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textColor = PrimaryBlue,
                            fontSize = 14.sp,
                            hapticFeedback = hapticsEnabled,
                            enableRepeat = true,
                            repeatIntervalMs = 100L,
                            onTrigger = {
                                onDispatchAction(PocketAction.MediaAction.VolumeUp)
                            }
                        )
                    }

                    // Compact Live Diagnostic Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONSUMER: ${consumerTelemetry.actionName} (${consumerTelemetry.usageHex})",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Report ID 3 • ${consumerTelemetry.transportStatus}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (consumerTelemetry.transportStatus.contains("SUCCESS")) StatusConnected else TextMuted
                        )
                    }
                }
            }

            3 -> {
                // SYSTEM MODE: Windows & Virtual Desktops Universal Command Controller
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Primary System Shell (OS-Aware Semantic Actions)
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeckKey(
                            text = "SHOW DESKTOP\n(Win + D)",
                            modifier = Modifier.weight(1.3f).fillMaxHeight(),
                            textColor = PrimaryBlue
                        ) {
                            onDispatchAction(PocketAction.SystemAction.ShowDesktop)
                        }
                        DeckKey(
                            text = "LOCK PC\n(Win + L)",
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                            textColor = Color(0xFFEF4444)
                        ) {
                            onDispatchAction(PocketAction.SystemAction.LockPC)
                        }
                        DeckKey(
                            text = "TASK VIEW\n(Win + Tab)",
                            modifier = Modifier.weight(1.3f).fillMaxHeight(),
                            textColor = PrimaryBlue
                        ) {
                            onDispatchAction(PocketAction.SystemAction.TaskView)
                        }
                    }

                    // Row 2: Virtual Desktops Management
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeckKey(text = "◀ DESKTOP", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            val mods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            onSendRawKey(HidConstants.KEY_LEFT, mods, "PrevDesktop")
                        }
                        DeckKey(text = "＋ NEW DESKTOP", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = Color(0xFF10B981)) {
                            val mods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            onSendRawKey(HidConstants.KEY_D, mods, "NewDesktop")
                        }
                        DeckKey(text = "✕ CLOSE DESK", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = Color(0xFFEF4444)) {
                            val mods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            onSendRawKey(HidConstants.KEY_F4, mods, "CloseDesktop")
                        }
                        DeckKey(text = "DESKTOP ▶", modifier = Modifier.weight(1.2f).fillMaxHeight(), textColor = PrimaryBlue) {
                            val mods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                            onSendRawKey(HidConstants.KEY_RIGHT, mods, "NextDesktop")
                        }
                    }

                    // Row 3: Windows Shell Tools & Snapping
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeckKey(text = "SNIP TOOL (Win+⇧+S)", modifier = Modifier.weight(1.3f).fillMaxHeight(), textColor = Color(0xFFF59E0B)) {
                            val mods = (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                            onSendRawKey(HidConstants.KEY_S, mods, "SnipTool")
                        }
                        DeckKey(text = "EXPLORER (Win+E)", modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_E, HidConstants.MOD_LEFT_GUI, "Explorer")
                        }
                        DeckKey(text = "RUN (Win+R)", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_R, HidConstants.MOD_LEFT_GUI, "Run")
                        }
                        DeckKey(text = "SNAP ◀", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_LEFT, HidConstants.MOD_LEFT_GUI, "SnapLeft")
                        }
                        DeckKey(text = "MAXIMIZE ▲", modifier = Modifier.weight(1.1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            onSendRawKey(HidConstants.KEY_UP, HidConstants.MOD_LEFT_GUI, "Maximize")
                        }
                        DeckKey(text = "SNAP ▶", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_RIGHT, HidConstants.MOD_LEFT_GUI, "SnapRight")
                        }
                    }
                }
            }

            4 -> {
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
                                onSendRawKey(code, 0, lbl)
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
                                onSendRawKey(code, 0, lbl)
                            }
                        }
                    }

                    // Extended System Keys
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        DeckKey(text = "PRTSC", modifier = Modifier.weight(1f).fillMaxHeight(), textColor = PrimaryBlue) {
                            onSendRawKey(HidConstants.KEY_PRINTSCREEN, 0, "PrtSc")
                        }
                        DeckKey(text = "HOME", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_HOME, 0, "Home")
                        }
                        DeckKey(text = "END", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_END, 0, "End")
                        }
                        DeckKey(text = "PGUP", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_PAGEUP, 0, "PgUp")
                        }
                        DeckKey(text = "PGDN", modifier = Modifier.weight(1f).fillMaxHeight()) {
                            onSendRawKey(HidConstants.KEY_PAGEDOWN, 0, "PgDn")
                        }
                    }
                }
            }

            5 -> {
                // NUMPAD MODE: Dedicated Financial / Code Numpad
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val numRows: List<List<Pair<String, Byte>>> = listOf(
                        listOf("7" to HidConstants.KEY_KP7, "8" to HidConstants.KEY_KP8, "9" to HidConstants.KEY_KP9, "/" to HidConstants.KEY_KPSLASH),
                        listOf("4" to HidConstants.KEY_KP4, "5" to HidConstants.KEY_KP5, "6" to HidConstants.KEY_KP6, "*" to HidConstants.KEY_KPASTERISK),
                        listOf("1" to HidConstants.KEY_KP1, "2" to HidConstants.KEY_KP2, "3" to HidConstants.KEY_KP3, "−" to HidConstants.KEY_KPMINUS),
                        listOf("0" to HidConstants.KEY_KP0, "." to HidConstants.KEY_KPDOT, "＝" to HidConstants.KEY_KPEQUAL, "＋" to HidConstants.KEY_KPPLUS)
                    )
                    for (rowKeys in numRows) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (item in rowKeys) {
                                DeckKey(text = item.first, modifier = Modifier.weight(1f).fillMaxHeight(), fontSize = 15.sp) {
                                    onSendRawKey(item.second, 0, item.first)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
