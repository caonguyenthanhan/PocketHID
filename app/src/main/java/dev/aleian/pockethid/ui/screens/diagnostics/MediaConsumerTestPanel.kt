package dev.aleian.pockethid.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaConsumerTestPanel(
    transport: InputTransport?,
    modifier: Modifier = Modifier
) {
    val telemetry by ConsumerDiagnosticsHub.telemetry.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONSUMER CONTROL (MEDIA) TEST",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "Report ID 3",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Text(
            text = "Tap buttons below to send standard HID Consumer reports with a 75ms hold cycle. This directly controls PC system volume/media without altering phone volume.",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 15.sp
        )

        // 7 Interactive Test Buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConsumerTestButton("VOL +") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_VOLUME_UP)
            }
            ConsumerTestButton("VOL -") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_VOLUME_DOWN)
            }
            ConsumerTestButton("MUTE") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_MUTE)
            }
            ConsumerTestButton("PLAY / PAUSE") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_PLAY_PAUSE)
            }
            ConsumerTestButton("STOP") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_STOP)
            }
            ConsumerTestButton("NEXT TRACK") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_SCAN_NEXT)
            }
            ConsumerTestButton("PREV TRACK") {
                transport?.sendConsumerClick(HidConstants.CONSUMER_SCAN_PREV)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Live Telemetry Readout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF14171F))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "LAST DISPATCH TELEMETRY",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            DiagnosticItem(
                label = "Action",
                value = telemetry.actionName,
                valueColor = if (telemetry.actionName != "IDLE") PrimaryBlue else TextPrimary
            )

            DiagnosticItem(
                label = "Report ID",
                value = "${telemetry.reportId}",
                valueColor = TextPrimary
            )

            DiagnosticItem(
                label = "Usage Code",
                value = telemetry.usageHex,
                valueColor = StatusConnected
            )

            DiagnosticItem(
                label = "Press SENT",
                value = if (telemetry.pressSent) "YES" else "NO",
                valueColor = if (telemetry.pressSent) StatusConnected else StatusDisconnected
            )

            DiagnosticItem(
                label = "Release SENT",
                value = if (telemetry.releaseSent) "YES" else "NO",
                valueColor = if (telemetry.releaseSent) StatusConnected else TextMuted
            )

            DiagnosticItem(
                label = "Transport Status",
                value = telemetry.transportStatus,
                valueColor = when {
                    telemetry.transportStatus.contains("SUCCESS") -> StatusConnected
                    telemetry.transportStatus == "IDLE" -> TextMuted
                    else -> StatusDisconnected
                }
            )
        }
    }
}

@Composable
private fun ConsumerTestButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF212634),
            contentColor = TextPrimary
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
