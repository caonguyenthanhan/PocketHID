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
import dev.aleian.pockethid.ui.components.ModifierToggleState
import dev.aleian.pockethid.ui.components.ThumbModifierKey
import dev.aleian.pockethid.ui.theme.*

/**
 * Compact Left Thumb Zone (~12–14% screen width).
 * Focused solely on high-value non-duplicated thumb controls (ESC, TAB, SUPER, Quick Macro).
 * Duplicated modifiers (CTRL, ALT, SHIFT) and PASTE are removed from this primary column.
 */
@Composable
fun DeckMacroZone(
    superState: ModifierToggleState,
    onCycleSuper: () -> Unit,
    onSendRawKey: (keyCode: Byte, extraMod: Byte, label: String) -> Unit,
    onQuickAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DECK",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        // Escape Key
        DeckKey(
            text = "ESC",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textColor = Color(0xFFEF4444),
            fontSize = 11.sp
        ) {
            onSendRawKey(HidConstants.KEY_ESC, 0, "ESC")
        }

        // Tab Key
        DeckKey(
            text = "TAB",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            fontSize = 11.sp
        ) {
            onSendRawKey(HidConstants.KEY_TAB, 0, "TAB")
        }

        // Super / Win / Cmd Key
        ThumbModifierKey(
            label = "SUPER",
            state = superState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
        ) {
            onCycleSuper()
        }

        // Quick Shortcuts / Command Palette Trigger Key
        DeckKey(
            text = "⌘ QUICK",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            containerColor = DarkSurface,
            textColor = PrimaryBlue,
            fontSize = 10.sp
        ) {
            onQuickAction()
        }
    }
}
