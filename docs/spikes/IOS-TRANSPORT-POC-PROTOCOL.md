# iOS → PC Transport Physical Proof-of-Concept Protocol (Spike Verification)

**Document ID:** `POC-PROTO-001-IOS-TRANSPORT`  
**Related ADR:** [`ADR-004-ios-pc-transport-research.md`](file:///d:/desktop/PocketHID/docs/adr/ADR-004-ios-pc-transport-research.md)  
**Status:** DRAFT SPECIFICATION — MANDATORY GATE FOR ANY FUTURE TRANSPORT IMPLEMENTATION  

---

## 1. Purpose & Principles

This document defines the strict, non-negotiable physical verification protocol required before any candidate transport backend (Option B: Custom BLE GATT, Option C: Local Wi-Fi LAN, Option D: USB Tunnel, or Option E: External Hardware HID Bridge) can be accepted into the PocketHID iOS codebase.

### Strict Verification Rules:
1. **Host-Side Verification Only:** An action is NEVER verified merely because an iOS method was called or an event log was emitted (`code path executed != verified`). Verification requires observable, physical host OS-level side effects (e.g., character input, OS keystroke reception, cursor displacement, window state toggle).
2. **Zero Fake State:** Mock, simulated, or synthetic loopback responses do not satisfy any verification step.
3. **Reproducibility:** Every test record must detail the exact physical hardware, host operating system version, and evidence capture mechanism.

---

## 2. Test Environment Specification

Every physical trial must log the following environmental metadata before commencing the test sequence:

- **Mobile Device:** Physical iPhone Model (e.g., iPhone 13 Pro / iPhone 15 Pro)
- **iOS Version:** Exact iOS build (e.g., iOS 17.4.1)
- **Host PC Hardware:** Physical PC / Mac (CPU, RAM, Bluetooth/Wi-Fi adapter chipset)
- **Host Operating System:** Exact OS and build (e.g., Windows 11 Pro 23H2 / macOS Sonoma 14.4 / Ubuntu 22.04 LTS)
- **Transport Under Test:** Candidate Option (Option B, Option C, Option D, or Option E)
- **Companion / Receiver Version (if applicable):** Version and hash of any host-side utility or microcontroller firmware
- **Radio / Network Environment:** 2.4 GHz vs 5 GHz Wi-Fi, channel interference level, distance between devices (1m standard)

---

## 3. Standard 20-Step Verification Sequence

The test runner must execute all 20 steps sequentially. Any failure halts the candidate evaluation until resolved.

| Step # | Test Name | Injected Action | Expected Host Behavior | Validation Verification Method |
|---|---|---|---|---|
| **1** | **Connect** | Trigger connection handshake from iPhone | Host establishes link; connection state transitions to connected within 3.0s | OS event log / network socket / BLE peripheral connection state |
| **2** | **Disconnect** | Terminate connection from iPhone UI / background kill | Host detects link termination; cleans up resources without hanging or crashing | Host event log / socket closed event |
| **3** | **Reconnect** | Re-initiate connection from iPhone UI | Host gracefully accepts re-association within 2.0s without requiring host reboot | Immediate data exchange restored |
| **4** | **Keyboard A** | Press & release 'a' key in Keyboard Mode | Lowercase letter 'a' appended to active text editor (Notepad / TextEdit) | Text editor character buffer inspection |
| **5** | **Shift+A** | Hold Shift modifier, press 'a', release both | Uppercase letter 'A' appended to active text editor | Text editor character buffer inspection |
| **6** | **Ctrl+C** | Select text, trigger Ctrl+C / Cmd+C | Host system clipboard populated with selected text | Clipboard buffer inspection (`Get-Clipboard` or paste) |
| **7** | **Win+D** | Trigger Win+D / Cmd+F3 | Host desktop toggled (all active windows minimized/restored) | Host window manager state inspection |
| **8** | **Mouse Move** | Swipe right on trackpad (+100 delta X) | Host cursor moves precisely to the right by proportional delta | Cursor coordinate polling (`GetCursorPos`) |
| **9** | **Left Click** | Tap trackpad surface / press left button | Host triggers primary click event (activates UI button / selects text) | Mouse event listener / OS UI state |
| **10** | **Scroll** | Two-finger vertical drag (+120 wheel delta) | Active document/webpage scrolls vertically downward/upward | Document scroll offset change |
| **11** | **Volume +** | Tap Volume Up consumer control | Host system audio master volume increments by 1 step | OS system audio indicator overlay visible |
| **12** | **Volume -** | Tap Volume Down consumer control | Host system audio master volume decrements by 1 step | OS system audio indicator overlay visible |
| **13** | **Presenter NEXT** | Tap NEXT in Presenter Mode | Active presentation advances to next slide (PowerPoint / Keynote) | Presentation slide index increments |
| **14** | **Presenter PREV** | Tap PREV in Presenter Mode | Active presentation returns to previous slide | Presentation slide index decrements |
| **15** | **Gamepad A** | Press Gamepad A button | Host gamepad API detects Button 0 / Button A pressed | `joy.cpl` (Windows Game Controllers) or HTML5 Gamepad Tester |
| **16** | **Gamepad Analog** | Deflect Left Stick to (X: 32767, Y: 0) | Host gamepad API detects X-axis at +100% full scale | `joy.cpl` axis deflection crosshair at extreme right |
| **17** | **Tablet Top-Left** | Touch Top-Left corner of Tablet canvas | Host cursor jumps directly to absolute display coordinates (0, 0) | Cursor coordinate inspection (0, 0) |
| **18** | **Tablet Center** | Touch exact Center of Tablet canvas | Host cursor jumps directly to absolute display coordinates (Width/2, Height/2) | Cursor coordinate inspection (`GetSystemMetrics`) |
| **19** | **Tablet Bottom-Right** | Touch Bottom-Right corner of Tablet canvas | Host cursor jumps directly to absolute display coordinates (Width-1, Height-1) | Cursor coordinate inspection (Max X, Max Y) |
| **20** | **Neutral Packet** | Lift all fingers / release all inputs | All modifier states, pressed keys, mouse buttons, and stick axes reset to 0 | Zero stuck keys in OS; `joy.cpl` sticks centered |

---

## 4. Evidence Recording Sheet Template

For every physical run, a standardized verification log must be recorded matching this schema:

```markdown
### Verification Trial Record

- **Date / Timestamp:** YYYY-MM-DD HH:MM:SS UTC
- **Evaluator:** [Name / GitHub Handle]
- **Transport Evaluated:** [Option B / Option C / Option D / Option E]
- **Hardware Setup:**
  - Phone: [e.g., iPhone 15 Pro, iOS 17.4.1]
  - Host: [e.g., Dell XPS 15, Windows 11 Pro 23H2 (Build 22631.3296)]
  - Bridge Hardware (if Option E): [e.g., ESP32-S3-DevKitC-1, TinyUSB CDC-BLE firmware v0.1]
- **Network / Radio Environment:** [e.g., 5GHz Wi-Fi 802.11ax, RSSI -45 dBm, 1.2m line-of-sight]

#### Execution Log:

| Step # | Test Name | Expected | Actual | Host OS | Latency (ms) | Result (PASS/FAIL) | Evidence Reference |
|---|---|---|---|---|---|---|---|
| 1 | Connect | Connected in <3s | Connected in 850ms | Win 11 | 850 | PASS | Video log / socket connect trace |
| 2 | Disconnect | Clean disconnect | Socket closed cleanly | Win 11 | 12 | PASS | Host socket log |
| 3 | Reconnect | Connected in <2s | Connected in 620ms | Win 11 | 620 | PASS | Socket connect trace |
| 4 | Keyboard A | 'a' in Notepad | 'a' received | Win 11 | 4.2 | PASS | Screenshot: notepad_a.png |
| 5 | Shift+A | 'A' in Notepad | 'A' received | Win 11 | 3.8 | PASS | Screenshot: notepad_shift_a.png |
| 6 | Ctrl+C | Copy to clipboard | Clipboard updated | Win 11 | 4.1 | PASS | Clip buffer: 'Test String' |
| 7 | Win+D | Toggle desktop | Windows minimized | Win 11 | 5.5 | PASS | Video timestamp 00:14 |
| 8 | Mouse Move | Cursor moves +100px | Cursor X += 100 | Win 11 | 2.1 | PASS | GetCursorPos log |
| 9 | Left Click | Button activated | Click registered | Win 11 | 2.3 | PASS | UI button click event |
| 10 | Scroll | Vertical scroll | Scroll delta 120 | Win 11 | 3.0 | PASS | Web browser scroll trace |
| 11 | Volume + | Volume +2% | Volume 44% -> 46% | Win 11 | 4.8 | PASS | Audio OSD log |
| 12 | Volume - | Volume -2% | Volume 46% -> 44% | Win 11 | 4.6 | PASS | Audio OSD log |
| 13 | Presenter NEXT | Slide N -> N+1 | Slide 1 -> 2 | Win 11 | 3.9 | PASS | PowerPoint slide event |
| 14 | Presenter PREV | Slide N -> N-1 | Slide 2 -> 1 | Win 11 | 4.0 | PASS | PowerPoint slide event |
| 15 | Gamepad A | Button 0 active | joy.cpl Button 0 lit | Win 11 | 3.2 | PASS | Screenshot: joy_cpl_btn0.png |
| 16 | Gamepad Analog | Stick X +100% | joy.cpl crosshair max | Win 11 | 3.4 | PASS | Screenshot: joy_cpl_axis.png |
| 17 | Tablet Top-Left | Point (0, 0) | Cursor at (0, 0) | Win 11 | 2.9 | PASS | Screenshot: paint_top_left.png |
| 18 | Tablet Center | Point (960, 540) | Cursor at (960, 540) | Win 11 | 3.1 | PASS | Screenshot: paint_center.png |
| 19 | Tablet Bottom-R | Point (1919, 1079)| Cursor at (1919, 1079)| Win 11 | 3.0 | PASS | Screenshot: paint_bottom_right.png|
| 20 | Release/Neutral| All inputs reset | Zero stuck keys | Win 11 | 1.8 | PASS | joy.cpl & OS key state clean |

- **Summary Verdict:** [PASS / CONDITIONAL / FAIL]
- **Evaluator Signature:** [Signature / Timestamp]
```

---

## 5. Acceptance Thresholds

For any candidate transport spike to be certified for production merging:
1. **Pass Rate:** 20 / 20 steps (100%) must pass on a physical target PC.
2. **Jitter & Latency:**
   - Round-trip input latency must remain below **15 ms** for trackpad and tablet absolute drawing.
   - Packet loss on local network/link must remain below **0.1%**.
3. **Safety / Stuck State Immunity:**
   - Release packet verification (Step 20) must be 100% robust. Zero tolerance for stuck modifier keys (Ctrl/Alt/Win stuck down) or drifting analog sticks upon connection loss.
4. **No Unapproved Dependencies:**
   - If Option B or C: The PC-side listener must run in user space without requiring kernel-mode driver installation (`.sys` files, test signing, or reboot).
   - If Option E: The hardware bridge must be recognized natively as standard USB HID across Windows, macOS, and Linux without vendor drivers.
