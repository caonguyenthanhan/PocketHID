package dev.aleian.pockethid.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

@Composable
fun EnumerationStatusCard(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ConnectionState.Connected
    val hostStatus = if (isConnected) "Detected" else "Not detected"
    val hostStatusColor = if (isConnected) StatusConnected else StatusDisconnected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HID ENUMERATION & DESCRIPTOR",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "SDP 0xC8 (Combo+Pad)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        DiagnosticItem(
            label = "Bluetooth HID",
            value = if (isConnected) "CONNECTED" else "DISCONNECTED",
            valueColor = if (isConnected) StatusConnected else StatusDisconnected
        )

        DiagnosticItem(
            label = "Descriptor Registered",
            value = "YES (Combo 4-in-1)",
            valueColor = StatusConnected
        )

        DiagnosticItem(
            label = "Keyboard Collection (ID 1)",
            value = "YES (8 bytes)",
            valueColor = StatusConnected
        )

        DiagnosticItem(
            label = "Mouse Collection (ID 2)",
            value = "YES (4 bytes)",
            valueColor = StatusConnected
        )

        DiagnosticItem(
            label = "Consumer Collection (ID 3)",
            value = "YES (2 bytes)",
            valueColor = StatusConnected
        )

        DiagnosticItem(
            label = "Gamepad Collection (ID 4)",
            value = "YES (${HidConstants.GAMEPAD_REPORT_LENGTH} bytes)",
            valueColor = StatusConnected
        )

        DiagnosticItem(
            label = "Gamepad Report ID",
            value = "${HidConstants.REPORT_ID_GAMEPAD}",
            valueColor = TextPrimary
        )

        DiagnosticItem(
            label = "Gamepad Report Length",
            value = "${HidConstants.GAMEPAD_REPORT_LENGTH} bytes",
            valueColor = TextPrimary
        )

        DiagnosticItem(
            label = "Gamepad Host Status",
            value = hostStatus,
            valueColor = hostStatusColor
        )
    }
}

@Composable
internal fun DiagnosticItem(
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
