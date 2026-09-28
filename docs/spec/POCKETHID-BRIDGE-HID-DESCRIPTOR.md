# PocketHID Bridge HID Descriptor & Host Output Specification

**Specification Version:** `1.0.0-DRAFT`  
**Status:** ARCHITECTURAL SPECIFICATION — NO IMPLEMENTATION YET  
**Reference Source:** Android [`HidConstants.kt`](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/model/HidConstants.kt) & [`BtHidTransport.kt`](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt)  

---

## 1. Overview & Contract Alignment

To ensure 100% behavioral parity between PocketHID Android and the external bridge used for iOS, the bridge microcontroller firmware MUST expose the exact same composite USB/Bluetooth HID Report Descriptor contract already proven on Android.

By retaining identical Report IDs, field offsets, bitmasks, logical bounds, and physical ranges, the host operating system (Windows, macOS, Linux) processes inputs identically regardless of whether the mobile client is Android or an iPhone communicating through the hardware bridge.

---

## 2. Verified Report Descriptor Map

The composite descriptor unites 5 distinct top-level application collections under a unified interface using 1-byte Report IDs:

| Report ID | Device Mode | Usage Page | Top-Level Usage | Report Size (Bytes) | Input Type |
|---|---|---|---|---|---|
| **`0x01`** | **Keyboard** | `0x01` (Generic Desktop) / `0x07` (Keyboard) | `0x06` (Keyboard) | 8 | Absolute 6KRO + Modifiers |
| **`0x02`** | **Mouse** | `0x01` (Generic Desktop) / `0x09` (Button) | `0x02` (Mouse) | 4 | Relative Motion (dx, dy, wheel) |
| **`0x03`** | **Consumer** | `0x0C` (Consumer Devices) | `0x01` (Consumer Control) | 2 | 16-bit Array Usage Code |
| **`0x04`** | **Gamepad** | `0x01` (Generic Desktop) / `0x09` (Button) | `0x05` (Gamepad) | 13 | 16 Buttons, 8-way Hat, 4 Axes, 2 Triggers |
| **`0x05`** | **Tablet** | `0x0D` (Digitizers) | `0x02` (Pen / Stylus) | 5 | Status Bits + 16-bit Absolute X, Y |

---

## 3. Detailed HID Report Definitions

### 3.1 Report ID 1 — Keyboard (8 Bytes)

Exposed as a standard USB Boot Keyboard compatible descriptor:

```
Byte 0: Modifier Keys Bitmask
        Bit 0: Left Control   (0x01)
        Bit 1: Left Shift     (0x02)
        Bit 2: Left Alt       (0x04)
        Bit 3: Left GUI / Win (0x08)
        Bit 4: Right Control  (0x10)
        Bit 5: Right Shift    (0x20)
        Bit 6: Right Alt      (0x40)
        Bit 7: Right GUI      (0x80)
Byte 1: Reserved byte (Must be 0x00 for standard HID compliance)
Byte 2: Keycode Slot 0 (Usage Page 0x07, e.g. 0x04 = 'a', 0x28 = Enter)
Byte 3: Keycode Slot 1
Byte 4: Keycode Slot 2
Byte 5: Keycode Slot 3
Byte 6: Keycode Slot 4
Byte 7: Keycode Slot 5
```

- **Logical Minimum:** `0x00`, **Logical Maximum:** `0x65` (101 keys).
- **Rollover:** Standard 6-Key Rollover (6KRO). Neutral report is 8 bytes of `0x00`.

---

### 3.2 Report ID 2 — Mouse (4 Bytes)

Exposed as a 3-button relative mouse pointer:

```
Byte 0: Button Bitmask (Bits 0..2) + 5 Bits Constant Padding
        Bit 0: Primary Button (Left Click)   (0x01)
        Bit 1: Secondary Button (Right Click)(0x02)
        Bit 2: Tertiary Button (Middle Click)(0x04)
        Bits 3..7: Padding (0)
Byte 1: Relative X Delta (dx): Signed 8-bit (-127 to +127)
Byte 2: Relative Y Delta (dy): Signed 8-bit (-127 to +127)
Byte 3: Relative Wheel Delta:  Signed 8-bit (-127 to +127)
```

- **Usage:** Generic Desktop Pointer (`Usage 0x01`) inside Mouse (`Usage 0x02`).
- **Neutral State:** 4 bytes of `0x00`.

---

### 3.3 Report ID 3 — Consumer Control (2 Bytes)

Exposed as a 16-bit Array Input collection for media and presentation remote keys:

```
Bytes 0..1: 16-bit Consumer Usage Code (Little-Endian)
            0x00E9: Volume Increment (Vol +)
            0x00EA: Volume Decrement (Vol -)
            0x00E2: Mute
            0x00CD: Play / Pause
            0x00B5: Scan Next Track
            0x00B6: Scan Previous Track
            0x00B7: Stop
```

- **Logical Minimum:** `0x0000`, **Logical Maximum:** `0x03FF`.
- **Neutral State:** 2 bytes of `0x00` (`0x0000` released).

---

### 3.4 Report ID 4 — Gamepad (13 Bytes)

Exposed as a standard dual-stick analog gamepad:

```
Bytes 0..1: 16 Digital Buttons (16-bit Bitmask)
            Bit 0: Button A
            Bit 1: Button B
            Bit 2: Button X
            Bit 3: Button Y
            Bit 4: Left Bumper (LB)
            Bit 5: Right Bumper (RB)
            Bit 6: Back / View
            Bit 7: Start / Menu
            Bit 8: Guide / Home
            Bit 9: Left Stick Click (L3)
            Bit 10: Right Stick Click (R3)
            Bits 11..14: Directional Buttons (Up, Down, Left, Right)
            Bit 15: Reserved
Byte 2:     Hat Switch (D-Pad, 4 bits active, 4 bits padding)
            0 = Centered / Neutral
            1 = Up, 2 = Up-Right, 3 = Right, 4 = Down-Right,
            5 = Down, 6 = Down-Left, 7 = Left, 8 = Up-Left
Bytes 3..4: Left Stick X  (Signed 16-bit: -32768 to +32767, Little-Endian)
Bytes 5..6: Left Stick Y  (Signed 16-bit: -32768 to +32767, Little-Endian)
Bytes 7..8: Right Stick X (Usage Z, Signed 16-bit: -32768 to +32767)
Bytes 9..10:Right Stick Y (Usage Rz, Signed 16-bit: -32768 to +32767)
Byte 11:    Left Trigger  (Usage Rx, Unsigned 8-bit: 0 to 255)
Byte 12:    Right Trigger (Usage Ry, Unsigned 8-bit: 0 to 255)
```

- **Neutral State:**
  - Buttons = `0x0000`
  - Hat = `0x00`
  - Left Stick (X, Y) = `(0, 0)`
  - Right Stick (X, Y) = `(0, 0)`
  - Triggers (LT, RT) = `(0, 0)`

---

### 3.5 Report ID 5 — Absolute Digitizer / Tablet (5 Bytes)

Exposed as a Windows Ink / Absolute Stylus Digitizer:

```
Byte 0: Status Bitmask
        Bit 0: Tip Switch     (0x01) — Pen tip touching screen (Contact active)
        Bit 1: In Range       (0x02) — Pen in proximity sensing range
        Bit 2: Barrel Switch  (0x04) — Stylus side button
        Bit 3: Invert         (0x08) — Eraser end inverted
        Bit 4: Eraser         (0x10) — Eraser active
        Bits 5..7: Constant Padding (0)
Bytes 1..2: Absolute X Coordinate (Unsigned 16-bit: 0 to 32767, Little-Endian)
Bytes 3..4: Absolute Y Coordinate (Unsigned 16-bit: 0 to 32767, Little-Endian)
```

- **Usage:** Digitizers (`0x0D`), Pen (`0x02`), Stylus (`0x20`).
- **Logical Min / Max:** `0` to `32767`.
- **Physical Min / Max:** `0` to `32767`.
- **Neutral State:** 5 bytes of `0x00` (Tip Switch = 0, In Range = 0).

---

## 4. USB HID vs Bluetooth HID Bridge Comparison

The external hardware bridge can emit reports to the PC via two distinct physical interfaces. The architecture keeps both options open.

| Architectural Dimension | Option 1: USB HID Bridge (Dongle plugged into PC USB port) | Option 2: Bluetooth HID Bridge (Dongle powered via battery/USB, BT to PC) |
|---|---|---|
| **Physical Topology** | `iPhone` → *BLE* → `Bridge Dongle` → *USB Cable* → `PC` | `iPhone` → *BLE* → `Bridge Dongle` → *Classic BT HID* → `PC` |
| **PC Host Recognition** | True hardware USB Composite Device (`Device Manager` → `USB Input Device`) | Bluetooth HID Device (`Device Manager` → `Bluetooth HID`) |
| **Driverless / In-Box Support** | 100% Driverless across Windows, macOS, Linux, ChromeOS | 100% Driverless (standard OS Bluetooth stack) |
| **BIOS / UEFI Boot Operation** | **YES.** Functions during boot menu, BitLocker, and BIOS setup | **NO.** Bluetooth stack only initializes after OS boots |
| **PC Companion Software** | **NONE REQUIRED** | **NONE REQUIRED** |
| **Pairing Burden** | Zero PC pairing. Plug into USB port and operate immediately | Requires initial pairing sequence on the PC host |
| **Power Source** | Powered directly from PC USB 5V (No battery required) | Requires dedicated LiPo battery or separate USB power brick |
| **Host Latency** | **1.0 ms** (1000 Hz USB Full-Speed polling) | **11.25 ms – 20 ms** (Bluetooth HID sniff/connection intervals)|
| **Firmware Complexity** | Low (Standard TinyUSB composite device stack) | High (Dual-mode Bluetooth: BLE Central/Peripheral + Classic HID) |
| **RF Coexistence / Jitter** | None on bridge-to-PC link (Conducted copper USB) | High (Simultaneous BLE rx and Classic BT tx on 2.4 GHz radio) |
| **Recommendation** | **PRIMARY POC TARGET:** USB HID Bridge provides lowest latency, zero PC pairing, and maximum host stability | **SECONDARY ALTERNATIVE:** Investigate only if physical USB connection to PC is unacceptable |
