package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.KeyLegend
import dev.aleian.pockethid.model.KeyLegends
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DedicatedNumberRow(
    isShiftActive: Boolean,
    onSendKey: (keyCode: Byte, extraModifier: Byte, label: String) -> Unit,
    onBackspaceRepeat: () -> Unit,
    hapticsEnabled: Boolean = true,
    fontSize: TextUnit = 12.sp,
    keyHeight: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }
    var isBackspacePressed by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val numberKeys = remember { KeyLegends.NUMBER_ROW }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(keyHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number Keys with Dual Primary / Shifted Character Legends via DeckLegendKey
        numberKeys.forEach { item ->
            DeckLegendKey(
                legend = item,
                isShiftActive = isShiftActive,
                isCapsLockActive = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = DarkSurfaceVariant,
                hapticFeedback = hapticsEnabled
            ) {
                val extraMod = if (isShiftActive) HidConstants.MOD_LEFT_SHIFT else 0.toByte()
                val label = if (isShiftActive) (item.shifted ?: item.primary) else item.primary
                onSendKey(item.keyCode, extraMod, label)
            }
        }

        // Backspace with Tap + Hold Repeat
        Box(
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isBackspacePressed) Color(0xFFEF4444).copy(alpha = 0.3f) else DarkSurfaceVariant)
                .border(1.dp, if (isBackspacePressed) Color(0xFFEF4444) else DarkBorder, RoundedCornerShape(6.dp))
                .pointerInteropFilter { ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            isBackspacePressed = true
                            triggerHaptic()
                            onBackspaceRepeat()

                            // Launch repeat job on hold
                            repeatJob?.cancel()
                            repeatJob = scope.launch {
                                delay(350)
                                while (isActive) {
                                    triggerHaptic()
                                    onBackspaceRepeat()
                                    delay(55)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isBackspacePressed = false
                            repeatJob?.cancel()
                            repeatJob = null
                            true
                        }
                        else -> true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌫",
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 14.sp
                ),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        }
    }
}
