# ESP32-S3 Firmware Build & Cross-Compilation Record

**Document ID:** `GATE-005-ESP32S3-CROSS-BUILD`
**Status:** ESP-IDF TOOLCHAIN BOOTSTRAP COMPLETE — REAL CROSS-COMPILE PASS — ARTIFACTS VERIFIED

---

## 1. Environment & Baseline

- **Host Operating System:** Windows 10 Home Single Language (Version 22H2, Build 19045.6456)
- **Host Shell:** PowerShell 5.1
- **Host Compiler (Unit Tests):** GCC 16.1.0 (MSYS2 Project, Rev5)
- **Host Python:** Python 3.11.9
- **Git Version:** Git 2.45.1.windows.1

---

## 2. Selected ESP-IDF Version & Lifecycle Research

### Supported Versions Review
Official Espressif documentation and release lifecycles confirm:
- **ESP-IDF v5.1:** End of Life (EOL).
- **ESP-IDF v5.2:** Reached End of Maintenance / EOL in August 2026. Not recommended for new setups.
- **ESP-IDF v5.3:** Actively supported stable release series (EOL: January 2027). Native support for ESP32-S3, USB OTG TinyUSB, and Apache NimBLE.
- **ESP-IDF v5.4:** Current active release series.

### Why ESP-IDF v5.3.1
1. **Active Official Support:** v5.3 is the newest mature supported LTS-like release branch compatible with the project source.
2. **First-Class ESP32-S3 USB OTG:** Component-managed `espressif/esp_tinyusb` (v1.7.6) and TinyUSB stack (v0.21.0) provide reliable multi-report composite HID device support.
3. **NimBLE BLE Host:** Complete vendor GATT server support with zero standard HID (`0x1812`) advertisement conflicts.

---

## 3. Installation Method

The installation was performed via the official **Espressif Installation Manager CLI (`eim`)**:
- Tool: `eim-cli-windows-x64.exe` (v0.19.0) installed via WinGet (`Espressif.EIM-CLI`).
- Command:
  ```powershell
  eim install -t esp32s3 -i v5.3.1 -n true --do-not-track true -p "D:\esp"
  ```
- Target paths:
  - ESP-IDF repository: `D:\esp\v5.3.1\esp-idf`
  - Toolchain & tools: `C:\Espressif\tools`
  - Python virtual environment: `C:\Espressif\tools\python\v5.3.1\venv`
  - PowerShell activation script: `C:\Espressif\tools\Microsoft.v5.3.1.PowerShell_profile.ps1`

---

## 4. Environment Verification

All required executables and environment variables resolved cleanly:

| Tool | Resolved Version | Executable / Path |
| :--- | :--- | :--- |
| `IDF_PATH` | `v5.3.1` | `D:\esp\v5.3.1\esp-idf` |
| `IDF_TOOLS_PATH` | — | `C:\Espressif\tools` |
| `idf.py` | `ESP-IDF v5.3.1` | Invoke-idfpy via Python venv |
| `xtensa-esp32s3-elf-gcc` | `13.2.0 (esp-13.2.0_20240530)` | `C:\Espressif\tools\xtensa-esp-elf\esp-13.2.0_20240530\xtensa-esp-elf\bin\xtensa-esp32s3-elf-gcc.exe` |
| `cmake` | `3.24.0` | `C:\Espressif\tools\cmake\3.24.0\bin\cmake.exe` |
| `ninja` | `1.11.1` | `C:\Espressif\tools\ninja\1.11.1\ninja.exe` |
| `python` | `3.11.9` | `C:\Espressif\tools\python\v5.3.1\venv\Scripts\python.exe` |
| `git` | `2.45.1.windows.1` | `C:\Program Files\Git\cmd\git.exe` |

---

## 5. Project Configuration

### Component Management
Added [`main/idf_component.yml`](file:///d:/desktop/PocketHID/firmware/esp32s3-bridge/main/idf_component.yml) declaring:
```yaml
dependencies:
  espressif/esp_tinyusb: "^1.4"
  idf: "^5.3"
```
Registered component requirement `esp_tinyusb` in [`main/CMakeLists.txt`](file:///d:/desktop/PocketHID/firmware/esp32s3-bridge/main/CMakeLists.txt).

### `sdkconfig.defaults`
Configured for ESP32-S3 with TinyUSB Composite HID and NimBLE Peripheral:
```ini
CONFIG_IDF_TARGET="esp32s3"
CONFIG_TINYUSB_HID_COUNT=1
CONFIG_BT_ENABLED=y
CONFIG_BT_NIMBLE_ENABLED=y
CONFIG_BT_CONTROLLER_ONLY=n
CONFIG_BT_NIMBLE_ROLE_PERIPHERAL=y
CONFIG_BT_NIMBLE_ROLE_CENTRAL=n
CONFIG_BT_NIMBLE_ROLE_BROADCASTER=y
CONFIG_BT_NIMBLE_ROLE_OBSERVER=n
CONFIG_ESP_TASK_WDT_TIMEOUT_S=5
CONFIG_ESP_SYSTEM_EVENT_TASK_STACK_SIZE=4096
CONFIG_FREERTOS_HZ=1000
```

---

## 6. Real Cross-Compilation Sequence

Executed from [`firmware/esp32s3-bridge/`](file:///d:/desktop/PocketHID/firmware/esp32s3-bridge):

```powershell
# 1. Activate toolchain environment
. C:\Espressif\tools\Microsoft.v5.3.1.PowerShell_profile.ps1

# 2. Clean build directory
idf.py fullclean

# 3. Configure target
idf.py set-target esp32s3

# 4. Cross-compile application, bootloader, partition table
idf.py build

# 5. Generate unified flash image
idf.py merge-bin
```

---

## 7. Compile Issues Resolved

1. **TinyUSB Component Migration in IDF v5.3:**
   - *Error:* `Failed to resolve component 'tinyusb'`.
   - *Resolution:* Added `main/idf_component.yml` with `espressif/esp_tinyusb` and updated `REQUIRES esp_tinyusb` in `main/CMakeLists.txt`. Updated `sdkconfig.defaults` with `CONFIG_TINYUSB_HID_COUNT=1`.
2. **Symbol Collision with NimBLE Core:**
   - *Error:* `conflicting types for 'ble_transport_init'; have 'void(void)'` from `nimble/transport.h`.
   - *Resolution:* Renamed application bridge transport functions to `bridge_ble_transport_init()`, `bridge_ble_transport_send_notify()`, and `bridge_ble_transport_is_connected()` to prevent global C symbol namespace pollution.
3. **TinyUSB Report Descriptor Callback:**
   - *Error:* Undefined reference to `tud_hid_descriptor_report_cb`.
   - *Resolution:* Implemented `tud_hid_descriptor_report_cb()` returning `pockethid_combo_report_descriptor` from `main/hid/usb_hid.c`.
4. **NVS Flash Initialization:**
   - *Requirement:* NimBLE on ESP32 requires NVS flash initialization before stack bringup.
   - *Resolution:* Added standard `nvs_flash_init()` block to `app_main.c`.

---

## 8. Compiler Warnings & Runtime Safety Audit

Rebuilt application source files (`app_main.c`, `ble_transport.c`, `usb_hid.c`) with compiler warnings enabled:
- **Warnings Emitted:** `0` (Zero compiler warnings).
- **Buffer Safety:** No unbound string or buffer copy operations.
- **Signedness:** All packet lengths and indices conform strictly to signed/unsigned conventions.
- **Type Coercion:** Explicit casts throughout all FreeRTOS, NimBLE, and TinyUSB callback interfaces.

---

## 9. Firmware Artifacts & Verification

All target binaries were generated directly by `xtensa-esp32s3-elf-gcc`:

| Artifact | Flash Offset | Size (Bytes) | SHA-256 Hash |
| :--- | :--- | :--- | :--- |
| `pockethid-esp32s3-bridge.elf` | — | 7,020,480 | `9D4E4291D0DD99B07FD97C78E374B38DC009137B44F7C82567F13C00C8E9ADB3` |
| `pockethid-esp32s3-bridge.bin` | `0x10000` | 568,160 (0x8AB60) | `5D06784D36942B760AB0318271E51DA82B8DED8A742FDC1790AC4DFEB72A05E3` |
| `bootloader/bootloader.bin` | `0x00000` | 21,088 (0x5260) | `D8016C8FD0D219CA09708B5D38E8DA64DE6A24FEBE6BE63FC83EFCD54F194E9D` |
| `partition_table/partition-table.bin` | `0x08000` | 3,072 | `7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820` |
| `merged-binary.bin` | `0x00000` | 633,696 (0x9AB60) | `D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD` |

---

## 10. Regressions & Validation Summary

- **USB HID Descriptor:** 313 bytes (`PASS`, verified by `verify_descriptor.py`)
  - Report ID 1: Keyboard (8B)
  - Report ID 2: Mouse (4B)
  - Report ID 3: Consumer (2B)
  - Report ID 4: Gamepad (13B)
  - Report ID 5: Digitizer / Tablet (5B)
- **Host Unit Tests:** 59 / 59 PASS (`PASS`, verified by `run_host_tests.py`)
- **Android Unit Tests:** 147 / 147 PASS across 18 test suites (`PASS`, `.\gradlew.bat testDebugUnitTest`)
- **Android APK Build:** assembleDebug PASS (`PASS`, `.\gradlew.bat assembleDebug`)

---

## 11. Physical Hardware Gate Status

- **ESP32-S3 Physical Board:** PENDING (no board attached to host COM ports).
- **USB Physical Enumeration:** PENDING.
- **BLE Physical Link:** PENDING.
- **Windows Device Manager / Input Verification:** PENDING.
- **iOS BridgeTransport:** NOT IMPLEMENTED (kept strictly as transport abstraction).
- **Production Readiness:** NOT CLAIMED.
