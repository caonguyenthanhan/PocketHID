# PocketHID iOS BridgeTransport Specification

**Document ID:** `docs/spec/POCKETHID-IOS-BRIDGE-TRANSPORT.md`  
**Status:** Architecture Definition / Feasibility POC (Code Implementation: `PENDING`)  
**Hardware Dependency:** `BLOCKED` (ESP32-S3 physical validation pending)

---

## 1. Context and Objective

While the Android Bluetooth HID transport serves as the primary baseline, iOS does not expose public APIs for acting as an arbitrary direct Bluetooth HID device to a PC. 
Therefore, the iOS architecture relies on the **External HID Bridge POC**: 
`iPhone -> BLE Custom GATT -> ESP32-S3 Bridge -> USB HID -> Windows PC`

This specification defines the strict contract for the `iOS BridgeTransport` module.

**Strict Constraints:**
- The existing wire protocol and golden vectors MUST be perfectly preserved.
- No second protocol will be invented for iOS.
- No fallback mechanisms (Wi-Fi, companion software, cloud, PC daemon) are permitted.

---

## 2. Architecture Boundaries

The pipeline is strictly segregated into the following layers:

1. **PocketHID Core / Domain (Platform-Agnostic):**
   - Generates and manages `PocketAction` events (button presses, axis movements).
   - Serializes actions into exact binary report payloads as defined in `POCKETHID-GOLDEN-VECTORS.md`.
2. **iOS BridgeTransport (Swift / CoreBluetooth):**
   - Implements the generic `HidTransport` interface.
   - Receives golden binary payloads from the Core.
   - Handles BLE framing, MTU negotiation, and dispatching to the ESP32-S3 via CoreBluetooth.
3. **BLE Custom GATT (Air Interface):**
   - A dedicated GATT Service and TX/RX Characteristic exposed by the ESP32-S3.
   - Transmits the raw frames from iOS to the Bridge.
4. **External HID Bridge (ESP32-S3 Hardware):**
   - Acts as a BLE GATT Server.
   - Receives packets, deframes them if necessary, and forwards the exact HID report payloads to the PC over native USB HID.

---

## 3. BridgeTransport State Model

The iOS `BridgeTransport` adheres to a strict state machine:

- **`DISCONNECTED`**: Default state. Bluetooth is off, or no bridge is currently targeted.
- **`SCANNING`**: Actively searching for the specific ESP32-S3 BLE Advertisement.
- **`CONNECTING`**: Establishing BLE link, discovering GATT services/characteristics, and negotiating MTU.
- **`CONNECTED`**: Link established, characteristics subscribed, and bridge explicitly acknowledges readiness. 
  *(Rule: Do NOT fake the `CONNECTED` state. The UI must only reflect `CONNECTED` when the GATT link is physically confirmed.)*
- **`ERROR`**: Connection dropped, GATT discovery failed, MTU too small, or Bluetooth disabled.

---

## 4. Connection Lifecycle

1. **Connect:** Core requests a connection. `BridgeTransport` initiates CoreBluetooth scan. Upon finding the Bridge, it connects, discovers the custom HID Service and RX Characteristic, and transitions to `CONNECTED`.
2. **Disconnect:** Core requests disconnection, or physical link drops. `BridgeTransport` explicitly cancels the peripheral connection and transitions to `DISCONNECTED`. Neutral safety reports MUST be sent prior to intentional disconnect.
3. **Error Handling:** If the bridge stops responding or a CoreBluetooth error occurs, `BridgeTransport` immediately fires an `ERROR` state event, ceases transmission, and safely cleans up CoreBluetooth references.

---

## 5. Packet Framing and Action Mapping

- **Protocol Preservation:** The iOS BridgeTransport utilizes the exact binary report structures defined in `POCKETHID-BRIDGE-WIRE-PROTOCOL.md`.
- **GATT MTU Constraints:** The Transport queries the BLE MTU. If a serialized HID report (plus bridge protocol headers, e.g., endpoint ID) exceeds the MTU, the `BridgeTransport` is responsible for safe fragmentation (if the custom GATT protocol requires it) or dropping with an error. 
- **Mapping:** 
  - `PocketAction` -> `Core` -> `Binary Array` -> `BridgeTransport` -> `CoreBluetooth WriteValue (WithoutResponse preferred for latency)`.

---

## 6. Device-Specific Handling

The BridgeTransport treats all device payloads opaquely. It routes them using the predefined Report IDs or Protocol Endpoint Identifiers:

- **Keyboard:** Standard 8-byte modifier/key arrays.
- **Mouse:** Standard relative X/Y/Wheel and button bitmaps.
- **Consumer Control:** Media keys (Volume, Play/Pause).
- **Gamepad:** Axes and button states.
- **Tablet:** Absolute coordinates, pressure, and stylus states.
- **Neutral/Safety Reports:** `BridgeTransport` guarantees the transmission of zeroed/neutral reports (e.g., all keys up, mouse buttons released) immediately prior to intentional disconnection or app backgrounding.

---

## 7. Testability and Hardware Dependencies

### A. Unit-Testable without Hardware
The following logic can and must be unit-tested using mocked CoreBluetooth dependencies:
- State machine transitions (`DISCONNECTED` -> `SCANNING` -> `CONNECTING` -> `CONNECTED`).
- Error propagation and lifecycle management.
- Protocol payload encapsulation (ensuring the byte array passed to the transport is untouched before being handed to the GATT write method).
- Neutral report injection on simulated disconnect.
- GATT MTU segmentation logic (if implemented).

### B. Hardware-Dependent (Currently `BLOCKED` / `PENDING`)
Because the physical ESP32-S3 validation is `BLOCKED`, the following iOS BridgeTransport elements remain physically unverified:
- `PENDING`: Real-world CoreBluetooth discovery latency of the ESP32-S3.
- `PENDING`: Actual BLE connection stability and GATT MTU negotiation.
- `PENDING`: End-to-end latency (iOS touch -> BLE -> ESP32-S3 -> USB -> Windows OS).
- `BLOCKED`: End-to-end correctness of host-input physical verification.
