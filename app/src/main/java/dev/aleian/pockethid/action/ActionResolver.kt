package dev.aleian.pockethid.action

import dev.aleian.pockethid.model.HidConstants

/**
 * Translates high-level [PocketAction]s into concrete [ActionExecutionPlan]s
 * based on the host computer's operating system [HostOs].
 */
object ActionResolver {

    fun resolve(
        action: PocketAction,
        hostOs: HostOs = HostOs.WINDOWS,
        zoomMode: String = "Wheel"
    ): ActionExecutionPlan {
        return when (action) {
            // --- System Actions ---
            is PocketAction.SystemAction.TaskView -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_GUI)
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_UP, HidConstants.MOD_LEFT_CTRL) // Mission Control
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFTGUI, 0) // Overview
            }

            is PocketAction.SystemAction.ShowDesktop -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI)
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_F11, 0)
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI)
            }

            is PocketAction.SystemAction.LockPC -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI)
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_Q,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_CTRL.toInt()).toByte()
                )
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI)
            }

            is PocketAction.SystemAction.AppSwitcherNext -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT)
            }

            is PocketAction.SystemAction.AppSwitcherPrev -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_TAB,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                else -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_TAB,
                    (HidConstants.MOD_LEFT_ALT.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
            }

            is PocketAction.SystemAction.Screenshot -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_S,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_4,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_PRINTSCREEN, 0)
            }

            is PocketAction.SystemAction.RunDialog -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_R, HidConstants.MOD_LEFT_GUI)
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_SPACE, HidConstants.MOD_LEFT_GUI) // Spotlight
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_F2, HidConstants.MOD_LEFT_ALT)
            }

            is PocketAction.SystemAction.ZoomIn -> {
                val mod = when (hostOs) {
                    HostOs.MACOS -> HidConstants.MOD_LEFT_GUI
                    else -> HidConstants.MOD_LEFT_CTRL
                }
                if (zoomMode.equals("Keys", ignoreCase = true)) {
                    ActionExecutionPlan.KeyStroke(HidConstants.KEY_EQUAL, mod)
                } else {
                    ActionExecutionPlan.ZoomWheel(wheelDelta = 1, modifier = mod)
                }
            }

            is PocketAction.SystemAction.ZoomOut -> {
                val mod = when (hostOs) {
                    HostOs.MACOS -> HidConstants.MOD_LEFT_GUI
                    else -> HidConstants.MOD_LEFT_CTRL
                }
                if (zoomMode.equals("Keys", ignoreCase = true)) {
                    ActionExecutionPlan.KeyStroke(HidConstants.KEY_MINUS, mod)
                } else {
                    ActionExecutionPlan.ZoomWheel(wheelDelta = -1, modifier = mod)
                }
            }

            // --- Navigation & Virtual Desktops ---
            is PocketAction.NavAction.DesktopNext -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_RIGHT,
                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                )
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_RIGHT, HidConstants.MOD_LEFT_CTRL)
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_RIGHT,
                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
                )
            }

            is PocketAction.NavAction.DesktopPrevious -> when (hostOs) {
                HostOs.WINDOWS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_LEFT,
                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                )
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFT, HidConstants.MOD_LEFT_CTRL)
                HostOs.LINUX -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_LEFT,
                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
                )
            }

            is PocketAction.NavAction.TabNext -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_RIGHTBRACE,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.NavAction.TabPrevious -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_LEFTBRACE,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                else -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_TAB,
                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
            }

            // --- Edit Actions ---
            is PocketAction.EditAction.Copy -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_C, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.EditAction.Cut -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_X, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_X, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.EditAction.Paste -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_V, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.EditAction.Undo -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_Z, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.EditAction.Redo -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_Z,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_Y, HidConstants.MOD_LEFT_CTRL)
            }

            is PocketAction.EditAction.SelectAll -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_A, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_A, HidConstants.MOD_LEFT_CTRL)
            }

            // --- Media Controls ---
            is PocketAction.MediaAction.PlayPause -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE)
            is PocketAction.MediaAction.NextTrack -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_SCAN_NEXT)
            is PocketAction.MediaAction.PrevTrack -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_SCAN_PREV)
            is PocketAction.MediaAction.VolumeUp -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_VOLUME_UP)
            is PocketAction.MediaAction.VolumeDown -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN)
            is PocketAction.MediaAction.Mute -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_MUTE)

            // --- Presenter Controls ---
            is PocketAction.PresenterAction.NextSlide -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_RIGHT, 0)
            is PocketAction.PresenterAction.PreviousSlide -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFT, 0)
            is PocketAction.PresenterAction.StartSlideshow -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_F5, 0)
            is PocketAction.PresenterAction.ResumeSlideshow -> ActionExecutionPlan.KeyStroke(
                HidConstants.KEY_F5,
                HidConstants.MOD_LEFT_SHIFT
            )
            is PocketAction.PresenterAction.ExitSlideshow -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_ESC, 0)
            is PocketAction.PresenterAction.BlankBlack -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_B, 0)
            is PocketAction.PresenterAction.BlankWhite -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_W, 0)

            // --- Pointer Actions ---
            is PocketAction.PointerAction.LeftClick -> ActionExecutionPlan.MouseButtonClick(HidConstants.MOUSE_BUTTON_LEFT)
            is PocketAction.PointerAction.RightClick -> ActionExecutionPlan.MouseButtonClick(HidConstants.MOUSE_BUTTON_RIGHT)
            is PocketAction.PointerAction.MiddleClick -> ActionExecutionPlan.MouseButtonClick(HidConstants.MOUSE_BUTTON_MIDDLE)
            is PocketAction.PointerAction.DoubleLeftClick -> ActionExecutionPlan.Sequence(
                listOf(
                    ActionExecutionPlan.MouseButtonClick(HidConstants.MOUSE_BUTTON_LEFT),
                    ActionExecutionPlan.MouseButtonClick(HidConstants.MOUSE_BUTTON_LEFT)
                ),
                stepDelayMs = 50L
            )

            // --- Raw Keystroke Fallback ---
            is PocketAction.RawKeyAction -> ActionExecutionPlan.KeyStroke(action.keyCode, action.modifiers)

            // --- Gamepad Semantic Action ---
            is PocketAction.GamepadAction -> ActionExecutionPlan.GamepadButtonClick(action.buttonMask)

            // --- Web Navigation Actions (One-Hand Mode) ---
            is PocketAction.WebAction.Back -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFTBRACE, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFT, HidConstants.MOD_LEFT_ALT)
            }
            is PocketAction.WebAction.Forward -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_RIGHTBRACE, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_RIGHT, HidConstants.MOD_LEFT_ALT)
            }
            is PocketAction.WebAction.Refresh -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_R, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_F5, 0)
            }
            is PocketAction.WebAction.Home -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(
                    HidConstants.KEY_H,
                    (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                )
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_HOME, HidConstants.MOD_LEFT_ALT)
            }
            is PocketAction.WebAction.AddressBar -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_L, HidConstants.MOD_LEFT_CTRL)
            }
            is PocketAction.WebAction.NewTab -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_T, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_T, HidConstants.MOD_LEFT_CTRL)
            }
            is PocketAction.WebAction.CloseTab -> when (hostOs) {
                HostOs.MACOS -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_W, HidConstants.MOD_LEFT_GUI)
                else -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_W, HidConstants.MOD_LEFT_CTRL)
            }

            // --- Video Control Actions (One-Hand Mode) ---
            is PocketAction.VideoAction.PlayPause -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_PLAY_PAUSE)
            is PocketAction.VideoAction.SeekForward -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_RIGHT, 0)
            is PocketAction.VideoAction.SeekBackward -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_LEFT, 0)
            is PocketAction.VideoAction.VolumeUp -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_VOLUME_UP)
            is PocketAction.VideoAction.VolumeDown -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_VOLUME_DOWN)
            is PocketAction.VideoAction.Mute -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_MUTE)
            is PocketAction.VideoAction.NextTrack -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_SCAN_NEXT)
            is PocketAction.VideoAction.PrevTrack -> ActionExecutionPlan.ConsumerKey(HidConstants.CONSUMER_SCAN_PREV)
            is PocketAction.VideoAction.Fullscreen -> ActionExecutionPlan.KeyStroke(HidConstants.KEY_F, 0)
        }
    }
}
