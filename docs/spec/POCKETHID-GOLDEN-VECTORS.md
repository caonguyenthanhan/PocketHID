# PocketHID — Golden Vector Lock
**Wave 10 — Hardware-Blocked Engineering Freeze**

> **Status: LOCKED**
> These vectors are the bridge interoperability contract.
> They MUST NOT be changed without a corresponding firmware rebuild, re-verification, and new SHA-256 manifest update.
>
> Source of truth: `firmware/esp32s3-bridge/tools/host_tests.c` → `test_golden_vector_packets()`
> All 6 vectors verified: 59/59 host unit tests PASS (Wave 8, commit `160fddfa`)

---

## Packet Structure Reference

Every golden packet follows the 7-byte header + payload + 2-byte CRC-16-CCITT structure:

```
Byte 0-1  : MAGIC = 0x5A 0xA5  (little-endian 0xA55A)
Byte 2    : VERSION = 0x01
Byte 3    : MSG_TYPE
Byte 4-5  : SEQUENCE_NO (little-endian uint16)
Byte 6    : PAYLOAD_LEN
Byte 7..N : PAYLOAD
Byte N+1..N+2 : CRC-16-CCITT (little-endian, over bytes 2..N)
```

All multi-byte integers: **Little-Endian** (`<stdint.h>` standard).

---

## Report ID Map (USB HID Contract)

| Report ID | Device | Report Size | Neutral State |
|---|---|---|---|
| `0x01` | Keyboard | 8 bytes | 8 × `0x00` |
| `0x02` | Mouse | 4 bytes | 4 × `0x00` |
| `0x03` | Consumer Control | 2 bytes | 2 × `0x00` |
| `0x04` | Gamepad | 13 bytes | all zero (sticks at 0, not center) |
| `0x05` | Tablet / Digitizer | 5 bytes | 5 × `0x00` (Tip=0, InRange=0) |

---

## Vector 1 — Keyboard A

**Semantic meaning:** Key 'a' pressed, no modifiers.

### Wire Packet

```
Seq: 1  MSG_TYPE: 0x10 (MSG_KB_REPORT)  PAYLOAD_LEN: 8
Hex: 5A A5 01 10 01 00 08 00 00 04 00 00 00 00 00 28 AD
     [MAGIC   ] VR MT [SEQ  ] PL [------ PAYLOAD ------] [CRC ]
```

**Annotated:**
```
5A A5       MAGIC (0xA55A LE)
01          VERSION = 1
10          MSG_TYPE = MSG_KB_REPORT
01 00       SEQUENCE_NO = 1
08          PAYLOAD_LEN = 8
---- payload (8 bytes) ----
00          modifiers = 0x00 (no modifier)
00          reserved = 0x00
04          keycode[0] = 0x04 (HID Usage 0x07/0x04 = 'a')
00 00 00    keycode[1..3] = 0x00
00 00       keycode[4..5] = 0x00
---- checksum ----
28 AD       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `header.magic` | `0xA55A` |
| `header.version` | `0x01` |
| `header.msg_type` | `0x10` (MSG_KB_REPORT) |
| `header.sequence_no` | `1` |
| `header.payload_len` | `8` |
| `payload.modifiers` | `0x00` |
| `payload.reserved` | `0x00` |
| `payload.keycodes[0]` | `0x04` ('a') |
| `payload.keycodes[1..5]` | `0x00` |

### Expected USB HID Report (Report ID 1)

```
Report ID: 01
Byte 0 (modifiers): 00
Byte 1 (reserved) : 00
Byte 2 (keycode 0): 04
Byte 3 (keycode 1): 00
Byte 4 (keycode 2): 00
Byte 5 (keycode 3): 00
Byte 6 (keycode 4): 00
Byte 7 (keycode 5): 00
```

**Host observable effect:** Letter `a` typed in focused text input.

### Source

`host_tests.c:219-227` — `test_golden_vector_packets()` golden_kb_a

---

## Vector 2 — Shift+A

**Semantic meaning:** Left Shift held, key 'a' pressed → uppercase 'A'.

### Wire Packet

```
Seq: 2  MSG_TYPE: 0x10 (MSG_KB_REPORT)  PAYLOAD_LEN: 8
Hex: 5A A5 01 10 02 00 08 02 00 04 00 00 00 00 00 74 5A
```

**Annotated:**
```
5A A5       MAGIC
01          VERSION = 1
10          MSG_TYPE = MSG_KB_REPORT
02 00       SEQUENCE_NO = 2
08          PAYLOAD_LEN = 8
---- payload ----
02          modifiers = 0x02 (Left Shift = Bit 1)
00          reserved
04          keycode[0] = 0x04 ('a')
00 00 00 00 00  keycodes[1..5] = 0x00
---- checksum ----
74 5A       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `payload.modifiers` | `0x02` (LShift) |
| `payload.keycodes[0]` | `0x04` ('a') |

### Expected USB HID Report (Report ID 1)

```
Byte 0: 02  (LShift asserted)
Byte 1: 00
Byte 2: 04  (keycode 'a')
Bytes 3..7: 00
```

**Host observable effect:** Letter `A` (uppercase) typed.

### Source

`host_tests.c:230-237`

---

## Vector 3 — Mouse Left Click

**Semantic meaning:** Left mouse button pressed, no movement, no scroll.

### Wire Packet

```
Seq: 3  MSG_TYPE: 0x20 (MSG_MOUSE_REPORT)  PAYLOAD_LEN: 4
Hex: 5A A5 01 20 03 00 04 01 00 00 00 09 AF
```

**Annotated:**
```
5A A5       MAGIC
01          VERSION
20          MSG_TYPE = MSG_MOUSE_REPORT
03 00       SEQUENCE_NO = 3
04          PAYLOAD_LEN = 4
---- payload ----
01          buttons = 0x01 (Left button = Bit 0)
00          dx = 0
00          dy = 0
00          wheel = 0
---- checksum ----
09 AF       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `payload.buttons` | `0x01` (Left click) |
| `payload.dx` | `0` |
| `payload.dy` | `0` |
| `payload.wheel` | `0` |

### Expected USB HID Report (Report ID 2)

```
Byte 0: 01  (Left button bit set, 5-bit padding = 0)
Byte 1: 00  (dx = 0)
Byte 2: 00  (dy = 0)
Byte 3: 00  (wheel = 0)
```

**Host observable effect:** Left click registered on host — UI element selected/activated.

### Source

`host_tests.c:240-247`

---

## Vector 4 — Volume+

**Semantic meaning:** Consumer control Volume Increment.

### Wire Packet

```
Seq: 4  MSG_TYPE: 0x30 (MSG_CONSUMER_CLICK)  PAYLOAD_LEN: 2
Hex: 5A A5 01 30 04 00 02 E9 00 6F 2B
```

**Annotated:**
```
5A A5       MAGIC
01          VERSION
30          MSG_TYPE = MSG_CONSUMER_CLICK
04 00       SEQUENCE_NO = 4
02          PAYLOAD_LEN = 2
---- payload ----
E9 00       usage_code = 0x00E9 (Volume Increment, LE)
---- checksum ----
6F 2B       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `payload.usage_code` | `0x00E9` (Volume Increment, Consumer Usage Page 0x0C) |

### Expected USB HID Report (Report ID 3)

```
Byte 0: E9  (usage_code low byte)
Byte 1: 00  (usage_code high byte)
```

**Host observable effect:** System volume increases — OS volume OSD visible on screen.

### Source

`host_tests.c:250-257`

---

## Vector 5 — Gamepad A

**Semantic meaning:** Button A pressed (bit 0), all other buttons/axes neutral.

### Wire Packet

```
Seq: 5  MSG_TYPE: 0x40 (MSG_GAMEPAD_REPORT)  PAYLOAD_LEN: 13
Hex: 5A A5 01 40 05 00 0D 01 00 00 00 00 00 00 00 00 00 00 00 00 00 A4 CD
```

**Annotated:**
```
5A A5       MAGIC
01          VERSION
40          MSG_TYPE = MSG_GAMEPAD_REPORT
05 00       SEQUENCE_NO = 5
0D          PAYLOAD_LEN = 13
---- payload (13 bytes) ----
01 00       buttons = 0x0001 (Button A = Bit 0, LE)
00          hat_switch = 0x00 (Neutral / Centered)
00 00       left_stick_x = 0 (LE int16)
00 00       left_stick_y = 0 (LE int16)
00 00       right_stick_x = 0
00 00       right_stick_y = 0
00          left_trigger = 0
00          right_trigger = 0
---- checksum ----
A4 CD       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `payload.buttons` | `0x0001` (Button A) |
| `payload.hat_switch` | `0x00` (Neutral) |
| `payload.left_stick_x` | `0` |
| `payload.left_stick_y` | `0` |
| `payload.right_stick_x` | `0` |
| `payload.right_stick_y` | `0` |
| `payload.left_trigger` | `0` |
| `payload.right_trigger` | `0` |

### Expected USB HID Report (Report ID 4)

```
Bytes 0..1 : 01 00  (buttons = 0x0001, Button A)
Byte  2    : 00     (hat = neutral)
Bytes 3..4 : 00 00  (LX = 0)
Bytes 5..6 : 00 00  (LY = 0)
Bytes 7..8 : 00 00  (RX = 0)
Bytes 9..10: 00 00  (RY = 0)
Byte  11   : 00     (LT = 0)
Byte  12   : 00     (RT = 0)
```

**Host observable effect:** Button A lit/registered in gamepad tester (gamepad-tester.com or joy.cpl).

### Source

`host_tests.c:260-267`

---

## Vector 6 — Tablet Center

**Semantic meaning:** Pen tip touching, in range, at center of digitizer (16384, 16384 of 0..32767 range).

### Wire Packet

```
Seq: 6  MSG_TYPE: 0x50 (MSG_TABLET_REPORT)  PAYLOAD_LEN: 5
Hex: 5A A5 01 50 06 00 05 03 00 40 00 40 2A FA
```

**Annotated:**
```
5A A5       MAGIC
01          VERSION
50          MSG_TYPE = MSG_TABLET_REPORT
06 00       SEQUENCE_NO = 6
05          PAYLOAD_LEN = 5
---- payload (5 bytes) ----
03          status = 0x03 (Bit 0: Tip Switch = 1, Bit 1: In Range = 1)
00 40       x = 0x4000 = 16384 (LE uint16)
00 40       y = 0x4000 = 16384 (LE uint16)
---- checksum ----
2A FA       CRC-16-CCITT (LE)
```

### Expected Parse

| Field | Expected Value |
|---|---|
| `payload.status` | `0x03` (Tip=1, InRange=1) |
| `payload.x` | `16384` (center of 0..32767) |
| `payload.y` | `16384` (center of 0..32767) |

### Expected USB HID Report (Report ID 5)

```
Byte 0    : 03     (Tip Switch + In Range bits set)
Bytes 1..2: 00 40  (X = 16384, LE)
Bytes 3..4: 00 40  (Y = 16384, LE)
```

**Host observable effect:** Pen cursor at center of screen in digitizer mode.

### Source

`host_tests.c:270-277`

---

## Vector 7 — Full Neutral (All Endpoints Released)

**Semantic meaning:** All inputs released — emitted after watchdog timeout or BLE disconnect.

### Neutral USB HID Reports (one per endpoint, all transmitted on neutralization)

| Report ID | Device | Neutral Bytes |
|---|---|---|
| `0x01` | Keyboard | `00 00 00 00 00 00 00 00` |
| `0x02` | Mouse | `00 00 00 00` |
| `0x03` | Consumer | `00 00` |
| `0x04` | Gamepad | `00 00 00 00 00 00 00 00 00 00 00 00 00` |
| `0x05` | Tablet | `00 00 00 00 00` (Tip=0, InRange=0) |

### Safety Contract

Per `safety_manager.h` — `DEFAULT_WATCHDOG_TIMEOUT_MS = 250`:
- Neutral flush fires after **≥250 ms** of no valid packets received.
- Neutral flush fires **immediately** on `BLE_GAP_EVENT_DISCONNECT`.
- Neutral flush is **idempotent** — repeated calls do not retransmit if already neutral.

### Source

`safety_manager.h:15` — `DEFAULT_WATCHDOG_TIMEOUT_MS 250`
`safety_manager.c` — `safety_manager_force_all_neutral()`
`host_tests.c:129-153` — `test_safety_manager_watchdog_timeout()`

---

## Contract Lock Summary

| Contract | Status |
|---|---|
| MAGIC = `0xA55A` | LOCKED |
| VERSION = `0x01` | LOCKED |
| Header size = 7 bytes | LOCKED |
| CRC algorithm = CRC-16-CCITT | LOCKED |
| Report ID 1 (Keyboard, 8B) | LOCKED |
| Report ID 2 (Mouse, 4B) | LOCKED |
| Report ID 3 (Consumer, 2B) | LOCKED |
| Report ID 4 (Gamepad, 13B) | LOCKED |
| Report ID 5 (Tablet, 5B) | LOCKED |
| Watchdog timeout = 250 ms | LOCKED |
| Malformed packet reject threshold = 3 | LOCKED |
| Byte order = Little-Endian | LOCKED |
| Keycode 'a' = HID 0x07/0x04 | LOCKED |
| LShift modifier = 0x02 | LOCKED |
| Vol+ usage = Consumer 0x00E9 | LOCKED |
| Gamepad Button A = bit 0 of buttons uint16 | LOCKED |
| Tablet center = (16384, 16384) of (0..32767) | LOCKED |

---

> **Change Policy:** Any modification to a locked contract requires:
> 1. Firmware source change
> 2. Re-run host unit tests (must be 59/59 PASS)
> 3. Firmware rebuild (`idf.py build` + `idf.py merge-bin`)
> 4. New SHA-256 hashes recorded in FLASH-MANIFEST.md
> 5. This document updated with new vectors

---

*Last updated: Wave 10 — 2026-09-29*
*Source commit: `f0cf1a3db507e86b8d57c0c9a9abe800b045d3aa`*
*Firmware commit: `160fddfa6763989cebe0aca67efe2d2e1dbdc65a`*
