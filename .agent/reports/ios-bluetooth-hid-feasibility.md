# PocketHID iOS — Bluetooth HID Feasibility Investigation
**Author:** PocketHID Systems Architecture Team  
**Date:** 2026-09-28  
**Scope:** Investigation of Public Apple iOS APIs for Bluetooth HID Peripheral Role  
**Status:** **HARD ARCHITECTURE GATE — NOT AVAILABLE THROUGH DOCUMENTED PUBLIC IOS APIS**

---

## 1. Executive Summary

PocketHID on Android utilizes the Android Bluetooth HID Device Profile (`android.bluetooth.BluetoothHidDevice`, added in Android 9 / API 28), which allows an Android smartphone to register as a standard Bluetooth HID peripheral (Composite Keyboard, Mouse, Gamepad, Consumer Control, Digitizer) recognized natively by Windows, macOS, and Linux without any companion software or host drivers.

**Finding for iOS:**  
Under documented public iOS SDKs (including iOS 15 through iOS 18+), **there is no documented public Apple API that allows a third-party iOS app to register the iPhone as a standard, driverless Bluetooth HID peripheral** for arbitrary PC hosts (Windows, macOS, Linux).

---

## 2. Technical Audit by Category

### Category A: CoreBluetooth & Peripheral Role
- **[FACT]**: `CoreBluetooth.framework` provides `CBPeripheralManager` to publish services and advertise GATT profiles over Bluetooth Low Energy.
- **[FACT]**: The standard Bluetooth HID Over GATT Profile (HOGP) utilizes SIG-assigned Service UUID `0x1812`.
- **[FACT]**: In documented iOS behavior, system Bluetooth daemons manage standard HID input devices. Attempting to publish SIG-reserved services such as `0x1812` via `CBPeripheralManager` is not supported for custom peripheral emulation to external hosts.
- **[FACT]**: Host operating systems (Windows, macOS) require specific Bluetooth pairing security (Security Mode 1 Level 2/3, authenticated encryption) and standard L2CAP signaling channels (PSM 0x0011 / 0x0013 for Classic HID) which `CBPeripheralManager` does not expose to user-space apps.
- **[TECHNICAL STATUS]**: Not available through documented public iOS APIs used by this project.

### Category B: Game Controller Framework
- **[FACT]**: `GameController.framework` provides discovery, connection, and input reading for MFi, Xbox, DualShock/DualSense, and standard gamepads connected to the iPhone.
- **[FACT]**: `GameController.framework` is strictly a Central / Consumer framework. It contains no public API to advertise or emulate an outbound virtual gamepad peripheral to a remote PC.
- **[TECHNICAL STATUS]**: Not available through documented public iOS APIs used by this project.

### Category C: Digitizer / Pen & Tablet Emulation
- **[FACT]**: Standard HID digitizer emulation requires HID Usage Page `0x0D` (Digitizers) inside an active HID profile.
- **[FACT]**: No public iOS framework allows an iPhone to register as an external absolute digitizer/pen device to a PC.
- **[TECHNICAL STATUS]**: Not available through documented public iOS APIs used by this project.

### Category D: macOS-Only vs iOS APIs
- **[FACT]**: `IOHIDFamily`, `IOHIDUserDevice`, and `HIDVirtualDevice` are documented Apple APIs that allow creating virtual HID devices in user-space.
- **[FACT]**: These APIs are explicitly marked `Availability: macOS only` (macOS 10.15+) and are absent from the iOS public SDK.
- **[FACT]**: `DriverKit` (`USBDriverKit`, `HIDDriverKit`) is available on macOS and selected iPadOS hardware (M-series iPads acting as USB Host). It does not provide an iPhone Bluetooth peripheral emulation role.

### Category E: Entitlements & Private Frameworks
- **[FACT]**: Third-party App Store submissions are limited to public entitlements. Private entitlements (such as `com.apple.private.hid.*`) or private frameworks (`IOBluetooth.framework`, private IOKit SPI) are prohibited by App Store guidelines and are not considered viable for this production architecture.

---

## 3. Platform Feasibility Matrix

| Capability | Documented Public iOS API | Direct PC Bluetooth HID | Architectural Status |
|---|---|---|---|
| **Keyboard** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **Mouse** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **Gamepad** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **Consumer Control** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **Digitizer / Tablet** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **Presenter Actions** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |
| **One-Hand Control** | None | No | **NOT AVAILABLE THROUGH DOCUMENTED PUBLIC APIS** |

---

## 4. Architectural Decisions

1. **No Fake HID**: The iOS port will not report false "Connected" statuses or claim active Bluetooth HID transmission to a host.
2. **No Proprietary Network Fallback**: PocketHID will not silently transform into a WebSocket, TCP, UDP, or companion desktop service without explicit user/owner instruction.
3. **Decoupled Architecture**:
   - The UI and domain logic operate against the `HIDTransport` protocol.
   - `iOSTransport` declares capability `.unsupportedDirectHID(reason: ...)` and connection status `.unavailable(reason: ...)`.
   - The app functions as a high-fidelity local command deck and graphics tablet canvas, ready to plug in an approved transport backend in the future without altering UI or domain logic.
