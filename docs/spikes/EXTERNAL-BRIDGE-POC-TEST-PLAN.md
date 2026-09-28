# External HID Bridge Proof-of-Concept Test Plan

**Document ID:** `TEST-PLAN-002-EXTERNAL-BRIDGE`  
**Related Specifications:**  
- [`POCKETHID-BRIDGE-WIRE-PROTOCOL.md`](file:///d:/desktop/PocketHID/docs/spec/POCKETHID-BRIDGE-WIRE-PROTOCOL.md)  
- [`POCKETHID-BRIDGE-HID-DESCRIPTOR.md`](file:///d:/desktop/PocketHID/docs/spec/POCKETHID-BRIDGE-HID-DESCRIPTOR.md)  
- [`ADR-004-ios-pc-transport-research.md`](file:///d:/desktop/PocketHID/docs/adr/ADR-004-ios-pc-transport-research.md)  
**Status:** DRAFT SPECIFICATION — PRE-IMPLEMENTATION TESTING CRITERIA  

---

## 1. Principles of Verification

1. **Host-Side Observable Side Effects Only:** Verification NEVER succeeds by confirming packet arrival on the bridge or observing a log line on the phone (`packet received != verified`). Every step must confirm an operating system side effect on the PC (e.g. character typed into Notepad, cursor displacement, OS volume change, Windows Ink stroke).
2. **Zero Injected Software on PC:** The test must be executed on a pristine Windows host without companion software, daemons, background listeners, or non-standard drivers installed.
3. **Fail-Safe Integrity:** Verification of step 23 and 24 (abrupt disconnection and stuck key immunity) is mandatory before certifying any prototype.

---

## 2. Test Execution Metadata Record

Every trial run must document the complete physical test bench before running step 1:

```markdown
### Test Bench Configuration
- **Date & UTC Time:** YYYY-MM-DD HH:MM:SS UTC
- **Test Runner:** [Engineer Name]
- **iPhone Model:** [e.g. iPhone 15 Pro (A3102)]
- **iOS Version:** [e.g. iOS 17.5 (Build 21F79)]
- **Bridge Hardware:** [e.g. ESP32-S3-DevKitC-1-N8R8 / Waveshare RP2040-Zero + nRF52840]
- **Bridge Firmware Hash / Tag:** [e.g. v0.1.0-alpha, commit abc1234]
- **Target Host PC:** [e.g. Lenovo ThinkPad T14 Gen 3 / Custom Desktop PC]
- **Host Operating System:** [e.g. Windows 11 Pro 23H2 (Build 22631.3593)]
- **Bridge Host Interface Under Test:** [USB Composite HID / Bluetooth Classic HID]
- **Windows Device Manager Hardware ID Evidence:** [e.g. USB\VID_303A&PID_1001&MI_00]
```

---

## 3. The 24-Step Verification Matrix

| Step # | Test Name | Injected Action | Expected Host-Side Result | Physical Evidence Required |
|---|---|---|---|---|
| **1** | **Bridge Power-on** | Connect bridge to PC USB port | Bridge powers on; Windows plays device connect chime; composite USB HID device enumerates in Device Manager | Windows Device Manager shows 5 HID interfaces under Human Interface Devices without warning icons (`!`) |
| **2** | **BLE Advertisement** | Firmware boot complete | Bridge begins advertising custom 128-bit PocketHID Service UUID | BLE Scanner / CoreBluetooth central detects `PocketHID-Bridge` |
| **3** | **iPhone Discovery** | Launch PocketHID iOS app | App scans and identifies bridge peripheral within 3.0s | PocketHID UI indicates discovered bridge hardware |
| **4** | **Pair / Connect** | Tap connect in app | BLE connection established, MTU exchanged, session hello handshake completes | Bridge status LED turns solid; app shows bridge connected |
| **5** | **Reconnect** | Disconnect via app, wait 5s, reconnect | Link cleanly re-established within 2.0s without re-plugging bridge | App restores active session; zero USB disconnect on PC |
| **6** | **Keyboard A** | Tap 'a' key in Keyboard Mode | Lowercase letter 'a' appended to active Notepad document | Text buffer inspect in Notepad contains 'a' |
| **7** | **Shift+A** | Hold Shift modifier, tap 'a' | Capital letter 'A' appended to Notepad | Text buffer inspect contains 'A' |
| **8** | **Ctrl+C** | Select text, tap Ctrl+C | Highlighted text copied to Windows clipboard | Windows clipboard buffer populated (`Get-Clipboard`) |
| **9** | **Win+D** | Tap Win+D shortcut | All active windows minimize, revealing Windows Desktop | Desktop window state changes |
| **10** | **Mouse Move** | Swipe on trackpad (+150 dx, -80 dy) | PC cursor displaces right by 150px and up by 80px | `GetCursorPos` delta coordinates match direction |
| **11** | **Mouse Click** | Tap trackpad surface | Primary click triggers (activates button under cursor) | Windows button state activates |
| **12** | **Scroll** | Two-finger drag (+120 wheel) | Web page / document scrolls vertically | Document viewport scroll offset changes |
| **13** | **Volume +** | Tap Volume Up in Presenter Mode | Windows master volume increments by 2% | Windows audio on-screen display (OSD) shows volume increase |
| **14** | **Volume -** | Tap Volume Down in Presenter Mode | Windows master volume decrements by 2% | Windows audio OSD shows volume decrease |
| **15** | **Presenter NEXT** | Tap NEXT in Presenter Mode | PowerPoint advances to next slide | Slide index in PowerPoint increments |
| **16** | **Presenter PREV** | Tap PREV in Presenter Mode | PowerPoint returns to previous slide | Slide index in PowerPoint decrements |
| **17** | **Gamepad Button** | Press Button A in Gamepad Mode | Windows Game Controller API registers Button 1 pressed | `joy.cpl` button 1 indicator lights up |
| **18** | **Gamepad Analog** | Deflect left stick to (+100% X, 0% Y) | Left stick axis deflects to maximum positive position | `joy.cpl` axis crosshair pegs to far right |
| **19** | **Tablet Top-Left** | Touch top-left corner of tablet canvas | Windows Ink cursor jumps to absolute coordinates (0, 0) | Cursor position inspect is (0, 0); stroke starts in Paint |
| **20** | **Tablet Center** | Touch center of tablet canvas | Windows Ink cursor jumps to absolute center (Width/2, Height/2) | Cursor position inspect is screen midpoint |
| **21** | **Tablet Bottom-Right**| Touch bottom-right of tablet canvas | Windows Ink cursor jumps to (Width-1, Height-1) | Cursor position inspect is screen maximum bounds |
| **22** | **Tablet Release** | Lift finger from tablet canvas | Pen tip switch and in-range flags drop to 0 | Paint stroke ends cleanly; no trailing line or stuck contact |
| **23** | **Forced BLE Disconnect**| Toggle Airplane Mode on iPhone while holding Key 'Z' and Left Mouse Button | Bridge detects link loss within 250ms, triggers watchdog, and sends neutral HID reports | Key 'Z' and Mouse Click do not repeat infinitely on PC |
| **24** | **Verify No Stuck Input**| Inspect PC input state immediately after Step 23 | Zero keys held, zero mouse buttons held, sticks centered | Typing in Notepad confirms no stuck modifiers (Ctrl/Alt/Win) |

---

## 4. Test Log Template

```markdown
| Step # | Test Name | Expected | Actual | Host OS | Latency (ms) | Result (PASS/FAIL) | Host Evidence Reference |
|---|---|---|---|---|---|---|---|
| 1 | Bridge Power-on | Enumerates 5 HID devices | 5 HID devices enumerated | Win 11 | - | PASS | Device Manager screenshot |
| 2 | BLE Advertisement | Advertises UUID 128-bit | Detected in 450ms | iOS 17 | - | PASS | CoreBluetooth scan log |
| ... | ... | ... | ... | ... | ... | ... | ... |
| 23 | Forced Disconnect | Watchdog zero within 250ms | Neutral report sent in 80ms | Win 11 | 80 | PASS | USB trace / no repeating keys |
| 24 | Stuck Input Check | Zero stuck modifiers | All keys/buttons clear | Win 11 | - | PASS | Notepad keystroke test clean |
```

---

## 5. Acceptance Thresholds for Prototype Certification

1. **Pass Rate:** 24 / 24 steps (100%) must pass on a physical target PC.
2. **Safety Integrity:** Zero stuck keys or drifting axes during forced disconnects (Steps 23–24). Any stuck input constitutes an immediate prototype failure.
3. **Latency Profile:**
   - Round-trip input latency (touch on iPhone screen to USB HID packet arrival on PC) must average `< 15 ms` for trackpad and tablet drawing.
4. **Driverless Standard:** Windows Device Manager must bind `hidusb.sys`, `kbdclass.sys`, and `mouclass.sys` with zero third-party software installed.
