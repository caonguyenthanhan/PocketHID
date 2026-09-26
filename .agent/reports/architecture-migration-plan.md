# PocketHID — Architecture Migration Plan: Semantic Action Engine

**Version:** 1.0  
**Date:** 2026-09-27  
**Status:** DRAFT → PROPOSED  
**Author:** AI-Master (T7) / Mobile UX & HID Systems Architect  

---

## 1. Inventory of Current Direct Transport Calls

Through full codebase inspection, the following direct calls to `InputTransport` were identified:

| Source File | Caller Context | Current Direct Call | Target Semantic Intent |
| :--- | :--- | :--- | :--- |
| `MultiTouchTrackpad.kt` | 1-Finger Tap | `transport.sendMouseClick(MOUSE_BUTTON_LEFT)` | `PointerAction.LeftClick` |
| `MultiTouchTrackpad.kt` | 2-Finger Tap | `transport.sendMouseClick(MOUSE_BUTTON_RIGHT)` | `PointerAction.RightClick` |
| `MultiTouchTrackpad.kt` | 3-Finger Tap | `transport.sendMouseClick(MOUSE_BUTTON_MIDDLE)` | `PointerAction.MiddleClick` |
| `MultiTouchTrackpad.kt` | 3-Finger Swipe Left | `transport.sendKeyClick(KEY_LEFT, CTRL \| GUI)` | `NavAction.DesktopPrevious` |
| `MultiTouchTrackpad.kt` | 3-Finger Swipe Right | `transport.sendKeyClick(KEY_RIGHT, CTRL \| GUI)` | `NavAction.DesktopNext` |
| `MultiTouchTrackpad.kt` | 3-Finger Swipe Up | `transport.sendKeyClick(KEY_TAB, GUI)` | `SystemAction.TaskView` / Mission Control |
| `MultiTouchTrackpad.kt` | 3-Finger Swipe Down | `transport.sendKeyClick(KEY_D, GUI)` | `SystemAction.ShowDesktop` |
| `MultiTouchTrackpad.kt` | 4-Finger Swipe Left | `transport.sendKeyClick(KEY_TAB, ALT \| SHIFT)` | `SystemAction.AppSwitcherPrev` |
| `MultiTouchTrackpad.kt` | 4-Finger Swipe Right | `transport.sendKeyClick(KEY_TAB, ALT)` | `SystemAction.AppSwitcherNext` |
| `MultiTouchTrackpad.kt` | Continuous Move/Scroll | `transport.sendMouseMove(dx, dy, buttons, wheel)` | Continuous Pointer Stream (direct high-speed path) |
| `PresenterScreen.kt` | Start Button | `transport.sendKeyClick(KEY_F5, 0)` | `PresenterAction.StartSlideshow` |
| `PresenterScreen.kt` | Resume Button | `transport.sendKeyClick(KEY_F5, SHIFT)` | `PresenterAction.ResumeSlideshow` |
| `PresenterScreen.kt` | Exit Button | `transport.sendKeyClick(KEY_ESC, 0)` | `PresenterAction.ExitSlideshow` |
| `PresenterScreen.kt` | Blank Black | `transport.sendKeyClick(KEY_B, 0)` | `PresenterAction.BlankBlack` |
| `PresenterScreen.kt` | Blank White | `transport.sendKeyClick(KEY_W, 0)` | `PresenterAction.BlankWhite` |
| `PresenterScreen.kt` | Next Slide | `transport.sendKeyClick(KEY_RIGHT, 0)` | `PresenterAction.NextSlide` |
| `PresenterScreen.kt` | Prev Slide | `transport.sendKeyClick(KEY_LEFT, 0)` | `PresenterAction.PreviousSlide` |
| `CommandPaletteSheet.kt` | Command rows | `cmd.action(transport)` | `PocketAction` resolution |
| `KeyboardScreen.kt` | Layer keys | `transport.sendKeyClick(code, mods)` | Raw Key / `KeyAction` |
| `LandscapeDeckScreen.kt` | Layer & macro keys | `transport.sendKeyClick(...)` | Raw Key / `KeyAction` |
| `LandscapeDeckScreen.kt` | Media keys | `transport.sendConsumerClick(usage)` | `MediaAction.*` |

---

## 2. Duplicate Mappings & Inconsistencies

1. **Task View & Desktop Actions**:
   - `MultiTouchTrackpad` sends `Win+Tab` for Task View.
   - `CommandPaletteSheet` also sends `Win+Tab` for Task View.
   - Both are hardcoded independently with no shared constant or action reference.
2. **Virtual Desktop Navigation**:
   - `MultiTouchTrackpad` hardcodes `Ctrl+Win+Left/Right`.
   - `CommandPaletteSheet` hardcodes `Ctrl+Win+Left/Right`.
   - On macOS, this shortcut is invalid (`Ctrl+Left/Right` is standard Spaces navigation).
3. **App Switcher**:
   - `MultiTouchTrackpad` hardcodes `Alt+Tab` and `Alt+Shift+Tab`.
   - `CommandPaletteSheet` hardcodes `Alt+Tab`.
   - On macOS, `Cmd+Tab` is required.
4. **Media Keys**:
   - `KeyboardScreen`, `DeckKeyboardZone`, and `CommandPaletteSheet` each invoke `CONSUMER_PLAY_PAUSE`, `CONSUMER_VOLUME_UP`, etc., via distinct lambdas.

---

## 3. Semantic Action Taxonomy

Every discrete action is categorized into a typed sealed hierarchy under `PocketAction`:

```
PocketAction
├── SystemAction
│   ├── TaskView (Win+Tab / Mission Control)
│   ├── ShowDesktop (Win+D / F11)
│   ├── LockPC (Win+L / Cmd+Ctrl+Q)
│   ├── AppSwitcherNext (Alt+Tab / Cmd+Tab)
│   ├── AppSwitcherPrev (Alt+Shift+Tab / Cmd+Shift+Tab)
│   ├── Screenshot (Win+Shift+S / Cmd+Shift+4)
│   └── RunDialog (Win+R / Spotlight Cmd+Space)
├── NavAction
│   ├── DesktopNext (Ctrl+Win+Right / Ctrl+Right)
│   ├── DesktopPrevious (Ctrl+Win+Left / Ctrl+Left)
│   └── TabNext / TabPrev
├── EditAction
│   ├── Copy (Ctrl+C / Cmd+C)
│   ├── Cut (Ctrl+X / Cmd+X)
│   ├── Paste (Ctrl+V / Cmd+V)
│   ├── Undo (Ctrl+Z / Cmd+Z)
│   ├── Redo (Ctrl+Y / Cmd+Shift+Z)
│   └── SelectAll (Ctrl+A / Cmd+A)
├── MediaAction
│   ├── PlayPause (Consumer 0xCD)
│   ├── NextTrack (Consumer 0xB5)
│   ├── PrevTrack (Consumer 0xB6)
│   ├── VolumeUp (Consumer 0xE9)
│   ├── VolumeDown (Consumer 0xEA)
│   └── Mute (Consumer 0xE2)
├── PresenterAction
│   ├── NextSlide (Right Arrow)
│   ├── PreviousSlide (Left Arrow)
│   ├── StartSlideshow (F5)
│   ├── ResumeSlideshow (Shift+F5)
│   ├── ExitSlideshow (Escape)
│   ├── BlankBlack ('B')
│   └── BlankWhite ('W')
├── PointerAction
│   ├── LeftClick
│   ├── RightClick
│   └── MiddleClick
└── RawKeyAction
    └── KeyStroke(keyCode: Byte, modifiers: Byte)
```

---

## 4. The Action Engine Pipeline

```
  Trigger Source (Gesture, Key, Button, Palette)
                         │
                         ▼
                    PocketAction
                         │
                         ▼
                   ActionResolver
       (Inputs: PocketAction + TargetHostOs)
                         │
                         ▼
                ActionExecutionPlan
  (SingleKey | KeyCombo | ConsumerReport | MouseButton)
                         │
                         ▼
                  ActionDispatcher
           (InputTransport Coroutine Host)
                         │
                         ▼
                   InputTransport
```

### High-Speed Continuous Mouse Path
*Note on continuous mouse movement:* High-frequency continuous touchpad motion (`dx, dy` at 125–250 Hz) bypasses semantic object allocation and streams directly via `transport.sendMouseMove` to maintain sub-millisecond dispatch performance without garbage collection pressure. Discrete gestures (taps, swipes, clicks) flow through the semantic `ActionDispatcher`.

---

## 5. Migration Boundaries & Unchanged Files

### Files to Introduce:
1. `dev/aleian/pockethid/action/HostOs.kt`: Enum (`WINDOWS`, `MACOS`, `LINUX`).
2. `dev/aleian/pockethid/action/PocketAction.kt`: Sealed semantic action hierarchy.
3. `dev/aleian/pockethid/action/ActionExecutionPlan.kt`: Low-level executable representation.
4. `dev/aleian/pockethid/action/ActionResolver.kt`: OS-aware translation logic.
5. `dev/aleian/pockethid/action/ActionDispatcher.kt`: Clean dispatcher wiring execution plans to `InputTransport`.
6. `dev/aleian/pockethid/action/ActionRegistry.kt`: Central catalog of all registered actions with metadata.

### Files to Migrate in this Safe Slice:
1. `AppSettings.kt`: Add `hostOs: HostOs = HostOs.WINDOWS` setting.
2. `MultiTouchTrackpad.kt`: Replace direct gesture `transport.sendKeyClick` and `sendMouseClick` with `actionDispatcher.dispatch(action)`.
3. `GestureInterpreter.kt`: Add semantic gesture output events (`GestureEvent.ActionTriggered(PocketAction)`).
4. `PresenterScreen.kt`: Delegate slide controls through `ActionDispatcher`.

### Files that MUST Remain Unchanged:
- `BtHidTransport.kt`: Core Bluetooth HID descriptor and driver state machine remain intact.
- `HidDeviceService.kt`: Background service and notification lifecycle remain untouched.
- `DedicatedNumberRow.kt`: Component UI logic remains identical.
- `DeckMacroZone.kt`, `DeckKeyboardZone.kt`, `DeckUtilityZone.kt`: Visual composable trees and parameter contracts remain backwards-compatible.
- `MainScreen.kt`, `MainActivity.kt`: Root navigation and orientation handling remain unchanged.

---

## 6. Verification & Test Plan
1. **Unit Tests**:
   - `ActionResolverTest.kt`: Verify Windows vs macOS vs Linux resolution for Virtual Desktops, Task View, App Switcher, and Edit shortcuts.
   - `ActionDispatcherTest.kt`: Verify mock transport receives exact scancodes for resolved plans.
   - `GestureInterpreterTest.kt`: Maintain 100% pass on gesture detection and priority lock.
2. **Build Verification**:
   - `gradlew.bat testDebugUnitTest` must pass with 0 failures.
   - `gradlew.bat assembleDebug` must compile cleanly with 0 errors.
