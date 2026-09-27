package dev.aleian.pockethid.drawing

import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DrawingControllerTest {

    private lateinit var controller: DrawingController

    @Before
    fun setUp() {
        controller = DrawingController()
    }

    @Test
    fun testStartStrokeAndAppendPoints() {
        controller.startStroke(DrawingPoint(10f, 10f), pointerCount = 1)
        var draft = controller.state.value.currentDraftStroke
        assertNotNull("Draft stroke must be active", draft)
        assertEquals(1, draft!!.points.size)

        controller.appendPoint(DrawingPoint(20f, 20f), pointerCount = 1)
        controller.appendPoint(DrawingPoint(30f, 30f), pointerCount = 1)

        draft = controller.state.value.currentDraftStroke
        assertNotNull(draft)
        assertEquals(3, draft!!.points.size)
        assertEquals(DrawingPoint(30f, 30f), draft.points.last())
    }

    @Test
    fun testFinishStrokeAppendsToState() {
        controller.startStroke(DrawingPoint(10f, 10f), pointerCount = 1)
        controller.appendPoint(DrawingPoint(20f, 20f), pointerCount = 1)
        controller.finishStroke()

        val state = controller.state.value
        assertNull("Draft stroke must be cleared after finish", state.currentDraftStroke)
        assertEquals(1, state.strokes.size)
        assertEquals(2, state.strokes[0].points.size)
    }

    @Test
    fun testUndoRemovesLastStrokeAndPushesRedo() {
        // Draw stroke 1
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()

        // Draw stroke 2
        controller.startStroke(DrawingPoint(50f, 50f))
        controller.finishStroke()

        assertEquals(2, controller.state.value.strokes.size)
        assertEquals(0, controller.state.value.redoStack.size)

        val undoSuccess = controller.undo()
        assertTrue(undoSuccess)
        assertEquals(1, controller.state.value.strokes.size)
        assertEquals(1, controller.state.value.redoStack.size)
        assertEquals(DrawingPoint(50f, 50f), controller.state.value.redoStack[0].points[0])
    }

    @Test
    fun testRedoRestoresStroke() {
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()

        controller.undo()
        assertEquals(0, controller.state.value.strokes.size)
        assertEquals(1, controller.state.value.redoStack.size)

        val redoSuccess = controller.redo()
        assertTrue(redoSuccess)
        assertEquals(1, controller.state.value.strokes.size)
        assertEquals(0, controller.state.value.redoStack.size)
        assertEquals(DrawingPoint(10f, 10f), controller.state.value.strokes[0].points[0])
    }

    @Test
    fun testNewStrokeClearsRedoStack() {
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()

        controller.undo()
        assertEquals(1, controller.state.value.redoStack.size)

        // Draw a new stroke -> should invalidate redo history
        controller.startStroke(DrawingPoint(30f, 30f))
        controller.finishStroke()

        assertEquals(1, controller.state.value.strokes.size)
        assertEquals("Redo stack must be cleared after new stroke", 0, controller.state.value.redoStack.size)
    }

    @Test
    fun testBoundedHistoryEnforcesMaxLimit() {
        // Test 1: Canonical default limit (DrawingController.MAX_HISTORY = 50)
        assertEquals("Canonical MAX_HISTORY must be 50", 50, DrawingController.MAX_HISTORY)
        val defaultController = DrawingController()

        for (i in 1..55) {
            defaultController.startStroke(DrawingPoint(i.toFloat(), i.toFloat()))
            defaultController.finishStroke()
        }

        assertEquals(
            "Strokes count must be clamped to canonical MAX_HISTORY (50)",
            DrawingController.MAX_HISTORY,
            defaultController.state.value.strokes.size
        )
        assertEquals(
            "Oldest 5 strokes must be dropped, keeping newest (55)",
            DrawingPoint(55f, 55f),
            defaultController.state.value.strokes.last().points[0]
        )
        assertEquals(
            "First stroke retained must be index 6",
            DrawingPoint(6f, 6f),
            defaultController.state.value.strokes.first().points[0]
        )

        // Test 2: Custom maxHistorySize parameter clamping
        val smallHistoryController = DrawingController(maxHistorySize = 5)

        for (i in 1..10) {
            smallHistoryController.startStroke(DrawingPoint(i.toFloat(), i.toFloat()))
            smallHistoryController.finishStroke()
        }

        assertEquals("Strokes count must be clamped to custom maxHistory (5)", 5, smallHistoryController.state.value.strokes.size)
        assertEquals(
            "Oldest strokes must be dropped, keeping newest (10)",
            DrawingPoint(10f, 10f),
            smallHistoryController.state.value.strokes.last().points[0]
        )
    }

    @Test
    fun testRequestClearSetsPendingConfirmation() {
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()

        assertFalse(controller.state.value.isClearConfirmationPending)

        controller.requestClear()
        assertTrue("Request clear must set confirmation pending", controller.state.value.isClearConfirmationPending)
        assertEquals("Strokes must NOT be cleared before confirmation", 1, controller.state.value.strokes.size)
    }

    @Test
    fun testConfirmClearRemovesAllStrokes() {
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()
        controller.requestClear()

        controller.confirmClear()
        assertFalse(controller.state.value.isClearConfirmationPending)
        assertEquals("Confirm clear must wipe all strokes", 0, controller.state.value.strokes.size)
    }

    @Test
    fun testCancelClearPreservesStrokes() {
        controller.startStroke(DrawingPoint(10f, 10f))
        controller.finishStroke()
        controller.requestClear()

        controller.cancelClear()
        assertFalse(controller.state.value.isClearConfirmationPending)
        assertEquals("Cancel clear must preserve strokes", 1, controller.state.value.strokes.size)
    }

    @Test
    fun testToolAndColorAndWidthSelection() {
        controller.setTool(DrawingTool.ERASER)
        assertEquals(DrawingTool.ERASER, controller.state.value.activeTool)

        controller.setColor(DrawingColor.CYAN)
        assertEquals(DrawingColor.CYAN, controller.state.value.selectedColor)

        controller.setStrokeWidth(12f)
        assertEquals(12f, controller.state.value.strokeWidth, 0.01f)

        // Switching back to Pen preserves stroke history
        controller.setTool(DrawingTool.PEN)
        assertEquals(DrawingTool.PEN, controller.state.value.activeTool)
    }

    @Test
    fun testEraseStrokeAtPoint() {
        // Draw stroke near (100, 100)
        controller.startStroke(DrawingPoint(100f, 100f))
        controller.appendPoint(DrawingPoint(110f, 110f))
        controller.finishStroke()

        // Draw distant stroke near (500, 500)
        controller.startStroke(DrawingPoint(500f, 500f))
        controller.appendPoint(DrawingPoint(510f, 510f))
        controller.finishStroke()

        assertEquals(2, controller.state.value.strokes.size)

        // Erase at (105, 105) with radius 20
        controller.eraseAt(DrawingPoint(105f, 105f), radius = 20f)

        assertEquals("Only intersecting stroke should be removed", 1, controller.state.value.strokes.size)
        assertEquals(DrawingPoint(500f, 500f), controller.state.value.strokes[0].points[0])
    }

    @Test
    fun testMultiTouchRejectionPreventsAccidentalStrokes() {
        // 2 fingers down -> must reject stroke start
        controller.startStroke(DrawingPoint(50f, 50f), pointerCount = 2)
        assertNull("2+ fingers must not start a stroke", controller.state.value.currentDraftStroke)

        // 1 finger down starts stroke
        controller.startStroke(DrawingPoint(50f, 50f), pointerCount = 1)
        assertNotNull(controller.state.value.currentDraftStroke)

        // Second finger appears during move -> cancels stroke
        controller.appendPoint(DrawingPoint(60f, 60f), pointerCount = 2)
        assertNull("Second finger appearance must cancel current draft", controller.state.value.currentDraftStroke)
    }
}
