package dev.aleian.pockethid.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.aleian.pockethid.drawing.DrawingController
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingStroke
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.ui.components.DrawingToolbar

private val CanvasBackground = Color(0xFF070B12)
private val GridDotColor = Color(0xFF1E293B).copy(alpha = 0.4f)

/**
 * Electronic Drawing Board Screen for PocketHID.
 *
 * Provides a local electronic drawing / presentation annotation surface.
 * Does not transmit raw drawing points over Bluetooth HID.
 * Optimized for low memory, smooth one-finger drawing, and multi-touch protection.
 */
@Composable
fun DrawingScreen(
    controller: DrawingController,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // TOP: Compact Toolbar
        DrawingToolbar(
            state = state,
            controller = controller,
            modifier = Modifier.fillMaxWidth()
        )

        // MAIN: Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CanvasBackground)
        ) {
            DrawingCanvas(
                strokes = state.strokes,
                currentDraftStroke = state.currentDraftStroke,
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Canvas Composable rendering persistent strokes and live draft stroke.
 * Intercepts touch inputs and routes them to [DrawingController] with 1-finger isolation.
 */
@Composable
private fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    currentDraftStroke: DrawingStroke?,
    controller: DrawingController,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(controller) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
                    val activePointerCount = currentEvent.changes.size

                    if (activePointerCount == 1) {
                        controller.startStroke(
                            point = DrawingPoint(down.position.x, down.position.y),
                            pointerCount = 1
                        )
                        down.consume()
                    } else {
                        // Multi-touch detected: cancel ongoing stroke
                        controller.cancelStroke()
                        return@awaitEachGesture
                    }

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val pointerCount = event.changes.size

                        if (pointerCount > 1) {
                            // Multi-touch appeared during stroke: abort stroke per spec 2.9
                            controller.cancelStroke()
                            break
                        }

                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            // Touch up: commit stroke
                            controller.finishStroke()
                            change.consume()
                            break
                        }

                        controller.appendPoint(
                            point = DrawingPoint(change.position.x, change.position.y),
                            pointerCount = 1
                        )
                        change.consume()
                    }
                }
            }
    ) {
        // Draw subtle engineering grid dots (spaced by 32dp)
        val stepPx = 32.dp.toPx()
        val dotRadius = 1.dp.toPx()
        val width = size.width
        val height = size.height

        var x = stepPx
        while (x < width) {
            var y = stepPx
            while (y < height) {
                drawCircle(
                    color = GridDotColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += stepPx
            }
            x += stepPx
        }

        // Draw committed strokes
        for (stroke in strokes) {
            renderStroke(stroke)
        }

        // Draw live active draft stroke
        if (currentDraftStroke != null) {
            renderStroke(currentDraftStroke)
        }
    }
}

/**
 * Extension to render an individual stroke with smooth joined segments.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderStroke(stroke: DrawingStroke) {
    if (stroke.points.isEmpty()) return
    if (stroke.tool == DrawingTool.ERASER) return // Eraser removes strokes, does not draw colored paths

    val strokeColor = Color(stroke.color.argb).copy(alpha = stroke.opacity)
    val strokeWidthPx = stroke.width.dp.toPx()

    if (stroke.points.size == 1) {
        // Single tap point: render as a small dot
        val single = stroke.points[0]
        drawCircle(
            color = strokeColor,
            radius = strokeWidthPx / 2f,
            center = Offset(single.x, single.y)
        )
        return
    }

    val path = Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val curr = stroke.points[i]
            // Quadratic bezier midpoint smoothing for natural handwriting
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            quadraticTo(prev.x, prev.y, midX, midY)
        }
        val last = stroke.points.last()
        lineTo(last.x, last.y)
    }

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
