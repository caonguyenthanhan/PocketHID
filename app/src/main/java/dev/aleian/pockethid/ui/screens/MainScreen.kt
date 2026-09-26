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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
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

    // When the phone is turned horizontally, launch the Landscape Command Deck
    if (isLandscape) {
        LandscapeDeckScreen(
            transport = transport,
            pairedDevices = pairedDevices,
            onConnectToDevice = onConnectToDevice,
            onMakeDiscoverable = onMakeDiscoverable,
            onExitLandscape = onToggleOrientation,
            onToggleOrientation = onToggleOrientation
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
                    snackbarHostState.showSnackbar("Connected to $name. Ready to control.")
                }
                is ConnectionState.Error -> {
                    snackbarHostState.showSnackbar("Error: ${current.message}", duration = SnackbarDuration.Long)
                }
                is ConnectionState.Disconnected -> {
                    if (prev is ConnectionState.Connected) {
                        val name = (prev as ConnectionState.Connected).device.name ?: "Host"
                        snackbarHostState.showSnackbar("Disconnected from $name.")
                    } else if (prev is ConnectionState.Connecting) {
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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Keyboard, 1: Mouse, 2: Gamepad, 3: Presenter
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
                    onSearchClick = { showCommandPalette = true }
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
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = "Keyboard") },
                    label = { Text("Keyboard", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Mouse, contentDescription = "Mouse") },
                    label = { Text("Mouse", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Gamepad") },
                    label = { Text("Gamepad", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Slideshow, contentDescription = "Presenter") },
                    label = { Text("Presenter", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = "One-Hand") },
                    label = { Text("One-Hand", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
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
