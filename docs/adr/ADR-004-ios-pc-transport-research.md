# ADR-004 — iOS → PC Transport Research

## Status
RESEARCH COMPLETE — USER DECISION REQUIRED

## Problem
PocketHID on Android operates as a driverless, zero-software Bluetooth HID peripheral using Android's native `BluetoothHidDevice` API (API 28+). An Android handset pairs directly to Windows, macOS, or Linux, enumerating as a composite keyboard, mouse, gamepad, consumer device, and digitizer.

On iOS, the public Apple SDK does not provide an equivalent Bluetooth HID Device peripheral API. Consequently, PocketHID iOS currently cannot transmit HID reports directly to a PC over standard Bluetooth without an approved transport strategy.

This document researches all viable physical and network transport pathways to resolve how an iPhone running PocketHID can control a real PC, establishing architectural boundaries, platform constraints, and trade-offs before any implementation begins.

---

## Non-Negotiable Requirements
1. **Zero Fake Capabilities**: Never simulate a false "Connected" state or claim native driverless Bluetooth HID when it is not technically supported by public APIs.
2. **Domain Architecture Preservation**: The PocketHID domain model (`Input -> PocketAction -> ActionResolver -> ActionDispatcher -> Transport`) must remain completely decoupled from the underlying transport.
3. **No Unapproved Transports**: No proprietary network protocol, WebSocket server, or companion background daemon may be coded until explicitly reviewed and approved by the user/project owner.
4. **Privacy & Offline Integrity**: The system must not rely on external cloud relays, third-party internet servers, or telemetry tracking.
5. **Multi-Mode Support**: The selected transport must eventually support all 6 PocketHID modes: Keyboard, Mouse, Gamepad, Presenter, One-Hand, and Graphics Tablet (16-bit absolute digitizer coordinates).

---

## Apple Platform Facts

### 1. Bluetooth Low Energy & CoreBluetooth
- **[SOURCE]**: Apple Developer Documentation — *CoreBluetooth Framework* (`CBPeripheralManager`, `CBMutableService`).
- **[FACT]**: Third-party iOS apps can instantiate and advertise custom GATT services with 128-bit UUIDs using `CBPeripheralManager`.
- **[FACT]**: The standard Bluetooth SIG HID-over-GATT Profile (HOGP) utilizes Service UUID `0x1812`.
- **[FACT]**: In documented iOS behavior, the system Bluetooth daemon (`bluetoothd`) exclusively manages standard HID services. User-space attempts to publish Service `0x1812` via `CBPeripheralManager` are rejected by the operating system.
- **[FACT]**: Standard operating systems (Windows, macOS) require Bluetooth Security Mode 1 Level 2/3 (Authenticated Encryption) and standard L2CAP fixed channels or Classic PSM 0x0011/0x0013 for HID enumeration, which `CBPeripheralManager` does not expose to user applications.

### 2. Game Controller Framework
- **[SOURCE]**: Apple Developer Documentation — *GameController Framework* (`GCController`, `GCDevicePhysicalInput`).
- **[FACT]**: `GameController.framework` is strictly a Central/Consumer API that reads input from physical MFi, Xbox, and PlayStation controllers paired to the iPhone.
- **[FACT]**: It contains no public API to advertise or emulate an outbound virtual gamepad peripheral to an external PC.

### 3. Digitizer & Virtual Devices
- **[SOURCE]**: Apple Developer Documentation — *IOHIDFamily* (`IOHIDUserDevice`, `HIDVirtualDevice`).
- **[FACT]**: `IOHIDUserDevice` and `HIDVirtualDevice` allow software creation of virtual HID peripherals.
- **[FACT]**: These APIs are explicitly marked `Availability: macOS only` (macOS 10.15+) and are completely absent from the iOS public SDK.

### 4. External Accessory & USB
- **[SOURCE]**: Apple Developer Documentation — *ExternalAccessory Framework* (`EAAccessoryManager`).
- **[FACT]**: `ExternalAccessory` allows communication with hardware accessories that connect via Lightning, USB-C, or Bluetooth Classic under the Apple Made for iPhone (MFi) licensing program.
- **[FACT]**: It does not allow an iPhone to change its own USB device descriptor to emulate a standard USB HID keyboard/mouse to an arbitrary host PC.

---

## Detailed Evaluation of Transport Options

### Option A — Direct Bluetooth HID
- **Architecture**: iPhone $\rightarrow$ Bluetooth $\rightarrow$ PC (Native OS HID Stack).
- **Feasibility**: **NOT AVAILABLE THROUGH PUBLIC API**.
- **Technical Analysis**:
  - Requires Classic Bluetooth HID profile or BLE HOGP `0x1812`.
  - Apple does not provide public APIs for third-party peripheral HID registration on iOS.
  - Enforcing this option requires private entitlements (`com.apple.private.hid.*`) or jailbreak SPI, which breaks standard App Store distribution.
- **Verdict**: Infeasible for public standard iOS SDK.

### Option B — BLE Custom GATT Service
- **Architecture**: iPhone $\rightarrow$ Custom BLE GATT Service (UUID: `8F3D...`) $\rightarrow$ PC.
- **Feasibility**: **REQUIRES PC SOFTWARE** / **POSSIBLE**.
- **Technical Analysis**:
  - **iOS Side**: 100% supported via public `CoreBluetooth` (`CBPeripheralManager`). Can advertise custom characteristics for Keys, Mouse vectors, Gamepad states, and Tablet coordinates.
  - **PC Side**: Windows/macOS/Linux native Bluetooth stacks do **NOT** recognize custom 128-bit GATT services as keyboards or mice. Windows Device Manager lists a generic "Bluetooth LE Device" with GATT attributes.
  - **Consumption**: Requires a companion background application on the PC (using Windows WinRT Bluetooth APIs or macOS CoreBluetooth) to read GATT notifications and inject them into the OS input stream (e.g. Windows `SendInput`).
  - **Web Bluetooth Limitation**: Chrome/Edge Web Bluetooth API can read the GATT service inside a web tab, but the browser cannot inject global OS keystrokes or mouse events outside the browser window.
  - **Latency**: BLE connection interval is negotiated (typical 15ms–30ms on iOS/Windows, minimum 7.5ms per Apple accessory guidelines). Moderate latency, suitable for presenter/media, suboptimal for high-speed gaming or smooth drawing.
  - **Background Limits**: iOS limits BLE advertising in the background unless specific overflow scanning is handled.
- **Verdict**: Technically viable, but strictly requires companion software on the PC.

### Option C — Wi-Fi Local Area Network (LAN)
- **Architecture**: iPhone $\rightarrow$ Local Wi-Fi (UDP / TCP / WebSocket) $\rightarrow$ PC.
- **Feasibility**: **REQUIRES PC SOFTWARE** / **AVAILABLE**.
- **Technical Analysis**:
  - **iOS Side**: 100% supported via standard `Network.framework` (`NWConnection`, `NWListener`) or POSIX BSD sockets.
  - **Discovery**: Zero-configuration pairing using Bonjour / mDNS (`_pockethid._tcp` / `_udp`). The phone automatically detects the PC on the local subnet without typing IP addresses.
  - **Protocols**:
    - **UDP**: Unreliable, low-overhead datagrams. Latency is ultra-low (< 1–3ms on modern 5GHz Wi-Fi). Ideal for continuous mouse deltas and 16-bit drawing coordinates.
    - **TCP / WebSocket**: Reliable, ordered byte stream. Ideal for state transitions, modifier latches, text paste, and Presenter slide commands.
  - **PC Side**: Requires a lightweight, open-source companion utility (portable `.exe` on Windows, menu bar item on macOS) that listens on the local port and calls OS input injection (`SendInput`, `uinput`, or `CGEvent`).
  - **Security**: Local network only, zero cloud dependencies. Can use TLS-PSK or AES-GCM local pairing token to prevent unauthorized LAN control.
  - **Driver Requirements**: Zero kernel drivers needed for Keyboard, Mouse, Consumer, and Presenter. Virtual Gamepad on Windows can leverage the open-source ViGEmBus driver.
- **Verdict**: Most practical, lowest latency, and most robust software-only pathway for iOS.

### Option D — Direct USB Cable
- **Architecture**: iPhone $\rightarrow$ Lightning / USB-C $\rightarrow$ PC.
- **Feasibility**: **NOT AVAILABLE DIRECTLY**; **REQUIRES PC SOFTWARE VIA TUNNEL**.
- **Technical Analysis**:
  - When plugged into a PC via USB, iOS exposes Apple Mobile Device descriptors (MTP/PTP, Apple USB Multiplexor). An iOS app cannot configure the iPhone's USB hardware controller to enumerate as a USB HID Composite Device.
  - **usbmuxd Tunnel**: If the PC runs a companion utility utilizing the Apple Mobile Device service / `usbmuxd` (the protocol used by iTunes, AltStore, and libimobiledevice), a peer-to-peer TCP socket can be established over the physical USB cable.
  - **Latency**: Sub-millisecond (< 1ms), perfectly stable, immune to Wi-Fi interference.
  - **Limitation**: Requires PC companion utility and Apple USB drivers installed on Windows.
- **Verdict**: Excellent secondary physical connection mode for high-reliability presentation, but cannot achieve driverless HID without PC software.

### Option E — External Hardware HID Bridge
- **Architecture**: iPhone $\rightarrow$ BLE / Wi-Fi $\rightarrow$ **Microcontroller Bridge** (ESP32-S3 / RP2040 / nRF52840) $\rightarrow$ Native USB / Bluetooth HID $\rightarrow$ PC.
- **Feasibility**: **REQUIRES EXTERNAL HARDWARE** / **AVAILABLE**.
- **Technical Analysis**:
  - **How it works**: An inexpensive ($3–$5) external hardware dongle acts as a protocol bridge.
    - **Facing the PC**: The dongle runs standard USB Composite HID firmware (using TinyUSB or ESP-IDF). It enumerates as a 100% genuine, driverless hardware Keyboard, Mouse, Gamepad, and Digitizer in Windows Device Manager.
    - **Facing the iPhone**: The dongle exposes a standard BLE GATT service or Wi-Fi SoftAP. The iPhone connects using standard public `CoreBluetooth` or local Wi-Fi without any special iOS privileges or MFi requirements.
  - **Driverless & Zero-Software on PC**: **YES.** The PC requires **ZERO companion software** and **ZERO custom drivers**. It works in BIOS, Windows Lock Screen, macOS, and Linux out of the box.
  - **Latency**: BLE to dongle (~7.5–15ms) + dongle to PC USB (1ms). Total latency ~10–16ms.
  - **Domain Alignment**: 100% preserves the existing PocketHID action engine. The phone simply serializes `ActionExecutionPlan` or raw HID report buffers over BLE/Wi-Fi to the bridge.
- **Verdict**: The ONLY pathway that achieves a **100% driverless, zero-PC-software** experience for iOS while remaining 100% compliant with public Apple APIs.

---

## Requirement Matrix

### Matrix 1: PocketHID Functional Requirements

| Requirement | Option A (Direct BT) | Option B (BLE GATT) | Option C (Wi-Fi LAN) | Option D (USB Cable) | Option E (HW Bridge) |
|---|:---:|:---:|:---:|:---:|:---:|
| **Keyboard** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **Mouse** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **Consumer Control** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **Gamepad** | ❌ (No API) | ⚠️ (Needs App) | ✅ (ViGEm/App) | ✅ (ViGEm/App) | ✅ (Driverless) |
| **Presenter** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **One-Hand** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **Graphics Tablet** | ❌ (No API) | ⚠️ (Needs App) | ✅ (Via App) | ✅ (Via App) | ✅ (Driverless) |
| **Driverless Host HID**| ❌ | ❌ | ❌ | ❌ | ✅ |
| **Zero PC Companion App**| ❌ | ❌ | ❌ | ❌ | ✅ |
| **No Cloud / Offline** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **iOS Public API Compliance**| ❌ | ✅ | ✅ | ✅ | ✅ |
| **Low Latency** | — | ~15–30ms | ~1–3ms | < 1ms | ~10–15ms |
| **Cross-Platform PC** | — | Win/Mac/Linux | Win/Mac/Linux | Win/Mac/Linux | Win/Mac/Linux |

### Matrix 2: Options Architectural Classification

| Option | iOS Public API | Native HID on PC | PC Software | Driver | Extra HW | Main Blocker / Constraint | Verification Status |
|---|---|---|---|---|---|---|---|
| **Option A: Direct BT HID** | NOT AVAILABLE | YES (If possible) | NO | NO | NO | Apple does not expose HID Device peripheral role | BLOCKED BY APPLE SDK |
| **Option B: BLE GATT** | AVAILABLE | NO | YES | NO | NO | PC requires companion to translate custom GATT | REQUIRES PHYSICAL POC |
| **Option C: Wi-Fi LAN** | AVAILABLE | NO | YES | NO* | NO | PC requires lightweight companion listener | REQUIRES PHYSICAL POC |
| **Option D: USB Cable** | AVAILABLE | NO | YES | NO* | NO | Requires usbmuxd tunnel & PC companion | REQUIRES PHYSICAL POC |
| **Option E: HW Bridge** | AVAILABLE | YES | NO | NO | YES | Requires inexpensive external microcontroller | REQUIRES PHYSICAL POC |

*\* Note: Optional ViGEmBus driver is only needed if virtual Xbox 360 gamepad emulation is desired on Windows; standard keyboard/mouse injection requires zero drivers.*

---

## Architectural Distinctions: Essential Definitions

To eliminate false equivalence, the project formally adopts the following technical definitions:

1. **Driverless**: The host operating system uses built-in, inbox operating system drivers (e.g. `hidusb.sys`, `hidbth.sys`). No `.sys`, `.kext`, or kernel-mode drivers are installed.
2. **Zero Companion Software**: The host PC runs no third-party desktop application, tray utility, service, or background daemon.
3. **Native OS HID Enumeration**: The device appears in Windows Device Manager under `Human Interface Devices`, `Keyboards`, or `Mice` as an official hardware/firmware node.
4. **Bluetooth Transport vs BLE GATT**: Bluetooth Classic HID and BLE HOGP are standardized profiles negotiated at the OS driver level. Custom BLE GATT is an application-level data stream that does not enumerate as an input device without software translation.
5. **Network Packet Delivery vs OS Input**: Receiving a UDP packet or WebSocket frame on a PC is communication, not input. Input only occurs when that data is translated and injected into the OS event queue via native platform APIs (`SendInput`).

---

## Architecture Impact & Domain Independence

The existing PocketHID clean architecture is:
$$\text{User Input} \longrightarrow \text{PocketAction} \longrightarrow \text{ActionResolver} \longrightarrow \text{ActionDispatcher} \longrightarrow \text{HIDTransport}$$

This design achieves complete decoupling:
- `KeyboardView`, `MouseView`, `GamepadView`, `PresenterView`, `OneHandView`, and `DrawingTabletView` dispatch semantic `PocketAction` items or standardized HID report buffers.
- `FocusLockController` operates purely at the navigation layer, entirely independent of the transport.
- Adapting to **Option C (Wi-Fi LAN)** or **Option E (Hardware Bridge)** requires writing a single transport adapter conforming to `protocol HIDTransport` (e.g. `WiFiLanTransport` or `HardwareBridgeTransport`).
- **Zero domain or UI code will require rewriting.**

---

## Security Considerations
1. **Local Network Privacy**: Option C must enforce local-only subnet binding (`127.0.0.1` or LAN `192.168.x.x`) with zero external UPnP or WAN port forwarding.
2. **Pairing Security**:
   - Option C (Wi-Fi) should use a short 4-digit PIN or ephemeral cryptographic token exchanged via QR code or manual entry to prevent unauthorized LAN devices from injecting keystrokes.
   - Option E (Hardware Bridge) uses standard Bluetooth LE pairing or Wi-Fi WPA2 pre-shared key.
3. **Input Sanitization**: PC companion listener must sanitize input payloads against buffer overflows.

---

## Physical Proof Required Before Accepting Any Transport
Before any candidate transport is designated as verified, it must execute the standardized 20-step hardware proof protocol detailed in [`docs/spikes/IOS-TRANSPORT-POC-PROTOCOL.md`](file:///d:/desktop/PocketHID/docs/spikes/IOS-TRANSPORT-POC-PROTOCOL.md), validating physical connection, low-latency keystrokes, smooth mouse movement, volume changes, and 16-bit digitizer mapping on a physical PC.

---

## Open Questions for Project Owner / User
1. **Product Direction**: Does the PocketHID iOS product prefer:
   - **Path 1 (Software-Only)**: Accept a lightweight, open-source PC tray companion over local Wi-Fi LAN (Option C)?
   - **Path 2 (Zero-Software Hardware Dongle)**: Maintain a 100% driverless, zero-companion-software experience identical to Android by introducing an inexpensive ESP32-S3 hardware bridge dongle (Option E)?
   - **Path 3 (Dual Architecture)**: Support Local Wi-Fi LAN by default, with optional Hardware Bridge mode for security-restricted enterprise PCs that forbid third-party software?

---

## Current Direction

External HID Bridge is selected as the working POC architecture, subject to physical validation.

This is NOT product-final approval.

Status:
"POC direction approved for engineering investigation" (not "production transport approved").

---

## Decision
**PENDING USER APPROVAL**

*No transport backend code will be written until the Project Owner reviews this research and explicitly authorizes the target pathway.*
