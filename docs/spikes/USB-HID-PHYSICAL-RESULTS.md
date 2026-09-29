# USB HID Physical Results
**PocketHID Wave 9R.1 — PATH B Closeout**

---

## Test Environment

> Fill in once hardware is connected. ALL fields required before any test result is recorded.

```
Hardware        :
Board revision  :
Serial port     :
USB VID         :
USB PID         :
Firmware file   : merged-binary.bin
Firmware SHA    : D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
Git commit      : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a
ESP-IDF version : v5.3.1
Host OS         :
Host OS build   :
esptool version :
Test date/time  :
```

---

## Status Summary

| Test | Status |
|---|---|
| Board connected | NOT CONNECTED |
| Firmware flashed | PENDING |
| Boot verified | PENDING |
| USB enumerated (Windows) | PENDING |
| Keyboard A | PENDING |
| Shift+A | PENDING |
| Ctrl+C | PENDING |
| Win+D | PENDING |
| Mouse movement | PENDING |
| Left click | PENDING |
| Scroll | PENDING |
| Volume+ | PENDING |
| Volume- | PENDING |
| Gamepad A | PENDING |
| Gamepad analog axis | PENDING |
| Tablet top-left | PENDING |
| Tablet center | PENDING |
| Tablet bottom-right | PENDING |
| Tablet release | PENDING |
| Safety — watchdog neutralization | PENDING |
| Safety — BLE disconnect neutralization | PENDING |
| Safety — USB replug clean startup | PENDING |

---

## Phase 1 — Flash

```
Hardware        :
Firmware        : merged-binary.bin  (SHA-256 pre-check: PASS / FAIL)
Host OS         :
Port            :
Erase used      : YES / NO
Command         :
Expected        : esptool exits 0, "Hash of data verified." shown
Actual          :
Evidence        : [paste full esptool output]
Result          : PENDING
```

---

## Phase 2 — Boot Verification

> Run: `.\tools\monitor.ps1 -Port COMx`
> Paste complete serial output verbatim below.

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : All 6 markers appear within 5 seconds of boot
Actual boot log :
  [paste verbatim serial output here]
Evidence        : Serial log file attached / pasted above
Result          : PENDING
```

### Boot Marker Checklist

| # | Marker | Found | Timestamp |
|---|---|---|---|
| 1 | `Booting PocketHID` | [ ] | |
| 2 | `NVS initialized` | [ ] | |
| 3 | `BLE stack initialized` | [ ] | |
| 4 | `USB stack initialized` | [ ] | |
| 5 | `Safety manager initialized` | [ ] | |
| 6 | `Ready.` | [ ] | |

> ⚠️ `USB stack initialized` ≠ `Windows HID enumerated`. These are separate events.

**Boot Verification: PENDING**

---

## Phase 3 — Windows USB Enumeration

> Run: `Get-PnpDevice -Class HIDClass | Select-Object Name, InstanceId, Status | Format-List`
> Also open: `devmgmt.msc` → Human Interface Devices

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : 5 HID collections: keyboard, mouse, consumer, gamepad, tablet
Actual output   :
  [paste Get-PnpDevice output here]
Evidence        : Device Manager screenshot + PowerShell output
Result          : PENDING
```

### Enumeration Checklist

| Collection | Expected name | Actual name | VID | PID | Status |
|---|---|---|---|---|---|
| Keyboard | HID Keyboard Device | | | | PENDING |
| Mouse | HID-compliant mouse | | | | PENDING |
| Consumer | HID-compliant consumer control device | | | | PENDING |
| Gamepad | HID-compliant game controller | | | | PENDING |
| Tablet | HID-compliant pen | | | | PENDING |
| Error/unknown devices | None | | | | PENDING |

**USB Enumeration: PENDING**

---

## Phase 4 — USB Input Tests (POC TEST MODE)

> Evidence MUST be host-side observable effect.
> "ESP32 function called" is NOT evidence.
> "USB stack reported send" is NOT evidence.
> Valid evidence: key appeared in text editor / cursor moved on screen / OSD appeared / gamepad tester registered button.

---

### Test 01 — Keyboard A

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Letter 'a' typed in open text field
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 02 — Shift+A

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Letter 'A' (uppercase) typed in open text field
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 03 — Ctrl+C

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Clipboard copy executed (paste confirms text copied)
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 04 — Win+D

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Windows desktop toggled (all windows minimized / restored)
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 05 — Mouse Movement

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Mouse cursor moves visibly on screen
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 06 — Left Click

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Click registered (button selection / focus change visible)
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 07 — Scroll

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Page content scrolls up or down in focused window
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 08 — Volume+

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : System volume increases — OSD / volume indicator visible on screen
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 09 — Volume-

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : System volume decreases — OSD / volume indicator visible on screen
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 10 — Gamepad A

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Test tool       : gamepad-tester.com or joy.cpl
Expected        : Button A lit/registered in gamepad tester
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 11 — Gamepad Analog Axis

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Test tool       : gamepad-tester.com or joy.cpl
Expected        : Joystick axis moves in gamepad tester
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 12 — Tablet Top-Left

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Test tool       : Windows Ink Workspace / mspaint tablet mode
Expected        : Pen cursor at top-left of digitizer area
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 13 — Tablet Center

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Pen cursor at center of digitizer area
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 14 — Tablet Bottom-Right

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Pen cursor at bottom-right of digitizer area
Actual          :
Evidence        :
Result          : PENDING
```

---

### Test 15 — Tablet Release

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Pen contact released (cursor in hover state, no pressure)
Actual          :
Evidence        :
Result          : PENDING
```

---

## Phase 5 — Safety / Neutralization Tests

### Safety Test A — Watchdog Timeout Neutralization

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Trigger         : Establish active keyboard input state via BLE, then stop packet stream
Expected        : All inputs neutralized within watchdog timeout (spec ≤250 ms)
Actual          :
Measured time   :   (record actual measured value — do not claim spec value)
Evidence        : Serial log showing neutralization + host confirms no stuck keys
Result          : PENDING
```

---

### Safety Test B — BLE Disconnect Neutralization

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Trigger         : Force BLE client disconnect while input state is active
Expected        : Disconnect callback fires; all inputs neutralized immediately
Actual          :
Measured time   :
Evidence        : Serial log + host confirms no stuck keys
Result          : PENDING
```

---

### Safety Test C — USB Replug Clean Startup

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Trigger         : Unplug USB while board is running; reconnect
Expected        : Clean re-enumeration, zero spurious HID reports at startup
Actual          :
Evidence        : Serial log + no ghost inputs observed on host
Result          : PENDING
```

---

## Evidence File Checklist

| Evidence | Status |
|---|---|
| Boot serial log | NOT COLLECTED |
| Device Manager screenshot | NOT COLLECTED |
| Input test video/recording | NOT COLLECTED |
| Gamepad tester screenshot | NOT COLLECTED |
| Safety test serial log | NOT COLLECTED |

---

## Wave 9R.1 Final Status

```
PHYSICAL ESP32-S3 : NOT CONNECTED
FIRMWARE FLASHED  : PENDING
BOOT VERIFIED     : PENDING
USB ENUMERATED    : PENDING
KEYBOARD          : PENDING
MOUSE             : PENDING
CONSUMER CONTROL  : PENDING
GAMEPAD           : PENDING
TABLET            : PENDING
SAFETY            : PENDING
```

---

*Last updated: Wave 9R.1 — 2026-09-29*
