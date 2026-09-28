package dev.aleian.pockethid.presenter

/**
 * Pure state machine managing safe exit confirmation for presenter mode.
 * Eliminates accidental presentation dismissal by requiring a 1.5s (1500ms) continuous press.
 */
class PresenterExitSafety(
    val requiredHoldMs: Long = 1500L
) {
    var pressStartTime: Long? = null
        private set

    var isHoldActive: Boolean = false
        private set

    /**
     * Called when the user presses down on the safe exit button.
     */
    fun onPointerDown(currentTimeMs: Long) {
        pressStartTime = currentTimeMs
        isHoldActive = true
    }

    /**
     * Computes completion progress between 0.0f and 1.0f based on elapsed time.
     */
    fun computeProgress(currentTimeMs: Long): Float {
        val start = pressStartTime ?: return 0f
        val elapsed = currentTimeMs - start
        if (elapsed <= 0L) return 0f
        return (elapsed.toFloat() / requiredHoldMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns true if the user held down for at least [requiredHoldMs].
     */
    fun isHoldComplete(currentTimeMs: Long): Boolean {
        val start = pressStartTime ?: return false
        return (currentTimeMs - start) >= requiredHoldMs
    }

    /**
     * Called when the user releases touch or touch is cancelled.
     * Returns true if exit requirement was fulfilled.
     */
    fun onPointerUp(currentTimeMs: Long): Boolean {
        val complete = isHoldComplete(currentTimeMs)
        reset()
        return complete
    }

    /**
     * Resets hold state.
     */
    fun reset() {
        pressStartTime = null
        isHoldActive = false
    }
}
