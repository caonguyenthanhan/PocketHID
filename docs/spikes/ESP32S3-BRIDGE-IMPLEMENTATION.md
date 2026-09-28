# ESP32-S3 Bridge Firmware Implementation Spike

**Document ID:** `SPIKE-003-ESP32S3-FIRMWARE-POC`  
**Related Code:** [`firmware/esp32s3-bridge/`](file:///d:/desktop/PocketHID/firmware/esp32s3-bridge/)  
**Status:** FIRMWARE POC IMPLEMENTED — HOST TESTS PASS — PHYSICAL VALIDATION PENDING  

---

## 1. Executive Summary

This spike implements the first firmware proof-of-concept (POC) for the PocketHID external hardware bridge on the Espressif ESP32-S3 microcontroller. The firmware ingests custom 128-bit BLE GATT packets from an iPhone, validates packet integrity, passes inputs through a safety watchdog gate, and emits native USB Composite HID reports to the host PC.

---

## 2. Architecture & Design Decisions

1. **Modular Subsystem Separation:**
   - Transport (`ble_transport.c` / NimBLE) is completely decoupled from HID report formatting (`hid_reports.c`) and USB stack (`usb_hid.c` / TinyUSB).
   - Packet framing and CRC validation are isolated in `packet_parser.c`.
   - Watchdog and neutralization semantics are encapsulated in `safety_manager.c`.
2. **Contract Consistency with Android:**
   - The USB HID report descriptor (`hid_descriptors.c`) mirrors Android's `HidConstants.COMBO_REPORT_DESCRIPTOR` 100% byte-for-byte.
   - All 5 Report IDs (Keyboard: 1, Mouse: 2, Consumer: 3, Gamepad: 4, Tablet: 5) and bit positions match the existing Android client implementation.
3. **Safety First (Idempotent Neutralization):**
   - The safety manager provides an idempotent `force_all_neutral()` function.
   - Any link disruption (BLE disconnect or 250 ms watchdog expiry) flushes zeroed release reports across all endpoints.
4. **Development-Only Test Mode:**
   - Implemented `poc_test_mode.c` to generate deterministic 15-action test streams directly to the host PC via USB without requiring an active iOS BLE connection.

---

## 3. Hardware Assumptions
- **SoC:** Espressif ESP32-S3 (WROOM-1 / DevKitC-1).
- **USB PHY:** Internal USB Full-Speed PHY connected to GPIO 19 (D-) and GPIO 20 (D+).
- **Power:** Bus-powered directly from target PC USB port (500 mA max).

---

## 4. Verification State

| Subsystem | Source State | Unit Test Status | Firmware Build Status | Physical Hardware Status |
|---|---|---|---|---|
| **Packet Parser** | `IMPLEMENTED` | `UNIT-TESTED` (46/46 PASS) | `BLOCKED` (ESP-IDF absent locally) | `PENDING` |
| **Safety Manager** | `IMPLEMENTED` | `UNIT-TESTED` (46/46 PASS) | `BLOCKED` (ESP-IDF absent locally) | `PENDING` |
| **Report Builders** | `IMPLEMENTED` | `UNIT-TESTED` (46/46 PASS) | `BLOCKED` (ESP-IDF absent locally) | `PENDING` |
| **USB HID Stack** | `IMPLEMENTED` | `UNIT-TESTED` (Stubs) | `BLOCKED` (ESP-IDF absent locally) | `PENDING` |
| **BLE Transport** | `IMPLEMENTED` | `UNIT-TESTED` (Stubs) | `BLOCKED` (ESP-IDF absent locally) | `PENDING` |
| **Test Injector** | `IMPLEMENTED` | `UNIT-TESTED` (Python) | `N/A` | `PENDING` |

---

## 5. Unsupported Features & Future Work
1. **Cryptographic Authentication:** Marked as `POC LIMITATION — NOT COMPLETE`. Will require LE Secure Connections passkey pairing for production.
2. **OTA Firmware Update:** Not implemented in POC.
3. **Hardware Button Bindings:** Test mode currently triggered via API/task call rather than physical GPIO button debounce.
