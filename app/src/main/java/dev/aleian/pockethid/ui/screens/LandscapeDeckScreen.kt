package dev.aleian.pockethid.ui.screens

import android.bluetooth.BluetoothDevice
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.aleian.pockethid.mapping.KeyMapper
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.FocusLockController
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.DeckModeSwitcher
import dev.aleian.pockethid.ui.components.ModifierToggleState
import dev.aleian.pockethid.ui.components.MultiTouchTrackpad
import dev.aleian.pockethid.ui.components.TopCommandBar
import dev.aleian.pockethid.ui.screens.deck.DeckKeyboardZone
import dev.aleian.pockethid.ui.screens.deck.DeckMacroZone
import dev.aleian.pockethid.ui.screens.deck.DeckUtilityZone
import dev.aleian.pockethid.ui.theme.SurfaceBase
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Landscape Command Deck Orchestrator
 * High-performance, low-recomposition horizontal controller for PocketHID.
 * Refactored into modular subcomponents: DeckMacroZone, DeckKeyboardZone, DeckUtilityZone.
 */
@Composable
fun LandscapeDeckScreen(
    transport: InputTransport?,
    pairedDevices: List<BluetoothDevice>,
    onConnectToDevice: (BluetoothDevice) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onExitLandscape: () -> Unit,
    onToggleOrientation: () -> Unit,
    drawingController: dev.aleian.pockethid.drawing.DrawingController = remember { dev.aleian.pockethid.drawing.DrawingController() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()

    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) })

    val isFocusLocked by FocusLockController.isLocked.collectAsState()

    LaunchedEffect(connState) {
        if (connState is ConnectionState.Disconnected) {
            FocusLockController.reset()
        }
    }

    var selectedTopMode by rememberSaveable { mutableIntStateOf(0) } // 0: Keyboard, 1: Mouse, 2: Gamepad, 3: Presenter, 4: One-Hand, 5: Draw
    
    LaunchedEffect(settings) {
        val isCurrentTabVisible = when (selectedTopMode) {
            0 -> settings.modeKeyboardVisible
            1 -> settings.modeMouseVisible
            2 -> settings.modeGamepadVisible
            3 -> settings.modePresenterVisible
            4 -> settings.modeOneHandVisible
            5 -> settings.modeDrawVisible
            else -> false
        }
        if (!isCurrentTabVisible) {
            val firstVisible = listOf(
                0 to settings.modeKeyboardVisible,
                1 to settings.modeMouseVisible,
                2 to settings.modeGamepadVisible,
                3 to settings.modePresenterVisible,
                4 to settings.modeOneHandVisible,
                5 to settings.modeDrawVisible
            ).firstOrNull { it.second }?.first ?: 0
            
            val resolved = FocusLockController.resolveModeSwitch(selectedTopMode, firstVisible)
            selectedTopMode = resolved
        }
    }

    var selectedKeyboardSubMode by rememberSaveable { mutableIntStateOf(0) } // 0: Type, 1: Shortcuts, 2: Media, 3: System, 4: F-Keys, 5: Numpad

    // Modifier Key States
    var ctrlState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var altState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var superState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var shiftState by remember { mutableStateOf(ModifierToggleState.OFF) }
    var capsLockState by remember { mutableStateOf(false) }

    // Telemetry & Diagnostics info
    var lastScancode by remember { mutableStateOf("0x00") }
    var lastInputLabel by remember { mutableStateOf("IDLE") }

    // Modals
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    var showPairingSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showCommandPalette by rememberSaveable { mutableStateOf(false) }

    fun triggerHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun getActiveModifiers(): Byte {
        var mods: Byte = 0
        if (ctrlState != ModifierToggleState.OFF) mods = (mods.toInt() or dev.aleian.pockethid.model.HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
        if (shiftState != ModifierToggleState.OFF) mods = (mods.toInt() or dev.aleian.pockethid.model.HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
        if (altState != ModifierToggleState.OFF) mods = (mods.toInt() or dev.aleian.pockethid.model.HidConstants.MOD_LEFT_ALT.toInt()).toByte()
        if (superState != ModifierToggleState.OFF) mods = (mods.toInt() or dev.aleian.pockethid.model.HidConstants.MOD_LEFT_GUI.toInt()).toByte()
        return mods
    }

    fun consumeStickyModifiers() {
        if (ctrlState == ModifierToggleState.STICKY) ctrlState = ModifierToggleState.OFF
        if (shiftState == ModifierToggleState.STICKY) shiftState = ModifierToggleState.OFF
        if (altState == ModifierToggleState.STICKY) altState = ModifierToggleState.OFF
        if (superState == ModifierToggleState.STICKY) superState = ModifierToggleState.OFF
    }

    var lastWarnTime by remember { mutableStateOf(0L) }

    fun canSendInput(): Boolean {
        if (connState is ConnectionState.Connected || transport?.isConnected == true) {
            return true
        }
        if (connState is ConnectionState.Connecting) {
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastWarnTime > 3000) {
            lastWarnTime = now
            Toast.makeText(context, "Connect to a host first.", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun sendRawKey(keyCode: Byte, extraModifier: Byte = 0, label: String = "") {
        if (!canSendInput()) return
        val totalMods = (getActiveModifiers().toInt() or extraModifier.toInt()).toByte()
        triggerHaptic()
        lastScancode = "0x" + Integer.toHexString(keyCode.toInt() and 0xFF).uppercase()
        lastInputLabel = label.ifEmpty { lastScancode }
        scope.launch {
            transport?.sendKeyClick(keyCode, totalMods)
            consumeStickyModifiers()
        }
    }

    fun sendConsumerKey(usageCode: Int, label: String = "") {
        if (!canSendInput()) return
        triggerHaptic()
        lastScancode = "0x" + Integer.toHexString(usageCode).uppercase()
        lastInputLabel = label.ifEmpty { lastScancode }
        scope.launch {
            transport?.sendConsumerClick(usageCode)
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

    fun toggleCapsLock() {
        capsLockState = !capsLockState
        triggerHaptic()
        lastScancode = "0x" + Integer.toHexString(dev.aleian.pockethid.model.HidConstants.KEY_CAPSLOCK.toInt() and 0xFF).uppercase()
        lastInputLabel = if (capsLockState) "CAPS ON" else "CAPS OFF"
        scope.launch {
            transport?.sendKeyClick(dev.aleian.pockethid.model.HidConstants.KEY_CAPSLOCK, 0)
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
        modifier = modifier
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
            onSettingsClick = { showSettingsScreen = true },
            onSearchClick = { showCommandPalette = true },
            isFocusLocked = isFocusLocked,
            onToggleFocusLock = { FocusLockController.toggle() }
        )

        // GLOBAL 3-MODE SWITCHER (KEYBOARD | MOUSE | PRESENTER)
        DeckModeSwitcher(
            selectedMode = selectedTopMode,
            onSelectMode = { targetIndex ->
                val resolved = FocusLockController.resolveModeSwitch(selectedTopMode, targetIndex)
                if (resolved != selectedTopMode) {
                    selectedTopMode = resolved
                    dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.MODE_CHANGED)
                }
            },
            settings = settings,
            isFocusLocked = isFocusLocked
        )

        // ACTIVE DECK CONTENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceContainerLowest)
                .padding(2.dp)
        ) {
            when (selectedTopMode) {
                0 -> {
                    // KEYBOARD COMMAND DECK (Thumb-first 3-Zone Architecture)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // ZONE 1: LEFT THUMB CLUSTER (ESC, TAB, CAPS with hardware LED, QUICK - 11% width)
                        DeckMacroZone(
                            isCapsLockActive = capsLockState,
                            onToggleCapsLock = { toggleCapsLock() },
                            onSendRawKey = { code, mod, lbl -> sendRawKey(code, mod, lbl) },
                            onQuickAction = { showCommandPalette = true },
                            modifier = Modifier.weight(1.1f)
                        )

                        // ZONE 2: CENTRAL DOMINANT KEYBOARD BAY (~78% Screen Width)
                        DeckKeyboardZone(
                            selectedSubMode = selectedKeyboardSubMode,
                            onSelectSubMode = { selectedKeyboardSubMode = it },
                            ctrlState = ctrlState,
                            altState = altState,
                            shiftState = shiftState,
                            superState = superState,
                            isCapsLockActive = capsLockState,
                            onCycleCtrl = { ctrlState = cycleModifier(ctrlState) },
                            onCycleAlt = { altState = cycleModifier(altState) },
                            onCycleShift = { shiftState = cycleModifier(shiftState) },
                            onCycleSuper = { superState = cycleModifier(superState) },
                            onSendRawKey = { code, mod, lbl -> sendRawKey(code, mod, lbl) },
                            onSendConsumerKey = { code, lbl -> sendConsumerKey(code, lbl) },
                            onDispatchAction = { action ->
                                if (!canSendInput()) return@DeckKeyboardZone
                                triggerHaptic()
                                lastInputLabel = action.displayName
                                scope.launch {
                                    transport?.let {
                                        dev.aleian.pockethid.action.ActionDispatcher.dispatch(action, it, settings.hostOs)
                                    }
                                    consumeStickyModifiers()
                                }
                            },
                            hapticsEnabled = settings.keyboardHaptics,
                            modifier = Modifier.weight(7.8f)
                        )

                        // ZONE 3: RIGHT THUMB CLUSTER (Natural 4-Way Arrows, DEL, 123# - 11% width)
                        DeckUtilityZone(
                            hapticsEnabled = settings.keyboardHaptics,
                            onSendRawKey = { code, mod, lbl -> sendRawKey(code, mod, lbl) },
                            onSwitchNumpad = { selectedKeyboardSubMode = 5 },
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }

                1 -> {
                    // MOUSE COMMAND DECK (Dominant Precision Multi-Touch Trackpad + Explicit Bottom Controls)
                    MultiTouchTrackpad(
                        transport = transport,
                        settings = settings,
                        canSendInput = { canSendInput() },
                        onOpenCommandDeck = { showCommandPalette = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                2 -> {
                    // GAMEPAD COMMAND DECK
                    GamepadScreen(
                        transport = transport,
                        connectionState = connState,
                        onOpenDiagnostics = { showDiagnostics = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                3 -> {
                    // PRESENTER COMMAND DECK
                    PresenterScreen(
                        transport = transport,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                4 -> {
                    // ONE-HAND REMOTE COMMAND DECK
                    OneHandScreen(
                        transport = transport,
                        connectionState = connState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                5 -> {
                    // ELECTRONIC GRAPHICS TABLET
                    DrawingScreen(
                        controller = drawingController,
                        transport = transport,
                        hostOs = settings.hostOs,
                        isFocusLocked = isFocusLocked,
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
            transport = transport,
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

    // Command Palette Modal
    if (showCommandPalette) {
        CommandPaletteSheet(
            transport = transport,
            onDismiss = { showCommandPalette = false }
        )
    }
}
