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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.AccentAmber
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
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
import java.util.Locale

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
                        text = "Cài đặt nâng cao",
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

                    // 3-col telemetry stats
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

            // SECTION 1: Mouse & Trackpad Engine (Subsystem 0x01)
            SectionHeader(icon = Icons.Default.Mouse, title = "Mouse & Trackpad Engine", subtitle = "Subsystem 0x01")

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
                            Text("Tần số lấy mẫu (Polling Rate)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
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
                            ) { pollingRate = 62.5f }

                            SegmentButton(
                                label = "125 Hz",
                                subLabel = "8ms PRD Std",
                                isSelected = pollingRate == 125f,
                                modifier = Modifier.weight(1f)
                            ) { pollingRate = 125f }

                            SegmentButton(
                                label = "250 Hz",
                                subLabel = "4ms Turbo",
                                isSelected = pollingRate == 250f,
                                modifier = Modifier.weight(1f)
                            ) { pollingRate = 250f }
                        }
                        Text(
                            text = "Khuyên dùng 125 Hz để cân bằng jitter & thời lượng pin.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Deadzone
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dead Zone chống trôi Tap-Click", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("Chặn rung vi mô trong 30ms đầu sau touch-down", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("$deadZone px", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Slider(
                            value = deadZone.toFloat(),
                            onValueChange = { deadZone = it.toInt() },
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
                            Text("Gia tốc phi tuyến (Acceleration Curve)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                text = "k = ${String.format(Locale.US, "%.1f", accelFactor)}×",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerLowest)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "v_out = v_in × (1 + ${String.format(Locale.US, "%.1f", accelFactor)} × min(v_in / 8, 1))",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }
                        Slider(
                            value = accelFactor,
                            onValueChange = { accelFactor = (Math.round(it * 10f) / 10f) },
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
                                Text("Phản hồi xúc giác Trackpad", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("Rung ERM/LRA khi click & drag-lock", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = hapticTrackpad,
                                onCheckedChange = { hapticTrackpad = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = PrimaryContainer
                                )
                            )
                        }

                        if (hapticTrackpad) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Nhẹ", "Vừa", "Mạnh").forEach { level ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (hapticIntensity == level) SurfaceRaised else SurfaceContainerLowest)
                                            .clickable { hapticIntensity = level }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = level,
                                            fontSize = 11.sp,
                                            fontWeight = if (hapticIntensity == level) FontWeight.Bold else FontWeight.Normal,
                                            color = if (hapticIntensity == level) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Natural Scroll Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Đảo chiều cuộn tự nhiên (Natural Scroll)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Kéo 2 ngón theo hướng nội dung di chuyển", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = naturalScroll,
                            onCheckedChange = { naturalScroll = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = PrimaryContainer
                            )
                        )
                    }

                    // Double-tap Drag Lock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Double-tap Drag Lock (Khóa kéo)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Ngưỡng nhả con trỏ tự do: 250ms", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = dragLock,
                            onCheckedChange = { dragLock = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = PrimaryContainer
                            )
                        )
                    }
                }
            }

            // SECTION 2: Keyboard & Scancode Engine (Subsystem 0x02)
            SectionHeader(icon = Icons.Default.Keyboard, title = "Keyboard & Scancode Engine", subtitle = "Subsystem 0x02")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Safe Paste Throttle Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Độ trễ dán chuỗi (Paste Throttle)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("Chống nghẽn buffer HID ring & nuốt ký tự host", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("$pasteDelay ms", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Slider(
                            value = pasteDelay.toFloat(),
                            onValueChange = { pasteDelay = it.toLong() },
                            valueRange = 10f..50f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue,
                                inactiveTrackColor = SurfaceRaised
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fast (10ms)", fontSize = 10.sp, color = TextMuted)
                            Text("Safe Zone (15ms - 25ms)", fontSize = 10.sp, color = StatusConnected, fontWeight = FontWeight.SemiBold)
                            Text("Ultra Safe (50ms)", fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    // Sticky Modifiers Mechanism
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cơ chế Sticky Modifiers", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Tap 1 lần: giữ phím kế tiếp • Tap 2 lần: Khóa cứng", fontSize = 11.sp, color = TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerLowest)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LATCH+LOCK",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    // Key Vibration Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rung phản hồi phím (Key Vibration)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Cảm giác tactile nảy micro-switch khi gõ", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = keyboardHaptics,
                            onCheckedChange = { keyboardHaptics = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = PrimaryContainer
                            )
                        )
                    }
                }
            }

            // SECTION 3: Power & Background Service
            SectionHeader(icon = Icons.Default.BatteryChargingFull, title = "Power & Background Service", subtitle = "TargetSdk 35")

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
                                Text("Type: CONNECTED_DEVICE (Active)", fontSize = 10.sp, color = TextSecondary)
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

                    // Keep Awake Timeout Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Giữ kết nối khi tắt màn hình", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(keepAwake, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLowest)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("5 Phút", "15 Phút", "Vô hạn").forEach { option ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (keepAwake == option) PrimaryContainer else SurfaceContainerLowest)
                                        .clickable { keepAwake = option }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            fontWeight = if (keepAwake == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (keepAwake == option) OnPrimaryContainer else TextSecondary
                                        )
                                        Text(
                                            text = when (option) {
                                                "5 Phút" -> "PRD Chuẩn"
                                                "15 Phút" -> "Mở rộng"
                                                else -> "Tốn pin"
                                            },
                                            fontSize = 9.sp,
                                            color = if (keepAwake == option) OnPrimaryContainer.copy(alpha = 0.8f) else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (keepAwake == "Vô hạn") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ErrorContainer.copy(alpha = 0.2f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chế độ vô hạn sẽ ngăn Android Doze Mode, làm tăng tiêu hao pin.",
                                    fontSize = 11.sp,
                                    color = AccentAmber
                                )
                            }
                        }
                    }
                }
            }

            // Quick Diagnostics Summary Box
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
                        text = "Các thông số này được lưu riêng biệt cho Host: $hostName",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Action Buttons
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
                            keepAwakeTimeout = keepAwake
                        )
                        SettingsRepository.updateSettings(updated)
                        Toast.makeText(context, "Đã lưu cấu hình PocketHID thành công!", Toast.LENGTH_SHORT).show()
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
                    Text("Lưu cấu hình Host này", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        SettingsRepository.resetToDefaults()
                        pollingRate = 125f
                        deadZone = 1
                        accelFactor = 1.5f
                        hapticTrackpad = true
                        hapticIntensity = "Vừa"
                        naturalScroll = false
                        dragLock = true
                        pasteDelay = 15L
                        keyboardHaptics = true
                        keepAwake = "5 Phút"
                        Toast.makeText(context, "Đã khôi phục cài đặt mặc định kỹ thuật.", Toast.LENGTH_SHORT).show()
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
                    Text("Khôi phục mặc định kỹ thuật")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun SegmentButton(
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
