package dev.aleian.pockethid.drawing

import dev.aleian.pockethid.drawing.model.AspectRatioPolicy
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.HidTabletPoint
import dev.aleian.pockethid.drawing.model.TabletCalibration
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.HidConstants
import kotlin.math.roundToInt

/**
 * Diagnostic container for each stage of the coordinate pipeline.
 */
data class PipelineResult(
    val rawPoint: DrawingPoint,
    val canvasPoint: DrawingPoint,
    val safeAreaPoint: DrawingPoint,
    val orientedPoint: DrawingPoint,
    val normalizedPoint: DrawingPoint,
    val calibratedPoint: DrawingPoint,
    val hidPoint: HidTabletPoint
)

/**
 * Explicit Coordinate Transformation Pipeline for PocketHID Graphics Tablet.
 *
 * Pipeline architecture:
 *   RAW TOUCH
 *       ↓ (Canvas origin subtraction)
 *   Canvas-local coordinate
 *       ↓ (Safe-area inset correction)
 *   Safe-area corrected coordinate
 *       ↓ (Orientation transform: landscape / portrait / reverse)
 *   Oriented coordinate
 *       ↓ (Aspect ratio policy: FIT letterbox or STRETCH, clamped 0..1)
 *   Normalized coordinate [0..1]
 *       ↓ (Calibration scale & offset)
 *   Calibration transform
 *       ↓ (Logical range mapping 0..32767)
 *   HID coordinate (Report ID 5)
 *       ↓ (Bluetooth HID)
 *   PC Cursor / Pen Contact
 */
class DrawingCoordinatePipeline(
    var canvasWidth: Float = 0f,
    var canvasHeight: Float = 0f,
    var insetsLeft: Float = 0f,
    var insetsTop: Float = 0f,
    var insetsRight: Float = 0f,
    var insetsBottom: Float = 0f,
    var orientation: TabletOrientation = TabletOrientation.LANDSCAPE,
    var aspectRatioPolicy: AspectRatioPolicy = AspectRatioPolicy.FIT,
    var hostAspectRatio: Float = 16f / 9f,
    var calibration: TabletCalibration = TabletCalibration.DEFAULT
) {

    // Stage 1: Raw Touch -> Canvas-local coordinate
    fun rawToCanvas(rawPoint: DrawingPoint, canvasOriginX: Float = 0f, canvasOriginY: Float = 0f): DrawingPoint {
        return DrawingPoint(rawPoint.x - canvasOriginX, rawPoint.y - canvasOriginY)
    }

    // Stage 2: Safe-area correction
    fun applySafeArea(canvasPoint: DrawingPoint): DrawingPoint {
        val activeWidth = maxOf(1f, canvasWidth - insetsLeft - insetsRight)
        val activeHeight = maxOf(1f, canvasHeight - insetsTop - insetsBottom)
        val clampedX = (canvasPoint.x - insetsLeft).coerceIn(0f, activeWidth)
        val clampedY = (canvasPoint.y - insetsTop).coerceIn(0f, activeHeight)
        return DrawingPoint(clampedX, clampedY)
    }

    // Stage 3: Orientation transform
    fun applyOrientation(point: DrawingPoint, width: Float, height: Float, orient: TabletOrientation): DrawingPoint {
        return when (orient) {
            TabletOrientation.LANDSCAPE -> point
            TabletOrientation.REVERSE_LANDSCAPE -> DrawingPoint(width - point.x, height - point.y)
            TabletOrientation.PORTRAIT -> DrawingPoint(point.y, width - point.x)
            TabletOrientation.REVERSE_PORTRAIT -> DrawingPoint(height - point.y, point.x)
        }
    }

    // Stage 4: Normalization [0..1] with Aspect Ratio policy (FIT or STRETCH)
    fun normalize(
        point: DrawingPoint,
        activeWidth: Float,
        activeHeight: Float,
        policy: AspectRatioPolicy = aspectRatioPolicy,
        hostAspect: Float = hostAspectRatio
    ): DrawingPoint {
        if (activeWidth <= 0f || activeHeight <= 0f) return DrawingPoint(0f, 0f)

        return when (policy) {
            AspectRatioPolicy.STRETCH -> {
                DrawingPoint(
                    x = (point.x / activeWidth).coerceIn(0f, 1f),
                    y = (point.y / activeHeight).coerceIn(0f, 1f)
                )
            }
            AspectRatioPolicy.FIT -> {
                val canvasAspect = activeWidth / activeHeight
                if (canvasAspect > hostAspect) {
                    // Phone canvas is wider than host screen -> letterbox horizontally
                    val contentWidth = activeHeight * hostAspect
                    val marginX = (activeWidth - contentWidth) / 2f
                    val normX = ((point.x - marginX) / contentWidth).coerceIn(0f, 1f)
                    val normY = (point.y / activeHeight).coerceIn(0f, 1f)
                    DrawingPoint(normX, normY)
                } else {
                    // Phone canvas is taller than host screen -> letterbox vertically
                    val contentHeight = activeWidth / hostAspect
                    val marginY = (activeHeight - contentHeight) / 2f
                    val normX = (point.x / activeWidth).coerceIn(0f, 1f)
                    val normY = ((point.y - marginY) / contentHeight).coerceIn(0f, 1f)
                    DrawingPoint(normX, normY)
                }
            }
        }
    }

    // Stage 5: Calibration transform
    fun applyCalibration(normPoint: DrawingPoint, calib: TabletCalibration = calibration): DrawingPoint {
        val (cx, cy) = calib.apply(normPoint.x, normPoint.y)
        return DrawingPoint(cx, cy)
    }

    // Stage 6: Normalized to HID coordinates (0..32767)
    fun normalizedToHid(
        normPoint: DrawingPoint,
        logicalMaxX: Int = HidConstants.TABLET_LOGICAL_MAX,
        logicalMaxY: Int = HidConstants.TABLET_LOGICAL_MAX
    ): HidTabletPoint {
        val hidX = (normPoint.x * logicalMaxX).roundToInt().coerceIn(0, logicalMaxX)
        val hidY = (normPoint.y * logicalMaxY).roundToInt().coerceIn(0, logicalMaxY)
        return HidTabletPoint(hidX, hidY)
    }

    /**
     * Executes the complete end-to-end pipeline transformation.
     */
    fun transform(
        canvasX: Float,
        canvasY: Float,
        rawX: Float = canvasX,
        rawY: Float = canvasY
    ): PipelineResult {
        val rawPoint = DrawingPoint(rawX, rawY)
        val canvasPoint = DrawingPoint(canvasX, canvasY)
        val safePoint = applySafeArea(canvasPoint)

        val activeWidth = maxOf(1f, canvasWidth - insetsLeft - insetsRight)
        val activeHeight = maxOf(1f, canvasHeight - insetsTop - insetsBottom)

        val orientedPoint = applyOrientation(safePoint, activeWidth, activeHeight, orientation)
        val (effWidth, effHeight) = if (orientation == TabletOrientation.PORTRAIT || orientation == TabletOrientation.REVERSE_PORTRAIT) {
            Pair(activeHeight, activeWidth)
        } else {
            Pair(activeWidth, activeHeight)
        }

        val normPoint = normalize(orientedPoint, effWidth, effHeight, aspectRatioPolicy, hostAspectRatio)
        val calibPoint = applyCalibration(normPoint, calibration)
        val hidPoint = normalizedToHid(calibPoint)

        return PipelineResult(
            rawPoint = rawPoint,
            canvasPoint = canvasPoint,
            safeAreaPoint = safePoint,
            orientedPoint = orientedPoint,
            normalizedPoint = normPoint,
            calibratedPoint = calibPoint,
            hidPoint = hidPoint
        )
    }
}
