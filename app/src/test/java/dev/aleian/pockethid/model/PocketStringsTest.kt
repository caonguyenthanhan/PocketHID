package dev.aleian.pockethid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketStringsTest {

    @Test
    fun `navigation strings in English do not break words`() {
        val keyboard = PocketStrings.navKeyboard(AppLanguage.ENGLISH)
        val mouse = PocketStrings.navMouse(AppLanguage.ENGLISH)
        val gamepad = PocketStrings.navGamepad(AppLanguage.ENGLISH)
        val presenter = PocketStrings.navPresenter(AppLanguage.ENGLISH)
        val oneHand = PocketStrings.navOneHand(AppLanguage.ENGLISH)
        val draw = PocketStrings.navDraw(AppLanguage.ENGLISH)

        assertEquals("Keyboard", keyboard)
        assertEquals("Mouse", mouse)
        assertEquals("Gamepad", gamepad)
        assertEquals("Presenter", presenter)
        assertEquals("1-Hand", oneHand)
        assertEquals("Draw", draw)

        assertFalse(oneHand.contains(" "))
    }

    @Test
    fun `navigation strings in Vietnamese are complete`() {
        val keyboard = PocketStrings.navKeyboard(AppLanguage.VIETNAMESE)
        val mouse = PocketStrings.navMouse(AppLanguage.VIETNAMESE)
        val gamepad = PocketStrings.navGamepad(AppLanguage.VIETNAMESE)
        val presenter = PocketStrings.navPresenter(AppLanguage.VIETNAMESE)
        val oneHand = PocketStrings.navOneHand(AppLanguage.VIETNAMESE)
        val draw = PocketStrings.navDraw(AppLanguage.VIETNAMESE)

        assertEquals("Bàn phím", keyboard)
        assertEquals("Chuột", mouse)
        assertEquals("Tay cầm", gamepad)
        assertEquals("Thuyết trình", presenter)
        assertEquals("1-Tay", oneHand)
        assertEquals("Bảng vẽ", draw)
    }

    @Test
    fun `all supported languages have non-empty essential strings`() {
        for (lang in AppLanguage.entries) {
            assertTrue(PocketStrings.navKeyboard(lang).isNotBlank())
            assertTrue(PocketStrings.navMouse(lang).isNotBlank())
            assertTrue(PocketStrings.navGamepad(lang).isNotBlank())
            assertTrue(PocketStrings.navPresenter(lang).isNotBlank())
            assertTrue(PocketStrings.navOneHand(lang).isNotBlank())
            assertTrue(PocketStrings.navDraw(lang).isNotBlank())

            assertTrue(PocketStrings.statusConnected(lang).isNotBlank())
            assertTrue(PocketStrings.statusConnecting(lang).isNotBlank())
            assertTrue(PocketStrings.statusDisconnected(lang).isNotBlank())

            assertTrue(PocketStrings.presenterStart(lang).isNotBlank())
            assertTrue(PocketStrings.presenterResume(lang).isNotBlank())
            assertTrue(PocketStrings.presenterExitSafe(lang).isNotBlank())

            assertTrue(PocketStrings.gamepadLandscapeHint(lang).isNotBlank())
            assertTrue(PocketStrings.settingsSave(lang).isNotBlank())
            assertTrue(PocketStrings.settingsReset(lang).isNotBlank())
        }
    }
}
