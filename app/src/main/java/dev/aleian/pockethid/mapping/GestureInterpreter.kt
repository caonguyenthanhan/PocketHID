package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.model.HidConstants
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

data class MouseAction(
    val dx: Int = 0,
    val dy: Int = 0,
    val buttons: Byte = HidConstants.MOUSE_BUTTON_NONE,
    val wheel: Int = 0
)

class GestureInterpreter(
    var accelerationFactor: Float = 1.5f,
    var scrollSensitivity: Float = 35.0f, // px per scroll step
    var naturalScroll: Boolean = false
) {
    private var accumulatedScrollY = 0f
    private var isDragging = false

    fun applyAcceleration(dx: Float, dy: Float): Pair<Int, Int> {
        val speed = hypot(dx, dy)
        // Spec curve: v_out = v_in * (1 + 0.5 * min(v_in / 8, 1))
        val accelMultiplier = (1.0f + 0.5f * min(speed / 8.0f, 1.0f)) * accelerationFactor
        val scaledDx = (dx * accelMultiplier).roundToInt()
        val scaledDy = (dy * accelMultiplier).roundToInt()
        return Pair(scaledDx, scaledDy)
    }

    fun processOneFingerMove(dx: Float, dy: Float, isLeftHeld: Boolean): MouseAction {
        val (finalDx, finalDy) = applyAcceleration(dx, dy)
        val buttons = if (isLeftHeld || isDragging) HidConstants.MOUSE_BUTTON_LEFT else HidConstants.MOUSE_BUTTON_NONE
        return MouseAction(dx = finalDx, dy = finalDy, buttons = buttons)
    }

    fun processTwoFingerScroll(deltaY: Float): MouseAction? {
        accumulatedScrollY += deltaY
        val steps = (accumulatedScrollY / scrollSensitivity).toInt()
        return if (steps != 0) {
            accumulatedScrollY -= steps * scrollSensitivity
            // Negative delta in touch means swipe up -> scroll down unless natural scroll is enabled
            val wheelVal = if (naturalScroll) steps else -steps
            MouseAction(wheel = wheelVal)
        } else {
            null
        }
    }

    fun setDragging(dragging: Boolean) {
        isDragging = dragging
    }
}
