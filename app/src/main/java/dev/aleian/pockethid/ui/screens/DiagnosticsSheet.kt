package dev.aleian.pockethid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsSheet(
    connectionState: ConnectionState,
    lastScancode: String = "0x00",
    lastInputLabel: String = "IDLE",
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings by SettingsRepository.settings.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DIAGNOSTICS & TELEMETRY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Thông số kỹ thuật phần cứng và trạng thái truyền tín hiệu HID qua Bluetooth.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            // Diagnostic Table Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosticRow(
                    label = "Connection Status",
                    value = when (connectionState) {
                        is ConnectionState.Connected -> "CONNECTED (${connectionState.device.name ?: connectionState.device.address})"
                        is ConnectionState.Connecting -> "CONNECTING..."
                        is ConnectionState.Error -> "ERROR: ${connectionState.message}"
                        is ConnectionState.Disconnected -> "DISCONNECTED"
                    },
                    valueColor = when (connectionState) {
                        is ConnectionState.Connected -> StatusConnected
                        is ConnectionState.Connecting -> PrimaryBlue
                        else -> StatusDisconnected
                    }
                )

                DiagnosticRow(
                    label = "Transmission Latency",
                    value = "8 ms (Nominal BLE)",
                    valueColor = TextPrimary
                )

                DiagnosticRow(
                    label = "Report Polling Rate",
                    value = "${settings.pollingRate.toInt()} Hz",
                    valueColor = PrimaryBlue
                )

                DiagnosticRow(
                    label = "Last Input Key",
                    value = lastInputLabel,
                    valueColor = TextPrimary
                )

                DiagnosticRow(
                    label = "HID Scancode (HEX)",
                    value = lastScancode,
                    valueColor = PrimaryBlue
                )

                DiagnosticRow(
                    label = "Haptic Actuator",
                    value = if (settings.keyboardHaptics) "ENABLED (KEYBOARD_TAP)" else "DISABLED",
                    valueColor = if (settings.keyboardHaptics) StatusConnected else TextMuted
                )

                DiagnosticRow(
                    label = "Paste Buffer Delay",
                    value = "${settings.pasteDelayMs} ms / stroke",
                    valueColor = TextPrimary
                )

                DiagnosticRow(
                    label = "HID Profile Type",
                    value = "Bluetooth Classic HID (Keyboard + Mouse)",
                    valueColor = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
