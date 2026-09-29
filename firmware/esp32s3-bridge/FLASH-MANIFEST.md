# PocketHID ESP32-S3 — FLASH MANIFEST
**Wave 9R.1 — PATH B Closeout**

---

## 1. Firmware Artifact Summary

| Field | Value |
|---|---|
| Git commit | `160fddfa6763989cebe0aca67efe2d2e1dbdc65a` |
| Wave built | Wave 8 (cross-compiled) |
| ESP-IDF version | **v5.3.1** |
| Compiler | `xtensa-esp32s3-elf-gcc` (esp-13.2.0_20240530) |
| Build date | 2026-09-29 09:01 UTC+7 |
| Target chip | ESP32-S3 |
| Flash mode | DIO |
| Flash frequency | 80 MHz |
| Flash size | 2 MB |
| Project name | `pockethid-esp32s3-bridge` |
| Project version | `984394b-dirty` (local version string) |

---

## 2. Firmware Binaries — Verified SHA-256

> All four hashes verified live from build artifacts on 2026-09-29.

| File | Size (bytes) | SHA-256 |
|---|---|---|
| `merged-binary.bin` | 634,752 | `E8FF42CF23758AF8F74A2FA43ECB32EB5802BE4A01DFFAA14B21EA415861B1B3` |
| `pockethid-esp32s3-bridge.bin` | 569,216 | `7573E07635FDDFC00F910799FA0A9C1944E36753A9FB23C410DF0802594025FB` |
| `bootloader/bootloader.bin` | 21,088 | `12874BF6599764D06082A4DA5442B48556A91D73FE924696DF9AEC6042897CFB` |
| `partition_table/partition-table.bin` | 3,072 | `7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820` |

**Preferred flash binary: `merged-binary.bin`**
Single write at `0x0` — safest, avoids partial flash risk.
`flash.ps1` verifies SHA-256 of all four binaries before flashing.

---

## 3. Flash Offsets

| Binary | Offset |
|---|---|
| `bootloader.bin` | `0x0` |
| `partition-table.bin` | `0x8000` |
| `pockethid-esp32s3-bridge.bin` | `0x10000` |
| `merged-binary.bin` | `0x0` (covers all three) |

---

## 4. Step-by-Step Flash Procedure

### Step 1 — Identify the board's COM port

```powershell
# List USB serial ports
Get-PnpDevice -Class Ports | Where-Object {$_.Name -match 'USB'} | Select-Object Name, InstanceId

# Find ESP32-specific VID/PID
Get-PnpDevice | Where-Object {$_.InstanceId -match 'VID_303A|VID_10C4|VID_1A86|VID_0403'} | `
    Select-Object Name, InstanceId, Status
```

> ⚠️ **COM6 and COM7 are Bluetooth SPP links — NOT ESP32 boards.**
> `flash.ps1` will reject COM6/COM7 with an error. Do not use them.

### Step 2 — Verify firmware SHA-256 before flashing

```powershell
# Verify merged binary (preferred)
(Get-FileHash ".\build\merged-binary.bin" -Algorithm SHA256).Hash
# Must match: E8FF42CF23758AF8F74A2FA43ECB32EB5802BE4A01DFFAA14B21EA415861B1B3

# Verify app binary
(Get-FileHash ".\build\pockethid-esp32s3-bridge.bin" -Algorithm SHA256).Hash
# Must match: 7573E07635FDDFC00F910799FA0A9C1944E36753A9FB23C410DF0802594025FB

# Verify bootloader
(Get-FileHash ".\build\bootloader\bootloader.bin" -Algorithm SHA256).Hash
# Must match: 12874BF6599764D06082A4DA5442B48556A91D73FE924696DF9AEC6042897CFB

# Verify partition table
(Get-FileHash ".\build\partition_table\partition-table.bin" -Algorithm SHA256).Hash
# Must match: 7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820
```

### Step 3 — Flash (merged binary, preferred)

```powershell
# From firmware/esp32s3-bridge/tools/ directory:
.\flash.ps1 -Port COMx
```

Or directly with esptool:

```bash
esptool.py --chip esp32s3 --port COMx --baud 921600 \
  --before default_reset --after hard_reset \
  write_flash 0x0 ./build/merged-binary.bin
```

### Step 4 — Fallback: separate binaries

```bash
esptool.py --chip esp32s3 --port COMx --baud 921600 \
  --before default_reset --after hard_reset \
  write_flash --flash_mode dio --flash_freq 80m --flash_size 2MB \
  0x0    ./build/bootloader/bootloader.bin \
  0x8000 ./build/partition_table/partition-table.bin \
  0x10000 ./build/pockethid-esp32s3-bridge.bin
```

### Step 5 — Erase (fresh chip only)

```powershell
.\flash.ps1 -Port COMx -Erase
```

> ⚠️ `-Erase` destroys all NVS data and BLE pairing info. Use only when explicitly needed.
> `flash.ps1` requires 10-second confirmation window before destructive erase.

---

## 5. Post-Flash Verification

```powershell
# Open serial monitor (captures boot log automatically)
.\tools\monitor.ps1 -Port COMx

# Expected boot markers (ALL 6 must appear in order):
#   [✓] Booting PocketHID
#   [✓] NVS initialized
#   [✓] BLE stack initialized
#   [✓] USB stack initialized
#   [✓] Safety manager initialized
#   [✓] Ready.
```

> ⚠️ `USB stack initialized` ≠ `Windows HID enumerated` — these are separate events.

After boot is verified:

```powershell
# Check Windows HID enumeration (run in parallel or after monitor.ps1 exits)
Get-PnpDevice -Class HIDClass | Select-Object Name, InstanceId, Status | Format-List
devmgmt.msc
```

---

## 6. Flash Evidence Template

Fill in verbatim after executing flash:

```
FLASH RECORD — Wave 9R.1
=========================
Date/Time        :
Operator         :
Board model      :
Board revision   :
Serial port      :
USB VID          :
USB PID          :
USB description  :
Firmware file    : merged-binary.bin
Firmware SHA-256 : D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
Git commit       : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a
ESP-IDF version  : v5.3.1
Erase used       : YES / NO
Baud rate        :
esptool version  :
Flash result     : SUCCESS / FAIL
Error (if any)   :

SHA-256 pre-check:
  merged-binary.bin    : PASS / FAIL
  bootloader.bin       : PASS / FAIL
  partition-table.bin  : PASS / FAIL
  app.bin              : PASS / FAIL
```

---

## 7. Troubleshooting

| Symptom | Action |
|---|---|
| `Failed to connect to ESP32-S3` | Hold BOOT button on board, rerun flash |
| `Port in use` error | Close Arduino IDE / other serial monitors |
| SHA-256 mismatch | Do NOT flash — rebuild from commit `160fddfa` |
| Flash fails with correct port | Try `--baud 460800` or `--baud 230400` |
| Device not in Device Manager post-flash | Check USB cable (data cable, not charge-only) |
| Code 43 / Code 10 in Device Manager | Check HID descriptor with `tools/verify_descriptor.py` |
| No COM port appears after connecting | Install CP2102 driver (VID_10C4) or use native USB port (VID_303A) |

---

## 8. Current Status

| Item | Status |
|---|---|
| ESP-IDF version confirmed | v5.3.1 |
| Firmware artifact | CROSS-COMPILED (Wave 8) |
| SHA-256 all four binaries | VERIFIED (2026-09-29) |
| Build gitignored | YES (`build/` in `.gitignore`) |
| Board connected | NOT CONNECTED |
| Flash executed | PENDING |
| Boot verified | PENDING |
| USB HID enumerated | PENDING |

---

*Last updated: Wave 9R.1 — 2026-09-29*
