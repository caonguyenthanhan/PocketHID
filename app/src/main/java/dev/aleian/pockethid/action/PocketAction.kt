package dev.aleian.pockethid.action

/**
 * Central sealed hierarchy of semantic user actions in PocketHID.
 * Decouples input triggers (gestures, keys, buttons, palette) from direct HID scancodes.
 */
sealed interface PocketAction {
    val id: String
    val displayName: String

    // --- System Actions ---
    sealed class SystemAction(override val id: String, override val displayName: String) : PocketAction {
        data object TaskView : SystemAction("sys.task_view", "Task View / Mission Control")
        data object ShowDesktop : SystemAction("sys.show_desktop", "Show Desktop")
        data object LockPC : SystemAction("sys.lock_pc", "Lock Computer")
        data object AppSwitcherNext : SystemAction("sys.app_switcher_next", "Next Application (Alt+Tab / Cmd+Tab)")
        data object AppSwitcherPrev : SystemAction("sys.app_switcher_prev", "Previous Application")
        data object Screenshot : SystemAction("sys.screenshot", "Screen Capture")
        data object RunDialog : SystemAction("sys.run_dialog", "Run Command / Spotlight")
        data object ZoomIn : SystemAction("sys.zoom_in", "Zoom In (Pinch Out)")
        data object ZoomOut : SystemAction("sys.zoom_out", "Zoom Out (Pinch In)")
    }

    // --- Navigation & Virtual Desktops ---
    sealed class NavAction(override val id: String, override val displayName: String) : PocketAction {
        data object DesktopNext : NavAction("nav.desktop_next", "Switch to Next Desktop")
        data object DesktopPrevious : NavAction("nav.desktop_prev", "Switch to Previous Desktop")
        data object TabNext : NavAction("nav.tab_next", "Next Browser Tab")
        data object TabPrevious : NavAction("nav.tab_prev", "Previous Browser Tab")
    }

    // --- Edit Actions ---
    sealed class EditAction(override val id: String, override val displayName: String) : PocketAction {
        data object Copy : EditAction("edit.copy", "Copy")
        data object Cut : EditAction("edit.cut", "Cut")
        data object Paste : EditAction("edit.paste", "Paste")
        data object Undo : EditAction("edit.undo", "Undo")
        data object Redo : EditAction("edit.redo", "Redo")
        data object SelectAll : EditAction("edit.select_all", "Select All")
    }

    // --- Media Controls ---
    sealed class MediaAction(override val id: String, override val displayName: String) : PocketAction {
        data object PlayPause : MediaAction("media.play_pause", "Play / Pause")
        data object NextTrack : MediaAction("media.next_track", "Next Track")
        data object PrevTrack : MediaAction("media.prev_track", "Previous Track")
        data object VolumeUp : MediaAction("media.volume_up", "Volume Up")
        data object VolumeDown : MediaAction("media.volume_down", "Volume Down")
        data object Mute : MediaAction("media.mute", "Mute Audio")
    }

    // --- Presenter Controls ---
    sealed class PresenterAction(override val id: String, override val displayName: String) : PocketAction {
        data object NextSlide : PresenterAction("pres.next_slide", "Next Slide")
        data object PreviousSlide : PresenterAction("pres.prev_slide", "Previous Slide")
        data object StartSlideshow : PresenterAction("pres.start", "Start Slideshow (F5)")
        data object ResumeSlideshow : PresenterAction("pres.resume", "Resume Slideshow (Shift+F5)")
        data object ExitSlideshow : PresenterAction("pres.exit", "Exit Slideshow (Esc)")
        data object BlankBlack : PresenterAction("pres.blank_black", "Blank Screen Black (B)")
        data object BlankWhite : PresenterAction("pres.blank_white", "Blank Screen White (W)")
    }

    // --- Pointer Actions ---
    sealed class PointerAction(override val id: String, override val displayName: String) : PocketAction {
        data object LeftClick : PointerAction("ptr.left_click", "Left Mouse Click")
        data object RightClick : PointerAction("ptr.right_click", "Right Mouse Click")
        data object MiddleClick : PointerAction("ptr.middle_click", "Middle Mouse Click")
        data object DoubleLeftClick : PointerAction("ptr.double_click", "Double Left Click")
    }

    // --- Raw Direct Keystroke Fallback ---
    data class RawKeyAction(
        val keyCode: Byte,
        val modifiers: Byte = 0,
        val label: String = "RawKey"
    ) : PocketAction {
        override val id: String = "raw.${keyCode}.${modifiers}"
        override val displayName: String = label
    }

    // --- Gamepad Semantic Digital Actions ---
    sealed class GamepadAction(override val id: String, override val displayName: String, val buttonMask: Int) : PocketAction {
        data object ButtonA : GamepadAction("gp.btn_a", "Gamepad A", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_A)
        data object ButtonB : GamepadAction("gp.btn_b", "Gamepad B", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_B)
        data object ButtonX : GamepadAction("gp.btn_x", "Gamepad X", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_X)
        data object ButtonY : GamepadAction("gp.btn_y", "Gamepad Y", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_Y)
        data object ButtonLB : GamepadAction("gp.btn_lb", "Gamepad LB", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_LB)
        data object ButtonRB : GamepadAction("gp.btn_rb", "Gamepad RB", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_RB)
        data object Select : GamepadAction("gp.select", "Gamepad Select", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_BACK)
        data object Start : GamepadAction("gp.start", "Gamepad Start", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_START)
        data object Guide : GamepadAction("gp.guide", "Gamepad Guide", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_GUIDE)
        data object ThumbL : GamepadAction("gp.l3", "Gamepad L3", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_L3)
        data object ThumbR : GamepadAction("gp.r3", "Gamepad R3", dev.aleian.pockethid.model.HidConstants.GAMEPAD_BTN_R3)
    }

    // --- Web Navigation Actions (One-Hand Mode) ---
    sealed class WebAction(override val id: String, override val displayName: String) : PocketAction {
        data object Back : WebAction("web.back", "Browser Back")
        data object Forward : WebAction("web.forward", "Browser Forward")
        data object Refresh : WebAction("web.refresh", "Browser Refresh")
        data object Home : WebAction("web.home", "Browser Home")
        data object AddressBar : WebAction("web.address_bar", "Focus Address Bar")
        data object NewTab : WebAction("web.new_tab", "New Browser Tab")
        data object CloseTab : WebAction("web.close_tab", "Close Current Tab")
    }

    // --- Video Control Actions (One-Hand Mode) ---
    sealed class VideoAction(override val id: String, override val displayName: String) : PocketAction {
        data object PlayPause : VideoAction("video.play_pause", "Play / Pause")
        data object SeekForward : VideoAction("video.seek_forward", "Seek Forward (+10s)")
        data object SeekBackward : VideoAction("video.seek_backward", "Seek Backward (-10s)")
        data object VolumeUp : VideoAction("video.volume_up", "Volume Up")
        data object VolumeDown : VideoAction("video.volume_down", "Volume Down")
        data object Mute : VideoAction("video.mute", "Mute Audio")
        data object NextTrack : VideoAction("video.next_track", "Next Video / Track")
        data object PrevTrack : VideoAction("video.prev_track", "Previous Video / Track")
        data object Fullscreen : VideoAction("video.fullscreen", "Toggle Fullscreen")
    }
}
