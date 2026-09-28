package dev.aleian.pockethid.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Control modes available in PocketHID.
 */
enum class ControlMode(val index: Int, val title: String) {
    KEYBOARD(0, "Keyboard"),
    MOUSE(1, "Mouse"),
    GAMEPAD(2, "Gamepad"),
    PRESENTER(3, "Presenter"),
    ONE_HAND(4, "One-Hand"),
    DRAW(5, "Draw");

    companion object {
        fun fromIndex(index: Int): ControlMode = entries.firstOrNull { it.index == index } ?: KEYBOARD
    }
}

/**
 * Single source of truth for Focus Mode / Global Mode Navigation Lock.
 *
 * Product Definition:
 * FOCUS = GLOBAL INTERACTION LOCK
 * Once the user has entered any control mode, FOCUS prevents accidental navigation
 * or mode switching, so the user can operate the active controller (Keyboard, Mouse,
 * Gamepad, Presenter, One-Hand, Draw) without accidentally jumping to another mode.
 *
 * Focus is NOT a general input lock or touch freeze: all valid controls, buttons,
 * keys, and gestures INSIDE the active mode continue executing at 100% full fidelity.
 */
object FocusLockController {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /**
     * Toggles the lock state.
     * @return the new lock state
     */
    fun toggle(): Boolean {
        _isLocked.value = !_isLocked.value
        return _isLocked.value
    }

    /**
     * Explicitly sets the lock state.
     */
    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }

    /**
     * Resets the lock state to OFF (unlocked).
     * Used when the connection session ends, Bluetooth disconnects, or the app resets.
     */
    fun reset() {
        _isLocked.value = false
    }

    /**
     * Checks if mode switching is currently allowed.
     */
    fun canSwitchMode(): Boolean {
        return !_isLocked.value
    }

    /**
     * Resolves an Int-indexed mode switch request.
     * If locked, returns [currentMode] (switch rejected).
     * If unlocked, returns [targetMode] (switch allowed).
     */
    fun resolveModeSwitch(currentMode: Int, targetMode: Int): Int {
        return if (_isLocked.value) currentMode else targetMode
    }

    /**
     * Resolves a [ControlMode] mode switch request.
     * If locked, returns [currentMode] (switch rejected).
     * If unlocked, returns [targetMode] (switch allowed).
     */
    fun resolveModeSwitch(currentMode: ControlMode, targetMode: ControlMode): ControlMode {
        return if (_isLocked.value) currentMode else targetMode
    }

    /**
     * Evaluates whether a mode-switch navigation gesture should be allowed.
     * Returns true if navigation is unlocked; false if Focus is ON.
     */
    fun allowNavigationGesture(): Boolean {
        return !_isLocked.value
    }

    /**
     * Checks whether an internal action inside the current mode is permitted.
     * Always returns true because Focus is a navigation lock, never an input lock.
     */
    fun allowsInternalAction(): Boolean {
        return true
    }
}
