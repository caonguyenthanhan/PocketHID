package dev.aleian.pockethid.ui.screens.deck

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.components.DeckKey
import dev.aleian.pockethid.ui.components.ModifierToggleState
import dev.aleian.pockethid.ui.components.ThumbModifierKey
import dev.aleian.pockethid.ui.theme.*

@Composable
fun DeckMacroZone(
    ctrlState: ModifierToggleState,
    altState: ModifierToggleState,
    superState: ModifierToggleState,
    shiftState: ModifierToggleState,
    onCycleCtrl: () -> Unit,
    onCycleAlt: () -> Unit,
    onCycleSuper: () -> Unit,
    onCycleShift: () -> Unit,
    onSendRawKey: (keyCode: Byte, extraMod: Byte, label: String) -> Unit,
    onSendStringSafe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            text = "MODIFIERS",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        DeckKey(
            text = "ESC",
            modifier = Modifier.fillMaxWidth().weight(1f),
            textColor = Color(0xFFEF4444),
            fontSize = 11.sp
        ) {
            onSendRawKey(HidConstants.KEY_ESC, 0, "ESC")
        }

        DeckKey(
            text = "TAB",
            modifier = Modifier.fillMaxWidth().weight(1f),
            fontSize = 10.sp
        ) {
            onSendRawKey(HidConstants.KEY_TAB, 0, "TAB")
        }

        ThumbModifierKey(
            label = "CTRL",
            state = ctrlState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            onCycleCtrl()
        }

        ThumbModifierKey(
            label = "ALT",
            state = altState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            onCycleAlt()
        }

        ThumbModifierKey(
            label = "SUPER",
            state = superState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            onCycleSuper()
        }

        ThumbModifierKey(
            label = "SHIFT",
            state = shiftState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            onCycleShift()
        }

        // Safe Paste Quick Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(5.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(5.dp))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrEmpty()) {
                        onSendStringSafe(clip)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "PASTE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}
