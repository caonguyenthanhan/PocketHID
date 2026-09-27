package dev.aleian.pockethid.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for Focus Mode / Mode Lock.
 *
 * When Focus is locked, mode navigation (tab switching / mode selector) is blocked
 * to prevent accidental touches from exiting the active mode during gameplay, typing,
 * or presentations. Internal controls within the current mode continue executing normally.
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
     * Used when the connection session ends or the app resets.
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
     * Resolves a mode switch request.
     * If locked, returns [currentMode] (switch rejected).
     * If unlocked, returns [targetMode] (switch allowed).
     */
    fun resolveModeSwitch(currentMode: Int, targetMode: Int): Int {
        return if (_isLocked.value) currentMode else targetMode
    }
}
