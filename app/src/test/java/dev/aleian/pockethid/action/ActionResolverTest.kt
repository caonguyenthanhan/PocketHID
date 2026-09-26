package dev.aleian.pockethid.action

import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionResolverTest {

    @Test
    fun testWindowsSystemActionResolution() {
        val taskView = ActionResolver.resolve(PocketAction.SystemAction.TaskView, HostOs.WINDOWS)
        assertTrue(taskView is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_TAB, (taskView as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, taskView.modifiers)

        val showDesktop = ActionResolver.resolve(PocketAction.SystemAction.ShowDesktop, HostOs.WINDOWS)
        assertTrue(showDesktop is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_D, (showDesktop as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, showDesktop.modifiers)

        val appSwitch = ActionResolver.resolve(PocketAction.SystemAction.AppSwitcherNext, HostOs.WINDOWS)
        assertTrue(appSwitch is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_TAB, (appSwitch as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_ALT, appSwitch.modifiers)
    }

    @Test
    fun testMacOsSystemActionResolution() {
        val missionControl = ActionResolver.resolve(PocketAction.SystemAction.TaskView, HostOs.MACOS)
        assertTrue(missionControl is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_UP, (missionControl as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, missionControl.modifiers)

        val showDesktop = ActionResolver.resolve(PocketAction.SystemAction.ShowDesktop, HostOs.MACOS)
        assertTrue(showDesktop is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_F11, (showDesktop as ActionExecutionPlan.KeyStroke).keyCode)

        val appSwitch = ActionResolver.resolve(PocketAction.SystemAction.AppSwitcherNext, HostOs.MACOS)
        assertTrue(appSwitch is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_TAB, (appSwitch as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, appSwitch.modifiers)
    }

    @Test
    fun testVirtualDesktopResolutionAcrossPlatforms() {
        // Windows: Ctrl+Win+Right
        val winNext = ActionResolver.resolve(PocketAction.NavAction.DesktopNext, HostOs.WINDOWS)
        val winExpectedMods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
        assertEquals(HidConstants.KEY_RIGHT, (winNext as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(winExpectedMods, winNext.modifiers)

        // macOS: Ctrl+Right
        val macNext = ActionResolver.resolve(PocketAction.NavAction.DesktopNext, HostOs.MACOS)
        assertEquals(HidConstants.KEY_RIGHT, (macNext as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, macNext.modifiers)

        // Linux: Ctrl+Alt+Right
        val linuxNext = ActionResolver.resolve(PocketAction.NavAction.DesktopNext, HostOs.LINUX)
        val linuxExpectedMods = (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
        assertEquals(HidConstants.KEY_RIGHT, (linuxNext as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(linuxExpectedMods, linuxNext.modifiers)
    }

    @Test
    fun testEditActionsCrossPlatform() {
        // Windows Copy: Ctrl+C
        val winCopy = ActionResolver.resolve(PocketAction.EditAction.Copy, HostOs.WINDOWS)
        assertEquals(HidConstants.KEY_C, (winCopy as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, winCopy.modifiers)

        // macOS Copy: Cmd+C
        val macCopy = ActionResolver.resolve(PocketAction.EditAction.Copy, HostOs.MACOS)
        assertEquals(HidConstants.KEY_C, (macCopy as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, macCopy.modifiers)
    }

    @Test
    fun testMediaAndPresenterActions() {
        val playPause = ActionResolver.resolve(PocketAction.MediaAction.PlayPause)
        assertTrue(playPause is ActionExecutionPlan.ConsumerKey)
        assertEquals(HidConstants.CONSUMER_PLAY_PAUSE, (playPause as ActionExecutionPlan.ConsumerKey).usageCode)

        val startSlide = ActionResolver.resolve(PocketAction.PresenterAction.StartSlideshow)
        assertTrue(startSlide is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_F5, (startSlide as ActionExecutionPlan.KeyStroke).keyCode)

        val nextSlide = ActionResolver.resolve(PocketAction.PresenterAction.NextSlide)
        assertTrue(nextSlide is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_RIGHT, (nextSlide as ActionExecutionPlan.KeyStroke).keyCode)
    }

    @Test
    fun testPointerActions() {
        val leftClick = ActionResolver.resolve(PocketAction.PointerAction.LeftClick)
        assertTrue(leftClick is ActionExecutionPlan.MouseButtonClick)
        assertEquals(HidConstants.MOUSE_BUTTON_LEFT, (leftClick as ActionExecutionPlan.MouseButtonClick).buttonMask)

        val rightClick = ActionResolver.resolve(PocketAction.PointerAction.RightClick)
        assertTrue(rightClick is ActionExecutionPlan.MouseButtonClick)
        assertEquals(HidConstants.MOUSE_BUTTON_RIGHT, (rightClick as ActionExecutionPlan.MouseButtonClick).buttonMask)

        val middleClick = ActionResolver.resolve(PocketAction.PointerAction.MiddleClick)
        assertTrue(middleClick is ActionExecutionPlan.MouseButtonClick)
        assertEquals(HidConstants.MOUSE_BUTTON_MIDDLE, (middleClick as ActionExecutionPlan.MouseButtonClick).buttonMask)
    }

    @Test
    fun testZoomActionResolution() {
        // Windows Wheel Zoom
        val winZoomIn = ActionResolver.resolve(PocketAction.SystemAction.ZoomIn, HostOs.WINDOWS, "Wheel")
        assertTrue(winZoomIn is ActionExecutionPlan.ZoomWheel)
        assertEquals(1, (winZoomIn as ActionExecutionPlan.ZoomWheel).wheelDelta)
        assertEquals(HidConstants.MOD_LEFT_CTRL, winZoomIn.modifier)

        val winZoomOut = ActionResolver.resolve(PocketAction.SystemAction.ZoomOut, HostOs.WINDOWS, "Wheel")
        assertTrue(winZoomOut is ActionExecutionPlan.ZoomWheel)
        assertEquals(-1, (winZoomOut as ActionExecutionPlan.ZoomWheel).wheelDelta)
        assertEquals(HidConstants.MOD_LEFT_CTRL, winZoomOut.modifier)

        // macOS Wheel Zoom
        val macZoomIn = ActionResolver.resolve(PocketAction.SystemAction.ZoomIn, HostOs.MACOS, "Wheel")
        assertTrue(macZoomIn is ActionExecutionPlan.ZoomWheel)
        assertEquals(1, (macZoomIn as ActionExecutionPlan.ZoomWheel).wheelDelta)
        assertEquals(HidConstants.MOD_LEFT_GUI, macZoomIn.modifier)

        // Keys mode Zoom (Ctrl/Cmd + [+/-])
        val winKeysZoomIn = ActionResolver.resolve(PocketAction.SystemAction.ZoomIn, HostOs.WINDOWS, "Keys")
        assertTrue(winKeysZoomIn is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_EQUAL, (winKeysZoomIn as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, winKeysZoomIn.modifiers)

        val macKeysZoomOut = ActionResolver.resolve(PocketAction.SystemAction.ZoomOut, HostOs.MACOS, "Keys")
        assertTrue(macKeysZoomOut is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_MINUS, (macKeysZoomOut as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, macKeysZoomOut.modifiers)
    }

    @Test
    fun testRemotePcLockResolution() {
        // Acceptance Criteria: PC Lock is an HID keystroke to the remote host, NEVER phone lock.
        // Windows: Win+L
        val winLock = ActionResolver.resolve(PocketAction.SystemAction.LockPC, HostOs.WINDOWS)
        assertTrue(winLock is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_L, (winLock as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, winLock.modifiers)

        // macOS: Cmd+Ctrl+Q
        val macLock = ActionResolver.resolve(PocketAction.SystemAction.LockPC, HostOs.MACOS)
        assertTrue(macLock is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_Q, (macLock as ActionExecutionPlan.KeyStroke).keyCode)
        val macExpectedMods = (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
        assertEquals(macExpectedMods, macLock.modifiers)

        // Linux: Super+L
        val linuxLock = ActionResolver.resolve(PocketAction.SystemAction.LockPC, HostOs.LINUX)
        assertTrue(linuxLock is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_L, (linuxLock as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, linuxLock.modifiers)
    }

    @Test
    fun testOneHandWebActionResolution() {
        // Windows/Linux Back: Alt+Left
        val winBack = ActionResolver.resolve(PocketAction.WebAction.Back, HostOs.WINDOWS)
        assertTrue(winBack is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_LEFT, (winBack as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_ALT, winBack.modifiers)

        // macOS Back: Cmd+[
        val macBack = ActionResolver.resolve(PocketAction.WebAction.Back, HostOs.MACOS)
        assertTrue(macBack is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_LEFTBRACE, (macBack as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, macBack.modifiers)

        // Windows Forward: Alt+Right
        val winFwd = ActionResolver.resolve(PocketAction.WebAction.Forward, HostOs.WINDOWS)
        assertTrue(winFwd is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_RIGHT, (winFwd as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_ALT, winFwd.modifiers)

        // macOS Refresh: Cmd+R vs Windows: F5
        val macRefresh = ActionResolver.resolve(PocketAction.WebAction.Refresh, HostOs.MACOS)
        assertTrue(macRefresh is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_R, (macRefresh as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, macRefresh.modifiers)

        val winRefresh = ActionResolver.resolve(PocketAction.WebAction.Refresh, HostOs.WINDOWS)
        assertTrue(winRefresh is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_F5, (winRefresh as ActionExecutionPlan.KeyStroke).keyCode)

        // New Tab: Ctrl+T / Cmd+T
        val winNewTab = ActionResolver.resolve(PocketAction.WebAction.NewTab, HostOs.WINDOWS)
        assertEquals(HidConstants.KEY_T, (winNewTab as ActionExecutionPlan.KeyStroke).keyCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, winNewTab.modifiers)
    }

    @Test
    fun testOneHandVideoActionResolution() {
        // Play/Pause: Consumer Control
        val playPause = ActionResolver.resolve(PocketAction.VideoAction.PlayPause, HostOs.WINDOWS)
        assertTrue(playPause is ActionExecutionPlan.ConsumerKey)
        assertEquals(HidConstants.CONSUMER_PLAY_PAUSE, (playPause as ActionExecutionPlan.ConsumerKey).usageCode)

        // Volume Up / Down: Consumer Control
        val volUp = ActionResolver.resolve(PocketAction.VideoAction.VolumeUp, HostOs.WINDOWS)
        assertTrue(volUp is ActionExecutionPlan.ConsumerKey)
        assertEquals(HidConstants.CONSUMER_VOLUME_UP, (volUp as ActionExecutionPlan.ConsumerKey).usageCode)

        val volDown = ActionResolver.resolve(PocketAction.VideoAction.VolumeDown, HostOs.WINDOWS)
        assertTrue(volDown is ActionExecutionPlan.ConsumerKey)
        assertEquals(HidConstants.CONSUMER_VOLUME_DOWN, (volDown as ActionExecutionPlan.ConsumerKey).usageCode)

        // Seek Forward / Backward: Standard Arrow Keys
        val seekFwd = ActionResolver.resolve(PocketAction.VideoAction.SeekForward, HostOs.WINDOWS)
        assertTrue(seekFwd is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_RIGHT, (seekFwd as ActionExecutionPlan.KeyStroke).keyCode)

        val seekBack = ActionResolver.resolve(PocketAction.VideoAction.SeekBackward, HostOs.WINDOWS)
        assertTrue(seekBack is ActionExecutionPlan.KeyStroke)
        assertEquals(HidConstants.KEY_LEFT, (seekBack as ActionExecutionPlan.KeyStroke).keyCode)
    }

    @Test
    fun testShowDesktopSemanticActionRequirements() {
        // 1. Semantic Action ID must match specification
        assertEquals("system.show_desktop", PocketAction.SystemAction.ShowDesktop.id)
        assertEquals("Show Desktop", PocketAction.SystemAction.ShowDesktop.displayName)

        // 2. Windows Resolution: KeyDown Win, KeyDown D -> Win + D
        val winPlan = ActionResolver.resolve(PocketAction.SystemAction.ShowDesktop, HostOs.WINDOWS)
        assertTrue(winPlan is ActionExecutionPlan.KeyStroke)
        val winKeyStroke = winPlan as ActionExecutionPlan.KeyStroke
        assertEquals(HidConstants.KEY_D, winKeyStroke.keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, winKeyStroke.modifiers)

        // 3. Linux Resolution: Win + D / Super + D
        val linuxPlan = ActionResolver.resolve(PocketAction.SystemAction.ShowDesktop, HostOs.LINUX)
        assertTrue(linuxPlan is ActionExecutionPlan.KeyStroke)
        val linuxKeyStroke = linuxPlan as ActionExecutionPlan.KeyStroke
        assertEquals(HidConstants.KEY_D, linuxKeyStroke.keyCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, linuxKeyStroke.modifiers)

        // 4. macOS Resolution: F11 (Show Desktop)
        val macPlan = ActionResolver.resolve(PocketAction.SystemAction.ShowDesktop, HostOs.MACOS)
        assertTrue(macPlan is ActionExecutionPlan.KeyStroke)
        val macKeyStroke = macPlan as ActionExecutionPlan.KeyStroke
        assertEquals(HidConstants.KEY_F11, macKeyStroke.keyCode)
        assertEquals(0.toByte(), macKeyStroke.modifiers)
    }
}
