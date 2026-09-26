package dev.aleian.pockethid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Virtual 4-Way D-Pad with 8-direction diagonal support and independent pointer tracking.
 */
@Composable
fun VirtualDpad(
    size: Dp = 140.dp,
    onDirectionChange: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var upActive by remember { mutableStateOf(false) }
    var downActive by remember { mutableStateOf(false) }
    var leftActive by remember { mutableStateOf(false) }
    var rightActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    val deadzoneRadius = size.toPx() * 0.15f

                    fun updatePosition(pos: Offset) {
                        val delta = pos - center
                        val dist = sqrt(delta.x * delta.x + delta.y * delta.y)

                        if (dist < deadzoneRadius) {
                            upActive = false
                            downActive = false
                            leftActive = false
                            rightActive = false
                        } else {
                            // Angle in degrees [-180 .. 180]
                            var deg = Math.toDegrees(atan2(delta.y.toDouble(), delta.x.toDouble())).toFloat()
                            // Normalize to [0 .. 360), where 0 is Right, 90 is Down, 180 is Left, 270 is Up
                            if (deg < 0) deg += 360f

                            // Up: 225° .. 315° (with overlap for diagonals)
                            // Right: 315° .. 360° or 0° .. 45°
                            // Down: 45° .. 135°
                            // Left: 135° .. 225°
                            // Diagonals allow 45° sectors:
                            // Up-Right: 292.5° .. 337.5°
                            // Right: 337.5° .. 22.5°
                            // Down-Right: 22.5° .. 67.5°
                            // Down: 67.5° .. 112.5°
                            // Down-Left: 112.5° .. 157.5°
                            // Left: 157.5° .. 202.5°
                            // Up-Left: 202.5° .. 247.5°
                            // Up: 247.5° .. 292.5°

                            val isUp = (deg in 202.5f..337.5f)
                            val isDown = (deg in 22.5f..157.5f)
                            val isLeft = (deg in 112.5f..247.5f)
                            val isRight = (deg >= 292.5f || deg <= 67.5f)

                            upActive = isUp
                            downActive = isDown
                            leftActive = isLeft
                            rightActive = isRight
                        }
                        onDirectionChange(upActive, downActive, leftActive, rightActive)
                    }

                    updatePosition(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) {
                            upActive = false
                            downActive = false
                            leftActive = false
                            rightActive = false
                            onDirectionChange(false, false, false, false)
                            break
                        }

                        updatePosition(change.position)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val armW = w * 0.32f
            val armH = h * 0.32f
            val cx = w / 2f
            val cy = h / 2f

            // Cross background
            val basePath = Path().apply {
                // Top arm
                moveTo(cx - armW / 2f, 0f)
                lineTo(cx + armW / 2f, 0f)
                lineTo(cx + armW / 2f, cy - armW / 2f)
                // Right arm
                lineTo(w, cy - armW / 2f)
                lineTo(w, cy + armW / 2f)
                lineTo(cx + armW / 2f, cy + armW / 2f)
                // Bottom arm
                lineTo(cx + armW / 2f, h)
                lineTo(cx - armW / 2f, h)
                lineTo(cx - armW / 2f, cy + armW / 2f)
                // Left arm
                lineTo(0f, cy + armW / 2f)
                lineTo(0f, cy - armW / 2f)
                lineTo(cx - armW / 2f, cy - armW / 2f)
                close()
            }

            drawPath(basePath, color = DarkSurfaceVariant)
            drawPath(basePath, color = DarkBorder, style = Stroke(width = 2.dp.toPx()))

            // Highlight active sectors
            val activeColor = PrimaryBlue.copy(alpha = 0.5f)
            if (upActive) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(cx - armW / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(armW, cy)
                )
            }
            if (downActive) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(cx - armW / 2f, cy),
                    size = androidx.compose.ui.geometry.Size(armW, cy)
                )
            }
            if (leftActive) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(0f, cy - armW / 2f),
                    size = androidx.compose.ui.geometry.Size(cx, armW)
                )
            }
            if (rightActive) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(cx, cy - armW / 2f),
                    size = androidx.compose.ui.geometry.Size(cx, armW)
                )
            }

            // Center pivot
            drawCircle(
                color = DarkSurface,
                radius = armW * 0.45f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = DarkBorder,
                radius = armW * 0.45f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Arrow icons / labels
        Box(modifier = Modifier.size(size)) {
            Text(
                "▲",
                modifier = Modifier.align(Alignment.TopCenter),
                color = if (upActive) PrimaryBlue else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "▼",
                modifier = Modifier.align(Alignment.BottomCenter),
                color = if (downActive) PrimaryBlue else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "◀",
                modifier = Modifier.align(Alignment.CenterStart),
                color = if (leftActive) PrimaryBlue else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "▶",
                modifier = Modifier.align(Alignment.CenterEnd),
                color = if (rightActive) PrimaryBlue else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
