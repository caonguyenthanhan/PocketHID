package dev.aleian.pockethid.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.ui.theme.AccentAmber
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.ErrorContainer
import dev.aleian.pockethid.ui.theme.OnPrimaryContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.PrimaryContainer
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainer
import dev.aleian.pockethid.ui.theme.SurfaceContainerHigh
import dev.aleian.pockethid.ui.theme.SurfaceContainerLow
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import dev.aleian.pockethid.ui.theme.SurfaceRaised
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import dev.aleian.pockethid.model.AppLanguage
import java.util.Locale

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
    }
}

@Composable
fun SegmentButton(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryContainer else SurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) OnPrimaryContainer else TextSecondary
            )
            Text(
                text = subLabel,
                fontSize = 8.sp,
                color = if (isSelected) OnPrimaryContainer.copy(alpha = 0.8f) else TextMuted
            )
        }
    }
}

@Composable
fun SettingsTelemetryBanner(
    hostName: String,
    pollingRate: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusConnected)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HOST: $hostName".uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "BT HID Profile v1.1 • Classic ACL",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LATENCY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Text("8.0 ms", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TICK RATE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Text("${pollingRate.toInt()} Hz", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DRAIN BUDGET", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Text("~3.2%/h", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = StatusConnected)
                }
            }
        }
    }
}

@Composable
fun MouseTrackpadSettingsSection(
    pollingRate: Float,
    onPollingRateChange: (Float) -> Unit,
    deadZone: Int,
    onDeadZoneChange: (Int) -> Unit,
    accelFactor: Float,
    onAccelFactorChange: (Float) -> Unit,
    hapticTrackpad: Boolean,
    onHapticTrackpadChange: (Boolean) -> Unit,
    hapticIntensity: String,
    onHapticIntensityChange: (String) -> Unit,
    naturalScroll: Boolean,
    onNaturalScrollChange: (Boolean) -> Unit,
    dragLock: Boolean,
    onDragLockChange: (Boolean) -> Unit,
    pinchZoomEnabled: Boolean,
    onPinchZoomEnabledChange: (Boolean) -> Unit,
    pinchThreshold: Float,
    onPinchThresholdChange: (Float) -> Unit,
    zoomMode: String,
    onZoomModeChange: (String) -> Unit,
    fastScrollEnabled: Boolean,
    onFastScrollEnabledChange: (Boolean) -> Unit,
    fastScrollMultiplier: Float,
    onFastScrollMultiplierChange: (Float) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.Mouse,
        title = if (language == AppLanguage.VIETNAMESE) "Cấu hình Chuột & Bàn rê" else "Mouse & Trackpad Engine",
        subtitle = "Subsystem 0x01"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Polling Rate
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Tần số lấy mẫu (Polling Rate)" else "Polling Rate",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text("${pollingRate.toInt()} Hz", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentButton(
                        label = "62.5 Hz",
                        subLabel = "16ms Low-Pwr",
                        isSelected = pollingRate == 62.5f,
                        modifier = Modifier.weight(1f)
                    ) { onPollingRateChange(62.5f) }

                    SegmentButton(
                        label = "125 Hz",
                        subLabel = "8ms PRD Std",
                        isSelected = pollingRate == 125f,
                        modifier = Modifier.weight(1f)
                    ) { onPollingRateChange(125f) }

                    SegmentButton(
                        label = "250 Hz",
                        subLabel = "4ms Turbo",
                        isSelected = pollingRate == 250f,
                        modifier = Modifier.weight(1f)
                    ) { onPollingRateChange(250f) }
                }
            }

            // Deadzone
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Dead Zone chống trôi Tap-Click" else "Tap-Click Anti-Drift Deadzone",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Chặn rung vi mô trong 30ms đầu sau touch-down" else "Suppresses micro-jitter in first 30ms after touchdown",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text("$deadZone px", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Slider(
                    value = deadZone.toFloat(),
                    onValueChange = { onDeadZoneChange(it.toInt()) },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = SurfaceRaised
                    )
                )
            }

            // Acceleration Factor
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Gia tốc phi tuyến (Acceleration Curve)" else "Non-linear Acceleration Curve",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "k = ${String.format(Locale.US, "%.1f", accelFactor)}×",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Slider(
                    value = accelFactor,
                    onValueChange = { onAccelFactorChange(Math.round(it * 10f) / 10f) },
                    valueRange = 0.5f..3.0f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = SurfaceRaised
                    )
                )
            }

            // Haptics & Intensity
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Phản hồi xúc giác Trackpad" else "Trackpad Haptic Feedback",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Rung ERM/LRA khi click & drag-lock" else "ERM/LRA haptics on click & drag-lock",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = hapticTrackpad,
                        onCheckedChange = onHapticTrackpadChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryContainer
                        )
                    )
                }
            }

            // Natural Scroll & Drag Lock Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Cuộn tự nhiên (Natural Scroll)" else "Natural Scrolling",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Đảo chiều cuộn 2 ngón giống macOS Trackpad" else "Invert 2-finger scroll direction (macOS style)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = naturalScroll,
                    onCheckedChange = onNaturalScrollChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryContainer
                    )
                )
            }

            // Pinch Zoom Settings
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Cử chỉ thu phóng (Pinch Zoom)" else "Pinch to Zoom Gesture",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Chụm/mở 2 ngón để phóng to/thu nhỏ trên PC" else "Pinch in/out to zoom in or out on PC",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = pinchZoomEnabled,
                        onCheckedChange = onPinchZoomEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryContainer
                        )
                    )
                }
            }

            // Right Edge Fast Scroll Settings
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Cuộn nhanh mép phải (Fast Scroll)" else "Edge Fast Scroll",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Vuốt mép phải bàn rê để cuộn trang tốc độ cao" else "Drag right edge of trackpad for rapid scrolling",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = fastScrollEnabled,
                        onCheckedChange = onFastScrollEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryContainer
                        )
                    )
                }

                if (fastScrollEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Hệ số tốc độ cuộn nhanh" else "Fast scroll multiplier",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", fastScrollMultiplier)}×",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    Slider(
                        value = fastScrollMultiplier,
                        onValueChange = { onFastScrollMultiplierChange(Math.round(it * 10f) / 10f) },
                        valueRange = 1.5f..5.0f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = SurfaceRaised
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardSettingsSection(
    pasteDelay: Long,
    onPasteDelayChange: (Long) -> Unit,
    keyboardHaptics: Boolean,
    onKeyboardHapticsChange: (Boolean) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.Keyboard,
        title = if (language == AppLanguage.VIETNAMESE) "Cấu hình Bàn phím & Scancode" else "Keyboard & Scancode Engine",
        subtitle = "Subsystem 0x02"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Độ trễ dán chuỗi (Paste Throttle)" else "Paste Keystroke Throttle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Chống nghẽn buffer HID ring & nuốt ký tự host" else "Prevents HID ring buffer overflow & dropped chars",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text("$pasteDelay ms", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Slider(
                    value = pasteDelay.toFloat(),
                    onValueChange = { onPasteDelayChange(it.toLong()) },
                    valueRange = 10f..50f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = SurfaceRaised
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Rung phản hồi phím (Key Vibration)" else "Keyboard Haptic Feedback",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Cảm giác tactile nảy micro-switch khi gõ" else "Tactile micro-switch click sensation on keypress",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = keyboardHaptics,
                    onCheckedChange = onKeyboardHapticsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun GamepadSettingsSection(
    gpLeftDeadzone: Float,
    onGpLeftDeadzoneChange: (Float) -> Unit,
    gpLeftSens: Float,
    onGpLeftSensChange: (Float) -> Unit,
    gpLeftCurve: String,
    onGpLeftCurveChange: (String) -> Unit,
    gpRightDeadzone: Float,
    onGpRightDeadzoneChange: (Float) -> Unit,
    gpRightSens: Float,
    onGpRightSensChange: (Float) -> Unit,
    gpRightCurve: String,
    onGpRightCurveChange: (String) -> Unit,
    gpTriggerSens: Float,
    onGpTriggerSensChange: (Float) -> Unit,
    gpInvertY: Boolean,
    onGpInvertYChange: (Boolean) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.SportsEsports,
        title = if (language == AppLanguage.VIETNAMESE) "Hiệu chỉnh Tay cầm Gamepad" else "Gamepad & Controller Calibration",
        subtitle = "Report ID 4 • Direct HID Joystick"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = if (language == AppLanguage.VIETNAMESE) "CẦN TRÁI (LEFT ANALOG STICK)" else "LEFT ANALOG STICK",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == AppLanguage.VIETNAMESE) "Deadzone (Vùng chết)" else "Deadzone", fontSize = 13.sp, color = TextPrimary)
                    Text("${(gpLeftDeadzone * 100).toInt()}%", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = gpLeftDeadzone,
                    onValueChange = onGpLeftDeadzoneChange,
                    valueRange = 0.0f..0.30f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue, inactiveTrackColor = SurfaceRaised)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == AppLanguage.VIETNAMESE) "Sensitivity (Độ nhạy)" else "Sensitivity", fontSize = 13.sp, color = TextPrimary)
                    Text(String.format(Locale.US, "%.1fx", gpLeftSens), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = gpLeftSens,
                    onValueChange = onGpLeftSensChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue, inactiveTrackColor = SurfaceRaised)
                )
            }

            Text(
                text = if (language == AppLanguage.VIETNAMESE) "CẦN PHẢI (RIGHT ANALOG STICK)" else "RIGHT ANALOG STICK",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == AppLanguage.VIETNAMESE) "Deadzone (Vùng chết)" else "Deadzone", fontSize = 13.sp, color = TextPrimary)
                    Text("${(gpRightDeadzone * 100).toInt()}%", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = gpRightDeadzone,
                    onValueChange = onGpRightDeadzoneChange,
                    valueRange = 0.0f..0.30f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue, inactiveTrackColor = SurfaceRaised)
                )
            }

            // Invert Y
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Đảo trục Y (Invert Y Axis)" else "Invert Y Axis",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Phù hợp game mô phỏng bay (Flight Sim)" else "Optimal for flight simulators",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = gpInvertY,
                    onCheckedChange = onGpInvertYChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = PrimaryContainer)
                )
            }
        }
    }
}

@Composable
fun OneHandSettingsSection(
    oneHandHandedness: String,
    onOneHandHandednessChange: (String) -> Unit,
    oneHandSeekStep: Int,
    onOneHandSeekStepChange: (Int) -> Unit,
    oneHandScrollSens: Float,
    onOneHandScrollSensChange: (Float) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.PanTool,
        title = if (language == AppLanguage.VIETNAMESE) "Điều khiển 1-Tay & Công thái học" else "One-Hand Remote Control",
        subtitle = "Thumb Ergonomics"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Handedness Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Tay thuận (Handedness)" else "Handedness",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (oneHandHandedness == "Right") {
                            if (language == AppLanguage.VIETNAMESE) "Tay Phải ✋" else "Right Hand ✋"
                        } else {
                            if (language == AppLanguage.VIETNAMESE) "✋ Tay Trái" else "✋ Left Hand"
                        },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentButton(
                        label = if (language == AppLanguage.VIETNAMESE) "Tay Phải (Right)" else "Right Hand",
                        subLabel = if (language == AppLanguage.VIETNAMESE) "Vùng ngón cái lệch phải" else "Thumb reach bias to right",
                        isSelected = oneHandHandedness == "Right",
                        modifier = Modifier.weight(1f),
                        onClick = { onOneHandHandednessChange("Right") }
                    )
                    SegmentButton(
                        label = if (language == AppLanguage.VIETNAMESE) "Tay Trái (Left)" else "Left Hand",
                        subLabel = if (language == AppLanguage.VIETNAMESE) "Vùng ngón cái lệch trái" else "Thumb reach bias to left",
                        isSelected = oneHandHandedness == "Left",
                        modifier = Modifier.weight(1f),
                        onClick = { onOneHandHandednessChange("Left") }
                    )
                }
            }

            // Seek Step Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Bước tua Video (Seek Step)" else "Video Seek Step",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text("${oneHandSeekStep}s", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(5 to "5s", 10 to "10s", 30 to "30s").forEach { (step, label) ->
                        SegmentButton(
                            label = if (language == AppLanguage.VIETNAMESE) "$step Giây" else label,
                            subLabel = if (step == 10) (if (language == AppLanguage.VIETNAMESE) "YouTube Chuẩn" else "Standard") else (if (language == AppLanguage.VIETNAMESE) "Tùy chỉnh" else "Custom"),
                            isSelected = oneHandSeekStep == step,
                            modifier = Modifier.weight(1f),
                            onClick = { onOneHandSeekStepChange(step) }
                        )
                    }
                }
            }

            // Web Scroll Sensitivity Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Độ nhạy cuộn trang Web" else "Web Scroll Sensitivity",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(String.format(Locale.US, "%.1fx", oneHandScrollSens), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
                Slider(
                    value = oneHandScrollSens,
                    onValueChange = onOneHandScrollSensChange,
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = SurfaceRaised
                    )
                )
            }
        }
    }
}

@Composable
fun ScreenBehaviorSettingsSection(
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    screenSleepTimeoutMinutes: Int,
    onScreenSleepTimeoutMinutesChange: (Int) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.BatteryChargingFull,
        title = if (language == AppLanguage.VIETNAMESE) "Hành vi Màn hình & Nguồn" else "Screen Behavior & Power",
        subtitle = "Inactivity & Display Sleep"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Service Status Pill Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerLowest)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StatusConnected,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("FOREGROUND_SERVICE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Loại: CONNECTED_DEVICE (Duy trì nền)" else "Type: CONNECTED_DEVICE (Active in background)",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusConnected.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "RUNNING",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = StatusConnected
                    )
                }
            }

            // Screen Behavior Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.VIETNAMESE) "Hành vi màn hình (Screen Behavior)" else "Screen Behavior",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Option 1: Allow screen to sleep (Default)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!keepScreenAwake) SurfaceContainerLowest else SurfaceContainer)
                        .clickable { onKeepScreenAwakeChange(false) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !keepScreenAwake,
                        onClick = { onKeepScreenAwakeChange(false) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Cho phép màn hình ngủ (Allow screen to sleep)" else "Allow screen to sleep",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Màn hình tự tắt sau thời gian không thao tác. Dịch vụ Bluetooth HID vẫn duy trì kết nối nền." else "Screen turns off after inactivity. Bluetooth HID service stays connected in background.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Timeout options for Sleep mode
                if (!keepScreenAwake) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.VIETNAMESE) "Thời gian chờ tắt màn hình:" else "Screen sleep timeout:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = if (language == AppLanguage.VIETNAMESE) "$screenSleepTimeoutMinutes phút" else "$screenSleepTimeoutMinutes min",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLowest)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(5, 10, 15).forEach { timeout ->
                                val label = if (language == AppLanguage.VIETNAMESE) {
                                    if (timeout == 10) "10 Phút (Chuẩn)" else "$timeout Phút"
                                } else {
                                    if (timeout == 10) "10 min (Std)" else "$timeout min"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (screenSleepTimeoutMinutes == timeout) PrimaryContainer else SurfaceContainerLowest)
                                        .clickable { onScreenSleepTimeoutMinutesChange(timeout) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (screenSleepTimeoutMinutes == timeout) FontWeight.Bold else FontWeight.Normal,
                                        color = if (screenSleepTimeoutMinutes == timeout) OnPrimaryContainer else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Option 2: Keep screen awake
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (keepScreenAwake) SurfaceContainerLowest else SurfaceContainer)
                        .clickable { onKeepScreenAwakeChange(true) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = keepScreenAwake,
                        onClick = { onKeepScreenAwakeChange(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Luôn giữ màn hình sáng khi mở PocketHID" else "Keep screen awake while PocketHID is active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.VIETNAMESE) "Giữ màn hình luôn bật khi PocketHID hiển thị (phù hợp khi thuyết trình liên tục)." else "Keeps screen permanently awake while PocketHID is open (recommended for presentations).",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Security notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerLowest)
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Lưu ý bảo mật: PocketHID KHÔNG BAO GIỜ tự khóa máy điện thoại. Sau khi màn hình ngủ, việc có yêu cầu xác thực vân tay/mật khẩu khi bật lại hay không hoàn toàn do cài đặt bảo mật của Android." else "Security note: PocketHID never locks the device. Android system lock screen security determines biometric or PIN requirements upon wake.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticsSummarySection(hostName: String, language: AppLanguage = AppLanguage.ENGLISH) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("HARDWARE CONFIG SYNC HASH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                Text("CRC-32: #4A92F1", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SurfaceRaised)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(4.dp)
                        .background(PrimaryBlue)
                )
            }
            Text(
                text = if (language == AppLanguage.VIETNAMESE) "Các thông số này được lưu riêng biệt cho Host: $hostName" else "These settings are saved specifically for Host: $hostName",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VisibleModesSection(
    modeKeyboardVisible: Boolean,
    onModeKeyboardVisibleChange: (Boolean) -> Unit,
    modeMouseVisible: Boolean,
    onModeMouseVisibleChange: (Boolean) -> Unit,
    modeGamepadVisible: Boolean,
    onModeGamepadVisibleChange: (Boolean) -> Unit,
    modePresenterVisible: Boolean,
    onModePresenterVisibleChange: (Boolean) -> Unit,
    modeOneHandVisible: Boolean,
    onModeOneHandVisibleChange: (Boolean) -> Unit,
    modeDrawVisible: Boolean,
    onModeDrawVisibleChange: (Boolean) -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    SectionHeader(
        icon = Icons.Default.Info,
        title = if (language == AppLanguage.VIETNAMESE) "Các Mode Hiển Thị" else "Visible Modes",
        subtitle = "UI Configuration"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val buildToggle = @Composable { title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = onChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryContainer
                        )
                    )
                }
            }

            buildToggle(if (language == AppLanguage.VIETNAMESE) "Bàn phím" else "Keyboard", "Mode 0: Keyboard", modeKeyboardVisible, onModeKeyboardVisibleChange)
            buildToggle(if (language == AppLanguage.VIETNAMESE) "Chuột" else "Mouse", "Mode 1: Mouse", modeMouseVisible, onModeMouseVisibleChange)
            buildToggle(if (language == AppLanguage.VIETNAMESE) "Gamepad" else "Gamepad", "Mode 2: Controller", modeGamepadVisible, onModeGamepadVisibleChange)
            buildToggle(if (language == AppLanguage.VIETNAMESE) "Trình chiếu" else "Presenter", "Mode 3: Presentation", modePresenterVisible, onModePresenterVisibleChange)
            buildToggle(if (language == AppLanguage.VIETNAMESE) "1-Tay" else "One-Hand", "Mode 4: Remote Control", modeOneHandVisible, onModeOneHandVisibleChange)
            buildToggle(if (language == AppLanguage.VIETNAMESE) "Bảng vẽ" else "Draw / Tablet", "Mode 5: Graphics Tablet", modeDrawVisible, onModeDrawVisibleChange)
        }
    }
}
