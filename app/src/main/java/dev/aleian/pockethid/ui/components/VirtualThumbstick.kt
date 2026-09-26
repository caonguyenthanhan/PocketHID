package dev.aleian.pockethid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.gamepad.GamepadMath
import dev.aleian.pockethid.gamepad.StickResponseCurve
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Virtual analog thumbstick with multi-touch pointer tracking, deadzone visualization,
 * and smooth return-to-center physics.
 */
@Composable
fun VirtualThumbstick(
    label: String,
    deadzone: Float,
    sensitivity: Float,
    curve: StickResponseCurve,
    invertY: Boolean = false,
    size: Dp = 140.dp,
    onStickMove: (Short, Short) -> Unit,
    onClickL3R3: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val radiusPx = sizePx / 2f
    val knobRadiusPx = radiusPx * 0.38f
    val maxTravelPx = radiusPx - knobRadiusPx

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(deadzone, sensitivity, curve, invertY, maxTravelPx) {
                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isTouching = true

                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    var currentPos = down.position - center

                    // Clamp to max travel
                    val dist = sqrt(currentPos.x * currentPos.x + currentPos.y * currentPos.y)
                    if (dist > maxTravelPx) {
                        val angle = atan2(currentPos.y, currentPos.x)
                        currentPos = Offset(cos(angle) * maxTravelPx, sin(angle) * maxTravelPx)
                    }

                    knobOffsetX = currentPos.x
                    knobOffsetY = currentPos.y

                    val (hidX, hidY) = GamepadMath.processStickInput(
                        dx = currentPos.x,
                        dy = currentPos.y,
                        maxRadius = maxTravelPx,
                        deadzone = deadzone,
                        curve = curve,
                        sensitivity = sensitivity,
                        invertY = invertY
                    )
                    onStickMove(hidX, hidY)

                    // Track this pointer continuously until release or cancel
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) {
                            // Touch released or cancelled -> return to center
                            knobOffsetX = 0f
                            knobOffsetY = 0f
                            isTouching = false
                            onStickMove(0, 0)
                            break
                        }

                        var dragPos = change.position - center
                        val curDist = sqrt(dragPos.x * dragPos.x + dragPos.y * dragPos.y)
                        if (curDist > maxTravelPx) {
                            val angle = atan2(dragPos.y, dragPos.x)
                            dragPos = Offset(cos(angle) * maxTravelPx, sin(angle) * maxTravelPx)
                        }

                        knobOffsetX = dragPos.x
                        knobOffsetY = dragPos.y

                        val (curHidX, curHidY) = GamepadMath.processStickInput(
                            dx = dragPos.x,
                            dy = dragPos.y,
                            maxRadius = maxTravelPx,
                            deadzone = deadzone,
                            curve = curve,
                            sensitivity = sensitivity,
                            invertY = invertY
                        )
                        onStickMove(curHidX, curHidY)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer Base Ring
            drawCircle(
                color = DarkSurfaceVariant,
                radius = radiusPx,
                center = center
            )
            drawCircle(
                color = if (isTouching) PrimaryBlue.copy(alpha = 0.6f) else DarkBorder,
                radius = radiusPx,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Crosshair guidelines
            val crosshairColor = DarkBorder.copy(alpha = 0.5f)
            drawLine(
                color = crosshairColor,
                start = Offset(center.x - radiusPx * 0.7f, center.y),
                end = Offset(center.x + radiusPx * 0.7f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = crosshairColor,
                start = Offset(center.x, center.y - radiusPx * 0.7f),
                end = Offset(center.x, center.y + radiusPx * 0.7f),
                strokeWidth = 1.dp.toPx()
            )

            // Deadzone Boundary (dashed circle)
            if (deadzone > 0f && deadzone < 1f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = maxTravelPx * deadzone,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                )
            }

            // Knob
            val knobCenter = Offset(center.x + knobOffsetX, center.y + knobOffsetY)
            drawCircle(
                color = DarkSurface,
                radius = knobRadiusPx,
                center = knobCenter
            )
            drawCircle(
                color = if (isTouching) PrimaryBlue else Color(0xFF4A5568),
                radius = knobRadiusPx,
                center = knobCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Inner Knob Core
            drawCircle(
                color = if (isTouching) PrimaryBlue.copy(alpha = 0.35f) else Color(0xFF2D3748),
                radius = knobRadiusPx * 0.5f,
                center = knobCenter
            )
        }

        // Stick Center Label
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isTouching) PrimaryBlue else TextMuted
        )
    }
}
