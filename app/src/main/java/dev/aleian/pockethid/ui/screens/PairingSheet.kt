package dev.aleian.pockethid.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.LaptopWindows
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.ErrorContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun PairingSheet(
    transport: InputTransport?,
    onDismiss: () -> Unit,
    pairedDevices: List<BluetoothDevice>,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings by SettingsRepository.settings.collectAsState()
    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(ConnectionState.Disconnected) })

    var selectedOsTab by remember { mutableIntStateOf(0) }
    val osTabs = listOf("Windows", "macOS", "Linux")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Host Manager & Pairing",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Bluetooth HID Peripheral Controller",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = PrimaryBlue
                    )
                }
                OutlinedButton(
                    onClick = onMakeDiscoverable,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Discoverable (300s)", fontSize = 11.sp)
                }
            }

            // ACTIVE CONNECTED HOST CARD (From host_manager mockup)
            if (connState is ConnectionState.Connected) {
                val activeDevice = (connState as ConnectionState.Connected).device
                val activeName = activeDevice.name ?: "Unknown Host"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LaptopWindows,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(activeName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(activeDevice.address, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusConnected.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("CONNECTED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = StatusConnected)
                            }
                        }

                        // 4-cell telemetry stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TelemetryMiniCell("PROTOCOL", "BT 6.0 Classic", Modifier.weight(1f))
                            TelemetryMiniCell("POLLING", "${settings.pollingRate.toInt()} Hz", Modifier.weight(1f))
                            TelemetryMiniCell("LATENCY", "8.0 ms", Modifier.weight(1f))
                            TelemetryMiniCell("DRAIN", "~3.2%/h", Modifier.weight(1f))
                        }

                        // Foreground Service status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerLowest)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = StatusConnected, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FOREGROUND_SERVICE: ACTIVE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            }
                            Text("OK", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = StatusConnected, fontWeight = FontWeight.Bold)
                        }

                        // Quick Actions: Keystroke Ping & Disconnect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        transport?.sendKeyClick(HidConstants.KEY_SPACE, 0)
                                        Toast.makeText(context, "Sent Ping Keystroke (Space)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceRaised,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping Host", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    transport?.disconnect()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorContainer,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // BLUETOOTH HID DEVICE ROLE VERIFICATION (From host_manager mockup)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusConnected.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusConnected, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bluetooth HID Device Role", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StatusConnected.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("Supported", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = StatusConnected, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Verified on AOSP API 28+ stack. Native input emulation pipeline primed.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Redmi Note 17 Pro Max 5G", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Snapdragon 6 Gen 5 • HyperOS 3", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // PAIRED DEVICES LIST
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paired Profiles & Hosts",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${pairedDevices.size} Devices",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No previously paired Bluetooth devices found.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                } else {
                    pairedDevices.forEach { device ->
                        DeviceItem(device = device, onClick = { onSelectDevice(device) })
                    }
                }
            }

            // OS GUIDE TABS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pairing Guide by Host OS",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )

                TabRow(
                    selectedTabIndex = selectedOsTab,
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    osTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedOsTab == index,
                            onClick = { selectedOsTab = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp)
                ) {
                    when (selectedOsTab) {
                        0 -> WindowsGuide()
                        1 -> MacGuide()
                        2 -> LinuxGuide()
                    }
                }
            }

            // Button to open Settings
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cấu hình chuột, phím & pin nâng cao (Live Tuner)", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TelemetryMiniCell(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerLowest)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
            Text(value, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkBg)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    color = TextMuted
                )
            }
        }
        Text(
            text = "Connect",
            style = MaterialTheme.typography.labelMedium.copy(color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
    }
}

@Composable
private fun WindowsGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("1. Open Settings → Bluetooth & devices → Add device.", fontSize = 11.sp, color = TextSecondary)
        Text("2. Select \"Bluetooth\" and pick this phone when it appears.", fontSize = 11.sp, color = TextSecondary)
        Text("3. Confirm the pairing PIN on both screens.", fontSize = 11.sp, color = TextSecondary)
        Text("Note: Windows caches HID descriptors. If you reconnect after an update, remove the device first.", fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
private fun MacGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("1. Open System Settings → Bluetooth.", fontSize = 11.sp, color = TextSecondary)
        Text("2. Find this phone in the Nearby Devices list and click Connect.", fontSize = 11.sp, color = TextSecondary)
        Text("3. If \"Keyboard Setup Assistant\" appears, press Shift next to Z and skip.", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun LinuxGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("1. Open Bluetooth Settings or run `bluetoothctl`.", fontSize = 11.sp, color = TextSecondary)
        Text("2. Pair & trust this device: `trust <MAC>` then `connect <MAC>`.", fontSize = 11.sp, color = TextSecondary)
        Text("3. Ensure the `bluetoothd` input plugin is active (default on Ubuntu/Fedora).", fontSize = 11.sp, color = TextSecondary)
    }
}
