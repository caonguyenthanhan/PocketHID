# PocketHID ESP32-S3 Bridge Firmware POC

## 1. Overview
This firmware implements an external hardware bridge converting custom binary BLE packets from the PocketHID iOS application into standard driverless USB HID reports for a target PC (Windows, macOS, Linux).

```
iPhone (PocketHID iOS) 
  → BLE Custom GATT (CoreBluetooth Central) 
  → ESP32-S3 Bridge 
  → USB Full-Speed Composite HID (TinyUSB) 
  → Host PC
```

---

## 2. Hardware Specification
- **Target MCU:** Espressif ESP32-S3 (Dual-core Xtensa LX7 @ 240 MHz)
- **Supported Boards:**
  - ESP32-S3-DevKitC-1-N8R8 (or N16R8)
  - Waveshare ESP32-S3-Zero
  - Generic ESP32-S3 modules with native USB D+/D- pins (GPIO 19: D-, GPIO 20: D+)
- **USB PHY:** Internal Full-Speed (12 Mbps) USB OTG PHY.

---

## 3. Toolchain & Build Environment
- **Framework:** ESP-IDF v5.1+ (Recommended v5.2 / v5.3)
- **Host Unit Testing:** MSYS2 / GCC 16+ or Clang + Python 3.11+
- **Build Commands:**
  ```bash
  # Set target
  idf.py set-target esp32s3

  # Build firmware
  idf.py build

  # Flash to device (replace COMx with your port)
  idf.py -p COMx flash

  # Monitor serial logs
  idf.py -p COMx monitor
  ```

---

## 4. USB HID Composite Descriptors
Exposes 5 Report Collections matching the proven Android contract byte-for-byte:
- **Report ID 1:** Keyboard (8 bytes: modifiers, reserved, 6KRO keycodes)
- **Report ID 2:** Mouse (4 bytes: 3 buttons, 8-bit signed dx, dy, wheel)
- **Report ID 3:** Consumer Control (2 bytes: 16-bit usage code)
- **Report ID 4:** Gamepad (13 bytes: 16 buttons, 8-way hat, 4 16-bit axes, 2 8-bit triggers)
- **Report ID 5:** Digitizer / Tablet (5 bytes: status bits, 16-bit unsigned X [0..32767], Y [0..32767])

---

## 5. Bluetooth Low Energy Service & Characteristics
- **Role:** BLE Peripheral / GATT Server
- **Service UUID:** `a55a0001-e234-4b56-8a78-9abcdef01234` (Custom 128-bit Vendor UUID)
- **Characteristic RX (Write Without Response):** `a55a0002-e234-4b56-8a78-9abcdef01234`
- **Characteristic TX (Notify):** `a55a0003-e234-4b56-8a78-9abcdef01234`
- *Note:* Does **NOT** advertise or use standard Bluetooth SIG `0x1812` (HID over GATT).

---

## 6. Deterministic POC Test Mode
A standalone test mode is implemented in `main/tests/poc_test_mode.c` to validate USB HID enumeration and host keystroke/cursor reception without requiring an iPhone or BLE connection.
- Runs 15 sequential actions: Keyboard A, Shift+A, Ctrl+C, Win+D, Mouse move, Left click, Scroll, Volume +, Volume -, Gamepad A, Gamepad analog axis, Tablet top-left, Tablet center, Tablet bottom-right, and Neutral release.

---

## 7. Fail-Safe Safety System
- **Watchdog Timer:** Configurable timeout (default: 250 ms). Automatically feeds on valid incoming packets.
- **Auto-Neutralization:** If communication halts for > 250 ms, the bridge calls `force_all_neutral()`, transmitting neutral release reports across all 5 HID endpoints.
- **BLE Disconnect Hook:** Disconnecting from the iPhone triggers immediate neutral report flushing within 5 ms.
- **Replay & Malformed Protection:** Packets with invalid CRC, corrupt magic (`0xA55A`), or sequence number regressions are rejected.

---

## 8. Host-Side Unit Tests
Unit tests for the packet parser, sequence validator, safety manager, and HID report builders are located in `tools/host_tests.c`:
```bash
python tools/run_host_tests.py
```

---

## 9. Known Limitations
1. **Cryptographic Authentication:** Current POC uses protocol sequence validation and CRC; full LE Secure Connections passkey pairing is pending production hardening.
2. **Physical Verification:** Requires physical ESP32-S3 board with native USB connection to PC for hardware verification.
