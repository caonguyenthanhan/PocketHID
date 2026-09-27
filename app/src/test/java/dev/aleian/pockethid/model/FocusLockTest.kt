package dev.aleian.pockethid.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusLockTest {

    @Before
    fun setUp() {
        FocusLockController.reset()
    }

    @After
    fun tearDown() {
        FocusLockController.reset()
    }

    @Test
    fun testInitialStateIsUnlocked() {
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())
    }

    @Test
    fun testFocusOffAllowsModeSwitch() {
        FocusLockController.setLocked(false)
        val initialMode = 0 // Keyboard
        val targetMode = 1  // Mouse
        val resolved = FocusLockController.resolveModeSwitch(initialMode, targetMode)
        assertEquals("When Focus is OFF, mode switch must be accepted", targetMode, resolved)
    }

    @Test
    fun testFocusOnBlocksModeSwitchAndKeepsCurrentMode() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)
        assertFalse(FocusLockController.canSwitchMode())

        val currentMode = 2 // Gamepad
        val attemptedMode = 0 // Keyboard
        val resolved = FocusLockController.resolveModeSwitch(currentMode, attemptedMode)
        assertEquals("When Focus is ON, mode switch must be rejected and stay on current mode", currentMode, resolved)
    }

    @Test
    fun testExplicitUnlockRestoresModeSwitching() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)

        // User taps Focus toggle again to unlock
        val newLockState = FocusLockController.toggle()
        assertFalse("Toggle from ON must result in OFF", newLockState)
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())

        val resolved = FocusLockController.resolveModeSwitch(currentMode = 3, targetMode = 4)
        assertEquals("After unlocking, mode switch must work normally", 4, resolved)
    }

    @Test
    fun testToggleDoesNotDuplicateState() {
        assertFalse(FocusLockController.isLocked.value)

        FocusLockController.toggle()
        assertTrue(FocusLockController.isLocked.value)

        FocusLockController.toggle()
        assertFalse(FocusLockController.isLocked.value)
    }

    @Test
    fun testResetClearsLockState() {
        FocusLockController.setLocked(true)
        assertTrue(FocusLockController.isLocked.value)

        FocusLockController.reset()
        assertFalse(FocusLockController.isLocked.value)
        assertTrue(FocusLockController.canSwitchMode())
    }
}
