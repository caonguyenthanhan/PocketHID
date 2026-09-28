# PocketHID Bridge Wire Protocol Specification

**Specification Version:** `1.0.0-DRAFT`  
**Status:** ARCHITECTURAL SPECIFICATION — NO IMPLEMENTATION YET  
**Domain Alignment:** PocketHID iOS / Android Domain Actions → External Microcontroller Bridge  

---

## 1. Overview & Architectural Role

The PocketHID Bridge Wire Protocol defines the binary over-the-air communication layer between the PocketHID iOS application (acting as a standard Bluetooth Low Energy Central) and an external hardware bridge microcontroller (acting as a BLE Peripheral / GATT Server and a native USB/Bluetooth HID host interface).

```
┌────────────────────────────────┐
│          iPhone (iOS)          │
│       PocketHID Application    │
│  [UI] → [PocketAction] → [Tx]  │
└───────────────┬────────────────┘
                │
                │ BLE Custom GATT Stream (Fast, Binary, Authenticated)
                │
┌───────────────▼────────────────┐
│    External Hardware Bridge    │
│  [GATT Rx] → [Parser/Safety]   │
│  [HID Report Builder Engine]   │
└───────────────┬────────────────┘
                │
                │ Native USB Composite HID (1000 Hz / Driverless)
                │
┌───────────────▼────────────────┐
│        Target Host PC          │
│   Windows / macOS / Linux      │
│ (Sees Standard Physical HID)   │
└────────────────────────────────┘
```

The protocol serves as a transparent, high-throughput, low-latency conduit. It strictly preserves the existing PocketHID semantic action hierarchy without embedding UI state or phone-specific abstractions into the wire frame.

---

## 2. Protocol Fundamentals

- **Byte Order:** Little-Endian (`<stdint.h>` standard: least significant byte transmitted first for all multi-byte fields: `uint16_t`, `int16_t`, `uint32_t`). This matches the native endianness of ARM Cortex-M, Xtensa (ESP32), x86_64, and Apple Silicon.
- **Maximum Transmission Unit (MTU):**
  - Default BLE MTU: 23 bytes (Attribute payload: 20 bytes).
  - Target Negotiated BLE MTU: 64 to 247 bytes (ATT MTU exchange initiated upon connection).
  - Standard PocketHID Frame Size: 8 to 24 bytes (fits entirely within a single BLE notification packet, preventing fragmentation).
- **Transport Reliability:**
  - Continuous streaming messages (Mouse movement, Tablet touch stream, Gamepad analog axes): BLE Write Without Response (`CBCharacteristicWriteWithoutResponse`) or Unacknowledged Notifications for lowest latency (< 5 ms).
  - Discrete state-critical messages (Handshake, Session Auth, Host Mode change): BLE Write With Response or application-level ACK.

---

## 3. Packet Framing & Header Structure

Every packet transmitted across the BLE GATT characteristic shares an identical 6-byte fixed header followed by a variable-length payload:

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          MAGIC (0xA55A)       |  VERSION (1)  |  MSG_TYPE     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          SEQUENCE_NO          |  PAYLOAD_LEN  |  PAYLOAD ...  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  ... PAYLOAD CONTINUED ...    |    CHECKSUM (CRC-16-CCITT)    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### Header Fields:

| Byte Offset | Field | Type | Description |
|---|---|---|---|
| `0x00 - 0x01` | `MAGIC` | `uint16_t` | Constant sync word: `0xA55A` (0x5A, 0xA5 in little-endian order) |
| `0x02` | `VERSION` | `uint8_t` | Protocol version (`0x01` for v1.0.0) |
| `0x03` | `MSG_TYPE` | `uint8_t` | Message category & payload definition identifier |
| `0x04 - 0x05` | `SEQUENCE_NO` | `uint16_t` | Monotonically increasing packet sequence counter (rolls over at 65535) |
| `0x06` | `PAYLOAD_LEN` | `uint8_t` | Length of payload in bytes (0 to 64) |
| `0x07 ... N` | `PAYLOAD` | `uint8_t[]` | Message-specific data bytes |
| `N+1 ... N+2` | `CHECKSUM` | `uint16_t` | CRC-16-CCITT calculated over `VERSION` through end of `PAYLOAD` |

---

## 4. Message Types & Payload Specifications

### 4.1 Category: SYSTEM / SESSION

#### `0x01` — `MSG_SYS_HELLO` (iPhone → Bridge)
Initiates the handshake and requests authentication.
- Payload Length: 12 bytes
- Payload Layout:
  - `uint32_t client_random_nonce` (Random challenge for anti-replay)
  - `uint16_t client_protocol_ver` (`0x0100`)
  - `uint16_t client_capabilities` (Bitmask of enabled modes: KB, Mouse, Gamepad, Tablet)
  - `uint32_t client_uptime_ms`

#### `0x02` — `MSG_SYS_READY` (Bridge → iPhone)
Bridge acknowledgment of session handshake.
- Payload Length: 8 bytes
- Payload Layout:
  - `uint32_t bridge_random_nonce`
  - `uint16_t bridge_firmware_ver`
  - `uint8_t  usb_host_status` (`0x01` = USB enumerated & active, `0x00` = USB suspended/unplugged)
  - `uint8_t  active_hid_endpoints` (Bitmask of exposed HID interfaces)

#### `0x03` — `MSG_SYS_HEARTBEAT` (Bidirectional)
Keeps the link alive during periods of input silence.
- Payload Length: 4 bytes
- Payload Layout:
  - `uint32_t timestamp_ms`

#### `0x04` — `MSG_SYS_EMERGENCY_NEUTRAL` (iPhone → Bridge)
Instantly forces the bridge to zero all active keys, buttons, sticks, and digitizer contacts.
- Payload Length: 0 bytes

---

### 4.2 Category: KEYBOARD

#### `0x10` — `MSG_KB_REPORT` (iPhone → Bridge)
Directly maps to standard USB 8-byte Keyboard Input Report (HID Report ID 1).
- Payload Length: 8 bytes
- Payload Layout:
  - `uint8_t modifiers`: Bitmask (Bit 0: LCtrl, Bit 1: LShift, Bit 2: LAlt, Bit 3: LGUI, Bit 4: RCtrl, Bit 5: RShift, Bit 6: RAlt, Bit 7: RGUI)
  - `uint8_t reserved`: Must be `0x00`
  - `uint8_t keycodes[6]`: Up to 6 simultaneous HID usage codes (Usage Page 0x07, e.g. 0x04 = Key 'A')

#### `0x11` — `MSG_KB_RELEASE_ALL` (iPhone → Bridge)
Emits an empty 8-byte keyboard report to clear all pressed keys.
- Payload Length: 0 bytes

---

### 4.3 Category: MOUSE

#### `0x20` — `MSG_MOUSE_REPORT` (iPhone → Bridge)
Directly maps to standard USB 4-byte Relative Mouse Input Report (HID Report ID 2).
- Payload Length: 4 bytes
- Payload Layout:
  - `uint8_t buttons`: Bitmask (Bit 0: Left, Bit 1: Right, Bit 2: Middle)
  - `int8_t   dx`: Relative horizontal displacement (-127 to 127)
  - `int8_t   dy`: Relative vertical displacement (-127 to 127)
  - `int8_t   wheel`: Relative vertical scroll steps (-127 to 127)

---

### 4.4 Category: CONSUMER CONTROL (MEDIA & PRESENTER)

#### `0x30` — `MSG_CONSUMER_CLICK` (iPhone → Bridge)
Sends a consumer control pulse (press followed by release).
- Payload Length: 2 bytes
- Payload Layout:
  - `uint16_t usage_code`: Consumer Usage (Usage Page 0x0C, e.g. `0x00E9` = Vol+, `0x00EA` = Vol-, `0x00CD` = Play/Pause)

---

### 4.5 Category: GAMEPAD

#### `0x40` — `MSG_GAMEPAD_REPORT` (iPhone → Bridge)
Directly maps to standard 13-byte Gamepad Input Report (HID Report ID 4).
- Payload Length: 13 bytes
- Payload Layout:
  - `uint16_t buttons`: 16 digital buttons (A, B, X, Y, LB, RB, Back, Start, Guide, L3, R3, D-pad)
  - `uint8_t  hat_switch`: 4-bit hat direction (0 = neutral, 1 = Up, 2 = Up-Right ... 8 = Up-Left) + 4-bit padding
  - `int16_t  left_stick_x`: Signed axis (-32768 to 32767)
  - `int16_t  left_stick_y`: Signed axis (-32768 to 32767)
  - `int16_t  right_stick_x`: Signed axis (-32768 to 32767)
  - `int16_t  right_stick_y`: Signed axis (-32768 to 32767)
  - `uint8_t  left_trigger`: Analog trigger (0 to 255)
  - `uint8_t  right_trigger`: Analog trigger (0 to 255)

---

### 4.6 Category: ABSOLUTE DIGITIZER / GRAPHICS TABLET

#### `0x50` — `MSG_TABLET_REPORT` (iPhone → Bridge)
Directly maps to 5-byte Absolute Tablet Report (HID Report ID 5).
- Payload Length: 5 bytes
- Payload Layout:
  - `uint8_t  status`: Bit 0 = Tip Switch (touch active), Bit 1 = In Range, Bit 2 = Barrel Switch, Bit 4 = Eraser
  - `uint16_t x`: Absolute coordinate (0 to 32767)
  - `uint16_t y`: Absolute coordinate (0 to 32767)

---

## 5. Input Safety & Fail-Safe Neutralization

### 5.1 The Stuck Key Threat Model
In software-controlled input devices, physical keys never physically "unpress" themselves if communication is interrupted while a key down packet was the last received transmission. Left unmitigated, a user typing `Ctrl+W` or `Win+D` who walks out of BLE range or receives a phone call would leave `Ctrl` or `Win` permanently asserted on the target PC, causing catastrophic unintended host behavior.

### 5.2 Mandatory Hardware Watchdogs on Bridge
The bridge firmware MUST implement a hardware/software safety watchdog timer:

1. **Heartbeat / Stream Watchdog (Timeout: 250 ms):**
   - If no valid packet of any type is received within 250 ms, the bridge automatically enters `FAILSAFE_DECAY` state.
   - The bridge immediately builds and transmits neutral HID reports across all active endpoints:
     - Keyboard: 8 zero bytes (all keys and modifiers released).
     - Mouse: 4 zero bytes (all buttons released, zero movement).
     - Consumer: 2 zero bytes (no media keys held).
     - Gamepad: Buttons = 0, Hat = 0, Left/Right Sticks = (0, 0), Triggers = 0.
     - Tablet: Status = 0 (Tip Switch = 0, In Range = 0).
2. **BLE Link Termination Hook:**
   - Upon `BLE_GAP_EVENT_DISCONNECT` interrupt, the bridge cancels all pending reports and synchronously flushes neutral HID reports across all USB endpoints within 5 ms.
3. **Sequence & Malformed Packet Rejection:**
   - Any packet with invalid CRC, incorrect `MAGIC`, or unsupported `VERSION` is immediately discarded without updating input states. If 3 consecutive malformed packets arrive, the bridge forces a neutral state and requests re-authentication.

---

## 6. Endianness & Data Alignment Verification

To ensure zero-copy deserialization on the microcontroller:
- All struct definitions must be packed (`__attribute__((packed))` in C / `#pragma pack(push, 1)`).
- Endianness is strictly Little-Endian:
  ```c
  uint16_t magic = (buffer[0]) | (buffer[1] << 8); // 0xA55A
  ```
- No bitfields spanning byte boundaries may be used to avoid compiler-dependent layout variances.
