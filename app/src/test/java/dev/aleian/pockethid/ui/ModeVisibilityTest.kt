package dev.aleian.pockethid.ui

import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModeVisibilityTest {

    @Before
    fun setup() {
        SettingsRepository.resetToDefaults()
    }

    @Test
    fun testDefaultVisibility() {
        val settings = SettingsRepository.settings.value
        assertTrue(settings.modeKeyboardVisible)
        assertTrue(settings.modeMouseVisible)
        assertFalse(settings.modeGamepadVisible)
        assertTrue(settings.modePresenterVisible)
        assertTrue(settings.modeOneHandVisible)
        assertFalse(settings.modeDrawVisible)
    }

    @Test
    fun testHideGamepadAndDraw() {
        // By default Gamepad and Draw are hidden. Let's explicitly try hiding others.
        // E.g., user hides Presenter
        var settings = SettingsRepository.settings.value
        val updated = settings.copy(modePresenterVisible = false)
        SettingsRepository.updateSettings(updated)
        
        settings = SettingsRepository.settings.value
        assertFalse(settings.modePresenterVisible)
    }

    @Test
    fun testReenableHiddenMode() {
        var settings = SettingsRepository.settings.value
        // Enable Gamepad
        val updated = settings.copy(modeGamepadVisible = true)
        SettingsRepository.updateSettings(updated)
        
        settings = SettingsRepository.settings.value
        assertTrue(settings.modeGamepadVisible)
    }

    @Test
    fun testMinimumThreeVisibleModesEnforced() {
        var settings = SettingsRepository.settings.value
        // Currently 4 are visible (KB, Mouse, Presenter, One-Hand)
        // Hide One-Hand -> 3 visible
        var updated = settings.copy(modeOneHandVisible = false)
        SettingsRepository.updateSettings(updated)
        
        settings = SettingsRepository.settings.value
        assertFalse(settings.modeOneHandVisible) // Succeeds
        
        // Try to hide Presenter -> would leave 2 visible (KB, Mouse)
        updated = settings.copy(modePresenterVisible = false)
        SettingsRepository.updateSettings(updated)
        
        settings = SettingsRepository.settings.value
        // Should be ignored, presenter remains visible
        assertTrue("Presenter should remain visible to enforce minimum 3 modes", settings.modePresenterVisible)
    }

    @Test
    fun testPersistenceAfterRelaunch() {
        val prefs = FakePrefs.createFakePrefs()
        SettingsRepository.init(prefs)

        var settings = SettingsRepository.settings.value
        val updated = settings.copy(modeDrawVisible = true, modeKeyboardVisible = false) // Now Draw is visible, KB hidden
        SettingsRepository.updateSettings(updated)
        
        // SettingsRepository keeps the updated values in memory, but we also want to test persistence.
        // Simulate relaunch by re-initializing repository with the same fake SharedPreferences
        SettingsRepository.init(prefs)
        
        settings = SettingsRepository.settings.value
        assertTrue(settings.modeDrawVisible)
        assertFalse(settings.modeKeyboardVisible)
    }
}
