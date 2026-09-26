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

@Composable
fun DeckUtilityZone(
    hapticsEnabled: Boolean,
    onSendRawKey: (keyCode: Byte, extraMod: Byte, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "ACTIONS",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        // Backspace
        DeckKey(
            text = "BKSP",
            modifier = Modifier.fillMaxWidth().weight(1f),
            textColor = Color(0xFFEF4444),
            fontSize = 11.sp
        ) {
            onSendRawKey(HidConstants.KEY_BACKSPACE, 0, "BKSP")
        }

        // Enter (Prominent Action Key)
        DeckKey(
            text = "ENTER",
            modifier = Modifier.fillMaxWidth().weight(1.3f),
            containerColor = PrimaryBlue.copy(alpha = 0.2f),
            textColor = PrimaryBlue,
            fontSize = 12.sp
        ) {
            onSendRawKey(HidConstants.KEY_ENTER, 0, "ENTER")
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Natural 4-Way Thumb Arrow Cluster
        NaturalThumbArrowPad(
            modifier = Modifier.fillMaxWidth().weight(1.8f),
            hapticFeedback = hapticsEnabled,
            onUp = { onSendRawKey(HidConstants.KEY_UP, 0, "UP") },
            onDown = { onSendRawKey(HidConstants.KEY_DOWN, 0, "DOWN") },
            onLeft = { onSendRawKey(HidConstants.KEY_LEFT, 0, "LEFT") },
            onRight = { onSendRawKey(HidConstants.KEY_RIGHT, 0, "RIGHT") }
        )
    }
}
