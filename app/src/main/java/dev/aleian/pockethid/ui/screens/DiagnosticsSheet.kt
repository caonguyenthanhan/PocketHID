package dev.aleian.pockethid.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.screens.diagnostics.EnumerationStatusCard
import dev.aleian.pockethid.ui.screens.diagnostics.GamepadTestPanel
import dev.aleian.pockethid.ui.screens.diagnostics.HostChecklistPanel
import dev.aleian.pockethid.ui.screens.diagnostics.MediaConsumerTestPanel
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
    transport: InputTransport? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings by SettingsRepository.settings.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SYSTEM", "GAMEPAD", "MEDIA", "CHECKLIST")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        fontSize = 13.sp,
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

            // Segmented Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141720))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // System Overview Tab
                    EnumerationStatusCard(connectionState = connectionState)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SYSTEM & HARDWARE TELEMETRY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        DiagnosticRow(
                            label = "Connection Status",
                            value = when (connectionState) {
                                is ConnectionState.Connected -> "CONNECTED (${connectionState.device.name ?: connectionState.device.address})"
                                is ConnectionState.Connecting -> "CONNECTING…"
                                is ConnectionState.Disconnecting -> "DISCONNECTING…"
                                is ConnectionState.Error -> "ERROR: ${connectionState.message}"
                                is ConnectionState.Disconnected -> "DISCONNECTED"
                            },
                            valueColor = when (connectionState) {
                                is ConnectionState.Connected -> StatusConnected
                                is ConnectionState.Connecting -> PrimaryBlue
                                is ConnectionState.Disconnecting -> PrimaryBlue
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
                            label = "Haptic Feedback",
                            value = if (settings.keyboardHaptics) "ENABLED (KEYBOARD_TAP)" else "DISABLED",
                            valueColor = if (settings.keyboardHaptics) StatusConnected else TextMuted
                        )

                        DiagnosticRow(
                            label = "HID Profile Type",
                            value = "Bluetooth Classic HID (Composite 4-in-1)",
                            valueColor = TextSecondary
                        )
                    }

                    // Gesture Telemetry Container
                    val gestureDiag by dev.aleian.pockethid.mapping.GestureDiagnosticsHub.diagnostics.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TRACKPAD GESTURE TELEMETRY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        DiagnosticRow(
                            label = "Active Touch Points",
                            value = "${gestureDiag.pointerCount} fingers",
                            valueColor = if (gestureDiag.pointerCount > 0) StatusConnected else TextMuted
                        )

                        DiagnosticRow(
                            label = "Detected Gesture",
                            value = gestureDiag.gestureType.name,
                            valueColor = PrimaryBlue
                        )

                        DiagnosticRow(
                            label = "Action Triggered",
                            value = gestureDiag.actionDescription,
                            valueColor = TextPrimary
                        )

                        DiagnosticRow(
                            label = "Output Shortcut",
                            value = gestureDiag.outputShortcut,
                            valueColor = StatusConnected
                        )
                    }

                    // Android IME to HID Pipeline Telemetry Container
                    val imeDiag by dev.aleian.pockethid.ui.components.ImeDiagnosticsHub.diagnostics.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ANDROID IME → HID PIPELINE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        DiagnosticRow(
                            label = "Last IME Event",
                            value = imeDiag.lastEvent,
                            valueColor = PrimaryBlue
                        )

                        DiagnosticRow(
                            label = "Committed Text",
                            value = if (imeDiag.committedText.isNotEmpty()) "\"${imeDiag.committedText}\"" else "(none)",
                            valueColor = TextPrimary
                        )

                        DiagnosticRow(
                            label = "Resolved HID Keys",
                            value = imeDiag.resolvedKeys,
                            valueColor = StatusConnected
                        )

                        DiagnosticRow(
                            label = "Pipeline Status",
                            value = imeDiag.status,
                            valueColor = if (imeDiag.status == "Sent") StatusConnected else TextMuted
                        )
                    }
                }

                1 -> {
                    // Gamepad Test Tab
                    EnumerationStatusCard(connectionState = connectionState)
                    GamepadTestPanel()
                }

                2 -> {
                    // Media Consumer Test Tab
                    MediaConsumerTestPanel(transport = transport)
                }

                3 -> {
                    // Host Verification Checklist Tab
                    HostChecklistPanel()
                }
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
