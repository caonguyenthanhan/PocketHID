package dev.aleian.pockethid.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.aleian.pockethid.gamepad.GamepadController
import dev.aleian.pockethid.gamepad.StickResponseCurve
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.PocketStrings
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.VirtualDpad
import dev.aleian.pockethid.ui.components.VirtualFaceButtons
import dev.aleian.pockethid.ui.components.VirtualShoulderCluster
import dev.aleian.pockethid.ui.components.VirtualSystemButtons
import dev.aleian.pockethid.ui.components.VirtualThumbstick
import dev.aleian.pockethid.ui.theme.CyanAccent
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusConnecting
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

/**
 * PocketHID Gamepad / Controller Screen.
 * Exposes a standard Bluetooth HID Gamepad (Report ID 4) to the host PC.
 * Optimized for low-latency, two-thumb ergonomic operation with multi-touch.
 */
@Composable
fun GamepadScreen(
    transport: InputTransport?,
    connectionState: ConnectionState = ConnectionState.Disconnected,
    onOpenDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settings by SettingsRepository.settings.collectAsState()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember(transport) { GamepadController(transport) }

    // Screen Transition Safety: Clean neutral zero report on dispose
    DisposableEffect(controller) {
        onDispose {
            controller.resetNeutral()
        }
    }

    // Lifecycle Safety: Send neutral report on pause/background/stop
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                controller.resetNeutral()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val leftCurve = remember(settings.gamepadLeftCurve) {
        StickResponseCurve.fromString(settings.gamepadLeftCurve)
    }
    val rightCurve = remember(settings.gamepadRightCurve) {
        StickResponseCurve.fromString(settings.gamepadRightCurve)
    }

    if (isLandscape) {
        LandscapeGamepadLayout(
            controller = controller,
            connectionState = connectionState,
            leftDeadzone = settings.gamepadLeftDeadzone,
            leftSensitivity = settings.gamepadLeftSensitivity,
            leftCurve = leftCurve,
            rightDeadzone = settings.gamepadRightDeadzone,
            rightSensitivity = settings.gamepadRightSensitivity,
            rightCurve = rightCurve,
            triggerSensitivity = settings.gamepadTriggerSensitivity,
            invertY = settings.gamepadInvertY,
            onOpenDiagnostics = onOpenDiagnostics,
            modifier = modifier
        )
    } else {
        PortraitGamepadLayout(
            controller = controller,
            settings = settings,
            connectionState = connectionState,
            leftDeadzone = settings.gamepadLeftDeadzone,
            leftSensitivity = settings.gamepadLeftSensitivity,
            leftCurve = leftCurve,
            rightDeadzone = settings.gamepadRightDeadzone,
            rightSensitivity = settings.gamepadRightSensitivity,
            rightCurve = rightCurve,
            triggerSensitivity = settings.gamepadTriggerSensitivity,
            invertY = settings.gamepadInvertY,
            onOpenDiagnostics = onOpenDiagnostics,
            modifier = modifier
        )
    }
}

/**
 * Landscape-first, two-thumb ergonomic Gamepad controller layout.
 */
@Composable
private fun LandscapeGamepadLayout(
    controller: GamepadController,
    connectionState: ConnectionState,
    leftDeadzone: Float,
    leftSensitivity: Float,
    leftCurve: StickResponseCurve,
    rightDeadzone: Float,
    rightSensitivity: Float,
    rightCurve: StickResponseCurve,
    triggerSensitivity: Float,
    invertY: Boolean,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP CONTROL BAR: Shoulders + Status + Triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VirtualShoulderCluster(
                triggerSensitivity = triggerSensitivity,
                onPressButton = { mask -> controller.pressButton(mask, view) },
                onReleaseButton = { mask -> controller.releaseButton(mask) },
                onLeftTriggerChange = { valByte -> controller.updateLeftTrigger(valByte) },
                onRightTriggerChange = { valByte -> controller.updateRightTrigger(valByte) },
                modifier = Modifier.weight(1f)
            )

            // Diagnostics HUD Icon
            IconButton(
                onClick = onOpenDiagnostics,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = "Diagnostics",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // MAIN DUAL-THUMB WINGS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT THUMB CLUSTER (Left Analog Stick + D-Pad)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VirtualThumbstick(
                    label = "LS",
                    deadzone = leftDeadzone,
                    sensitivity = leftSensitivity,
                    curve = leftCurve,
                    invertY = invertY,
                    size = 145.dp,
                    onStickMove = { x, y -> controller.updateLeftStick(x, y) }
                )

                VirtualDpad(
                    size = 135.dp,
                    onDirectionChange = { u, d, l, r ->
                        controller.setDpadDirection(u, d, l, r, view)
                    }
                )
            }

            // CENTER COLUMN: System Buttons & Controller Status
            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Status Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusColor, statusText) = when (connectionState) {
                        is ConnectionState.Connected -> Pair(StatusConnected, "CONNECTED")
                        is ConnectionState.Connecting -> Pair(StatusConnecting, "CONNECTING")
                        else -> Pair(StatusDisconnected, "DISCONNECTED")
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // System & Auxiliary Buttons
                VirtualSystemButtons(
                    onPress = { mask -> controller.pressButton(mask, view) },
                    onRelease = { mask -> controller.releaseButton(mask) }
                )

                Text(
                    text = "HID GAMEPAD",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            // RIGHT THUMB CLUSTER (Face Buttons ABXY + Right Analog Stick)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VirtualThumbstick(
                    label = "RS",
                    deadzone = rightDeadzone,
                    sensitivity = rightSensitivity,
                    curve = rightCurve,
                    invertY = invertY,
                    size = 145.dp,
                    onStickMove = { x, y -> controller.updateRightStick(x, y) }
                )

                VirtualFaceButtons(
                    size = 145.dp,
                    buttonSize = 44.dp,
                    onPress = { mask -> controller.pressButton(mask, view) },
                    onRelease = { mask -> controller.releaseButton(mask) }
                )
            }
        }
    }
}

/**
 * Portrait fallback layout for Gamepad controller.
 */
@Composable
private fun PortraitGamepadLayout(
    controller: GamepadController,
    settings: dev.aleian.pockethid.model.AppSettings,
    connectionState: ConnectionState,
    leftDeadzone: Float,
    leftSensitivity: Float,
    leftCurve: StickResponseCurve,
    rightDeadzone: Float,
    rightSensitivity: Float,
    rightCurve: StickResponseCurve,
    triggerSensitivity: Float,
    invertY: Boolean,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP 1: Landscape Recommendation Guidance Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = PocketStrings.gamepadLandscapeHint(settings.language),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onOpenDiagnostics, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = "Diagnostics",
                        tint = CyanAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // TOP 2: Shoulder & Trigger Bar
        VirtualShoulderCluster(
            triggerSensitivity = triggerSensitivity,
            onPressButton = { mask -> controller.pressButton(mask, view) },
            onReleaseButton = { mask -> controller.releaseButton(mask) },
            onLeftTriggerChange = { valByte -> controller.updateLeftTrigger(valByte) },
            onRightTriggerChange = { valByte -> controller.updateRightTrigger(valByte) }
        )

        // MIDDLE: D-PAD (Left) & ABXY (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VirtualDpad(
                size = 125.dp,
                onDirectionChange = { u, d, l, r ->
                    controller.setDpadDirection(u, d, l, r, view)
                }
            )

            VirtualFaceButtons(
                size = 125.dp,
                buttonSize = 38.dp,
                onPress = { mask -> controller.pressButton(mask, view) },
                onRelease = { mask -> controller.releaseButton(mask) }
            )
        }

        // LOWER: DUAL ANALOG STICKS (Primary thumb touch area)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VirtualThumbstick(
                label = "LS",
                deadzone = leftDeadzone,
                sensitivity = leftSensitivity,
                curve = leftCurve,
                invertY = invertY,
                size = 125.dp,
                onStickMove = { x, y -> controller.updateLeftStick(x, y) }
            )

            VirtualThumbstick(
                label = "RS",
                deadzone = rightDeadzone,
                sensitivity = rightSensitivity,
                curve = rightCurve,
                invertY = invertY,
                size = 125.dp,
                onStickMove = { x, y -> controller.updateRightStick(x, y) }
            )
        }

        // BOTTOM: SYSTEM BUTTONS (Back, Guide, Start)
        VirtualSystemButtons(
            onPress = { mask -> controller.pressButton(mask, view) },
            onRelease = { mask -> controller.releaseButton(mask) }
        )
    }
}
