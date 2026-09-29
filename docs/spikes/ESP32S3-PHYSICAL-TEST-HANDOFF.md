# ESP32-S3 Physical Test Handoff
**PocketHID Wave 9R.1 — PATH B Closeout**

---

## Current Firmware

| Field | Value |
|---|---|
| Git commit | `160fddfa6763989cebe0aca67efe2d2e1dbdc65a` |
| Project name | `pockethid-esp32s3-bridge` |
| ESP-IDF version | **v5.3.1** |
| Compiler | `xtensa-esp32s3-elf-gcc` (esp-13.2.0_20240530) |
| Build wave | Wave 8 (cross-compiled) |
| Build date | 2026-09-29 09:01 UTC+7 |
| Flash mode | DIO |
| Flash frequency | 80 MHz |
| Flash size | 2 MB |

### Firmware SHA-256 Hashes

| File | SHA-256 |
|---|---|
| `merged-binary.bin` (use this) | `D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD` |
| `pockethid-esp32s3-bridge.bin` | `5D06784D36942B760AB0318271E51DA82B8DED8A742FDC1790AC4DFEB72A05E3` |
| `bootloader/bootloader.bin` | `D8016C8FD0D219CA09708B5D38E8DA64DE6A24FEBE6BE63FC83EFCD54F194E9D` |
| `partition_table/partition-table.bin` | `7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820` |

---

## Required Hardware

| Item | Notes |
|---|---|
| ESP32-S3 development board | e.g., ESP32-S3-DevKitC-1, Unexpected Maker FeatherS3, Adafruit QT Py ESP32-S3 |
| USB cable | **MUST be a data cable** — charge-only cables will not work |
| Windows PC | Windows 10 or 11 — needed for USB HID Device Manager verification |
| BLE test client | nRF Connect for Desktop or Python + `bleak` library |

> ⚠️ **Important for this machine:**
> COM6 and COM7 are Bluetooth SPP links (not ESP32).
> `flash.ps1` will explicitly reject these ports.
> You must use the actual USB serial port assigned to the ESP32-S3.

---

## Before First Flash

### Step 1 — Verify COM port

```powershell
# List USB serial ports
Get-PnpDevice -Class Ports | Where-Object {$_.Name -match 'USB'} | Select-Object Name, InstanceId

# Find ESP32-specific VID/PID (any of these indicate an ESP32-compatible board)
Get-PnpDevice | Where-Object {$_.InstanceId -match 'VID_303A|VID_10C4|VID_1A86|VID_0403'} | `
    Select-Object Name, InstanceId, Status | Format-List
```

> Known VID mappings:
> - `VID_303A` — Espressif native USB (ESP32-S3 with no external UART chip)
> - `VID_10C4` — Silicon Labs CP2102
> - `VID_1A86` — WCH CH340
> - `VID_0403` — FTDI

### Step 2 — Record board identity (before touching firmware)

```
Board model     :
Serial port     :
USB VID         :
USB PID         :
Description     :
Driver          :
```

### Step 3 — Verify firmware hash on disk

```powershell
cd d:\desktop\PocketHID\firmware\esp32s3-bridge

(Get-FileHash ".\build\merged-binary.bin" -Algorithm SHA256).Hash
# Must be: D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
```

Do NOT flash if hash does not match. Rebuild from commit `160fddfa6763989cebe0aca67efe2d2e1dbdc65a`.

---

## Flash Procedure

```powershell
# From the tools directory:
cd d:\desktop\PocketHID\firmware\esp32s3-bridge\tools

# Flash (replace COMx with actual port identified above):
.\flash.ps1 -Port COMx
```

The script will:
1. Reject COM6/COM7 with an error
2. Check that esptool.py is available
3. Verify port is accessible
4. SHA-256 verify `merged-binary.bin` before flashing
5. Show exact target port and hash before writing
6. Flash `merged-binary.bin` at `0x0`
7. Report SUCCESS or FAIL with troubleshooting guidance

**Record full console output verbatim in** [FLASH-MANIFEST.md](../../firmware/esp32s3-bridge/FLASH-MANIFEST.md).

---

## Boot Verification

```powershell
# Open serial monitor immediately after flash completes:
.\monitor.ps1 -Port COMx
```

### Expected boot sequence (all 6 markers must appear in order)

```
[HH:mm:ss.fff] Booting PocketHID
[HH:mm:ss.fff] NVS initialized
[HH:mm:ss.fff] BLE stack initialized
[HH:mm:ss.fff] USB stack initialized
[HH:mm:ss.fff] Safety manager initialized
[HH:mm:ss.fff] Ready.
```

> ⚠️ `USB stack initialized` is logged by firmware.
> This is **NOT** the same as "Windows HID enumerated".
> Windows enumeration is verified separately in Device Manager.

**Save the full boot log.** Paste verbatim into [USB-HID-PHYSICAL-RESULTS.md](./USB-HID-PHYSICAL-RESULTS.md).

---

## USB HID Verification

### Step 1 — Trigger USB enumeration

Disconnect and reconnect the USB cable while the board is running (or let it enumerate fresh after flash).

### Step 2 — Check Windows Device Manager

```powershell
# PowerShell check
Get-PnpDevice -Class HIDClass | Select-Object Name, InstanceId, Status | Format-List

# GUI
devmgmt.msc
# → Human Interface Devices
# → Expand the ESP32-S3 composite device
```

### Step 3 — Verify expected HID collections

| Collection | Expected Windows name |
|---|---|
| Keyboard | HID Keyboard Device |
| Mouse | HID-compliant mouse |
| Consumer control | HID-compliant consumer control device |
| Gamepad | HID-compliant game controller |
| Tablet / pen | HID-compliant pen |

### Step 4 — Run POC TEST MODE input sequence

Run all 15 tests in order. For each, record:
- Hardware
- Firmware (SHA-256)
- Host OS
- Expected host-side effect
- Actual host-side effect
- Evidence (key typed, cursor moved, OSD shown — NOT "ESP32 reported send")
- PASS / FAIL

Details in: [USB-HID-PHYSICAL-RESULTS.md](./USB-HID-PHYSICAL-RESULTS.md)

---

## BLE Verification

> **Gate:** Run BLE verification ONLY after all USB HID tests pass.

### Setup

```python
# Install bleak: pip install bleak
# Or use nRF Connect for Desktop (https://www.nordicsemi.com/Products/Development-tools/nRF-Connect-for-Desktop)
```

### BLE test sequence

1. Scan for BLE advertisement — expect `PocketHID` device
2. Connect and discover GATT services
3. Verify RX characteristic (Write Without Response)
4. Write a golden keyboard packet — verify serial parse log
5. Verify invalid packets are rejected
6. Force disconnect — verify neutralization
7. Run full BLE → USB → Windows end-to-end path

Details in: [BLE-PHYSICAL-RESULTS.md](./BLE-PHYSICAL-RESULTS.md)

---

## Windows Verification

### Device Manager state

```powershell
# Confirm no error/warning devices
Get-PnpDevice | Where-Object {$_.Status -eq 'Error' -or $_.Status -eq 'Degraded'} | `
    Select-Object Name, InstanceId, Status, Problem | Format-List
```

### Host-side input effects

| Input | Verification method |
|---|---|
| Keyboard | Open Notepad → watch for typed character |
| Mouse | Watch cursor move / button register |
| Consumer (Volume) | Watch OSD appear on screen |
| Gamepad | Open gamepad-tester.com or joy.cpl |
| Tablet | Open Windows Ink Workspace / mspaint |

Details in: [WINDOWS-HID-ENUMERATION.md](./WINDOWS-HID-ENUMERATION.md)

---

## Safety Verification

Run after USB HID input tests pass.

| Test | Trigger | Expected outcome | Measured time |
|---|---|---|---|
| Watchdog timeout | Stop BLE packet stream with active input state | All inputs neutralized (spec ≤250 ms) | Record actual |
| BLE disconnect | Force disconnect BLE client | Immediate neutralization | Record actual |
| USB replug | Unplug and replug USB | Clean startup, zero spurious HID reports | N/A |

> ⚠️ Do not claim "250 ms verified" unless you physically measure it with timestamps.
> Record `Measured time: [actual value]` or `Measured time: NOT MEASURED`.

---

## Evidence Requirements

| Evidence type | Format | Required for |
|---|---|---|
| Flash output | Console log pasted verbatim | FLASH-MANIFEST.md |
| Boot serial log | `.log` file from `monitor.ps1` | USB-HID-PHYSICAL-RESULTS.md |
| Device Manager | Screenshot (`.png`) | WINDOWS-HID-ENUMERATION.md |
| HIDClass PowerShell | Console output pasted | WINDOWS-HID-ENUMERATION.md |
| Input test evidence | Screenshot / recording | USB-HID-PHYSICAL-RESULTS.md (each test) |
| BLE scan result | Console output / screenshot | BLE-PHYSICAL-RESULTS.md |
| GATT tree | nRF Connect screenshot | BLE-PHYSICAL-RESULTS.md |
| Safety serial log | `.log` file | USB-HID-PHYSICAL-RESULTS.md (safety tests) |

---

## Stop Conditions

**STOP immediately and do NOT continue** if any of the following occur:

| Condition | Action |
|---|---|
| COM port detected is COM6 or COM7 | Do not flash — those are Bluetooth SPP links |
| VID/PID does not match any known ESP32 VID | Identify the actual device before proceeding |
| SHA-256 mismatch on any firmware binary | Do not flash — rebuild from commit `160fddfa` |
| Boot sequence does not complete within 10 seconds | Record serial log; do not proceed to USB HID tests |
| Board resets repeatedly after flash | Record log; check USB power; do not claim "USB enumerated" |
| Windows shows `Unknown Device` or Code 10/43 | Record and investigate before proceeding |
| Any HID input occurs BEFORE `Ready.` marker | SAFETY CONCERN — record and investigate |
| Stuck keyboard key or stuck mouse button on host | SAFETY CONCERN — pull USB immediately; record |
| BLE advertisement name is not `PocketHID` | Verify firmware is correct version; do not proceed |
| Any unexpected USB device appears on host | Document and investigate — do not proceed |

---

## Final Status

```
PHYSICAL VERIFICATION:
PENDING

PHYSICAL ESP32-S3 : NOT CONNECTED
FIRMWARE FLASHED  : PENDING
BOOT VERIFIED     : PENDING
USB HID           : PENDING
WINDOWS HID       : PENDING
BLE               : PENDING
BLE → USB         : PENDING
iOS BRIDGE        : NOT IMPLEMENTED
PRODUCTION READINESS: NOT CLAIMED
```

---

## Companion Documents

| Document | Purpose |
|---|---|
| [FLASH-MANIFEST.md](../../firmware/esp32s3-bridge/FLASH-MANIFEST.md) | SHA-256, flash commands, evidence template |
| [USB-HID-PHYSICAL-RESULTS.md](./USB-HID-PHYSICAL-RESULTS.md) | All 15 input tests + 3 safety tests |
| [BLE-PHYSICAL-RESULTS.md](./BLE-PHYSICAL-RESULTS.md) | BLE GATT validation |
| [WINDOWS-HID-ENUMERATION.md](./WINDOWS-HID-ENUMERATION.md) | Device Manager guide + enumeration checklist |
| [IOS-TRANSPORT-POC-PROTOCOL.md](./IOS-TRANSPORT-POC-PROTOCOL.md) | iOS protocol spec (not yet implemented) |

---

*Last updated: Wave 9R.1 — 2026-09-29*
