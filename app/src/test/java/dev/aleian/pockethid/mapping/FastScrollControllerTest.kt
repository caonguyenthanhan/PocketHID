package dev.aleian.pockethid.mapping

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class FastScrollControllerTest {

    private lateinit var controller: FastScrollController

    @Before
    fun setUp() {
        controller = FastScrollController(
            enabled = true,
            multiplier = 2.5f,
            widthPercent = 0.10f, // 10%
            minHitWidthDp = 32f,
            maxHitWidthDp = 64f,
            maxWheelPerReport = 15,
            stepThresholdPx = 14f
        )
    }

    @Test
    fun testIsInHitZone_disabled_returnsFalse() {
        controller.enabled = false
        val inZone = controller.isInHitZone(x = 950f, totalWidth = 1000f, density = 1f)
        assertFalse(inZone)
    }

    @Test
    fun testIsInHitZone_centerOfTrackpad_returnsFalse() {
        val inZone = controller.isInHitZone(x = 500f, totalWidth = 1000f, density = 1f)
        assertFalse(inZone)
    }

    @Test
    fun testIsInHitZone_rightEdge_returnsTrue() {
        // totalWidth = 1000, 10% = 100px. Clamped between 32 and 64 px => 64px.
        // With padding factor (1.15) => ~73px from right edge.
        // Touch at 960px is within 73px of right edge (1000 - 73 = 927).
        val inZone = controller.isInHitZone(x = 960f, totalWidth = 1000f, density = 1f)
        assertTrue(inZone)
    }

    @Test
    fun testOnTouchDown_insideZone_activatesFastScroll() {
        val activated = controller.onTouchDown(x = 980f, y = 300f, totalWidth = 1000f, pointerCount = 1, density = 1f)
        assertTrue(activated)
        assertTrue(controller.isFastScrolling)
    }

    @Test
    fun testOnTouchDown_outsideZone_doesNotActivate() {
        val activated = controller.onTouchDown(x = 200f, y = 300f, totalWidth = 1000f, pointerCount = 1, density = 1f)
        assertFalse(activated)
        assertFalse(controller.isFastScrolling)
    }

    @Test
    fun testOnTouchDown_multiFinger_doesNotActivate() {
        val activated = controller.onTouchDown(x = 980f, y = 300f, totalWidth = 1000f, pointerCount = 2, density = 1f)
        assertFalse(activated)
        assertFalse(controller.isFastScrolling)
    }

    @Test
    fun testOnTouchMove_fingerDown_traditionalScrollIsNegative() {
        controller.onTouchDown(x = 980f, y = 100f, totalWidth = 1000f, pointerCount = 1, density = 1f)

        // Move finger down: currentY = 160f (delta = +60px)
        val wheel = controller.onTouchMove(currentY = 160f, pointerCount = 1, naturalScroll = false)
        assertNotNull(wheel)
        assertTrue("Traditional wheel down should be negative", wheel!! < 0)
    }

    @Test
    fun testOnTouchMove_fingerDown_naturalScrollIsPositive() {
        controller.onTouchDown(x = 980f, y = 100f, totalWidth = 1000f, pointerCount = 1, density = 1f)

        // Move finger down: currentY = 160f (delta = +60px)
        val wheel = controller.onTouchMove(currentY = 160f, pointerCount = 1, naturalScroll = true)
        assertNotNull(wheel)
        assertTrue("Natural scroll down should be positive", wheel!! > 0)
    }

    @Test
    fun testOnTouchMove_fingerUp_traditionalScrollIsPositive() {
        controller.onTouchDown(x = 980f, y = 300f, totalWidth = 1000f, pointerCount = 1, density = 1f)

        // Move finger up: currentY = 240f (delta = -60px)
        val wheel = controller.onTouchMove(currentY = 240f, pointerCount = 1, naturalScroll = false)
        assertNotNull(wheel)
        assertTrue("Traditional wheel up should be positive", wheel!! > 0)
    }

    @Test
    fun testOnTouchMove_fingerUp_naturalScrollIsNegative() {
        controller.onTouchDown(x = 980f, y = 300f, totalWidth = 1000f, pointerCount = 1, density = 1f)

        // Move finger up: currentY = 240f (delta = -60px)
        val wheel = controller.onTouchMove(currentY = 240f, pointerCount = 1, naturalScroll = true)
        assertNotNull(wheel)
        assertTrue("Natural scroll up should be negative", wheel!! < 0)
    }

    @Test
    fun testOnTouchMove_clampingPreventsWildReports() {
        controller.onTouchDown(x = 980f, y = 100f, totalWidth = 1000f, pointerCount = 1, density = 1f)

        // Huge leap: currentY = 2000f
        val wheel = controller.onTouchMove(currentY = 2000f, pointerCount = 1, naturalScroll = false)
        assertNotNull(wheel)
        assertTrue(abs(wheel!!) <= controller.maxWheelPerReport)
    }

    @Test
    fun testMultiTouchRelinquishesFastScroll() {
        controller.onTouchDown(x = 980f, y = 100f, totalWidth = 1000f, pointerCount = 1, density = 1f)
        assertTrue(controller.isFastScrolling)

        // A second finger touches down during move
        val wheel = controller.onTouchMove(currentY = 120f, pointerCount = 2, naturalScroll = false)
        assertNull(wheel)
        assertFalse("Fast scroll should be cancelled when multiple fingers are present", controller.isFastScrolling)
    }

    @Test
    fun testOnTouchUp_resetsActiveState() {
        controller.onTouchDown(x = 980f, y = 100f, totalWidth = 1000f, pointerCount = 1, density = 1f)
        assertTrue(controller.isFastScrolling)

        val wasActive = controller.onTouchUp()
        assertTrue(wasActive)
        assertFalse(controller.isFastScrolling)
    }
}
