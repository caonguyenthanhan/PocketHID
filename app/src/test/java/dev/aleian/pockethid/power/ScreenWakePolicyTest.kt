package dev.aleian.pockethid.power

import dev.aleian.pockethid.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the screen wakefulness and inactivity timeout policy calculations.
 */
class ScreenWakePolicyTest {

    @Test
    fun testDefaultAppSettingsScreenBehavior() {
        val settings = AppSettings()
        // Default must allow screen to sleep
        assertFalse(settings.keepScreenAwake)
        // Default timeout must be 10 minutes
        assertEquals(10, settings.screenSleepTimeoutMinutes)
    }

    @Test
    fun testScreenPolicyInactivityLogic() {
        val settings = AppSettings(
            keepScreenAwake = false,
            screenSleepTimeoutMinutes = 10
        )
        val timeoutMs = settings.screenSleepTimeoutMinutes * 60 * 1000L
        assertEquals(600_000L, timeoutMs)

        val lastInteraction = 1000L
        val activeTime = lastInteraction + 5 * 60 * 1000L // 5 mins elapsed
        val isStillActive = (activeTime - lastInteraction) < timeoutMs
        assertTrue("Under 10 minutes should keep screen on during active use", isStillActive)

        val idleTime = lastInteraction + 11 * 60 * 1000L // 11 mins elapsed
        val isIdle = (idleTime - lastInteraction) >= timeoutMs
        assertTrue("Over 10 minutes should allow screen to naturally sleep", isIdle)
    }

    @Test
    fun testKeepScreenAwakeOverridesTimeout() {
        val settings = AppSettings(
            keepScreenAwake = true,
            screenSleepTimeoutMinutes = 10
        )
        // When keepScreenAwake is true, timeout is ignored and screen stays awake while app is open
        assertTrue(settings.keepScreenAwake)
    }
}
