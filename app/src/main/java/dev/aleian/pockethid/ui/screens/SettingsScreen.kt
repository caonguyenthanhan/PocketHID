package dev.aleian.pockethid.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.AppLanguage
import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.PocketStrings
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.CyanAccent
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.OnPrimaryContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.PrimaryContainer
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainerHigh
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    transport: InputTransport?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings by SettingsRepository.settings.collectAsState()
    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) })

    var pollingRate by remember(currentSettings) { mutableFloatStateOf(currentSettings.pollingRate) }
    var deadZone by remember(currentSettings) { mutableIntStateOf(currentSettings.deadZonePx) }
    var accelFactor by remember(currentSettings) { mutableFloatStateOf(currentSettings.accelerationFactor) }
    var hapticTrackpad by remember(currentSettings) { mutableStateOf(currentSettings.hapticsTrackpad) }
    var hapticIntensity by remember(currentSettings) { mutableStateOf(currentSettings.hapticIntensity) }
    var naturalScroll by remember(currentSettings) { mutableStateOf(currentSettings.naturalScroll) }
    var dragLock by remember(currentSettings) { mutableStateOf(currentSettings.dragLock) }
    var pasteDelay by remember(currentSettings) { mutableLongStateOf(currentSettings.pasteDelayMs) }
    var keyboardHaptics by remember(currentSettings) { mutableStateOf(currentSettings.keyboardHaptics) }
    var keepAwake by remember(currentSettings) { mutableStateOf(currentSettings.keepAwakeTimeout) }
    var gpLeftDeadzone by remember(currentSettings) { mutableFloatStateOf(currentSettings.gamepadLeftDeadzone) }
    var gpLeftSens by remember(currentSettings) { mutableFloatStateOf(currentSettings.gamepadLeftSensitivity) }
    var gpLeftCurve by remember(currentSettings) { mutableStateOf(currentSettings.gamepadLeftCurve) }
    var gpRightDeadzone by remember(currentSettings) { mutableFloatStateOf(currentSettings.gamepadRightDeadzone) }
    var gpRightSens by remember(currentSettings) { mutableFloatStateOf(currentSettings.gamepadRightSensitivity) }
    var gpRightCurve by remember(currentSettings) { mutableStateOf(currentSettings.gamepadRightCurve) }
    var gpTriggerSens by remember(currentSettings) { mutableFloatStateOf(currentSettings.gamepadTriggerSensitivity) }
    var gpInvertY by remember(currentSettings) { mutableStateOf(currentSettings.gamepadInvertY) }
    var pinchZoomEnabled by remember(currentSettings) { mutableStateOf(currentSettings.pinchZoomEnabled) }
    var pinchThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.pinchThresholdPx) }
    var zoomMode by remember(currentSettings) { mutableStateOf(currentSettings.zoomMode) }
    var keepScreenAwake by remember(currentSettings) { mutableStateOf(currentSettings.keepScreenAwake) }
    var screenSleepTimeoutMinutes by remember(currentSettings) { mutableIntStateOf(currentSettings.screenSleepTimeoutMinutes) }
    var oneHandHandedness by remember(currentSettings) { mutableStateOf(currentSettings.oneHandHandedness) }
    var oneHandSeekStep by remember(currentSettings) { mutableIntStateOf(currentSettings.oneHandSeekStepSeconds) }
    var oneHandScrollSens by remember(currentSettings) { mutableFloatStateOf(currentSettings.oneHandScrollSensitivity) }
    var fastScrollEnabled by remember(currentSettings) { mutableStateOf(currentSettings.fastScrollEnabled) }
    var fastScrollMultiplier by remember(currentSettings) { mutableFloatStateOf(currentSettings.fastScrollMultiplier) }
    var appLanguage by remember(currentSettings) { mutableStateOf(currentSettings.language) }

    var modeKeyboardVisible by remember(currentSettings) { mutableStateOf(currentSettings.modeKeyboardVisible) }
    var modeMouseVisible by remember(currentSettings) { mutableStateOf(currentSettings.modeMouseVisible) }
    var modeGamepadVisible by remember(currentSettings) { mutableStateOf(currentSettings.modeGamepadVisible) }
    var modePresenterVisible by remember(currentSettings) { mutableStateOf(currentSettings.modePresenterVisible) }
    var modeOneHandVisible by remember(currentSettings) { mutableStateOf(currentSettings.modeOneHandVisible) }
    var modeDrawVisible by remember(currentSettings) { mutableStateOf(currentSettings.modeDrawVisible) }

    val visibleModesCount = listOf(
        modeKeyboardVisible, modeMouseVisible, modeGamepadVisible,
        modePresenterVisible, modeOneHandVisible, modeDrawVisible
    ).count { it }

    val handleModeVisibilityChange: (Boolean, (Boolean) -> Unit) -> Unit = { newValue, setter ->
        if (!newValue && visibleModesCount <= 3) {
            val msg = if (appLanguage == AppLanguage.VIETNAMESE) "Cần ít nhất 3 chế độ!" else "At least 3 modes must be visible!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            setter(newValue)
        }
    }

    val hostName = when (connState) {
        is ConnectionState.Connected -> (connState as ConnectionState.Connected).device.name ?: "BT-HID-HOST"
        else -> "WIN-11-PRO-DESK"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = PocketStrings.settingsTitle(appLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "PocketHID Live Tuner",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = PrimaryBlue
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = StatusConnected,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Tuner",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryBlue
                    )
                }
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Telemetry Banner
            SettingsTelemetryBanner(hostName = hostName, pollingRate = pollingRate)

            // Language Selection Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = PocketStrings.settingsLanguage(appLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (appLanguage == AppLanguage.ENGLISH) "English (Active)" else "Tiếng Việt (Đang bật)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (appLanguage == AppLanguage.ENGLISH) CyanAccent.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .border(1.dp, if (appLanguage == AppLanguage.ENGLISH) CyanAccent else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { appLanguage = AppLanguage.ENGLISH }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appLanguage == AppLanguage.ENGLISH) CyanAccent else TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (appLanguage == AppLanguage.VIETNAMESE) CyanAccent.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .border(1.dp, if (appLanguage == AppLanguage.VIETNAMESE) CyanAccent else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { appLanguage = AppLanguage.VIETNAMESE }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "VI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appLanguage == AppLanguage.VIETNAMESE) CyanAccent else TextMuted
                            )
                        }
                    }
                }
            }

            // Section 1: Mouse & Trackpad Engine
            MouseTrackpadSettingsSection(
                pollingRate = pollingRate,
                onPollingRateChange = { pollingRate = it },
                deadZone = deadZone,
                onDeadZoneChange = { deadZone = it },
                accelFactor = accelFactor,
                onAccelFactorChange = { accelFactor = it },
                hapticTrackpad = hapticTrackpad,
                onHapticTrackpadChange = { hapticTrackpad = it },
                hapticIntensity = hapticIntensity,
                onHapticIntensityChange = { hapticIntensity = it },
                naturalScroll = naturalScroll,
                onNaturalScrollChange = { naturalScroll = it },
                dragLock = dragLock,
                onDragLockChange = { dragLock = it },
                pinchZoomEnabled = pinchZoomEnabled,
                onPinchZoomEnabledChange = { pinchZoomEnabled = it },
                pinchThreshold = pinchThreshold,
                onPinchThresholdChange = { pinchThreshold = it },
                zoomMode = zoomMode,
                onZoomModeChange = { zoomMode = it },
                fastScrollEnabled = fastScrollEnabled,
                onFastScrollEnabledChange = { fastScrollEnabled = it },
                fastScrollMultiplier = fastScrollMultiplier,
                onFastScrollMultiplierChange = { fastScrollMultiplier = it },
                language = appLanguage
            )

            // Section 2: Keyboard & Scancode Engine
            KeyboardSettingsSection(
                pasteDelay = pasteDelay,
                onPasteDelayChange = { pasteDelay = it },
                keyboardHaptics = keyboardHaptics,
                onKeyboardHapticsChange = { keyboardHaptics = it },
                language = appLanguage
            )

            // Section 3: Gamepad Calibration
            GamepadSettingsSection(
                gpLeftDeadzone = gpLeftDeadzone,
                onGpLeftDeadzoneChange = { gpLeftDeadzone = it },
                gpLeftSens = gpLeftSens,
                onGpLeftSensChange = { gpLeftSens = it },
                gpLeftCurve = gpLeftCurve,
                onGpLeftCurveChange = { gpLeftCurve = it },
                gpRightDeadzone = gpRightDeadzone,
                onGpRightDeadzoneChange = { gpRightDeadzone = it },
                gpRightSens = gpRightSens,
                onGpRightSensChange = { gpRightSens = it },
                gpRightCurve = gpRightCurve,
                onGpRightCurveChange = { gpRightCurve = it },
                gpTriggerSens = gpTriggerSens,
                onGpTriggerSensChange = { gpTriggerSens = it },
                gpInvertY = gpInvertY,
                onGpInvertYChange = { gpInvertY = it },
                language = appLanguage
            )

            // Section 4: One-Hand Remote Control
            OneHandSettingsSection(
                oneHandHandedness = oneHandHandedness,
                onOneHandHandednessChange = { oneHandHandedness = it },
                oneHandSeekStep = oneHandSeekStep,
                onOneHandSeekStepChange = { oneHandSeekStep = it },
                oneHandScrollSens = oneHandScrollSens,
                onOneHandScrollSensChange = { oneHandScrollSens = it },
                language = appLanguage
            )

            // Section 5: Screen Behavior & Power
            ScreenBehaviorSettingsSection(
                keepScreenAwake = keepScreenAwake,
                onKeepScreenAwakeChange = { keepScreenAwake = it },
                screenSleepTimeoutMinutes = screenSleepTimeoutMinutes,
                onScreenSleepTimeoutMinutesChange = { screenSleepTimeoutMinutes = it },
                language = appLanguage
            )

            // Section 6: Visible Modes
            VisibleModesSection(
                modeKeyboardVisible = modeKeyboardVisible,
                onModeKeyboardVisibleChange = { handleModeVisibilityChange(it) { modeKeyboardVisible = it } },
                modeMouseVisible = modeMouseVisible,
                onModeMouseVisibleChange = { handleModeVisibilityChange(it) { modeMouseVisible = it } },
                modeGamepadVisible = modeGamepadVisible,
                onModeGamepadVisibleChange = { handleModeVisibilityChange(it) { modeGamepadVisible = it } },
                modePresenterVisible = modePresenterVisible,
                onModePresenterVisibleChange = { handleModeVisibilityChange(it) { modePresenterVisible = it } },
                modeOneHandVisible = modeOneHandVisible,
                onModeOneHandVisibleChange = { handleModeVisibilityChange(it) { modeOneHandVisible = it } },
                modeDrawVisible = modeDrawVisible,
                onModeDrawVisibleChange = { handleModeVisibilityChange(it) { modeDrawVisible = it } },
                language = appLanguage
            )

            // Quick Diagnostics Summary Box
            DiagnosticsSummarySection(
                hostName = hostName,
                language = appLanguage
            )

            // Action Buttons (Save / Reset)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val updated = AppSettings(
                            pollingRate = pollingRate,
                            deadZonePx = deadZone,
                            accelerationFactor = accelFactor,
                            hapticsTrackpad = hapticTrackpad,
                            hapticIntensity = hapticIntensity,
                            naturalScroll = naturalScroll,
                            dragLock = dragLock,
                            pasteDelayMs = pasteDelay,
                            keyboardHaptics = keyboardHaptics,
                            keepAwakeTimeout = keepAwake,
                            gamepadLeftDeadzone = gpLeftDeadzone,
                            gamepadLeftSensitivity = gpLeftSens,
                            gamepadLeftCurve = gpLeftCurve,
                            gamepadRightDeadzone = gpRightDeadzone,
                            gamepadRightSensitivity = gpRightSens,
                            gamepadRightCurve = gpRightCurve,
                            gamepadTriggerSensitivity = gpTriggerSens,
                            gamepadInvertY = gpInvertY,
                            pinchZoomEnabled = pinchZoomEnabled,
                            pinchThresholdPx = pinchThreshold,
                            zoomMode = zoomMode,
                            keepScreenAwake = keepScreenAwake,
                            screenSleepTimeoutMinutes = screenSleepTimeoutMinutes,
                            oneHandHandedness = oneHandHandedness,
                            oneHandSeekStepSeconds = oneHandSeekStep,
                            oneHandScrollSensitivity = oneHandScrollSens,
                            fastScrollEnabled = fastScrollEnabled,
                            fastScrollMultiplier = fastScrollMultiplier,
                            language = appLanguage,
                            modeKeyboardVisible = modeKeyboardVisible,
                            modeMouseVisible = modeMouseVisible,
                            modeGamepadVisible = modeGamepadVisible,
                            modePresenterVisible = modePresenterVisible,
                            modeOneHandVisible = modeOneHandVisible,
                            modeDrawVisible = modeDrawVisible
                        )
                        SettingsRepository.updateSettings(updated)
                        val msg = if (appLanguage == AppLanguage.VIETNAMESE) "Đã lưu cài đặt!" else "Settings saved successfully!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = OnPrimaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(PocketStrings.settingsSave(appLanguage), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        SettingsRepository.resetToDefaults()
                        pollingRate = 125f
                        deadZone = 1
                        accelFactor = 1.5f
                        hapticTrackpad = true
                        hapticIntensity = "Medium"
                        naturalScroll = false
                        dragLock = true
                        pasteDelay = 15L
                        keyboardHaptics = true
                        keepAwake = "10 Minutes"
                        appLanguage = AppLanguage.ENGLISH
                        gpLeftDeadzone = 0.10f
                        gpLeftSens = 1.0f
                        gpLeftCurve = "Linear"
                        gpRightDeadzone = 0.10f
                        gpRightSens = 1.0f
                        gpRightCurve = "Linear"
                        gpTriggerSens = 1.0f
                        gpInvertY = false
                        pinchZoomEnabled = true
                        pinchThreshold = 35.0f
                        zoomMode = "Wheel"
                        keepScreenAwake = false
                        screenSleepTimeoutMinutes = 10
                        oneHandHandedness = "Right"
                        oneHandSeekStep = 10
                        oneHandScrollSens = 1.0f
                        fastScrollEnabled = true
                        fastScrollMultiplier = 2.5f
                        modeKeyboardVisible = true
                        modeMouseVisible = true
                        modeGamepadVisible = false
                        modePresenterVisible = true
                        modeOneHandVisible = true
                        modeDrawVisible = false
                        val resetMsg = if (appLanguage == AppLanguage.VIETNAMESE) "Đã khôi phục cài đặt mặc định." else "Reset to default settings."
                        Toast.makeText(context, resetMsg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(PocketStrings.settingsReset(appLanguage))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
