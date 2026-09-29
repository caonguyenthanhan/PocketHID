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

The pipeline strictly preserves the existing architecture and is segregated into the following layers:

1. **INPUT SOURCE & Core Domain (Platform-Agnostic):**
   - Generates and manages semantic `PocketAction` events (e.g., button presses, axis movements).
   - Contains no BLE/GATT details and does not serialize binary HID reports.
2. **ActionResolver:**
   - Resolves semantic/platform-aware actions based on the current context.
3. **ActionDispatcher / Execution:**
   - Converts the resolved actions into the required binary HID report payload (as defined in `POCKETHID-GOLDEN-VECTORS.md`) or invokes the appropriate report-building path.
4. **iOS BridgeTransport (Swift / CoreBluetooth):**
   - Serves strictly as a transport boundary implementing the `HidTransport` interface.
   - Receives the already-resolved/serialized HID report payload and does not inspect semantic actions.
   - Handles bridge-specific wire framing, MTU negotiation, and dispatching to the ESP32-S3 via CoreBluetooth.
5. **BLE Custom GATT (Air Interface):**
   - A dedicated GATT Service and TX/RX Characteristic exposed by the ESP32-S3 to transmit raw frames.
6. **External HID Bridge (ESP32-S3 Hardware):**
   - Acts as a BLE GATT Server. Receives packets, deframes them if necessary, and forwards the exact HID report payloads to the PC over native USB HID.

---

## 3. BridgeTransport State Model

The iOS `BridgeTransport` adheres to a strict semantic state machine. The required states and valid transitions are:

- **`DISCONNECTED`**: Default state. Bluetooth is off, or no connection is active.
- **`CONNECTING`**: Establishing BLE link, discovering GATT services/characteristics, and negotiating MTU.
- **`CONNECTED`**: Link established, characteristics subscribed, and bridge explicitly acknowledges readiness. 
  *(Rule: Do NOT fake the `CONNECTED` state. The UI must only reflect `CONNECTED` when the GATT link is physically confirmed.)*
- **`DISCONNECTING`**: The transitional state prior to closing the physical link. **Why it exists:** It provides the critical lifecycle window required for flushing neutral/safety reports (e.g., releasing all keys/buttons) to the host before the connection is severed.
- **`ERROR`**: Connection dropped, GATT discovery failed, MTU too small, or Bluetooth disabled.

**Valid Transitions:**
- `DISCONNECTED` → `CONNECTING`
- `CONNECTING` → `CONNECTED`
- `CONNECTING` → `ERROR`
- `CONNECTED` → `DISCONNECTING`
- `DISCONNECTING` → `DISCONNECTED`
- *(Any transport failure)* → `ERROR`

---

## 4. Connection Lifecycle

1. **Connect:** Core requests a connection. `BridgeTransport` initiates connection via CoreBluetooth. Upon discovering the custom HID Service and RX Characteristic, it transitions to `CONNECTED`.
2. **Disconnect:** Core requests disconnection. `BridgeTransport` transitions to `DISCONNECTING`. Neutral safety reports MUST be sent during this window. Once flushed, it explicitly cancels the peripheral connection and transitions to `DISCONNECTED`.
3. **Error Handling:** If the bridge stops responding or a CoreBluetooth error occurs, `BridgeTransport` immediately transitions to `ERROR`, ceases transmission, and safely cleans up CoreBluetooth references.

---

## 5. Packet Framing and Protocol Boundary

- **Protocol Preservation:** The iOS BridgeTransport utilizes the exact binary report structures defined in `POCKETHID-BRIDGE-WIRE-PROTOCOL.md`.
- **Framing Ownership & Opacity:** To prevent leaking BLE/GATT implementation details into the Core/Domain, and to prevent the BridgeTransport from introspecting opaque HID payloads:
  - The **ActionDispatcher** provides the raw, opaque HID report payload.
  - The **BridgeTransport** is solely responsible for encapsulating this payload within the bridge wire envelope (e.g., adding bridge-specific routing fields, endpoint IDs, or custom framing) before BLE transmission.
- **GATT MTU Constraints:** The Transport queries the BLE MTU. If the fully framed packet (wire envelope + HID report payload) exceeds the MTU, the `BridgeTransport` is responsible for safe fragmentation (if the custom GATT protocol requires it) or dropping with an error.
- **Mapping:** 
  - `PocketAction` → `ActionResolver` → `ActionDispatcher` (HID Payload) → `BridgeTransport` (Wire Envelope) → `CoreBluetooth WriteValue (WithoutResponse preferred)`.

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
- State machine transitions (`DISCONNECTED` → `CONNECTING` → `CONNECTED` → `DISCONNECTING` → `DISCONNECTED`).
- Error propagation and lifecycle management.
- Protocol payload encapsulation (ensuring the opaque HID payload is correctly wrapped in the bridge wire envelope without content inspection).
- Neutral report injection during the `DISCONNECTING` state window.
- GATT MTU segmentation logic (if implemented).

### B. Hardware-Dependent (Currently `BLOCKED` / `PENDING`)
Because the physical ESP32-S3 validation is `BLOCKED`, the following iOS BridgeTransport elements remain physically unverified:
- `PENDING`: Real-world CoreBluetooth discovery latency of the ESP32-S3.
- `PENDING`: Actual BLE connection stability and GATT MTU negotiation.
- `PENDING`: End-to-end latency (iOS touch -> BLE -> ESP32-S3 -> USB -> Windows OS).
- `BLOCKED`: End-to-end correctness of host-input physical verification.
