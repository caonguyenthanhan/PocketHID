package dev.aleian.pockethid.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.drawing.DrawingController
import dev.aleian.pockethid.drawing.DrawingTabletController
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingStroke
import dev.aleian.pockethid.drawing.model.DrawingTargetMode
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.ConnectionState
import android.content.res.Configuration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import dev.aleian.pockethid.model.FocusLockController
import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.components.FloatingTabletToolbar
import dev.aleian.pockethid.ui.theme.TextMuted

private val CanvasBackground = Color(0xFF070B12)
private val GridDotColor = Color(0xFF1E293B).copy(alpha = 0.35f)
private val CyanAccent = Color(0xFF00E5FF)

/**
 * Electronic Graphics Tablet Screen for PocketHID.
 *
 * Turns the phone screen into a professional portable graphics tablet (90-95% active canvas area).
 * Single finger touch directly drives the active application on the PC host via standard
 * Bluetooth HID Mouse (Left Button Down on touch, relative motion deltas, Left Button Up on release).
 */
@Composable
fun DrawingScreen(
    tabletController: DrawingTabletController,
    isFocusLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by tabletController.state.collectAsState()
    val targetMode by tabletController.targetMode.collectAsState()
    val configuration = LocalConfiguration.current

    LaunchedEffect(configuration.orientation) {
        val orient = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TabletOrientation.LANDSCAPE
        } else {
            TabletOrientation.PORTRAIT
        }
        tabletController.updateOrientation(orient)
    }

    // Safety cleanup: Ensure left mouse button is ALWAYS released when leaving this screen
    DisposableEffect(tabletController) {
        onDispose {
            tabletController.releaseDrawing()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // 1. Dominant Touch Surface & Canvas (95% Screen Footprint)
        DrawingCanvas(
            strokes = state.strokes,
            currentDraftStroke = state.currentDraftStroke,
            tabletController = tabletController,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Slim Top Status Strip (Minimal Chrome ~24dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (tabletController.isDrawingActive) CyanAccent else Color(0xFF10B981))
                )
                val statusTitle = when {
                    tabletController.isDrawingActive && targetMode == DrawingTargetMode.ABSOLUTE_TABLET -> "DRAWING (ABS TABLET)"
                    tabletController.isDrawingActive -> "DRAWING (REL MOUSE)"
                    targetMode == DrawingTargetMode.ABSOLUTE_TABLET -> "GRAPHICS TABLET (ABS)"
                    else -> "GRAPHICS TABLET (REL)"
                }
                Text(
                    text = statusTitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tabletController.isDrawingActive) CyanAccent else TextMuted,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isFocusLocked) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyanAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = CyanAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "FOCUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // 3. Compact Floating Tablet Toolbar (Bottom-Right Anchor)
        FloatingTabletToolbar(
            tabletController = tabletController,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/**
 * Overload for backward compatibility and direct controller injection.
 */
@Composable
fun DrawingScreen(
    controller: DrawingController,
    transport: InputTransport? = null,
    hostOs: HostOs = HostOs.WINDOWS,
    isFocusLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tabletController = remember(controller, transport, hostOs) {
        DrawingTabletController(
            drawingController = controller,
            transportProvider = { transport },
            hostOsProvider = { hostOs },
            scope = scope
        )
    }

    DrawingScreen(
        tabletController = tabletController,
        isFocusLocked = isFocusLocked,
        modifier = modifier
    )
}

/**
 * Interactive Canvas surface.
 * Intercepts touch events and routes them to [DrawingTabletController].
 * Multi-touch is rejected for safety; 1 finger triggers HID mouse drawing.
 */
@Composable
private fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    currentDraftStroke: DrawingStroke?,
    tabletController: DrawingTabletController,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .onSizeChanged { size ->
                tabletController.updateCanvasSize(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(tabletController) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
                    val activePointerCount = currentEvent.changes.size

                    if (activePointerCount == 1) {
                        tabletController.onTouchDown(
                            point = DrawingPoint(down.position.x, down.position.y),
                            pointerCount = 1
                        )
                        down.consume()
                    } else {
                        tabletController.releaseDrawing()
                        return@awaitEachGesture
                    }

                    var isStrokeCommitted = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val pointerCount = event.changes.size

                            if (pointerCount > 1) {
                                // Multi-touch appeared during stroke: abort stroke for safety
                                tabletController.releaseDrawing()
                                break
                            }

                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                // Touch Up: commit stroke and release left mouse button
                                tabletController.onTouchUp()
                                change.consume()
                                isStrokeCommitted = true
                                break
                            }

                            tabletController.onTouchMove(
                                point = DrawingPoint(change.position.x, change.position.y),
                                pointerCount = 1
                            )
                            change.consume()
                        }
                    } finally {
                        if (!isStrokeCommitted && tabletController.isDrawingActive) {
                            tabletController.releaseDrawing()
                        }
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

        // Draw committed local preview strokes
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
    if (stroke.tool == DrawingTool.ERASER) return // Eraser removes strokes from local preview

    val strokeColor = Color(stroke.color.argb).copy(alpha = stroke.opacity)
    val strokeWidthPx = stroke.width.dp.toPx()

    if (stroke.points.size == 1) {
        val single = stroke.points[0]
        drawCircle(
            color = strokeColor,
            radius = strokeWidthPx / 2f,
            center = Offset(single.x, single.y)
        )
        return
    }

    val path = Path()
    path.moveTo(stroke.points[0].x, stroke.points[0].y)

    for (i in 1 until stroke.points.size) {
        val prev = stroke.points[i - 1]
        val curr = stroke.points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    val last = stroke.points.last()
    path.lineTo(last.x, last.y)

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
