package dev.aleian.pockethid.gamepad

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure mathematical routines for gamepad sticks, deadzones, response curves, and triggers.
 */
object GamepadMath {

    /**
     * Compute raw thumbstick displacement into normalized unit circle components [-1.0 .. +1.0].
     * Returns Pair(normalizedX, normalizedY) and magnitude [0.0 .. 1.0].
     */
    fun processStickInput(
        dx: Float,
        dy: Float,
        maxRadius: Float,
        deadzone: Float,
        curve: StickResponseCurve,
        sensitivity: Float,
        invertY: Boolean = false
    ): Pair<Short, Short> {
        if (maxRadius <= 0f) return Pair(0, 0)

        val distance = sqrt(dx * dx + dy * dy)
        val rawMagnitude = (distance / maxRadius).coerceIn(0f, 1f)

        if (rawMagnitude <= deadzone || deadzone >= 1.0f) {
            return Pair(0, 0)
        }

        // Seamless deadzone scaling (0.0 to 1.0 after deadzone boundary)
        val scaledMagnitude = ((rawMagnitude - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)

        // Apply selected response curve
        val curvedMagnitude = when (curve) {
            StickResponseCurve.LINEAR -> scaledMagnitude
            StickResponseCurve.PRECISION -> scaledMagnitude.pow(1.7f)
            StickResponseCurve.AGGRESSIVE -> scaledMagnitude.pow(0.6f)
        }

        // Apply sensitivity and clamp
        val finalMagnitude = (curvedMagnitude * sensitivity).coerceIn(0f, 1f)

        val angle = atan2(dy, dx)
        val outX = (cos(angle) * finalMagnitude).coerceIn(-1f, 1f)
        var outY = (sin(angle) * finalMagnitude).coerceIn(-1f, 1f)

        if (invertY) {
            outY = -outY
        }

        val hidX = (outX * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
        val hidY = (outY * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()

        return Pair(hidX, hidY)
    }

    /**
     * Convert trigger analog fraction [0.0 .. 1.0] to unsigned 8-bit HID byte [0 .. 255].
     */
    fun processTriggerInput(
        fraction: Float,
        sensitivity: Float = 1.0f
    ): Byte {
        val clamped = (fraction * sensitivity).coerceIn(0f, 1f)
        val intVal = (clamped * 255f).roundToInt().coerceIn(0, 255)
        return (intVal and 0xFF).toByte()
    }
}
