package dev.aleian.pockethid.ui.screens.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.components.DeckKey
import dev.aleian.pockethid.ui.components.NaturalThumbArrowPad
import dev.aleian.pockethid.ui.theme.*

/**
 * Compact Right Thumb Navigation Zone (~11% screen width).
 * Safe-area constrained with responsive thumb controls:
 * DEL -> HOME | END -> 4-way Inverted-T Arrows (↑, ←, ↓, →) -> 123#
 */
@Composable
fun DeckUtilityZone(
    hapticsEnabled: Boolean,
    onSendRawKey: (keyCode: Byte, extraMod: Byte, label: String) -> Unit,
    onSwitchNumpad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "NAV",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        // Forward Delete (DEL)
        DeckKey(
            text = "DEL",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textColor = Color(0xFFEF4444),
            fontSize = 11.sp
        ) {
            onSendRawKey(HidConstants.KEY_DELETE, 0, "DEL")
        }

        // HOME | END Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DeckKey(
                text = "HOME",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                fontSize = 9.sp
            ) {
                onSendRawKey(HidConstants.KEY_HOME, 0, "HOME")
            }
            DeckKey(
                text = "END",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                fontSize = 9.sp
            ) {
                onSendRawKey(HidConstants.KEY_END, 0, "END")
            }
        }

        // Compact Natural 4-Way Thumb Arrow Cluster (Inverted-T with zero overflow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2.2f),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            NaturalThumbArrowPad(
                modifier = Modifier.fillMaxSize(),
                hapticFeedback = hapticsEnabled,
                onUp = { onSendRawKey(HidConstants.KEY_UP, 0, "UP") },
                onDown = { onSendRawKey(HidConstants.KEY_DOWN, 0, "DOWN") },
                onLeft = { onSendRawKey(HidConstants.KEY_LEFT, 0, "LEFT") },
                onRight = { onSendRawKey(HidConstants.KEY_RIGHT, 0, "RIGHT") }
            )
        }

        // Quick Numpad / Utility Toggle
        DeckKey(
            text = "123#",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            containerColor = DarkSurface,
            textColor = PrimaryBlue,
            fontSize = 11.sp
        ) {
            onSwitchNumpad()
        }
    }
}
