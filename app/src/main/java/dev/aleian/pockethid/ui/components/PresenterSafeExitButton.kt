package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.AppLanguage
import dev.aleian.pockethid.model.PocketStrings
import dev.aleian.pockethid.presenter.PresenterExitSafety
import dev.aleian.pockethid.ui.theme.AccentRed
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * Safe Exit button for Presenter mode.
 * Protects against accidental slide dismissal with a mandatory 1.5s press-and-hold interaction.
 */
@Composable
fun PresenterSafeExitButton(
    language: AppLanguage,
    onExitConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val safety = remember { PresenterExitSafety(requiredHoldMs = 1500L) }
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var showTapHint by remember { mutableStateOf(false) }

    // Hold loop updates progress continuously while finger is down
    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            safety.onPointerDown(startTime)
            while (isPressed) {
                val now = System.currentTimeMillis()
                val progress = safety.computeProgress(now)
                holdProgress = progress
                if (safety.isHoldComplete(now)) {
                    view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onExitConfirmed()
                    isPressed = false
                    holdProgress = 0f
                    safety.reset()
                    break
                }
                delay(20L)
            }
        } else {
            safety.reset()
            holdProgress = 0f
        }
    }

    // Auto-dismiss short tap warning hint after 2 seconds
    LaunchedEffect(showTapHint) {
        if (showTapHint) {
            delay(2000L)
            showTapHint = false
        }
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) AccentRed.copy(alpha = 0.2f) else DarkSurfaceVariant)
            .border(
                1.dp,
                if (isPressed) AccentRed else DarkBorder,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    showTapHint = false

                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.id == down.id && it.pressed })

                    val wasComplete = safety.isHoldComplete(System.currentTimeMillis())
                    if (!wasComplete && isPressed) {
                        showTapHint = true
                    }
                    isPressed = false
                }
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Slideshow",
                        tint = if (isPressed) AccentRed else TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showTapHint) {
                            "HOLD 1.5s!"
                        } else if (isPressed) {
                            PocketStrings.presenterExitHolding(language)
                        } else {
                            PocketStrings.presenterExitSafe(language)
                        },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isPressed || showTapHint) AccentRed else TextPrimary
                    )
                }
            }

            // Visual Fill Bar showing 0..100% completion progress
            if (isPressed) {
                LinearProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentRed,
                    trackColor = DarkSurfaceVariant
                )
            }
        }
    }
}
