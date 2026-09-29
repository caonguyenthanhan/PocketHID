package dev.aleian.pockethid.ui.screens

import android.bluetooth.BluetoothDevice
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.FocusLockController
import dev.aleian.pockethid.model.PocketStrings
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.TopCommandBar
import dev.aleian.pockethid.ui.theme.AccentAmber
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.ErrorContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

private val CyanAccent = Color(0xFF00E5FF)

@Composable
fun MainScreen(
    transport: InputTransport?,
    pairedDevices: List<BluetoothDevice>,
    onConnectToDevice: (BluetoothDevice) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onToggleOrientation: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settings by SettingsRepository.settings.collectAsState()
    val drawingController = remember { dev.aleian.pockethid.drawing.DrawingController() }

    // When the phone is turned horizontally, launch the Landscape Command Deck
    if (isLandscape) {
        LandscapeDeckScreen(
            transport = transport,
            pairedDevices = pairedDevices,
            onConnectToDevice = onConnectToDevice,
            onMakeDiscoverable = onMakeDiscoverable,
            onExitLandscape = onToggleOrientation,
            onToggleOrientation = onToggleOrientation,
            drawingController = drawingController
        )
        return
    }

    val connectionState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) })

    val snackbarHostState = remember { SnackbarHostState() }
    var previousState by remember { mutableStateOf<ConnectionState?>(null) }

    // Automatic push notification feedback via Snackbar
    LaunchedEffect(connectionState) {
        val prev = previousState
        previousState = connectionState

        if (prev != null && prev != connectionState) {
            when (val current = connectionState) {
                is ConnectionState.Connected -> {
                    val name = current.device.name ?: current.device.address
                    dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.CONNECTED)
                    snackbarHostState.showSnackbar("Connected to $name. Ready to control.")
                }
                is ConnectionState.Error -> {
                    dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.ERROR)
                    snackbarHostState.showSnackbar("Error: ${current.message}", duration = SnackbarDuration.Long)
                }
                is ConnectionState.Disconnected -> {
                    FocusLockController.reset()
                    if (prev is ConnectionState.Connected) {
                        dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.DISCONNECTED)
                        val name = (prev as ConnectionState.Connected).device.name ?: "Host"
                        snackbarHostState.showSnackbar("Disconnected from $name.")
                    } else if (prev is ConnectionState.Connecting) {
                        dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.ERROR)
                        snackbarHostState.showSnackbar("Could not connect. Please check host Bluetooth.")
                    }
                }
                is ConnectionState.Connecting -> {
                    val name = current.device?.name ?: "Host"
                    snackbarHostState.showSnackbar("Connecting to $name…")
                }
                is ConnectionState.Disconnecting -> {}
            }
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Keyboard, 1: Mouse, 2: Gamepad, 3: Presenter, 4: One-Hand, 5: Draw
    
    LaunchedEffect(settings) {
        val isCurrentTabVisible = when (selectedTab) {
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
            
            val resolved = FocusLockController.resolveModeSwitch(selectedTab, firstVisible)
            selectedTab = resolved
        }
    }

    var isDrawModesExpanded by rememberSaveable { mutableStateOf(false) }
    val isFocusLocked by FocusLockController.isLocked.collectAsState()
    var showPairingSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showDiagnosticsSheet by rememberSaveable { mutableStateOf(false) }
    var showCommandPalette by rememberSaveable { mutableStateOf(false) }

    if (showSettingsScreen) {
        SettingsScreen(
            transport = transport,
            onBack = { showSettingsScreen = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = DarkBg,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(data.visuals.message, fontSize = 13.sp)
                }
            }
        },
        topBar = {
            Column {
                TopCommandBar(
                    connectionState = connectionState,
                    hapticEnabled = settings.keyboardHaptics,
                    onRotateClick = onToggleOrientation,
                    onHostClick = { showPairingSheet = true },
                    onToggleHaptic = {
                        SettingsRepository.updateSettings(
                            settings.copy(keyboardHaptics = !settings.keyboardHaptics)
                        )
                    },
                    onDiagnosticsClick = { showDiagnosticsSheet = true },
                    onSettingsClick = { showSettingsScreen = true },
                    onSearchClick = { showCommandPalette = true },
                    isFocusLocked = isFocusLocked,
                    onToggleFocusLock = { FocusLockController.toggle() }
                )

                // Inline Contextual Alert Banners
                AnimatedVisibility(
                    visible = connectionState !is ConnectionState.Connected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (val state = connectionState) {
                        is ConnectionState.Disconnected -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { showPairingSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "No host connected. Tap to select a host.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "Connect",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        is ConnectionState.Connecting -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connecting to ${state.device?.name ?: "Host"}…",
                                        fontSize = 11.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        is ConnectionState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ErrorContainer.copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { showPairingSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = state.message,
                                            fontSize = 11.sp,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    Text(
                                        text = "Retry",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab == 5 && !isDrawModesExpanded) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable {
                            if (!isFocusLocked) {
                                isDrawModesExpanded = true
                            }
                        },
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "DRAW TABLET (FULL SCREEN)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        if (isFocusLocked) {
                            Text(
                                text = "🔒 FOCUS",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "MODES ⌃",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                val showNavLabels = configuration.screenWidthDp >= 360
                val onSelectTab: (Int) -> Unit = { targetIndex ->
                    val resolved = FocusLockController.resolveModeSwitch(selectedTab, targetIndex)
                    if (resolved != selectedTab) {
                        selectedTab = resolved
                        dev.aleian.pockethid.audio.AudioFeedbackManager.play(dev.aleian.pockethid.audio.AudioEvent.MODE_CHANGED)
                    }
                    isDrawModesExpanded = false
                }

                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        unselectedIconColor = if (isFocusLocked) TextMuted.copy(alpha = 0.4f) else TextMuted,
                        unselectedTextColor = if (isFocusLocked) TextMuted.copy(alpha = 0.4f) else TextMuted,
                        indicatorColor = CyanAccent.copy(alpha = 0.16f)
                    )

                    if (settings.modeKeyboardVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(0) },
                            icon = { Icon(Icons.Default.Keyboard, contentDescription = "Keyboard") },
                            label = {
                                Text(
                                    text = PocketStrings.navKeyboard(settings.language),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                    if (settings.modeMouseVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(1) },
                            icon = { Icon(Icons.Default.Mouse, contentDescription = "Mouse") },
                            label = {
                                Text(
                                    text = PocketStrings.navMouse(settings.language),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                    if (settings.modeGamepadVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(2) },
                            icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Gamepad") },
                            label = {
                                Text(
                                    text = PocketStrings.navGamepad(settings.language),
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                    if (settings.modePresenterVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(3) },
                            icon = { Icon(Icons.Default.Slideshow, contentDescription = "Presenter") },
                            label = {
                                Text(
                                    text = PocketStrings.navPresenter(settings.language),
                                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                    if (settings.modeOneHandVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(4) },
                            icon = { Icon(Icons.Default.TouchApp, contentDescription = "1-Hand") },
                            label = {
                                Text(
                                    text = PocketStrings.navOneHand(settings.language),
                                    fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                    if (settings.modeDrawVisible) {
                        NavigationBarItem(
                            selected = selectedTab == 5,
                            alwaysShowLabel = showNavLabels,
                            onClick = { onSelectTab(5) },
                            icon = { Icon(Icons.Default.Gesture, contentDescription = "Draw") },
                            label = {
                                Text(
                                    text = PocketStrings.navDraw(settings.language),
                                    fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = navItemColors
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> KeyboardScreen(
                    transport = transport,
                    connectionState = connectionState
                )
                1 -> MouseScreen(
                    transport = transport,
                    connectionState = connectionState,
                    onPairClick = { showPairingSheet = true },
                    onHostInfoClick = { showDiagnosticsSheet = true },
                    onSettingsClick = { showSettingsScreen = true }
                )
                2 -> GamepadScreen(
                    transport = transport,
                    connectionState = connectionState,
                    onOpenDiagnostics = { showDiagnosticsSheet = true }
                )
                3 -> PresenterScreen(transport = transport)
                4 -> OneHandScreen(
                    transport = transport,
                    connectionState = connectionState
                )
                5 -> DrawingScreen(
                    controller = drawingController,
                    transport = transport,
                    hostOs = settings.hostOs,
                    isFocusLocked = isFocusLocked
                )
            }
        }

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

        if (showDiagnosticsSheet) {
            DiagnosticsSheet(
                connectionState = connectionState,
                transport = transport,
                onDismiss = { showDiagnosticsSheet = false }
            )
        }

        if (showCommandPalette) {
            CommandPaletteSheet(
                transport = transport,
                onDismiss = { showCommandPalette = false }
            )
        }
    }
}
