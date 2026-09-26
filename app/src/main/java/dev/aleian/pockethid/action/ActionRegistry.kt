package dev.aleian.pockethid.action

/**
 * Metadata descriptor for an action registered in PocketHID.
 */
data class ActionDescriptor(
    val action: PocketAction,
    val category: String,
    val defaultShortcutText: String,
    val description: String = ""
)

/**
 * Central catalog of all known semantic actions in PocketHID.
 */
object ActionRegistry {

    private val actions: Map<String, ActionDescriptor> = listOf(
        // System
        ActionDescriptor(
            PocketAction.SystemAction.TaskView,
            "System",
            "Win+Tab / Mission Control",
            "Opens window and virtual desktop overview"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.ShowDesktop,
            "System",
            "Win+D / F11",
            "Minimizes all windows to reveal desktop"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.LockPC,
            "System",
            "Win+L / Cmd+Ctrl+Q",
            "Locks the host computer"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.AppSwitcherNext,
            "System",
            "Alt+Tab / Cmd+Tab",
            "Switch to next open application"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.AppSwitcherPrev,
            "System",
            "Alt+Shift+Tab / Cmd+Shift+Tab",
            "Switch to previous application"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.Screenshot,
            "System",
            "Win+Shift+S / Cmd+Shift+4",
            "Launches screenshot snipping tool"
        ),
        ActionDescriptor(
            PocketAction.SystemAction.RunDialog,
            "System",
            "Win+R / Cmd+Space",
            "Opens run prompt or Spotlight search"
        ),

        // Navigation
        ActionDescriptor(
            PocketAction.NavAction.DesktopNext,
            "Navigation",
            "Ctrl+Win+Right / Ctrl+Right",
            "Switch to the next virtual desktop"
        ),
        ActionDescriptor(
            PocketAction.NavAction.DesktopPrevious,
            "Navigation",
            "Ctrl+Win+Left / Ctrl+Left",
            "Switch to the previous virtual desktop"
        ),
        ActionDescriptor(
            PocketAction.NavAction.TabNext,
            "Navigation",
            "Ctrl+Tab / Cmd+Shift+]",
            "Switch to the next browser tab"
        ),
        ActionDescriptor(
            PocketAction.NavAction.TabPrevious,
            "Navigation",
            "Ctrl+Shift+Tab / Cmd+Shift+[",
            "Switch to the previous browser tab"
        ),

        // Edit
        ActionDescriptor(
            PocketAction.EditAction.Copy,
            "Edit",
            "Ctrl+C / Cmd+C",
            "Copy selection to clipboard"
        ),
        ActionDescriptor(
            PocketAction.EditAction.Cut,
            "Edit",
            "Ctrl+X / Cmd+X",
            "Cut selection to clipboard"
        ),
        ActionDescriptor(
            PocketAction.EditAction.Paste,
            "Edit",
            "Ctrl+V / Cmd+V",
            "Paste from clipboard"
        ),
        ActionDescriptor(
            PocketAction.EditAction.Undo,
            "Edit",
            "Ctrl+Z / Cmd+Z",
            "Undo previous operation"
        ),
        ActionDescriptor(
            PocketAction.EditAction.Redo,
            "Edit",
            "Ctrl+Y / Cmd+Shift+Z",
            "Redo undone operation"
        ),
        ActionDescriptor(
            PocketAction.EditAction.SelectAll,
            "Edit",
            "Ctrl+A / Cmd+A",
            "Select all content"
        ),

        // Media
        ActionDescriptor(
            PocketAction.MediaAction.PlayPause,
            "Media",
            "Media Play/Pause",
            "Toggle audio/video playback"
        ),
        ActionDescriptor(
            PocketAction.MediaAction.NextTrack,
            "Media",
            "Next Track",
            "Skip to next track"
        ),
        ActionDescriptor(
            PocketAction.MediaAction.PrevTrack,
            "Media",
            "Prev Track",
            "Skip to previous track"
        ),
        ActionDescriptor(
            PocketAction.MediaAction.VolumeUp,
            "Media",
            "Volume +",
            "Increase system audio volume"
        ),
        ActionDescriptor(
            PocketAction.MediaAction.VolumeDown,
            "Media",
            "Volume -",
            "Decrease system audio volume"
        ),
        ActionDescriptor(
            PocketAction.MediaAction.Mute,
            "Media",
            "Mute",
            "Toggle system audio mute"
        ),

        // Presenter
        ActionDescriptor(
            PocketAction.PresenterAction.NextSlide,
            "Presenter",
            "Right Arrow",
            "Advance to next presentation slide"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.PreviousSlide,
            "Presenter",
            "Left Arrow",
            "Return to previous slide"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.StartSlideshow,
            "Presenter",
            "F5",
            "Start presentation slideshow"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.ResumeSlideshow,
            "Presenter",
            "Shift+F5",
            "Resume slideshow from current slide"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.ExitSlideshow,
            "Presenter",
            "Esc",
            "Exit presentation slideshow"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.BlankBlack,
            "Presenter",
            "B",
            "Blank presentation screen to black"
        ),
        ActionDescriptor(
            PocketAction.PresenterAction.BlankWhite,
            "Presenter",
            "W",
            "Blank presentation screen to white"
        ),

        // Pointer
        ActionDescriptor(
            PocketAction.PointerAction.LeftClick,
            "Pointer",
            "Left Click",
            "Standard primary mouse click"
        ),
        ActionDescriptor(
            PocketAction.PointerAction.RightClick,
            "Pointer",
            "Right Click",
            "Secondary mouse click (context menu)"
        ),
        ActionDescriptor(
            PocketAction.PointerAction.MiddleClick,
            "Pointer",
            "Middle Click",
            "Auxiliary mouse wheel click"
        ),
        ActionDescriptor(
            PocketAction.PointerAction.DoubleLeftClick,
            "Pointer",
            "Double Click",
            "Double left mouse click"
        ),

        // Web Navigation (One-Hand)
        ActionDescriptor(
            PocketAction.WebAction.Back,
            "Web Navigation",
            "Alt+Left / Cmd+[",
            "Navigate back in browser history"
        ),
        ActionDescriptor(
            PocketAction.WebAction.Forward,
            "Web Navigation",
            "Alt+Right / Cmd+]",
            "Navigate forward in browser history"
        ),
        ActionDescriptor(
            PocketAction.WebAction.Refresh,
            "Web Navigation",
            "F5 / Cmd+R",
            "Reload current web page"
        ),
        ActionDescriptor(
            PocketAction.WebAction.Home,
            "Web Navigation",
            "Alt+Home",
            "Navigate to browser home page"
        ),
        ActionDescriptor(
            PocketAction.WebAction.AddressBar,
            "Web Navigation",
            "Ctrl+L / Cmd+L",
            "Focus address bar for URL entry"
        ),
        ActionDescriptor(
            PocketAction.WebAction.NewTab,
            "Web Navigation",
            "Ctrl+T / Cmd+T",
            "Open a new browser tab"
        ),
        ActionDescriptor(
            PocketAction.WebAction.CloseTab,
            "Web Navigation",
            "Ctrl+W / Cmd+W",
            "Close the active browser tab"
        ),

        // Video Control (One-Hand)
        ActionDescriptor(
            PocketAction.VideoAction.PlayPause,
            "Video Control",
            "Media Play/Pause",
            "Toggle video/media playback"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.SeekForward,
            "Video Control",
            "Right Arrow",
            "Seek forward 10 seconds"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.SeekBackward,
            "Video Control",
            "Left Arrow",
            "Seek backward 10 seconds"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.VolumeUp,
            "Video Control",
            "Media Vol+",
            "Increase system audio volume"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.VolumeDown,
            "Video Control",
            "Media Vol-",
            "Decrease system audio volume"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.Mute,
            "Video Control",
            "Media Mute",
            "Mute system audio"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.NextTrack,
            "Video Control",
            "Media Next",
            "Skip to next video or track"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.PrevTrack,
            "Video Control",
            "Media Prev",
            "Return to previous video or track"
        ),
        ActionDescriptor(
            PocketAction.VideoAction.Fullscreen,
            "Video Control",
            "F",
            "Toggle fullscreen video mode"
        )
    ).associateBy { it.action.id }

    fun getAll(): List<ActionDescriptor> = actions.values.toList()

    fun get(actionId: String): ActionDescriptor? = actions[actionId]

    fun findByCategory(category: String): List<ActionDescriptor> {
        return actions.values.filter { it.category.equals(category, ignoreCase = true) }
    }
}
