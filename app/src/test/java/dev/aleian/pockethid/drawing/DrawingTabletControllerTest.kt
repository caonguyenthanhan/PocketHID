package dev.aleian.pockethid.drawing

import android.bluetooth.BluetoothDevice
import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.drawing.model.AspectRatioPolicy
import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingTargetMode
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DrawingTabletControllerTest {

    private class RecordingInputTransport : InputTransport {
        override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        override val isSupported: Boolean = true
        override val connectedDevice: BluetoothDevice? = null
        override val isConnected: Boolean = true

        val mouseReports = mutableListOf<MouseReport>()
        data class MouseReport(val dx: Int, val dy: Int, val buttons: Byte, val wheel: Int)

        val tabletReports = mutableListOf<TabletReport>()
        data class TabletReport(val status: Byte, val x: Int, val y: Int)

        val keyClicks = mutableListOf<KeyClick>()
        data class KeyClick(val keyCode: Byte, val modifiers: Byte)

        override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean {
            mouseReports.add(MouseReport(dx, dy, buttons, wheel))
            return true
        }

        override fun sendTabletReport(status: Byte, x: Int, y: Int): Boolean {
            tabletReports.add(TabletReport(status, x, y))
            return true
        }

        override fun sendTabletNeutral(): Boolean {
            tabletReports.add(TabletReport(HidConstants.TABLET_STATUS_NONE, 0, 0))
            return true
        }

        override suspend fun sendMouseClick(buttons: Byte) {}
        override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte): Boolean = true
        override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean = true
        override fun sendKeyRelease(): Boolean = true
        override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {
            keyClicks.add(KeyClick(keyCode, modifiers))
        }
        override fun sendConsumerClick(usageCode: Int): Boolean = true
        override fun sendConsumerPress(usageCode: Int): Boolean = true
        override fun sendConsumerRelease(): Boolean = true
        override fun sendGamepadReport(
            buttons: Int,
            hat: Byte,
            leftX: Short,
            leftY: Short,
            rightX: Short,
            rightY: Short,
            leftTrigger: Byte,
            rightTrigger: Byte
        ): Boolean = true
        override fun sendGamepadNeutral(): Boolean = true
        override fun register() {}
        override fun unregister() {}
        override fun syncConnectionState() {}
        override fun connect(device: BluetoothDevice): Boolean = true
        override fun disconnect(): Boolean = true
    }

    private lateinit var transport: RecordingInputTransport
    private lateinit var tabletController: DrawingTabletController

    @Before
    fun setUp() {
        transport = RecordingInputTransport()
        tabletController = DrawingTabletController(
            transportProvider = { transport },
            hostOsProvider = { HostOs.WINDOWS },
            scope = CoroutineScope(Dispatchers.Unconfined),
            sensitivity = 1.0f,
            initialTargetMode = DrawingTargetMode.RELATIVE_MOUSE
        )
    }

    @Test
    fun testTouchDownSendsLeftButtonDown() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)

        assertTrue("Drawing should be active after Touch Down", tabletController.isDrawingActive)
        assertTrue("Left button must be marked DOWN", tabletController.isLeftButtonDown)
        assertEquals("Should send 1 report on Touch Down", 1, transport.mouseReports.size)

        val report = transport.mouseReports[0]
        assertEquals("dx should be 0 on initial touch down", 0, report.dx)
        assertEquals("dy should be 0 on initial touch down", 0, report.dy)
        assertEquals("Left mouse button must be DOWN", HidConstants.MOUSE_BUTTON_LEFT, report.buttons)

        // Local preview check
        assertNotNull("Local preview stroke should be initialized", tabletController.state.value.currentDraftStroke)
    }

    @Test
    fun testTouchMoveSendsDeltasWithLeftButtonHeld() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        transport.mouseReports.clear()

        // Move by +15 x, +25 y
        tabletController.onTouchMove(DrawingPoint(115f, 125f), pointerCount = 1)

        assertEquals("Should send 1 move report", 1, transport.mouseReports.size)
        val moveReport = transport.mouseReports[0]
        assertEquals(15, moveReport.dx)
        assertEquals(25, moveReport.dy)
        assertEquals("Left button must remain DOWN during move", HidConstants.MOUSE_BUTTON_LEFT, moveReport.buttons)
        assertTrue("Drawing remains active", tabletController.isDrawingActive)
    }

    @Test
    fun testSubPixelAccumulationPreservesFractionalDeltas() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        transport.mouseReports.clear()

        // Move 0.6px -> int delta is 0, remainder 0.6
        tabletController.onTouchMove(DrawingPoint(100.6f, 100f), pointerCount = 1)
        assertEquals(0, transport.mouseReports[0].dx)

        // Move another 0.6px -> accumulated is 1.2px -> int delta is 1, remainder 0.2
        tabletController.onTouchMove(DrawingPoint(101.2f, 100f), pointerCount = 1)
        assertEquals(1, transport.mouseReports[1].dx)
    }

    @Test
    fun testTouchUpSendsLeftButtonUpAndCommitsStroke() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        tabletController.onTouchMove(DrawingPoint(110f, 110f), pointerCount = 1)
        transport.mouseReports.clear()

        tabletController.onTouchUp()

        assertFalse("Drawing must not be active after Touch Up", tabletController.isDrawingActive)
        assertFalse("Left button must be released", tabletController.isLeftButtonDown)
        assertEquals("Should send 1 release report", 1, transport.mouseReports.size)

        val report = transport.mouseReports[0]
        assertEquals("Left button must be released (MOUSE_BUTTON_NONE)", HidConstants.MOUSE_BUTTON_NONE, report.buttons)
        assertEquals("Committed local preview stroke must be saved", 1, tabletController.state.value.strokes.size)
        assertNull("Draft stroke must be cleared", tabletController.state.value.currentDraftStroke)
    }

    @Test
    fun testTouchCancelSendsLeftButtonUpAndCancelsDraft() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        transport.mouseReports.clear()

        tabletController.onTouchCancel()

        assertFalse("Drawing must be inactive", tabletController.isDrawingActive)
        assertFalse("Left button must be released", tabletController.isLeftButtonDown)
        assertEquals(1, transport.mouseReports.size)
        assertEquals(HidConstants.MOUSE_BUTTON_NONE, transport.mouseReports[0].buttons)
        assertNull("Draft stroke must be cancelled", tabletController.state.value.currentDraftStroke)
    }

    @Test
    fun testSecondFingerDownReleasesLeftButtonAndAborts() {
        // Start stroke with 1 finger
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        assertTrue(tabletController.isDrawingActive)
        transport.mouseReports.clear()

        // Second finger appears during move -> aborts stroke per spec
        tabletController.onTouchMove(DrawingPoint(120f, 120f), pointerCount = 2)

        assertFalse("Stroke must be cancelled when second finger touches", tabletController.isDrawingActive)
        assertFalse("Left button must be released immediately", tabletController.isLeftButtonDown)
        assertEquals(1, transport.mouseReports.size)
        assertEquals("Must send release report", HidConstants.MOUSE_BUTTON_NONE, transport.mouseReports[0].buttons)
    }

    @Test
    fun testTouchDownWithMultipleFingersIsRejected() {
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 2)

        assertFalse("2+ fingers must not start drawing", tabletController.isDrawingActive)
        assertFalse(tabletController.isLeftButtonDown)
        // Must send neutral release to guarantee no stuck button
        assertEquals(1, transport.mouseReports.size)
        assertEquals(HidConstants.MOUSE_BUTTON_NONE, transport.mouseReports[0].buttons)
    }

    @Test
    fun testReleaseDrawingEnsuresNoStuckButton() {
        tabletController.onTouchDown(DrawingPoint(50f, 50f), pointerCount = 1)
        assertTrue(tabletController.isLeftButtonDown)
        transport.mouseReports.clear()

        // Call releaseDrawing (simulates disconnect, screen exit, or background)
        tabletController.releaseDrawing()

        assertFalse(tabletController.isDrawingActive)
        assertFalse(tabletController.isLeftButtonDown)
        assertEquals(1, transport.mouseReports.size)
        assertEquals("Must send MOUSE_BUTTON_NONE", HidConstants.MOUSE_BUTTON_NONE, transport.mouseReports[0].buttons)
    }

    @Test
    fun testDefaultToolbarStateIsCollapsed() {
        assertFalse("Floating toolbar must be collapsed by default", tabletController.isToolbarExpanded.value)
    }

    @Test
    fun testTouchDownAutoCollapsesExpandedToolbar() {
        tabletController.expandToolbar()
        assertTrue(tabletController.isToolbarExpanded.value)

        // User starts drawing on canvas -> toolbar should auto-collapse for zero distraction
        tabletController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)

        assertFalse("Toolbar must auto-collapse when drawing starts", tabletController.isToolbarExpanded.value)
    }

    @Test
    fun testToolSelectionAndStrokeWidthCycle() {
        tabletController.setTool(DrawingTool.ERASER)
        assertEquals(DrawingTool.ERASER, tabletController.state.value.activeTool)

        tabletController.setTool(DrawingTool.PEN)
        assertEquals(DrawingTool.PEN, tabletController.state.value.activeTool)

        // Cycle stroke width: 4 -> 8 -> 12 -> 2 -> 4
        tabletController.setStrokeWidth(4f)
        tabletController.cycleStrokeWidth()
        assertEquals(8f, tabletController.state.value.strokeWidth, 0.01f)
        tabletController.cycleStrokeWidth()
        assertEquals(12f, tabletController.state.value.strokeWidth, 0.01f)
        tabletController.cycleStrokeWidth()
        assertEquals(2f, tabletController.state.value.strokeWidth, 0.01f)
        tabletController.cycleStrokeWidth()
        assertEquals(4f, tabletController.state.value.strokeWidth, 0.01f)
    }

    @Test
    fun testColorSelectionAndClearDialog() {
        tabletController.setColor(DrawingColor.CYAN)
        assertEquals(DrawingColor.CYAN, tabletController.state.value.selectedColor)

        // Draw a stroke first so there is content to clear
        tabletController.onTouchDown(DrawingPoint(10f, 10f), 1)
        tabletController.onTouchUp()
        assertEquals(1, tabletController.state.value.strokes.size)

        // Clear request sets pending
        tabletController.requestClear()
        assertTrue(tabletController.state.value.isClearConfirmationPending)

        tabletController.cancelClear()
        assertFalse(tabletController.state.value.isClearConfirmationPending)
        assertEquals(1, tabletController.state.value.strokes.size)

        // Now confirm clear
        tabletController.requestClear()
        assertTrue(tabletController.state.value.isClearConfirmationPending)
        tabletController.confirmClear()
        assertFalse(tabletController.state.value.isClearConfirmationPending)
        assertEquals(0, tabletController.state.value.strokes.size)
    }

    @Test
    fun testUndoSendsCtrlZToWindowsHostAndRollsBackLocalPreview() {
        tabletController.onTouchDown(DrawingPoint(10f, 10f), 1)
        tabletController.onTouchUp()
        assertEquals(1, tabletController.state.value.strokes.size)

        tabletController.undo()

        assertEquals("Local preview strokes must be rolled back", 0, tabletController.state.value.strokes.size)
        assertEquals("Redo stack must contain rolled back stroke", 1, tabletController.state.value.redoStack.size)
        assertEquals("Must send 1 key click to host", 1, transport.keyClicks.size)
        assertEquals(HidConstants.KEY_Z, transport.keyClicks[0].keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, transport.keyClicks[0].modifiers)
    }

    @Test
    fun testRedoSendsCtrlYToWindowsHostAndRestoresLocalPreview() {
        tabletController.onTouchDown(DrawingPoint(10f, 10f), 1)
        tabletController.onTouchUp()
        tabletController.undo()
        transport.keyClicks.clear()

        tabletController.redo()

        assertEquals("Local preview stroke must be restored", 1, tabletController.state.value.strokes.size)
        assertEquals(0, tabletController.state.value.redoStack.size)
        assertEquals("Must send 1 key click to host", 1, transport.keyClicks.size)
        assertEquals(HidConstants.KEY_Y, transport.keyClicks[0].keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, transport.keyClicks[0].modifiers)
    }

    @Test
    fun testUndoAndRedoOnMacOSSendsCmdZAndCmdShiftZ() {
        val macController = DrawingTabletController(
            transportProvider = { transport },
            hostOsProvider = { HostOs.MACOS },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        macController.onTouchDown(DrawingPoint(10f, 10f), 1)
        macController.onTouchUp()
        transport.keyClicks.clear()

        macController.undo()
        assertEquals(HidConstants.KEY_Z, transport.keyClicks[0].keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, transport.keyClicks[0].modifiers)

        transport.keyClicks.clear()
        macController.redo()
        assertEquals(HidConstants.KEY_Z, transport.keyClicks[0].keyCode)
        val expectedMods = (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
        assertEquals(expectedMods, transport.keyClicks[0].modifiers)
    }

    // --- Absolute Tablet Tests (Option B) ---

    @Test
    fun testAbsoluteTabletTouchDownSendsTipSwitchDown() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.setAspectRatioPolicy(AspectRatioPolicy.STRETCH)

        absController.onTouchDown(DrawingPoint(500f, 500f), pointerCount = 1)

        assertTrue(absController.isDrawingActive)
        assertTrue(absController.isTipSwitchDown)
        assertEquals(1, transport.tabletReports.size)

        val report = transport.tabletReports[0]
        val expectedStatus = (HidConstants.TABLET_STATUS_TIP_SWITCH.toInt() or HidConstants.TABLET_STATUS_IN_RANGE.toInt()).toByte()
        assertEquals(expectedStatus, report.status)
        // 500/1000 -> 0.5 -> 16384
        assertTrue("X should be center", kotlin.math.abs(report.x - 16384) <= 1)
        assertTrue("Y should be center", kotlin.math.abs(report.y - 16384) <= 1)
    }

    @Test
    fun testAbsoluteTabletTouchMoveSendsUpdatedCoordinates() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.setAspectRatioPolicy(AspectRatioPolicy.STRETCH)

        absController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        transport.tabletReports.clear()

        // Move to (900, 900)
        absController.onTouchMove(DrawingPoint(900f, 900f), pointerCount = 1)

        assertEquals(1, transport.tabletReports.size)
        val report = transport.tabletReports[0]
        val expectedStatus = (HidConstants.TABLET_STATUS_TIP_SWITCH.toInt() or HidConstants.TABLET_STATUS_IN_RANGE.toInt()).toByte()
        assertEquals(expectedStatus, report.status)

        // 0.9 * 32767 = 29490
        assertEquals((0.9f * 32767).toInt(), report.x)
        assertEquals((0.9f * 32767).toInt(), report.y)
    }

    @Test
    fun testAbsoluteTabletTouchUpSendsTipSwitchUp() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.onTouchDown(DrawingPoint(500f, 500f), pointerCount = 1)
        transport.tabletReports.clear()

        absController.onTouchUp()

        assertFalse(absController.isDrawingActive)
        assertFalse(absController.isTipSwitchDown)

        // Should send lift report (In Range only), then neutral (out of range)
        assertTrue("Must send release reports", transport.tabletReports.size >= 1)
        val liftReport = transport.tabletReports[0]
        assertEquals(HidConstants.TABLET_STATUS_IN_RANGE, liftReport.status)
    }

    @Test
    fun testSecondFingerReleasesContact() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.onTouchDown(DrawingPoint(100f, 100f), pointerCount = 1)
        assertTrue(absController.isDrawingActive)
        transport.tabletReports.clear()

        // 2nd finger touches during move
        absController.onTouchMove(DrawingPoint(150f, 150f), pointerCount = 2)

        assertFalse("Drawing must be stopped when 2nd finger touches", absController.isDrawingActive)
        assertFalse("Tip switch must be released", absController.isTipSwitchDown)
        assertTrue("Neutral report must be sent", transport.tabletReports.any { it.status == HidConstants.TABLET_STATUS_NONE })
    }

    @Test
    fun testLeavingDrawReleasesContact() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.onTouchDown(DrawingPoint(200f, 200f), pointerCount = 1)
        assertTrue(absController.isDrawingActive)
        transport.tabletReports.clear()

        // Screen dispose / leave
        absController.releaseDrawing()

        assertFalse(absController.isDrawingActive)
        assertFalse(absController.isTipSwitchDown)
        assertTrue(transport.tabletReports.any { it.status == HidConstants.TABLET_STATUS_NONE })
    }

    @Test
    fun testDisconnectReleasesContact() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.onTouchDown(DrawingPoint(300f, 300f), pointerCount = 1)
        assertTrue(absController.isDrawingActive)
        transport.tabletReports.clear()

        // Bluetooth disconnect event triggers releaseDrawing
        absController.releaseDrawing()

        assertFalse(absController.isDrawingActive)
        assertFalse(absController.isTipSwitchDown)
        assertTrue(transport.tabletReports.any { it.status == HidConstants.TABLET_STATUS_NONE })
        assertEquals(HidConstants.MOUSE_BUTTON_NONE, transport.mouseReports.lastOrNull()?.buttons)
    }

    @Test
    fun testAbsoluteEraserToolIncludesEraserBit() {
        val absController = DrawingTabletController(
            transportProvider = { transport },
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )
        absController.updateCanvasSize(1000f, 1000f)
        absController.setTool(DrawingTool.ERASER)

        absController.onTouchDown(DrawingPoint(400f, 400f), pointerCount = 1)

        val report = transport.tabletReports.last()
        val expectedStatus = (HidConstants.TABLET_STATUS_TIP_SWITCH.toInt() or
                HidConstants.TABLET_STATUS_IN_RANGE.toInt() or
                HidConstants.TABLET_STATUS_ERASER.toInt()).toByte()
        assertEquals(expectedStatus, report.status)
    }

    @Test
    fun testTargetModeToggle() {
        assertEquals(DrawingTargetMode.RELATIVE_MOUSE, tabletController.targetMode.value)
        tabletController.toggleTargetMode()
        assertEquals(DrawingTargetMode.ABSOLUTE_TABLET, tabletController.targetMode.value)
        tabletController.toggleTargetMode()
        assertEquals(DrawingTargetMode.RELATIVE_MOUSE, tabletController.targetMode.value)
    }
}
