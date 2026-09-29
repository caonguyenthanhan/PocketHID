# Windows HID Enumeration
**PocketHID Wave 9R.1 — PATH B Closeout**

---

## Test Environment

> Fill in once hardware is connected.

```
Hardware        :
Board revision  :
Serial port     :
Firmware file   : merged-binary.bin
Firmware SHA    : D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
Git commit      : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a
ESP-IDF version : v5.3.1
Host OS         :
Host OS build   :
Test date/time  :
```

---

## Status Summary

| Item | Status |
|---|---|
| Board connected | NOT CONNECTED |
| Device enumerated by Windows | PENDING |
| VID matches expected | PENDING |
| PID matches expected | PENDING |
| Keyboard HID collection | PENDING |
| Mouse HID collection | PENDING |
| Consumer control collection | PENDING |
| Gamepad collection | PENDING |
| Tablet / pen collection | PENDING |
| No warning / error flags | PENDING |

---

## Phase 1 — Pre-Enumeration: Board Identification

> Run BEFORE powering on USB HID mode. Record everything.

```
Hardware        :
Host OS         :
Expected        : ESP32-S3 board visible in PnP device list with known VID/PID
Commands run    :
```

```powershell
# Step 1: List USB serial ports
Get-PnpDevice -Class Ports | Where-Object {$_.Name -match 'USB'} | Select-Object Name, InstanceId

# Step 2: Find ESP32-specific VID/PID
Get-PnpDevice | Where-Object {$_.InstanceId -match 'VID_303A|VID_10C4|VID_1A86|VID_0403'} | `
    Select-Object Name, InstanceId, Status | Format-List

# Step 3: Full USB device list
Get-PnpDevice -Class USB | Select-Object Name, InstanceId | Sort-Object Name | Format-List
```

```
Board ID evidence:
  VID      :
  PID      :
  COM port :
  Name     :
  Driver   :
Result          : PENDING
```

---

## Phase 2 — Post-Boot HID Enumeration

> Run AFTER firmware boots (after `Ready.` appears in serial monitor).
> Run AFTER clean USB reconnect to trigger HID enumeration.

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : 5 HID collections visible under HIDClass
```

```powershell
# Primary enumeration check
Get-PnpDevice -Class HIDClass | Select-Object Name, InstanceId, Status | Format-List

# Targeted ESP32-S3 native USB
Get-PnpDevice | Where-Object {$_.InstanceId -match 'VID_303A'} | `
    Select-Object Name, InstanceId, Status | Format-List

# Check for any error state devices
Get-PnpDevice | Where-Object {$_.Status -eq 'Error' -or $_.Status -eq 'Degraded'} | `
    Select-Object Name, InstanceId, Status, Problem | Format-List

# Device Manager GUI
# devmgmt.msc → View → Devices by connection → expand USB composite device
```

```
Actual enumeration output:
  [paste Get-PnpDevice output here]

Evidence        : [Device Manager screenshot path / paste]
Result          : PENDING
```

### Enumeration Checklist

| Collection | Expected Windows Name | Actual Name | VID | PID | InstanceId | Error flags | Status |
|---|---|---|---|---|---|---|---|
| Keyboard | HID Keyboard Device | | | | | | PENDING |
| Mouse | HID-compliant mouse | | | | | | PENDING |
| Consumer | HID-compliant consumer control device | | | | | | PENDING |
| Gamepad | HID-compliant game controller | | | | | | PENDING |
| Tablet | HID-compliant pen | | | | | | PENDING |
| Unexpected devices | None expected | | | | | | PENDING |

> ⚠️ Record ACTUAL Windows names. Do not assume they match expected names.

**Windows HID Enumeration: PENDING**

---

## Phase 3 — HID Descriptor Inspection (if enumeration fails)

> Only needed if collections are missing or show Code 10/43 errors.

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Tool            : tools/verify_descriptor.py
Expected        : No descriptor errors reported
Actual          :
Evidence        :
Result          : NOT TESTED
```

```powershell
# Run descriptor verifier from firmware tools directory
cd d:\desktop\PocketHID\firmware\esp32s3-bridge\tools
python verify_descriptor.py
```

---

## Phase 4 — Error / Warning Devices

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Expected        : Zero devices with Status = Error or Degraded
```

```powershell
Get-PnpDevice | Where-Object {$_.Status -eq 'Error' -or $_.Status -eq 'Degraded'} | `
    Select-Object Name, InstanceId, Status, Problem | Format-List
```

```
Actual          :
Evidence        :
Result          : PENDING
```

---

## Known Issues & Fixes

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Unknown Device` in Device Manager | Descriptor error or missing driver | Run `verify_descriptor.py`; check HID descriptor |
| Only 1-2 HID collections appear | Descriptor parse failure | Inspect descriptor with USB Device Tree Viewer |
| Device enumerates then disappears | Power issue or firmware crash | Check serial monitor; reduce USB draw |
| `Code 43` | Driver rejection | Try WinUSB fallback via Zadig |
| `Code 10` | HID descriptor invalid | Fix descriptor bytes; re-flash |
| Multiple identical keyboard entries | Duplicate report IDs | Fix report descriptor in firmware |

---

## WinUSB Fallback (last resort debugging only)

> ⚠️ WinUSB removes normal HID integration. Windows will NOT see keyboard/mouse/gamepad.
> Use only as debugging fallback to inspect raw descriptors.

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Tool            : Zadig (https://zadig.akeo.ie)
Trigger         : Native HID driver completely fails to bind
Action          : Replace driver with WinUSB via Zadig
Expected        : Raw device accessible via libusb/pyusb for descriptor inspection
Result          : NOT TESTED
```

---

## Evidence File Checklist

| Evidence | Status |
|---|---|
| Board ID (VID/PID/COM) | NOT COLLECTED |
| Device Manager screenshot (post-boot) | NOT COLLECTED |
| PowerShell HIDClass output | NOT COLLECTED |
| Descriptor verifier output | NOT COLLECTED |

---

## Wave 9R.1 Final Status

```
WINDOWS HID ENUMERATION : PENDING
KEYBOARD COLLECTION     : PENDING
MOUSE COLLECTION        : PENDING
CONSUMER COLLECTION     : PENDING
GAMEPAD COLLECTION      : PENDING
TABLET COLLECTION       : PENDING
ERROR FLAGS             : PENDING
```

---

*Last updated: Wave 9R.1 — 2026-09-29*
