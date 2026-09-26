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
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

@Composable
fun HostChecklistPanel(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WINDOWS HOST TEST PROCEDURE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "joy.cpl",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = StatusConnected
            )
        }

        Text(
            text = "Follow this verification procedure to validate Windows HID enumeration and input handling without cached descriptors.",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 15.sp
        )

        ChecklistStep(
            step = "1",
            title = "Clear Cached Descriptor",
            desc = "If PocketHID was previously paired, open Windows Settings > Bluetooth & devices > Devices, find PocketHID and click 'Remove device'."
        )

        ChecklistStep(
            step = "2",
            title = "Pair Bluetooth Composite Device",
            desc = "Turn on Bluetooth, pair PocketHID from Windows. Windows will query the new SDP descriptor with subclass 0xC8 (Combo + Gamepad)."
        )

        ChecklistStep(
            step = "3",
            title = "Launch Game Controllers Panel",
            desc = "Press Win + R on Windows, type joy.cpl and press Enter. Verify 'PocketHID' or 'Bluetooth HID Game Controller' appears in the list."
        )

        ChecklistStep(
            step = "4",
            title = "Inspect Device Manager",
            desc = "Open Device Manager > Human Interface Devices. Verify standard 'HID-compliant game controller' and 'Bluetooth HID Device' are loaded without yellow exclamation marks."
        )

        ChecklistStep(
            step = "5",
            title = "Test Buttons & Analog Inputs",
            desc = "In joy.cpl, click 'Properties' > 'Test'. Test A/B/X/Y, LB/RB, Back/Start/Guide, L3/R3, D-pad Hat Switch, Left/Right sticks, and Triggers."
        )

        ChecklistStep(
            step = "6",
            title = "Verify Media Consumer Keys",
            desc = "Switch to MEDIA TEST tab or use One-Hand mode. Test Vol+, Vol-, Mute. Verify Windows taskbar volume slider adjusts smoothly."
        )

        ChecklistStep(
            step = "7",
            title = "Zero Regression Check",
            desc = "Verify Trackpad Mouse, Keyboard typing, and Presenter modes still work seamlessly as part of the composite HID device."
        )
    }
}

@Composable
private fun ChecklistStep(
    step: String,
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF14171F))
            .border(1.dp, Color(0xFF232838), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "[$step]",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Text(
            text = desc,
            fontSize = 10.sp,
            color = TextMuted,
            lineHeight = 14.sp
        )
    }
}
