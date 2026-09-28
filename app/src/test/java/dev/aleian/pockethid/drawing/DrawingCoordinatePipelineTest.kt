package dev.aleian.pockethid.drawing

import dev.aleian.pockethid.drawing.model.AspectRatioPolicy
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.TabletCalibration
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Coordinate Mapping and Transformation Pipeline Tests.
 *
 * Validates Section 6, 7, 8, 9, 10, 11, 14, and 19 of the specification.
 */
class DrawingCoordinatePipelineTest {

    private lateinit var pipeline: DrawingCoordinatePipeline

    // Standard test canvas: 1920x1080 (16:9)
    private val canvasW = 1920f
    private val canvasH = 1080f

    @Before
    fun setUp() {
        pipeline = DrawingCoordinatePipeline(
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            orientation = TabletOrientation.LANDSCAPE,
            aspectRatioPolicy = AspectRatioPolicy.STRETCH, // pure 1:1 for corner tests
            hostAspectRatio = 16f / 9f,
            calibration = TabletCalibration.DEFAULT
        )
    }

    @Test
    fun testTopLeftMapsToMinimum() {
        val result = pipeline.transform(canvasX = 0f, canvasY = 0f)
        assertEquals(0f, result.normalizedPoint.x, 0.0001f)
        assertEquals(0f, result.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, result.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, result.hidPoint.y)
    }

    @Test
    fun testTopRightMapsToMaximumX() {
        val result = pipeline.transform(canvasX = canvasW, canvasY = 0f)
        assertEquals(1.0f, result.normalizedPoint.x, 0.0001f)
        assertEquals(0.0f, result.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, result.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, result.hidPoint.y)
    }

    @Test
    fun testBottomLeftMapsToMaximumY() {
        val result = pipeline.transform(canvasX = 0f, canvasY = canvasH)
        assertEquals(0.0f, result.normalizedPoint.x, 0.0001f)
        assertEquals(1.0f, result.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, result.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, result.hidPoint.y)
    }

    @Test
    fun testBottomRightMapsToMaximum() {
        val result = pipeline.transform(canvasX = canvasW, canvasY = canvasH)
        assertEquals(1.0f, result.normalizedPoint.x, 0.0001f)
        assertEquals(1.0f, result.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, result.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, result.hidPoint.y)
    }

    @Test
    fun testCenterMapsToMidpoint() {
        val result = pipeline.transform(canvasX = canvasW / 2f, canvasY = canvasH / 2f)
        assertEquals(0.5f, result.normalizedPoint.x, 0.001f)
        assertEquals(0.5f, result.normalizedPoint.y, 0.001f)

        // Midpoint of 0..32767 is 16384 (or 16383.5)
        assertTrue("X should be near 16384", abs(result.hidPoint.x - 16384) <= 1)
        assertTrue("Y should be near 16384", abs(result.hidPoint.y - 16384) <= 1)
    }

    @Test
    fun testCanvasInsetsDoNotIntroduceOffset() {
        // Phone with Top Status Bar: 80px, Bottom Navigation Bar: 100px
        pipeline.insetsLeft = 0f
        pipeline.insetsTop = 80f
        pipeline.insetsRight = 0f
        pipeline.insetsBottom = 100f

        // Touch at top edge of the active drawing surface (y = 80f)
        val topResult = pipeline.transform(canvasX = 0f, canvasY = 80f)
        assertEquals("Safe-area corrected Y should be 0", 0f, topResult.safeAreaPoint.y, 0.001f)
        assertEquals("Normalized Y should be 0.0", 0f, topResult.normalizedPoint.y, 0.001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, topResult.hidPoint.y)

        // Touch at bottom edge of active drawing surface (y = 1080 - 100 = 980f)
        val bottomResult = pipeline.transform(canvasX = 0f, canvasY = 980f)
        assertEquals("Normalized Y should be 1.0 at active bottom", 1.0f, bottomResult.normalizedPoint.y, 0.001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, bottomResult.hidPoint.y)
    }

    @Test
    fun testCoordinateClamping() {
        // Touch beyond boundaries (e.g. gesture slop or margin touch)
        val underflow = pipeline.transform(canvasX = -50f, canvasY = -50f)
        assertEquals(0f, underflow.normalizedPoint.x, 0.0001f)
        assertEquals(0f, underflow.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, underflow.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MIN, underflow.hidPoint.y)

        val overflow = pipeline.transform(canvasX = canvasW + 100f, canvasY = canvasH + 100f)
        assertEquals(1.0f, overflow.normalizedPoint.x, 0.0001f)
        assertEquals(1.0f, overflow.normalizedPoint.y, 0.0001f)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, overflow.hidPoint.x)
        assertEquals(HidConstants.TABLET_LOGICAL_MAX, overflow.hidPoint.y)
    }

    @Test
    fun testLandscapeMapping() {
        pipeline.orientation = TabletOrientation.LANDSCAPE
        val result = pipeline.transform(canvasX = 480f, canvasY = 270f)
        assertEquals(0.25f, result.normalizedPoint.x, 0.001f)
        assertEquals(0.25f, result.normalizedPoint.y, 0.001f)

        // Reverse landscape (180 degree rotation)
        pipeline.orientation = TabletOrientation.REVERSE_LANDSCAPE
        val revResult = pipeline.transform(canvasX = 480f, canvasY = 270f)
        assertEquals(0.75f, revResult.normalizedPoint.x, 0.001f)
        assertEquals(0.75f, revResult.normalizedPoint.y, 0.001f)
    }

    @Test
    fun testPortraitMappingIfSupported() {
        // Portrait device: 1080 x 1920
        val portraitPipeline = DrawingCoordinatePipeline(
            canvasWidth = 1080f,
            canvasHeight = 1920f,
            orientation = TabletOrientation.PORTRAIT,
            aspectRatioPolicy = AspectRatioPolicy.STRETCH
        )

        // In portrait mode rotated to landscape PC, touch (0, 0) maps to top-right or consistent origin
        val result = portraitPipeline.transform(canvasX = 0f, canvasY = 0f)
        assertTrue("HID X in valid range", result.hidPoint.x in 0..HidConstants.TABLET_LOGICAL_MAX)
        assertTrue("HID Y in valid range", result.hidPoint.y in 0..HidConstants.TABLET_LOGICAL_MAX)
    }

    @Test
    fun testAspectRatioPolicy() {
        // Phone canvas is ultra-wide (21:9 = 2520 x 1080), host screen is 16:9 (1920 x 1080)
        val ultraWidePipeline = DrawingCoordinatePipeline(
            canvasWidth = 2520f,
            canvasHeight = 1080f,
            orientation = TabletOrientation.LANDSCAPE,
            aspectRatioPolicy = AspectRatioPolicy.FIT,
            hostAspectRatio = 16f / 9f
        )

        // Center should always map to center regardless of policy
        val centerResult = ultraWidePipeline.transform(canvasX = 2520f / 2f, canvasY = 1080f / 2f)
        assertEquals(0.5f, centerResult.normalizedPoint.x, 0.01f)
        assertEquals(0.5f, centerResult.normalizedPoint.y, 0.01f)

        // Left pillarbox margin: active content width is 1080 * (16/9) = 1920f.
        // Margin X = (2520 - 1920) / 2 = 300f.
        val leftActiveEdge = ultraWidePipeline.transform(canvasX = 300f, canvasY = 540f)
        assertEquals(0.0f, leftActiveEdge.normalizedPoint.x, 0.01f)

        val rightActiveEdge = ultraWidePipeline.transform(canvasX = 2220f, canvasY = 540f)
        assertEquals(1.0f, rightActiveEdge.normalizedPoint.x, 0.01f)
    }

    @Test
    fun testCalibrationTransform() {
        // Calibration with 10% scaling and 5% offset
        val calib = TabletCalibration(
            scaleX = 0.9f,
            scaleY = 0.9f,
            offsetX = 0.05f,
            offsetY = 0.05f
        )
        pipeline.calibration = calib

        val result = pipeline.transform(canvasX = canvasW / 2f, canvasY = canvasH / 2f)
        // 0.5 * 0.9 + 0.05 = 0.50
        assertEquals(0.5f, result.calibratedPoint.x, 0.001f)
        assertEquals(0.5f, result.calibratedPoint.y, 0.001f)

        val originResult = pipeline.transform(canvasX = 0f, canvasY = 0f)
        // 0 * 0.9 + 0.05 = 0.05
        assertEquals(0.05f, originResult.calibratedPoint.x, 0.001f)
        assertEquals(0.05f, originResult.calibratedPoint.y, 0.001f)
    }

    @Test
    fun testCalibrationDerivationFromCorners() {
        val derived = TabletCalibration.fromCorners(
            topLeft = DrawingPoint(100f, 100f),
            topRight = DrawingPoint(1900f, 100f),
            bottomRight = DrawingPoint(1900f, 980f),
            bottomLeft = DrawingPoint(100f, 980f),
            canvasWidth = 2000f,
            canvasHeight = 1000f
        )
        // Width measured: (1900-100)/2000 = 1800/2000 = 0.9
        // Height measured: (980-100)/1000 = 880/1000 = 0.88
        assertEquals(1.0f / 0.9f, derived.scaleX, 0.01f)
        assertEquals(1.0f / 0.88f, derived.scaleY, 0.01f)
    }
}
