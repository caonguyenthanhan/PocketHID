# PocketHID iOS Status
**Last updated: Wave 12.1 — 2026-09-29**

---

## Current Status

```
External HID Bridge:
POC DIRECTION

Firmware:
CROSS-COMPILED (Wave 11.1, commit 89412ccd, ESP-IDF v5.3.1)

Physical bridge:
BLOCKED (Hardware not connected)

USB HID host verification:
PENDING

BLE physical verification:
PENDING

iOS BridgeTransport:
NOT IMPLEMENTED
```

---

## Current iOS Transport Direction

EXTERNAL HID BRIDGE — POC INVESTIGATION

## Current Implementation

NOT IMPLEMENTED

## Physical Validation

PENDING

## Direct iPhone Bluetooth HID

NOT AVAILABLE THROUGH DOCUMENTED PUBLIC IOS API BASELINE

## Current Transport Decision

POC direction approved for engineering investigation (Subject to physical validation).


## Verified Platform Constraints

- **Direct Bluetooth HID:** NOT AVAILABLE THROUGH PUBLIC API. Apple's CoreBluetooth (`CBPeripheralManager`) forbids advertising or publishing standard Bluetooth SIG-reserved 16-bit Service UUIDs, specifically `0x1812` (Human Interface Device Service).
- **Classic Bluetooth HID Device Role:** Public iOS SDK exposes zero APIs for Classic Bluetooth HID peripheral (slave) operation. Private entitlement `com.apple.developer.bluetooth.hid` is required for direct Bluetooth HID emulation and will result in immediate App Store rejection.
- **GameController Framework:** Provides only Central/Client capabilities (querying external MFi, Xbox, DualShock/DualSense controllers). Cannot emulate a virtual controller peripheral outward to a PC.
- **IOHIDFamily / CoreHID:** Private / internal framework on iOS; no public virtual HID creation API (`HIDVirtualDevice` exists only on macOS 10.15+ / DriverKit).
- **ExternalAccessory / MFi:** Requires physical Apple MFi cryptographic coprocessor / MFi license. Does not expose standard USB composite HID descriptors to a generic Windows PC.
- **App Store & Background Limits:** CoreBluetooth peripheral advertising in background is severely throttled (omits local name, places service UUID in overflow area). Background execution for real-time input transmission requires active UI foreground or designated background audio/VoIP sessions.

## Research Complete

- **ADR-004 Generated:** Comprehensive analysis documented in [`ADR-004-ios-pc-transport-research.md`](file:///d:/desktop/PocketHID/docs/adr/ADR-004-ios-pc-transport-research.md).
- **Candidate Transports Evaluated:**
  - Option A: Direct Bluetooth HID (Status: `NOT AVAILABLE THROUGH PUBLIC API`)
  - Option B: Custom BLE GATT (Status: `REQUIRES PC SOFTWARE`)
  - Option C: Local Wi-Fi LAN - UDP/WebSocket (Status: `REQUIRES PC SOFTWARE` / `AVAILABLE`)
  - Option D: Direct USB Cable (Status: `NOT AVAILABLE THROUGH PUBLIC API` directly / `REQUIRES PC SOFTWARE` via usbmuxd)
  - Option E: External Hardware HID Bridge - ESP32-S3/RP2040 (Status: `REQUIRES EXTERNAL HARDWARE` / `AVAILABLE`)
- **Key Distinctions Documented:**
  - Driverless ≠ No companion software ≠ Native OS HID enumeration ≠ Bluetooth transport ≠ BLE transport ≠ Custom network transport.
- **Physical POC Protocol Created:** 20-step standardized verification sequence documented in [`IOS-TRANSPORT-POC-PROTOCOL.md`](file:///d:/desktop/PocketHID/docs/spikes/IOS-TRANSPORT-POC-PROTOCOL.md).

## Implementation Status

- **iOS Transport Implementation:** NOT IMPLEMENTED IN THIS WAVE. `iOSTransport` maintains `.unavailable` capability and `.notConnected` state.
- **ESP32-S3 Bridge Firmware POC:** IMPLEMENTED in `firmware/esp32s3-bridge/`.
  - Wire protocol parser (`packet_parser.c`) and sequence validator: UNIT-TESTED (PASS).
  - Safety watchdog & auto-neutralization (`safety_manager.c`): UNIT-TESTED (PASS).
  - Composite USB HID report builders (`hid_reports.c`): UNIT-TESTED (PASS).
  - Descriptor definition matching Android contract byte-for-byte: VERIFIED.
  - TinyUSB and NimBLE integration stubs / driver code: IMPLEMENTED.
  - Host unit tests (`tools/host_tests.c`): 46 / 46 PASSED.
- **Domain Action Architecture:** `PocketAction`, `ActionResolver`, `ActionDispatcher`, `FocusLockController` remain 100% decoupled and ready for a future `BridgeTransport` adapter.

## Physical Verification

PENDING

- Physical iPhone device testing: PENDING
- Physical ESP32-S3 hardware bridge USB enumeration: PENDING
- Real host PC input injection verification: PENDING


---

## Implemented Components Summary

- **Native SwiftUI Shell:** `MainView`, `TopCommandBarView`, `ModeNavigationBar`, `DeckModeSwitcherView`
- **Domain Action Architecture:** `PocketAction`, `ActionResolver`, `ActionDispatcher`, `HostOs`
- **Global Focus Lock:** `FocusLockController` (single source of truth across all 6 modes)
- **UI Modes (6):** Keyboard, Mouse, Gamepad, Presenter (with 1.5s safe exit), One-Hand (Web & Video), Drawing Tablet (full-bleed canvas & 8-stage pipeline)
- **Localization:** English & Vietnamese via `PocketStrings`
- **Project Structure:** Validated `Package.swift` and `PocketHID.xcodeproj` (32 source files + 5 test suites)
- **Platform Compatibility:** iOS 15.0+ deployment target
