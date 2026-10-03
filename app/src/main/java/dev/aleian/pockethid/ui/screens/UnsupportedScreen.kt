package dev.aleian.pockethid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.ui.theme.AccentRed
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

@Composable
fun UnsupportedScreen(
    reason: String = "Your device firmware does not support the Bluetooth HID Device role.",
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "HID Device Role Not Supported",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PocketHID requires the Android Bluetooth HID Device profile (API 28+). The manufacturer's firmware must have this profile enabled.",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
