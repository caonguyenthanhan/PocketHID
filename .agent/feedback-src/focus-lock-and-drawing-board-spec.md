# PocketHID — Add Focus Lock + Electronic Drawing Board

You are working directly inside the existing PocketHID Android repository.

IMPORTANT:
- Inspect current HEAD before modifying anything.
- Preserve all existing working features.
- Do not redesign the entire app.
- Do not introduce a PC companion application.
- Do not add cloud/network dependency.
- Keep PocketHID as a standard driverless Bluetooth HID controller.
- Reuse the existing Action Engine / ActionResolver / ActionDispatcher architecture where appropriate.
- Do not modify unrelated Keyboard, Media, Gamepad, Mouse, Presenter behavior unless required by these two features.
- After implementation, run unit tests and assembleDebug.

==================================================
FEATURE 1 — FOCUS MODE / MODE LOCK
==================================================

Goal:

Add a "FOCUS" function that locks the current PocketHID mode so
accidental touches cannot switch to another mode.

Concept:

NORMAL:
    KEYBOARD | MOUSE | GAMEPAD | PRESENTER | ONE-HAND

FOCUS ON:
    Current Mode = locked
    Mode switching = disabled

Example:

    KEYBOARD
       ↓
    FOCUS ON
       ↓
    accidental swipe/tap on mode selector
       ↓
    still KEYBOARD

The user must explicitly unlock Focus before another mode can be selected.

--------------------------------------------------
1.1 UI
--------------------------------------------------

Add a compact Focus control to the existing UI.

Preferred visual:

FOCUS OFF:
    ○ FOCUS

FOCUS ON:
    ● FOCUS
    cyan accent / lock indicator

Use the existing PocketHID visual language:

- dark navy
- dark blue-gray
- cyan accent
- white/off-white text
- subtle state indication
- no RGB
- no excessive glow

The control must be clearly understandable but not consume excessive
screen space.

--------------------------------------------------
1.2 LOCK SEMANTICS
--------------------------------------------------

When Focus is ON:

- mode selector cannot change mode
- swipe/tap intended for mode switching must be ignored
- accidental touches must not exit the current mode
- current mode remains active
- normal controls INSIDE the current mode continue working

Examples:

Focus ON + Keyboard:
    typing still works
    Shift still works
    Caps still works
    arrows still work
    media layer still works
    mode switch does NOT work

Focus ON + Mouse:
    pointer still works
    gestures still work
    mode switch does NOT work

Focus ON + Gamepad:
    gamepad controls still work
    mode switch does NOT work

Focus ON + Presenter:
    presenter controls still work
    mode switch does NOT work

--------------------------------------------------
1.3 UNLOCK
--------------------------------------------------

The user must have an explicit way to leave Focus mode.

Preferred:

    tap FOCUS again
        ↓
    FOCUS OFF
        ↓
    mode switching enabled

Do NOT make unlocking depend on obscure gestures.

Do NOT require restarting the app.

--------------------------------------------------
1.4 STATE ARCHITECTURE
--------------------------------------------------

There must be ONE source of truth.

Prefer:

    isFocusModeLocked: Boolean

The mode selector must respect this state.

Do NOT independently disable buttons in multiple screens.

Conceptually:

    User Input
        ↓
    Mode Switch Request
        ↓
    Focus Lock Check
       / \
     LOCK OPEN
      ↓     ↓
    reject  switch

Normal controls must bypass the mode-switch lock.

--------------------------------------------------
1.5 LIFECYCLE
--------------------------------------------------

Define and test the desired behavior explicitly.

Recommended:

- Focus state survives ordinary recomposition.
- Focus state remains while changing internal layers.
- Focus state is cleared when the app is completely restarted unless
  the existing app state architecture already persists UI state.

Do NOT accidentally persist Focus forever.

When Bluetooth disconnects, do not leave the user in a confusing
state. Prefer clearing Focus when the main connection/session is
destroyed if that matches the existing connection lifecycle.

Inspect the current architecture before deciding.

--------------------------------------------------
1.6 TESTS
--------------------------------------------------

Add tests for:

- Focus OFF → mode switch allowed
- Focus ON → mode switch rejected
- Focus ON → current mode unchanged
- Focus ON → normal controls still execute
- Focus OFF → mode switch works again
- Focus toggle does not duplicate state
- recomposition does not reset Focus unexpectedly

==================================================
FEATURE 2 — ELECTRONIC DRAWING BOARD
==================================================

Add a new mode/tool called:

    DRAW
or
    DRAWING BOARD

This feature turns the phone into a small electronic drawing board
/ annotation surface.

Primary use cases:

- presentation annotation
- drawing
- handwriting
- marking on screen
- sketching
- temporary diagrams
- whiteboard-style interaction

IMPORTANT:

This is NOT a generic photo editor.

It should behave like a professional portable annotation pad.

--------------------------------------------------
2.1 MODE
--------------------------------------------------

Add:

    DRAW

to the existing mode architecture.

Current:

    KEYBOARD
    MOUSE
    GAMEPAD
    PRESENTER
    ONE-HAND

New:

    DRAW

Do not allow DRAW to bypass the unified connection state.

--------------------------------------------------
2.2 CORE DRAWING SURFACE
--------------------------------------------------

The central area should become a drawing canvas.

Concept:

┌──────────────────────────────────────┐
│ TOOLBAR                              │
│                                      │
│                                      │
│             DRAW CANVAS              │
│                                      │
│                                      │
│                                      │
│                                      │
└──────────────────────────────────────┘

Touch interaction:

1 finger DOWN
    → begin stroke

1 finger MOVE
    → continue stroke

1 finger UP
    → finish stroke

Each stroke should be represented as a sequence of points.

Do not create one independent object for every touch event.

--------------------------------------------------
2.3 DRAWING TOOLS
--------------------------------------------------

Initial tool set:

- Pen
- Eraser
- Clear
- Undo
- Redo

Keep the toolbar compact.

Preferred:

[ PEN ] [ ERASER ] [ UNDO ] [ REDO ] [ CLEAR ]

Do not overload the main canvas with buttons.

--------------------------------------------------
2.4 PEN SETTINGS
--------------------------------------------------

Minimum settings:

- stroke size
- opacity if the rendering architecture supports it

Suggested sizes:

    2
    4
    8
    12 dp

Use a simple selector rather than a large settings panel.

--------------------------------------------------
2.5 COLOR
--------------------------------------------------

Provide a small professional palette:

- White
- Cyan
- Yellow
- Red
- Blue

Use the existing UI language.

Avoid neon rainbow palettes.

Make the currently selected color visually obvious.

--------------------------------------------------
2.6 ERASER
--------------------------------------------------

Eraser must support drawing-level erase interaction.

Preferred behavior:

    touch/move over stroke
       ↓
    erase intersecting content

If true geometric stroke erasing is too complex for the current
architecture, use a practical implementation appropriate to the
existing Compose rendering stack.

Do NOT destroy the underlying stroke history when switching tools.

--------------------------------------------------
2.7 UNDO / REDO
--------------------------------------------------

Use stroke-level history.

Example:

    Stroke 1
    Stroke 2
    Stroke 3

Undo:
    remove Stroke 3

Redo:
    restore Stroke 3

Do not snapshot the entire screen for every touch event unless
there is a strong existing reason to do so.

Limit history to a reasonable size to avoid unbounded memory growth.

--------------------------------------------------
2.8 CLEAR
--------------------------------------------------

CLEAR must not trigger accidentally.

Require a confirmation step or second confirmation action.

Example:

    CLEAR
      ↓
    Are you sure?
      ↓
    CLEAR / CANCEL

Do not silently erase the drawing.

--------------------------------------------------
2.9 MULTI-TOUCH BEHAVIOR
--------------------------------------------------

Drawing is primarily one-finger.

When a second finger appears:

    current stroke
        ↓
    finish/cancel according to gesture state
        ↓
    do NOT create an accidental second drawing stroke

Reserve multi-touch for future navigation/zoom if needed.

For the first implementation:

    1 finger = draw
    2+ fingers = ignored / gesture handoff

Do not conflict with existing Fast Scroll or GestureInterpreter unless
the architecture requires explicit routing.

--------------------------------------------------
2.10 HAPTIC
--------------------------------------------------

Use subtle haptic feedback only for:

- tool selection
- clear confirmation
- possibly stroke start/end if existing haptic architecture supports it

Do not vibrate continuously during drawing.

--------------------------------------------------
2.11 PRESENTER INTEGRATION
--------------------------------------------------

This feature should be designed to work especially well with
PRESENTER mode.

Do NOT automatically merge DRAW and PRESENTER yet.

Instead, provide a clean path for future integration.

Potential future architecture:

    PRESENTER
       +
    Annotation Overlay

But for this task:
    DRAW = independent mode.

--------------------------------------------------
2.12 DATA MODEL
--------------------------------------------------

Create reusable drawing models.

Conceptually:

Stroke
    points
    color
    width
    opacity
    tool

Point
    x
    y

Do NOT store raw Android View references in the model.

Keep drawing state independent of Compose UI.

--------------------------------------------------
2.13 ARCHITECTURE
--------------------------------------------------

Prefer:

Touch Input
    ↓
DrawingController
    ↓
DrawingState
    ↓
Canvas Renderer

Example:

    DrawingState
       ├── strokes
       ├── redoStack
       ├── activeTool
       ├── color
       └── strokeWidth

UI should render state rather than own the business logic.

--------------------------------------------------
2.14 PERSISTENCE
--------------------------------------------------

For the first implementation:

- drawing lives in current session
- leaving DRAW mode should preserve it while the screen/session lives
- app restart may clear it

Do NOT add a database unless the existing architecture already requires
one.

Future extension can support Save Drawing / Export PNG.

--------------------------------------------------
2.15 BLUETOOTH ROLE
--------------------------------------------------

The drawing canvas itself is local Android interaction.

Do NOT transmit every drawing point over Bluetooth HID.

Bluetooth HID should only be used if later implementing host-side
annotation commands.

For this initial feature:

    Touch
      ↓
    Local Drawing Canvas

No companion PC software.

--------------------------------------------------
2.16 OPTIONAL HOST CONTROL — DO NOT IMPLEMENT WITHOUT INSPECTION
--------------------------------------------------

Do NOT invent a proprietary HID protocol.

If host-side drawing/annotation is later required, it must be designed
as a standards-based HID capability or a separate future feature.

For this task, prioritize the local electronic drawing board.

==================================================
3. NAVIGATION / CONNECTION INTEGRATION
==================================================

The new DRAW mode must obey the same real connection state model.

When:

CONNECTED:
    DRAW is available

DISCONNECTED:
    DRAW remains viewable only according to existing app policy,
    but must not pretend that HID is connected.

Focus lock must work across DRAW as well.

Example:

    DRAW
      ↓
    FOCUS ON
      ↓
    accidental swipe
      ↓
    remain DRAW

==================================================
4. UI CONSISTENCY
==================================================

Maintain:

dark navy
dark blue-gray
cyan accent
white/off-white
muted blue-gray

Avoid:

- gaming RGB
- neon
- hacker aesthetic
- excessive borders
- excessive animations
- giant toolbars

PocketHID should continue to feel like:

Professional Mobile Command Deck

not:

Gaming controller
or
generic drawing app

==================================================
5. RESPONSIVE DESIGN
==================================================

DRAW must work on:

- portrait
- landscape

Optimize the canvas for maximum usable area.

Landscape should favor:

    canvas = majority of screen
    toolbar = compact edge/bottom zone

Do not hard-code one screen resolution.

Respect WindowInsets / display cutouts.

==================================================
6. TESTS
==================================================

Add unit tests for:

FOCUS:

- lock
- unlock
- blocked mode switch
- allowed normal action

DRAW:

- start stroke
- append points
- finish stroke
- undo
- redo
- clear
- tool selection
- color selection
- width selection
- multi-touch behavior
- bounded history

==================================================
7. BUILD
==================================================

Run:

./gradlew.bat testDebugUnitTest

./gradlew.bat assembleDebug

Then:

git diff --stat
git status --short

Do not claim physical-device verification unless an Android device
was actually connected and tested.

==================================================
8. IMPLEMENTATION ORDER
==================================================

Do NOT implement everything in one uncontrolled refactor.

Order:

PHASE 1
    Inspect architecture and existing navigation/state.

PHASE 2
    Implement Focus Lock.

PHASE 3
    Unit test Focus Lock.

PHASE 4
    Implement DRAW mode + DrawingState + Canvas.

PHASE 5
    Implement Pen / Eraser / Undo / Redo / Clear.

PHASE 6
    Implement color + stroke width.

PHASE 7
    Unit tests.

PHASE 8
    Build APK.

PHASE 9
    Physical-device validation when device is available.

==================================================
9. ACCEPTANCE CRITERIA
==================================================

FOCUS LOCK:

[ ] Focus OFF allows mode switching
[ ] Focus ON blocks accidental mode switching
[ ] Current mode remains active
[ ] Controls within current mode still work
[ ] Explicit unlock works
[ ] Visual state is obvious
[ ] No duplicate Focus state

DRAWING BOARD:

[ ] DRAW appears as a real PocketHID mode
[ ] Canvas occupies most usable screen area
[ ] Pen works smoothly
[ ] Eraser works
[ ] Undo works
[ ] Redo works
[ ] Clear requires confirmation
[ ] Color selection works
[ ] Stroke width works
[ ] Multi-touch does not create accidental strokes
[ ] Display cutout safe area respected
[ ] Drawing is preserved while remaining in session
[ ] UI matches PocketHID Command Deck design language

==================================================
10. FINAL REPORT
==================================================

Report:

1. Files changed
2. Focus Lock architecture
3. Focus Lock UI
4. Drawing Board architecture
5. Drawing state model
6. Drawing tools implemented
7. Tests added
8. testDebugUnitTest result
9. assembleDebug result
10. Physical-device verification status
11. Any remaining limitations

IMPORTANT:

Do NOT claim that DRAW can annotate the PC screen unless that
functionality was actually implemented and tested.

The initial DRAW feature is a local electronic drawing board on the
phone.

Focus Lock must be a real mode-switch lock, not merely a visual
indicator.
