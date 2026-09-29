# BLE Physical Results
**PocketHID Wave 9R.1 — PATH B Closeout**

---

## Gate

> **BLE testing is BLOCKED until USB HID physical validation passes.**
>
> See: [USB-HID-PHYSICAL-RESULTS.md](./USB-HID-PHYSICAL-RESULTS.md)

### Gate Checklist (all must be PASS before BLE testing begins)

| Prerequisite | Status |
|---|---|
| USB HID enumeration physically verified | PENDING |
| Keyboard test physically verified | PENDING |
| Mouse test physically verified | PENDING |
| Consumer control test physically verified | PENDING |
| Gamepad test physically verified | PENDING |
| Tablet test physically verified | PENDING |

**BLE Gate Status: BLOCKED**

---

## Test Environment

> Fill in once hardware is connected and USB HID gate passes.

```
Hardware        :
Board revision  :
Serial port     :
Firmware file   : merged-binary.bin
Firmware SHA    : D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
Git commit      : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a
ESP-IDF version : v5.3.1
BLE test client : (e.g., nRF Connect for Desktop, bleak Python script)
BLE adapter     :
Host OS         :
Host OS build   :
ESP32 BLE MAC   :
Test date/time  :
```

---

## Status Summary

| Test | Status |
|---|---|
| BLE advertisement detected | BLOCKED |
| Service discovery | BLOCKED |
| RX characteristic found | BLOCKED |
| Packet write accepted | BLOCKED |
| Parser — valid golden packet | BLOCKED |
| Sequence handling | BLOCKED |
| Invalid packet rejection | BLOCKED |
| Disconnect callback | BLOCKED |
| Neutralization on disconnect | BLOCKED |
| BLE → USB → Windows end-to-end | BLOCKED |

---

## Scope Clarification

> ⚠️ This document validates **Custom BLE GATT** protocol only.
> It does **NOT** validate Bluetooth HID (HID over GATT / BT Classic HID).
> iOS BridgeTransport (which uses CoreBluetooth → custom GATT) is a **separate gate** not yet implemented.

---

## Phase 1 — BLE Advertisement Detection

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
BLE client      :
Expected        : Device "PocketHID" (or configured name) visible in BLE scan within 5s of boot
Actual          :
Evidence        : [paste nRF Connect scan result or bleak output]
Result          : BLOCKED
```

### Detection Commands

```python
# Option A: Python (pip install bleak)
import asyncio
from bleak import BleakScanner

async def scan():
    devices = await BleakScanner.discover(timeout=5.0)
    for d in devices:
        print(d.address, d.name, d.rssi)

asyncio.run(scan())
```

```
# Option B: nRF Connect for Desktop
# Open nRF Connect → Scanner tab → Scan → filter by name "PocketHID"
```

---

## Phase 2 — Service Discovery

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
BLE client      :
Expected GATT structure:
  Service: PocketHID Bridge Service
    Characteristic: RX (Write Without Response)
      Properties: WRITE, WRITE_WITHOUT_RESPONSE
    Characteristic: TX (Notify, if implemented)
      Properties: NOTIFY
Actual GATT tree:
  [paste output here]
RX characteristic found : YES / NO
Write permission        : YES / NO
Evidence        : [nRF Connect GATT tree screenshot / bleak service dump]
Result          : BLOCKED
```

---

## Phase 3 — Packet Write Tests

### Valid Packet Acceptance

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Test            : Write a valid keyboard-A golden packet to RX characteristic
Packet (hex)    : [from firmware poc_test.c golden packets]
Expected        : Packet accepted (no error response); serial log shows parse OK
Serial log      :
Evidence        :
Result          : BLOCKED
```

### Invalid Packet Rejection Tests

| Test | Hardware | Firmware | Host OS | Input | Expected | Actual | Evidence | Result |
|---|---|---|---|---|---|---|---|---|
| Packet too short | | merged-binary.bin | | `< min length` | Rejected, no USB report | | | BLOCKED |
| Wrong magic bytes | | merged-binary.bin | | wrong header | Rejected, no USB report | | | BLOCKED |
| Wrong sequence number | | merged-binary.bin | | out-of-order seq | Per spec behavior | | | BLOCKED |
| Oversized payload | | merged-binary.bin | | `> max length` | Rejected, no USB report | | | BLOCKED |
| Zero-length payload | | merged-binary.bin | | empty write | Rejected, no USB report | | | BLOCKED |

---

## Phase 4 — Sequence Handling

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Test A          : Send packets with sequence 0, 1, 2, 3 — all expected accepted
Test B          : Send duplicate sequence — record actual firmware behavior
Test C          : Send out-of-order sequence — record actual firmware behavior
Expected        : Per firmware sequence validation spec
Actual          :
Evidence        : Serial log
Result          : BLOCKED
```

---

## Phase 5 — Disconnect Callback & Neutralization

```
Hardware        :
Firmware        : merged-binary.bin
Host OS         :
Trigger         : Force BLE disconnect (turn off or close BLE test client)
Expected        : Disconnect callback fires → all inputs neutralized
Serial log      :
Host effect     : No stuck keys or mouse movement after disconnect
Measured time   : (record actual — do not assume)
Evidence        :
Result          : BLOCKED
```

---

## Phase 6 — BLE → USB → Windows End-to-End

> Run AFTER Phases 1-5 all PASS.
> Evidence = host-side observable effect (key typed, cursor moved, OSD shown).
> "BLE write acknowledged" is NOT sufficient evidence alone.

| Packet | Hardware | Firmware | Host OS | BLE RX | Parsed | USB Report | Host Effect | Result |
|---|---|---|---|---|---|---|---|---|
| Keyboard A | | merged-binary.bin | | | | | | BLOCKED |
| Shift+A | | merged-binary.bin | | | | | | BLOCKED |
| Mouse click | | merged-binary.bin | | | | | | BLOCKED |
| Volume+ | | merged-binary.bin | | | | | | BLOCKED |
| Gamepad A | | merged-binary.bin | | | | | | BLOCKED |
| Tablet center | | merged-binary.bin | | | | | | BLOCKED |

**BLE → USB End-to-End: BLOCKED**

---

## Evidence File Checklist

| Evidence | Status |
|---|---|
| BLE scan output | NOT COLLECTED |
| GATT tree / service discovery | NOT COLLECTED |
| Serial log — packet parsed | NOT COLLECTED |
| Serial log — disconnect callback | NOT COLLECTED |
| Host-side input effect | NOT COLLECTED |

---

## Wave 9R.1 Final Status

```
BLE GATE         : BLOCKED (USB HID not yet physically validated)
BLE ADVERTISED   : PENDING
BLE CONNECTED    : PENDING
BLE → USB        : PENDING
IOS BRIDGE       : NOT IMPLEMENTED
```

---

*Last updated: Wave 9R.1 — 2026-09-29*
