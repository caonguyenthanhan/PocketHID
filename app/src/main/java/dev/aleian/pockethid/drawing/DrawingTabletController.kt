package dev.aleian.pockethid.drawing

import dev.aleian.pockethid.action.ActionDispatcher
import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.action.PocketAction
import dev.aleian.pockethid.drawing.model.AspectRatioPolicy
import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingState
import dev.aleian.pockethid.drawing.model.DrawingTargetMode
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.drawing.model.TabletCalibration
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Graphics Tablet Controller for PocketHID.
 *
 * Implements two distinct target modes:
 * 1. MODE 2 (Default): True Absolute HID Tablet / Digitizer (Report ID 5).
 *    Normalizes phone touch coordinates [0..1] and maps them directly to PC tablet logical range (0..32767).
 *    Touch DOWN -> sends Tip Switch = 1, In Range = 1 with absolute X/Y.
 *    Touch MOVE -> sends Tip Switch = 1 with updated absolute X/Y.
 *    Touch UP -> sends Tip Switch = 0, In Range = 1 (hover/lift), then neutral.
 * 2. MODE 1 (Fallback): Relative Mouse Drawing (Report ID 2).
 *    Precision trackpad surface with explicit reference origin and sub-pixel accumulation.
 *
 * Architecture:
 * - Safety Lifecycle -> Guarantees Tip Switch and Left Mouse Button release on multi-touch,
 *   screen leave, Bluetooth disconnect, and app background.
 * - Local Preview -> Canonical coordinates are shared between local rendering and HID output.
 * - Floating Toolbar -> Collapsed by default, minimal controls on demand.
 */
class DrawingTabletController(
    val drawingController: DrawingController = DrawingController(),
    private val transportProvider: () -> InputTransport? = { null },
    private val hostOsProvider: () -> HostOs = { HostOs.WINDOWS },
    private val scope: CoroutineScope? = null,
    var sensitivity: Float = 1.0f,
    initialTargetMode: DrawingTargetMode = DrawingTargetMode.ABSOLUTE_TABLET,
    val pipeline: DrawingCoordinatePipeline = DrawingCoordinatePipeline()
) {
    // Target Mode state: Absolute Tablet (Default) vs Relative Mouse (Fallback)
    private val _targetMode = MutableStateFlow(initialTargetMode)
    val targetMode: StateFlow<DrawingTargetMode> = _targetMode.asStateFlow()

    // Floating Toolbar state: collapsed by default per specification
    private val _isToolbarExpanded = MutableStateFlow(false)
    val isToolbarExpanded: StateFlow<Boolean> = _isToolbarExpanded.asStateFlow()

    private val _isColorPickerExpanded = MutableStateFlow(false)
    val isColorPickerExpanded: StateFlow<Boolean> = _isColorPickerExpanded.asStateFlow()

    // Tablet touch state
    private var isDrawing = false
    private var isTipDown = false
    private var isButtonDown = false
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var remainderX: Float = 0f
    private var remainderY: Float = 0f

    private var lastHidX: Int = 0
    private var lastHidY: Int = 0

    val isDrawingActive: Boolean
        get() = isDrawing

    val isLeftButtonDown: Boolean
        get() = if (_targetMode.value == DrawingTargetMode.ABSOLUTE_TABLET) isTipDown else isButtonDown

    val isTipSwitchDown: Boolean
        get() = isTipDown

    val state: StateFlow<DrawingState>
        get() = drawingController.state

    // --- Configuration & Sizing ---

    fun setTargetMode(mode: DrawingTargetMode) {
        if (_targetMode.value != mode) {
            releaseDrawing()
            _targetMode.value = mode
        }
    }

    fun toggleTargetMode() {
        val next = if (_targetMode.value == DrawingTargetMode.ABSOLUTE_TABLET) {
            DrawingTargetMode.RELATIVE_MOUSE
        } else {
            DrawingTargetMode.ABSOLUTE_TABLET
        }
        setTargetMode(next)
    }

    fun updateCanvasSize(width: Float, height: Float) {
        pipeline.canvasWidth = width
        pipeline.canvasHeight = height
    }

    fun updateInsets(left: Float, top: Float, right: Float, bottom: Float) {
        pipeline.insetsLeft = left
        pipeline.insetsTop = top
        pipeline.insetsRight = right
        pipeline.insetsBottom = bottom
    }

    fun updateOrientation(orientation: TabletOrientation) {
        pipeline.orientation = orientation
    }

    fun setAspectRatioPolicy(policy: AspectRatioPolicy) {
        pipeline.aspectRatioPolicy = policy
    }

    fun setCalibration(calibration: TabletCalibration) {
        pipeline.calibration = calibration
    }

    // --- Touch Lifecycle ---

    /**
     * Touch Down: Begins a drawing stroke.
     * Rejects multi-touch (pointerCount > 1).
     * Dispatches either Absolute Tablet Report (ID 5) or Relative Mouse Button DOWN (ID 2).
     */
    fun onTouchDown(point: DrawingPoint, pointerCount: Int = 1) {
        if (pointerCount > 1) {
            releaseDrawing()
            return
        }

        // Auto-collapse floating toolbar when user touches the canvas to draw
        if (_isToolbarExpanded.value) {
            _isToolbarExpanded.value = false
            _isColorPickerExpanded.value = false
        }

        isDrawing = true
        lastX = point.x
        lastY = point.y
        remainderX = 0f
        remainderY = 0f

        if (_targetMode.value == DrawingTargetMode.ABSOLUTE_TABLET) {
            isTipDown = true
            isButtonDown = false

            val result = pipeline.transform(canvasX = point.x, canvasY = point.y)
            lastHidX = result.hidPoint.x
            lastHidY = result.hidPoint.y

            var status = (HidConstants.TABLET_STATUS_TIP_SWITCH.toInt() or HidConstants.TABLET_STATUS_IN_RANGE.toInt()).toByte()
            if (state.value.activeTool == DrawingTool.ERASER) {
                status = (status.toInt() or HidConstants.TABLET_STATUS_ERASER.toInt()).toByte()
            }

            val reportHex = String.format(
                "%02X %02X %02X %02X %02X",
                status,
                lastHidX and 0xFF,
                (lastHidX shr 8) and 0xFF,
                lastHidY and 0xFF,
                (lastHidY shr 8) and 0xFF
            )

            transportProvider()?.sendTabletReport(status, lastHidX, lastHidY)

            TabletDiagnosticsHub.update(
                rawX = result.rawPoint.x,
                rawY = result.rawPoint.y,
                canvasX = result.canvasPoint.x,
                canvasY = result.canvasPoint.y,
                safeX = result.safeAreaPoint.x,
                safeY = result.safeAreaPoint.y,
                normalizedX = result.normalizedPoint.x,
                normalizedY = result.normalizedPoint.y,
                hidX = lastHidX,
                hidY = lastHidY,
                orientation = pipeline.orientation.name,
                canvasWidth = pipeline.canvasWidth,
                canvasHeight = pipeline.canvasHeight,
                insetsDesc = "L${pipeline.insetsLeft.toInt()} T${pipeline.insetsTop.toInt()} R${pipeline.insetsRight.toInt()} B${pipeline.insetsBottom.toInt()}",
                targetMode = _targetMode.value.name,
                aspectPolicy = pipeline.aspectRatioPolicy.name,
                isTipDown = true,
                isInRange = true,
                activeTool = state.value.activeTool.name,
                lastReportHex = reportHex
            )
        } else {
            isTipDown = false
            isButtonDown = true

            // Send Relative Mouse: Left Button DOWN (0 dx, 0 dy)
            transportProvider()?.sendMouseMove(
                dx = 0,
                dy = 0,
                buttons = HidConstants.MOUSE_BUTTON_LEFT,
                wheel = 0
            )
        }

        // Start local preview stroke with canonical canvas point
        drawingController.startStroke(point, pointerCount = 1)
    }

    /**
     * Touch Move: Continues drawing.
     * In Absolute mode: computes pipeline transformation and sends absolute coordinates with Tip Switch DOWN.
     * In Relative mode: computes relative delta with sub-pixel accumulation.
     */
    fun onTouchMove(point: DrawingPoint, pointerCount: Int = 1) {
        if (!isDrawing) return

        if (pointerCount > 1) {
            // Multi-touch detected during drawing: abort stroke immediately for safety
            releaseDrawing()
            return
        }

        if (_targetMode.value == DrawingTargetMode.ABSOLUTE_TABLET) {
            val result = pipeline.transform(canvasX = point.x, canvasY = point.y)
            lastHidX = result.hidPoint.x
            lastHidY = result.hidPoint.y

            var status = (HidConstants.TABLET_STATUS_TIP_SWITCH.toInt() or HidConstants.TABLET_STATUS_IN_RANGE.toInt()).toByte()
            if (state.value.activeTool == DrawingTool.ERASER) {
                status = (status.toInt() or HidConstants.TABLET_STATUS_ERASER.toInt()).toByte()
            }

            val reportHex = String.format(
                "%02X %02X %02X %02X %02X",
                status,
                lastHidX and 0xFF,
                (lastHidX shr 8) and 0xFF,
                lastHidY and 0xFF,
                (lastHidY shr 8) and 0xFF
            )

            transportProvider()?.sendTabletReport(status, lastHidX, lastHidY)

            TabletDiagnosticsHub.update(
                rawX = result.rawPoint.x,
                rawY = result.rawPoint.y,
                canvasX = result.canvasPoint.x,
                canvasY = result.canvasPoint.y,
                safeX = result.safeAreaPoint.x,
                safeY = result.safeAreaPoint.y,
                normalizedX = result.normalizedPoint.x,
                normalizedY = result.normalizedPoint.y,
                hidX = lastHidX,
                hidY = lastHidY,
                orientation = pipeline.orientation.name,
                canvasWidth = pipeline.canvasWidth,
                canvasHeight = pipeline.canvasHeight,
                insetsDesc = "L${pipeline.insetsLeft.toInt()} T${pipeline.insetsTop.toInt()} R${pipeline.insetsRight.toInt()} B${pipeline.insetsBottom.toInt()}",
                targetMode = _targetMode.value.name,
                aspectPolicy = pipeline.aspectRatioPolicy.name,
                isTipDown = true,
                isInRange = true,
                activeTool = state.value.activeTool.name,
                lastReportHex = reportHex
            )
        } else {
            val rawDx = (point.x - lastX) * sensitivity + remainderX
            val rawDy = (point.y - lastY) * sensitivity + remainderY

            val moveX = rawDx.toInt().coerceIn(-127, 127)
            val moveY = rawDy.toInt().coerceIn(-127, 127)

            remainderX = rawDx - moveX
            remainderY = rawDy - moveY

            lastX = point.x
            lastY = point.y

            transportProvider()?.sendMouseMove(
                dx = moveX,
                dy = moveY,
                buttons = HidConstants.MOUSE_BUTTON_LEFT,
                wheel = 0
            )
        }

        // Append to local preview
        drawingController.appendPoint(point, pointerCount = 1)
    }

    /**
     * Touch Up: Commits the stroke and releases the active contact / button.
     */
    fun onTouchUp() {
        if (!isDrawing && !isButtonDown && !isTipDown) return

        isDrawing = false
        remainderX = 0f
        remainderY = 0f

        if (_targetMode.value == DrawingTargetMode.ABSOLUTE_TABLET) {
            isTipDown = false
            // Send tip switch lifted (hover/in-range), then neutral
            transportProvider()?.sendTabletReport(HidConstants.TABLET_STATUS_IN_RANGE, lastHidX, lastHidY)
            transportProvider()?.sendTabletNeutral()
            TabletDiagnosticsHub.onContactRelease()
        } else {
            isButtonDown = false
            transportProvider()?.sendMouseMove(
                dx = 0,
                dy = 0,
                buttons = HidConstants.MOUSE_BUTTON_NONE,
                wheel = 0
            )
        }

        // Commit local preview stroke
        drawingController.finishStroke()
    }

    /**
     * Touch Cancel: Aborts the stroke and releases contact.
     */
    fun onTouchCancel() {
        releaseDrawing()
    }

    /**
     * Safety Release: Guarantees both Tablet Contact and Mouse Button are released on host PC
     * and resets active drawing state.
     * Called on screen exit, app background, Bluetooth disconnect, and multi-touch cancellation.
     */
    fun releaseDrawing() {
        val wasDrawing = isDrawing || isButtonDown || isTipDown
        isDrawing = false
        isTipDown = false
        isButtonDown = false
        remainderX = 0f
        remainderY = 0f

        // Guarantee release on BOTH channels
        transportProvider()?.sendTabletNeutral()
        transportProvider()?.sendMouseMove(
            dx = 0,
            dy = 0,
            buttons = HidConstants.MOUSE_BUTTON_NONE,
            wheel = 0
        )
        TabletDiagnosticsHub.onContactRelease()

        if (wasDrawing) {
            drawingController.cancelStroke()
        }
    }

    // --- Toolbar & Tool Actions ---

    fun toggleToolbar() {
        _isToolbarExpanded.update { !it }
        if (!_isToolbarExpanded.value) {
            _isColorPickerExpanded.value = false
        }
    }

    fun expandToolbar() {
        _isToolbarExpanded.value = true
    }

    fun collapseToolbar() {
        _isToolbarExpanded.value = false
        _isColorPickerExpanded.value = false
    }

    fun toggleColorPicker() {
        _isColorPickerExpanded.update { !it }
    }

    fun setTool(tool: DrawingTool) {
        drawingController.setTool(tool)
        // Auto-collapse after selecting tool for minimal screen footprint
        collapseToolbar()
    }

    fun setColor(color: DrawingColor) {
        drawingController.setColor(color)
        _isColorPickerExpanded.value = false
    }

    fun cycleStrokeWidth() {
        val current = state.value.strokeWidth
        val next = when (current) {
            2f -> 4f
            4f -> 8f
            8f -> 12f
            else -> 2f
        }
        drawingController.setStrokeWidth(next)
    }

    fun setStrokeWidth(width: Float) {
        drawingController.setStrokeWidth(width)
    }

    fun undo() {
        drawingController.undo()
        // Send OS-aware Undo (Ctrl+Z / Cmd+Z) to PC application
        val transport = transportProvider()
        if (transport != null && scope != null) {
            scope.launch {
                ActionDispatcher.dispatch(
                    action = PocketAction.EditAction.Undo,
                    transport = transport,
                    hostOs = hostOsProvider()
                )
            }
        }
    }

    fun redo() {
        drawingController.redo()
        // Send OS-aware Redo (Ctrl+Y / Cmd+Shift+Z) to PC application
        val transport = transportProvider()
        if (transport != null && scope != null) {
            scope.launch {
                ActionDispatcher.dispatch(
                    action = PocketAction.EditAction.Redo,
                    transport = transport,
                    hostOs = hostOsProvider()
                )
            }
        }
    }

    fun requestClear() {
        drawingController.requestClear()
    }

    fun confirmClear() {
        drawingController.confirmClear()
        collapseToolbar()
    }

    fun cancelClear() {
        drawingController.cancelClear()
    }
}
