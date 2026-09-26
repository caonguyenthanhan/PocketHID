package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GestureInterpreterTest {

    private lateinit var interpreter: GestureInterpreter

    @Before
    fun setUp() {
        interpreter = GestureInterpreter(
            accelerationFactor = 1.0f,
            scrollSensitivity = 30.0f,
            naturalScroll = false,
            deadZonePx = 4.0f
        )
    }

    @Test
    fun testOneFingerMoveWithAcceleration() {
        interpreter.onTouchDown(0f, 0f, 1)
        val (gesture, action) = interpreter.onTouchMove(10f, 10f, 1)

        assertEquals(GestureType.ONE_FINGER_MOVE, gesture)
        assertNotNull(action)
        assertTrue(action!!.dx > 0)
        assertTrue(action.dy > 0)
        assertEquals(HidConstants.MOUSE_BUTTON_NONE, action.buttons)
    }

    @Test
    fun testDeadzoneSuppressesSmallMovement() {
        interpreter.onTouchDown(0f, 0f, 1)
        val (gesture, action) = interpreter.onTouchMove(2f, 2f, 1)

        assertEquals(GestureType.NONE, gesture)
        assertNull(action)
    }

    @Test
    fun testTwoFingerVerticalScroll() {
        interpreter.onTouchDown(50f, 50f, 2)
        val (gesture, action) = interpreter.onTouchMove(50f, 120f, 2)

        assertEquals(GestureType.TWO_FINGER_SCROLL_V, gesture)
        assertNotNull(action)
        assertTrue(action!!.wheel != 0)
    }

    @Test
    fun testThreeFingerSwipeLeftDetection() {
        interpreter.onTouchDown(200f, 200f, 3)
        val (gesture, action) = interpreter.onTouchMove(50f, 200f, 3)

        assertEquals(GestureType.THREE_FINGER_SWIPE_LEFT, gesture)
        assertNull(action)
    }

    @Test
    fun testFourFingerSwipeDownDetection() {
        interpreter.onTouchDown(200f, 200f, 4)
        val (gesture, action) = interpreter.onTouchMove(200f, 350f, 4)

        assertEquals(GestureType.FOUR_FINGER_SWIPE_DOWN, gesture)
        assertNull(action)
    }

    @Test
    fun testMultiFingerPriorityLockPreventsAccidentalCursorMove() {
        // User puts down 3 fingers
        interpreter.onTouchDown(100f, 100f, 3)

        // Movement with 3 fingers should NOT emit 1-finger move
        val (gesture, action) = interpreter.onTouchMove(110f, 105f, 3)
        assertNotEquals(GestureType.ONE_FINGER_MOVE, gesture)
    }

    @Test
    fun testTwoFingerPinchOutDetection() {
        // Pointer 1 at (100, 100), Pointer 2 at (150, 100) -> initial distance = 50px
        interpreter.onTouchDown(100f, 100f, 2, secondX = 150f, secondY = 100f)

        // Pinch OUT: pointers move apart to (60, 100) and (190, 100) -> distance = 130px, delta = +80px
        val (gesture, action) = interpreter.onTouchMove(
            currentX = 60f,
            currentY = 100f,
            pointerCount = 2,
            secondX = 190f,
            secondY = 100f
        )

        assertEquals(GestureType.TWO_FINGER_PINCH_OUT, gesture)
        assertNull(action) // No mouse cursor or scroll events emitted during pinch
    }

    @Test
    fun testTwoFingerPinchInDetection() {
        // Pointer 1 at (60, 100), Pointer 2 at (200, 100) -> initial distance = 140px
        interpreter.onTouchDown(60f, 100f, 2, secondX = 200f, secondY = 100f)

        // Pinch IN: pointers move together to (110, 100) and (150, 100) -> distance = 40px, delta = -100px
        val (gesture, action) = interpreter.onTouchMove(
            currentX = 110f,
            currentY = 100f,
            pointerCount = 2,
            secondX = 150f,
            secondY = 100f
        )

        assertEquals(GestureType.TWO_FINGER_PINCH_IN, gesture)
        assertNull(action)
    }

    @Test
    fun testTwoFingersMovingParallelClassifiedAsScrollNotPinch() {
        // Pointer 1 at (100, 100), Pointer 2 at (150, 100) -> distance = 50px
        interpreter.onTouchDown(100f, 100f, 2, secondX = 150f, secondY = 100f)

        // Both fingers move down together by 80px (parallel movement)
        // Pointer 1 at (100, 180), Pointer 2 at (150, 180) -> distance = 50px (span delta = 0, center delta = 80px)
        val (gesture, action) = interpreter.onTouchMove(
            currentX = 100f,
            currentY = 180f,
            pointerCount = 2,
            secondX = 150f,
            secondY = 180f
        )

        assertEquals(GestureType.TWO_FINGER_SCROLL_V, gesture)
        assertNotNull(action)
        assertTrue(action!!.wheel != 0)
    }

    @Test
    fun testPinchSuppressesRightClickTapOnRelease() {
        // User pinches out
        interpreter.onTouchDown(100f, 100f, 2, secondX = 150f, secondY = 100f)
        interpreter.onTouchMove(
            currentX = 60f,
            currentY = 100f,
            pointerCount = 2,
            secondX = 190f,
            secondY = 100f
        )

        // When fingers lift, should NOT fire TWO_FINGER_TAP (which triggers right-click)
        val finalGesture = interpreter.onTouchUp(60f, 100f)
        assertNotEquals(GestureType.TWO_FINGER_TAP, finalGesture)
        assertEquals(GestureType.NONE, finalGesture)
    }
}
