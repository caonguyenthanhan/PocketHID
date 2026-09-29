# Required Hardware for POC
**PocketHID Wave 10 — Hardware-Blocked Engineering Freeze**

---

## Purpose

This document defines the **minimum technical hardware capabilities** required to perform the physical validation sequence described in:

- [ESP32S3-PHYSICAL-TEST-HANDOFF.md](./ESP32S3-PHYSICAL-TEST-HANDOFF.md)
- [USB-HID-PHYSICAL-RESULTS.md](./USB-HID-PHYSICAL-RESULTS.md)
- [BLE-PHYSICAL-RESULTS.md](./BLE-PHYSICAL-RESULTS.md)
- [WINDOWS-HID-ENUMERATION.md](./WINDOWS-HID-ENUMERATION.md)

No specific brands or products are prescribed. Technical capabilities only.

---

## Device 1 — ESP32-S3 Development Board (REQUIRED for all stages)

| Capability | Requirement |
|---|---|
| Microcontroller | Espressif ESP32-S3 (must be **S3** — not S2, S0, C3, or C6) |
| USB interface | Native USB OTG capable port **OR** external USB-to-UART bridge chip |
| BLE radio | Bluetooth 5.0 Low Energy (built-in to ESP32-S3) |
| Flash capacity | ≥ 2 MB (firmware requires 633,696 bytes merged binary at 0x0) |
| PSRAM | Not required |
| Boot mode control | Must have access to BOOT/IO0 pin for download mode entry |
| Form factor | Any (DevKit, feather, breakout) — must expose USB and serial |

> **Note on USB interface type:**
> - If the board exposes **native USB OTG** (USB port connected directly to ESP32-S3 D+/D-), it will enumerate as VID `0x303A` (Espressif) when running firmware. The same port is used for both flashing and HID output.
> - If the board uses an **external UART bridge** (CP2102, CH340, FTDI), a separate USB port is used for flashing/serial, and native USB is used for HID output. Two USB ports are needed simultaneously.
> - Either topology is acceptable for this POC.

---

## Device 2 — USB Cable (REQUIRED — data capable)

| Capability | Requirement |
|---|---|
| Type | Must match the board's USB connector (typically USB-C or Micro-USB) |
| Data lines | **REQUIRED** — charge-only cables will not work |
| Length | Any reasonable length (under 3 m recommended) |

> **Critical:** Many cables marketed as "USB cables" carry only VBUS and GND (charging cables). A data-capable cable must carry all 4 conductors: VBUS, GND, D+, D−. If the board does not appear in Device Manager after connecting, try a different cable before troubleshooting firmware.

---

## Device 3 — Windows PC (REQUIRED for USB HID and Windows Device Manager stages)

| Capability | Requirement |
|---|---|
| Operating system | Windows 10 (build 19041+) or Windows 11 |
| USB port | USB 2.0 or USB 3.x port with working drivers |
| Device Manager access | Administrator privileges required to view HID device tree |
| PowerShell | Version 5.1+ (built into Windows 10/11) |
| Internet access | Optional — needed for gamepad-tester.com if used |
| Python 3.x | Required for esptool.py and host unit test runner |
| pip packages | `esptool` (`pip install esptool`) |

> **Note:** macOS or Linux can be used for flashing only. Windows is required for USB HID Device Manager verification (Phases 3–7 of the test sequence). BLE testing can be performed from any OS with `bleak` installed.

---

## Device 4 — BLE Test Client (REQUIRED for BLE stages — Stages 9–10)

One of the following is sufficient:

| Option | Software | Platform |
|---|---|---|
| A | nRF Connect for Desktop | Windows, macOS, Linux |
| B | Python + `bleak` library (`pip install bleak`) | Windows, macOS, Linux |
| C | nRF Connect mobile app | Android / iOS |

> No iPhone is required for BLE testing. The custom GATT protocol can be exercised by any generic BLE Central implementation. iPhone is only required for **Stage 11** (CoreBluetooth end-to-end), which is gated on all prior stages passing.

---

## Device 5 — iPhone (REQUIRED only for Stage 11 — iOS CoreBluetooth)

| Capability | Requirement |
|---|---|
| Device | iPhone or iPad with Bluetooth LE |
| OS version | iOS 15.0 or later |
| Xcode | Required to install development build |
| Developer account | Apple Developer account required for device provisioning |

> **Gate:** iPhone testing is the final stage. It is **blocked** until:
> - USB HID physical verification passes (Stages 1–8)
> - BLE GATT physical verification passes (Stage 9)
> - BLE → USB → Windows end-to-end passes (Stage 10)
>
> iOS BridgeTransport is currently NOT IMPLEMENTED.

---

## Minimum Viable Test Setup (Stages 1–8 only)

For the first hardware session (flash + boot + USB HID validation only):

1. ESP32-S3 development board
2. Data-capable USB cable
3. Windows PC with PowerShell and Python 3.x

That is all that is needed to validate Stages 1 through 8.

---

## Test Stage to Hardware Mapping

| Stage | Description | Hardware Required |
|---|---|---|
| 1 | ESP32-S3 boot | Board + USB cable + Windows PC |
| 2 | USB HID enumeration | Board + USB cable + Windows PC |
| 3 | USB keyboard | Board + USB cable + Windows PC |
| 4 | USB mouse | Board + USB cable + Windows PC |
| 5 | Consumer control | Board + USB cable + Windows PC |
| 6 | Gamepad | Board + USB cable + Windows PC |
| 7 | Tablet | Board + USB cable + Windows PC |
| 8 | Safety / neutralization | Board + USB cable + Windows PC |
| 9 | BLE GATT | Board + USB cable + Windows PC + BLE client |
| 10 | BLE → USB → Windows | Board + USB cable + Windows PC + BLE client |
| 11 | iPhone CoreBluetooth | All of above + iPhone + Xcode |

---

## What Is NOT Required

- External antenna (built-in PCB antenna on most dev boards is sufficient for short-range POC)
- Oscilloscope or logic analyzer (useful but not required for initial validation)
- Custom PCB or hardware modifications
- Apple MFi certification
- PC companion software
- Bluetooth dongle (PC built-in BLE adapter is sufficient for BLE test client)

---

*Last updated: Wave 10 — 2026-09-29*
