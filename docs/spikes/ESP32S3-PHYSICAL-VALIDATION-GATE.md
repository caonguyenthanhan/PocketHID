# ESP32-S3 Physical Validation Gate

**Document ID:** `docs/spikes/ESP32S3-PHYSICAL-VALIDATION-GATE.md`  
**Diagnostic Wave:** Wave 12.4  
**Target Device:** ESP32-S3 (PocketHID Bridge)

---

## 1. Core Axioms

The physical validation of this system strictly adheres to the following axioms:
- **CODE EXISTS != CODE RUNS** (Firmware present on disk does not equal firmware running on the MCU).
- **CODE RUNS != HOST VERIFIED** (MCU executing logic does not mean the host OS acknowledges it).
- **PACKET SENT != INPUT VERIFIED** (Device sending a report does not mean the host parsed it as valid HID input).
- **ENUMERATED != ALL FEATURES VERIFIED** (USB or BLE connection does not imply all endpoints or reports function correctly).

---

## 2. Current Validation Status

**STATUS:** `BLOCKED`

**Blocker Reason:** Windows currently shows no present ESP32-S3-class USB device and no corresponding serial/JTAG interface. Physical validation cannot proceed without hardware enumeration.

*(Note: Root causes such as cable data capability, specific USB port connections, OS drivers, or electrical issues are NOT inferred in this document.)*

---

## 3. Preconditions for Physical Validation

1. An ESP32-S3 hardware board is physically connected to the host PC.
2. The board must successfully enumerate on the USB bus or be discoverable via BLE.

---

## 4. Hardware Handoff Checklist & Evidence Collection

### A. Windows Device-Detection Evidence
- [ ] Record PnP Device Arrival event in Windows Event Viewer.
- [ ] Identify Hardware VID/PID (e.g., `VID_303A` for Espressif or `VID_10C4` / `VID_1A86` for UART bridge).
- **Status:** `PENDING`

### B. Flash Prerequisites
- [ ] ESP-IDF flash tool can detect the MCU via UART/JTAG.
- [ ] Bootloader and application binaries are available in `build/`.
- **Status:** `PENDING`

### C. USB Enumeration Evidence
- [ ] Windows Device Manager displays "USB Input Device" matching the PocketHID descriptor.
- [ ] Retrieve USB descriptors (Device, Configuration, Interface, HID, Report).
- **Status:** `PENDING`

### D. BLE Advertising/Connection Evidence
- [ ] Windows Bluetooth scanner detects "PocketHID-Bridge" (or configured name).
- [ ] Successful pairing and connection without GATT profile rejection.
- **Status:** `PENDING`

### E. Windows HID Enumeration Evidence
- [ ] USB/BLE HID endpoints correctly map to Top-Level Collections (TLCs) in Windows.
- [ ] Verification of Report Descriptors using tools (e.g., Wireshark, USBPcap, or Windows Hardware Lab Kit).
- **Status:** `PENDING`

---

## 5. Host-Input Verification Evidence

For each logical device, verify host reception of parsed input data:

### Keyboard
- [ ] Test standard key presses (e.g., Alphanumeric keys).
- [ ] Test modifier keys (Ctrl, Shift, Alt, GUI).
- **Status:** `PENDING`

### Mouse
- [ ] Verify X/Y coordinate translation.
- [ ] Verify Left/Right/Middle clicks and scroll wheel events.
- **Status:** `PENDING`

### Consumer
- [ ] Test media controls (Volume Up/Down/Mute, Play/Pause).
- **Status:** `PENDING`

### Gamepad
- [ ] Verify analog stick axes (X/Y, Z/Rz).
- [ ] Verify buttons and D-Pad (Hat switch) states.
- **Status:** `PENDING`

### Tablet (Electronic Drawing Board)
- [ ] Verify absolute coordinate mapping.
- [ ] Verify stylus pressure / tip switch.
- **Status:** `PENDING`

---

## 6. Required Evidence Classification Taxonomy

All items in this validation gate must be tracked using the project's strict classification taxonomy:
- `SOURCE-IMPLEMENTED`
- `HOST-UNIT-TESTED`
- `BUILD-VERIFIED`
- `CROSS-COMPILED`
- `FLASHED`
- `BOOT-VERIFIED`
- `USB-ENUMERATED`
- `BLE-ADVERTISED`
- `BLE-CONNECTED`
- `HOST-INPUT-VERIFIED`
- `PHYSICALLY-VERIFIED`
- `PENDING`
- `BLOCKED`

*(Currently, all physical validation steps are marked as `PENDING` due to the `BLOCKED` status of hardware detection.)*
