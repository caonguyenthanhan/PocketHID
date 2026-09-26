package dev.aleian.pockethid.ui.screens.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.gamepad.GamepadDiagnosticsHub
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamepadTestPanel(
    modifier: Modifier = Modifier
) {
    val gpDiag by GamepadDiagnosticsHub.telemetry.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GAMEPAD TEST & INPUT MONITOR",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "Report ID 4",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        // Neutral Report Indicator Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (gpDiag.isNeutral) Color(0xFF0F291E) else Color(0xFF332014))
                .border(
                    1.dp,
                    if (gpDiag.isNeutral) StatusConnected.copy(alpha = 0.5f) else Color(0xFFE5A93C).copy(alpha = 0.5f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (gpDiag.isNeutral) StatusConnected else Color(0xFFE5A93C))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (gpDiag.isNeutral) "NEUTRAL REPORT: YES (RESTING STATE)" else "NEUTRAL REPORT: NO (ACTIVE INPUT)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (gpDiag.isNeutral) StatusConnected else Color(0xFFE5A93C)
                )
            }
        }

        // Live Digital Buttons Indicator Grid (11 Buttons + D-Pad)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DIGITAL BUTTONS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ButtonBadge("A", gpDiag.a)
                ButtonBadge("B", gpDiag.b)
                ButtonBadge("X", gpDiag.x)
                ButtonBadge("Y", gpDiag.y)
                ButtonBadge("LB", gpDiag.lb)
                ButtonBadge("RB", gpDiag.rb)
                ButtonBadge("BACK", gpDiag.back)
                ButtonBadge("START", gpDiag.start)
                ButtonBadge("GUIDE", gpDiag.guide)
                ButtonBadge("L3", gpDiag.l3)
                ButtonBadge("R3", gpDiag.r3)
                ButtonBadge("D-UP", gpDiag.dpadUp)
                ButtonBadge("D-DOWN", gpDiag.dpadDown)
                ButtonBadge("D-LEFT", gpDiag.dpadLeft)
                ButtonBadge("D-RIGHT", gpDiag.dpadRight)
            }
        }

        // 2D Stick Visual Crosshairs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Stick Crosshair Box
            StickCrosshairBox(
                label = "LEFT STICK (LX, LY)",
                x = gpDiag.lxNormalized,
                y = gpDiag.lyNormalized,
                xStr = gpDiag.formatAxis(gpDiag.lxNormalized),
                yStr = gpDiag.formatAxis(gpDiag.lyNormalized),
                modifier = Modifier.weight(1f)
            )

            // Right Stick Crosshair Box
            StickCrosshairBox(
                label = "RIGHT STICK (RX, RY)",
                x = gpDiag.rxNormalized,
                y = gpDiag.ryNormalized,
                xStr = gpDiag.formatAxis(gpDiag.rxNormalized),
                yStr = gpDiag.formatAxis(gpDiag.ryNormalized),
                modifier = Modifier.weight(1f)
            )
        }

        // Analog Triggers Pressure Bars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF14171F))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ANALOG TRIGGERS (0.00 .. 1.00)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            // Left Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.width(22.dp)
                )
                LinearProgressIndicator(
                    progress = { gpDiag.ltNormalized },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = Color(0xFF232838),
                )
                Text(
                    text = gpDiag.formatTrigger(gpDiag.ltNormalized),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (gpDiag.leftTrigger != 0.toByte()) StatusConnected else TextMuted,
                    modifier = Modifier.width(36.dp)
                )
            }

            // Right Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.width(22.dp)
                )
                LinearProgressIndicator(
                    progress = { gpDiag.rtNormalized },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = Color(0xFF232838),
                )
                Text(
                    text = gpDiag.formatTrigger(gpDiag.rtNormalized),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (gpDiag.rightTrigger != 0.toByte()) StatusConnected else TextMuted,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        // Raw 13-Byte HID Report Monitor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0E1118))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAW HID REPORT MONITOR",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "ID: 04 | 13 Bytes",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryBlue
                )
            }

            Text(
                text = "Bytes: [ 04 ${gpDiag.rawReportHex} ]",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (gpDiag.isNeutral) TextMuted else StatusConnected
            )
        }

        // Multi-Touch Latch & Stuck-Button Tracking
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF14171F))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BUTTON LATCH & MULTI-TOUCH TEST",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Button(
                    onClick = { GamepadDiagnosticsHub.resetCounters() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF252B3B),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("RESET", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            DiagnosticItem(
                label = "Held Buttons",
                value = if (gpDiag.activeButtonsList.isNotEmpty()) gpDiag.activeButtonsList.joinToString(" + ") else "(none)",
                valueColor = if (gpDiag.activeButtonsList.isNotEmpty()) StatusConnected else TextMuted
            )

            DiagnosticItem(
                label = "Down Transitions",
                value = "${gpDiag.downCount}",
                valueColor = TextPrimary
            )

            DiagnosticItem(
                label = "Up Transitions",
                value = "${gpDiag.upCount}",
                valueColor = TextPrimary
            )

            val isStuck = gpDiag.downCount != gpDiag.upCount && gpDiag.isNeutral
            DiagnosticItem(
                label = "Stuck Button Check",
                value = if (isStuck) "SUSPECTED STUCK" else "CLEAN (LATCH OK)",
                valueColor = if (isStuck) StatusDisconnected else StatusConnected
            )
        }
    }
}

@Composable
private fun ButtonBadge(
    name: String,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) StatusConnected else Color(0xFF1C202C))
            .border(
                1.dp,
                if (isActive) StatusConnected else Color(0xFF2C3246),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.Black else TextPrimary
        )
    }
}

@Composable
private fun StickCrosshairBox(
    label: String,
    x: Float,
    y: Float,
    xStr: String,
    yStr: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF14171F))
            .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )

        // 2D Canvas Box
        Canvas(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0D0F15))
                .border(1.dp, Color(0xFF1D2230), RoundedCornerShape(4.dp))
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            // Center grid cross lines
            drawLine(
                color = Color(0xFF262C3E),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF262C3E),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1f
            )

            // Current stick offset position
            val pointX = center.x + (x.coerceIn(-1f, 1f) * (size.width / 2f - 6f))
            val pointY = center.y + (y.coerceIn(-1f, 1f) * (size.height / 2f - 6f))

            drawCircle(
                color = PrimaryBlue,
                radius = 5f,
                center = Offset(pointX, pointY)
            )
        }

        Text(
            text = "X: $xStr  Y: $yStr",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = if (x != 0f || y != 0f) StatusConnected else TextMuted
        )
    }
}
