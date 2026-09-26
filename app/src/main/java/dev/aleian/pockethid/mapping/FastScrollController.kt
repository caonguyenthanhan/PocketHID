package dev.aleian.pockethid.mapping

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * FastScrollController manages high-speed vertical scrolling along the right edge of the trackpad.
 *
 * Responsibilities:
 * - Recognize activation zone (right edge 8-12% responsive, bounded)
 * - Calculate finger delta and velocity
 * - Apply configurable multiplier and acceleration curve
 * - Respect natural scroll setting
 * - Clamp maximum scroll speed
 * - Isolate gesture from normal cursor movement, taps, and clicks
 */
class FastScrollController(
    var enabled: Boolean = true,
    var multiplier: Float = 2.5f,
    var widthPercent: Float = 0.10f,
    val minHitWidthDp: Float = 32f,
    val maxHitWidthDp: Float = 64f,
    val maxWheelPerReport: Int = 15,
    val stepThresholdPx: Float = 14f
) {
    var isFastScrolling: Boolean = false
        private set

    private var startY: Float = 0f
    private var lastY: Float = 0f
    private var lastTimeMs: Long = 0L
    private var accumulatedScrollY: Float = 0f
    private var lastVelocity: Float = 0f

    /**
     * Hit test whether an (x, y) coordinate falls into the right-edge fast scroll zone.
     * The interactive hit zone can be slightly wider than visual strip for finger landing reliability.
     */
    fun isInHitZone(
        x: Float,
        totalWidth: Float,
        density: Float = 1f,
        hitPaddingFactor: Float = 1.15f
    ): Boolean {
        if (!enabled || totalWidth <= 0f) return false
        val minPx = minHitWidthDp * density
        val maxPx = maxHitWidthDp * density
        val baseWidthPx = (totalWidth * widthPercent).coerceIn(minPx, maxPx)
        val hitWidthPx = min(baseWidthPx * hitPaddingFactor, totalWidth * 0.20f)
        return x >= (totalWidth - hitWidthPx)
    }

    /**
     * Call when a touch DOWN event occurs.
     * Activates and locks Fast Scroll if within the hit zone and exactly 1 pointer.
     * Returns true if fast scroll was activated.
     */
    fun onTouchDown(
        x: Float,
        y: Float,
        totalWidth: Float,
        pointerCount: Int = 1,
        density: Float = 1f
    ): Boolean {
        if (!enabled || pointerCount != 1) {
            isFastScrolling = false
            return false
        }

        if (isInHitZone(x, totalWidth, density)) {
            isFastScrolling = true
            startY = y
            lastY = y
            lastTimeMs = System.currentTimeMillis()
            accumulatedScrollY = 0f
            lastVelocity = 0f
            return true
        }

        isFastScrolling = false
        return false
    }

    /**
     * Call on touch MOVE event.
     * If fast scroll is active, calculates velocity-based delta and returns
     * Mouse Wheel delta (positive or negative) or null if threshold not crossed.
     */
    fun onTouchMove(
        currentY: Float,
        pointerCount: Int = 1,
        naturalScroll: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Int? {
        if (!isFastScrolling) return null

        // Obey multi-touch hierarchy (4 > 3 > 2 > 1):
        // If extra fingers arrive, relinquish fast scroll so multi-touch gestures can take over
        if (pointerCount > 1) {
            isFastScrolling = false
            return null
        }

        val deltaY = currentY - lastY
        val dt = max(1L, currentTimeMs - lastTimeMs)
        val instantVelocity = abs(deltaY) / dt.toFloat() // px per ms

        // Smooth velocity with EMA
        lastVelocity = 0.7f * instantVelocity + 0.3f * lastVelocity
        lastY = currentY
        lastTimeMs = currentTimeMs

        // Fast Scroll acceleration curve:
        // Controlled slow scrolling at base multiplier, up to 3.5x boost at high swipe speeds
        val velocityBoost = (1.0f + (lastVelocity * 1.5f)).coerceIn(1.0f, 3.5f)
        val effectiveMultiplier = multiplier * velocityBoost

        accumulatedScrollY += deltaY * effectiveMultiplier

        val steps = (accumulatedScrollY / stepThresholdPx).toInt()
        return if (steps != 0) {
            accumulatedScrollY -= steps * stepThresholdPx
            val clampedSteps = steps.coerceIn(-maxWheelPerReport, maxWheelPerReport)
            // Respect existing Natural Scroll convention:
            // Moving finger DOWN -> deltaY > 0 -> steps > 0
            // Natural Scroll ON: positive wheel (content moves down with finger)
            // Natural Scroll OFF: negative wheel (traditional mouse wheel down)
            if (naturalScroll) clampedSteps else -clampedSteps
        } else {
            null
        }
    }

    /**
     * Call when touch UP or CANCEL occurs.
     * Returns true if fast scroll was active.
     */
    fun onTouchUp(): Boolean {
        val wasActive = isFastScrolling
        isFastScrolling = false
        accumulatedScrollY = 0f
        lastVelocity = 0f
        return wasActive
    }

    /**
     * Reset controller state.
     */
    fun reset() {
        isFastScrolling = false
        accumulatedScrollY = 0f
        lastVelocity = 0f
    }
}
