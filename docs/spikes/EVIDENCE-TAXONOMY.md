# PocketHID Verification Evidence Taxonomy
**Document ID:** `docs/spikes/EVIDENCE-TAXONOMY.md`  
**Standard Version:** 1.0 (Wave 12.1)  
**Status:** ACTIVE STANDARD  

---

## 1. Purpose & Core Principles

This taxonomy establishes an unambiguous, rigorous standard for claiming verification levels across PocketHID subsystems (firmware, Android, iOS, USB HID, and BLE).

### Critical Ground Rules
1. **`CODE EXISTS` ≠ `CODE RUNS`**: Writing source code or compiling mock tests does not prove runtime viability on embedded hardware.
2. **`CODE RUNS` ≠ `HOST VERIFIED`**: A unit test passing on an x86 development machine or an API returning `true` does not prove host operating system recognition.
3. **`PACKET SENT` ≠ `INPUT VERIFIED`**: Transmitting bytes over USB or BLE does not prove the host OS dispatched an input event to user-space applications.
4. **`ENUMERATED` ≠ `ALL FEATURES VERIFIED`**: A USB composite device showing in Device Manager does not prove that all Report IDs, axes, buttons, or relative deltas function properly.
5. **`RESERVED STATUS: HOST-INPUT-VERIFIED`**: Strictly reserved for an observable, measured input effect on a physical host OS driven by a physical device. Unit tests on host machines MUST NEVER use this label.

---

## 2. Defined Verification States & Required Evidence

### `SOURCE-IMPLEMENTED`
- **Definition:** Source code has been written and committed into the repository according to specification.
- **Minimum Evidence Required:** Syntactically valid code files committed to git.
- **What It Proves:** Code structure, types, and logic exist.
- **What It Does NOT Prove:** Compilation, runtime correctness, or hardware compatibility.

### `HOST-UNIT-TESTED`
- **Definition:** Code logic has been exercised and asserted using host-side automated unit test harnesses (e.g., JUnit on Android, GCC on host x86 for firmware parser/safety logic).
- **Minimum Evidence Required:** Test runner execution log with discrete test counts and zero failures (e.g. `99 / 99 PASSED` for C harness, `147 / 147 PASSED` for Gradle).
- **What It Proves:** Algorithmic correctness, state transitions, bounds checking, and protocol parsing in an isolated environment.
- **What It Does NOT Prove:** Embedded hardware execution, hardware peripherals, or real OS driver interaction.

### `BUILD-VERIFIED`
- **Definition:** The application, module, or harness builds into its standard distributable or testable artifact without compile or link errors.
- **Minimum Evidence Required:** Clean build exit code 0 (e.g. `.\gradlew.bat assembleDebug` generating valid APK, GCC producing host test binary).
- **What It Proves:** Dependency resolution, type checking, resource packaging, and linker resolution succeeded.
- **What It Does NOT Prove:** Execution on target target hardware or runtime defect freedom.

### `CROSS-COMPILED`
- **Definition:** Embedded firmware code has been compiled and linked using the target-specific cross-compiler toolchain for the actual target microcontroller architecture (e.g., `xtensa-esp32s3-elf-gcc`).
- **Minimum Evidence Required:** ESP-IDF build output showing successful creation of target binary images (`bootloader.bin`, `partition-table.bin`, application `.bin`, `merged-binary.bin`) and live SHA-256 hash manifest.
- **What It Proves:** Firmware fits within target flash/RAM partitions, symbols resolve against ESP-IDF / FreeRTOS / TinyUSB / NimBLE headers, and valid Xtensa machine code was generated.
- **What It Does NOT Prove:** Execution on physical silicon or peripheral hardware functionality.

### `FLASHED`
- **Definition:** Firmware binary image has been physically programmed into the on-board flash memory of the physical microcontroller via serial/JTAG bootloader.
- **Minimum Evidence Required:** `esptool.py` (or `flash.ps1`) console log showing flash address mapping, write progress (100%), verified hash digest, and reset.
- **What It Proves:** Target flash memory holds the cross-compiled binary bytes.
- **What It Does NOT Prove:** Successful bootloader handoff, firmware initialization, or operational stability.

### `BOOT-VERIFIED`
- **Definition:** Target microcontroller powered on, successfully passed ROM bootloader, loaded application image, and completed `app_main()` initialization.
- **Minimum Evidence Required:** Live UART / USB-CDC serial monitor log capturing initial boot markers: NVS initialized, Safety initialized, USB stack initialized, and BLE advertising started.
- **What It Proves:** Microcontroller boots, clock trees configure, memory partitions initialize, and tasks spawn.
- **What It Does NOT Prove:** Host enumeration or input transmission.

### `USB-ENUMERATED`
- **Definition:** Target device physically attached to host computer via USB; host USB controller parsed device/configuration descriptors and bound appropriate driver stack.
- **Minimum Evidence Required:** Host OS Device Manager / PnP device enumeration report showing matching VID/PID and composite HID collection hierarchy without Code 10 / Code 43 driver errors.
- **What It Proves:** USB physical layer electrical handshake, enumeration timing, and report descriptor parsing succeeded on the host OS.
- **What It Does NOT Prove:** Keystroke, cursor, or gamepad input reception.

### `BLE-ADVERTISED`
- **Definition:** BLE peripheral controller successfully initialized radio and is actively broadcasting advertising packets with configured service UUIDs.
- **Minimum Evidence Required:** Over-the-air packet capture or central scanner log (e.g. nRF Connect or CoreBluetooth scan) showing device name and 128-bit Service UUID.
- **What It Proves:** BLE radio is active and transmission power/advertising parameters are functional.
- **What It Does NOT Prove:** Central connection, MTU exchange, or GATT characteristic data transmission.

### `BLE-CONNECTED`
- **Definition:** A Central device (phone or PC) established an active BLE ACL connection and discovered GATT primary services and characteristics.
- **Minimum Evidence Required:** Central connection handle assigned, GAP connect event logged with status 0, and characteristic handles discovered.
- **What It Proves:** BLE link layer connection and attribute table discovery succeeded.
- **What It Does NOT Prove:** Packet stream integrity or HID bridging.

### `HOST-INPUT-VERIFIED`
- **Definition:** A physical input action emitted by the device is successfully received, decoded, and manifested as an observable user-space input effect on a physical host operating system.
- **Minimum Evidence Required:** 
  - Physical board + physical connection + physical host OS.
  - Observable host action: Character typed into text editor (Keyboard), cursor movement or button click (Mouse), system volume step (Consumer), `joy.cpl` axis/button actuation (Gamepad), or coordinate drawing in paint application (Tablet).
- **CRITICAL RESTRICTION:** Cannot be claimed based on mock test execution, Gradle unit test runs, or software stubs.

### `PHYSICALLY-VERIFIED`
- **Definition:** Comprehensive end-to-end operation across all stages of physical hardware: Central Phone App → BLE RF → ESP32-S3 Bridge → USB OTG → Host OS Input.
- **Minimum Evidence Required:** Multi-stage physical evidence recording BLE reception, packet parsing, safety watchdog evaluation, USB report dispatch, and verified host OS reaction.
- **What It Proves:** Full product operational pipeline is functional on real hardware.

### `PENDING`
- **Definition:** Implementation or artifact is prepared and awaiting a prerequisite milestone (typically physical hardware availability or previous stage completion).
- **Usage:** Indicates planned work ready for execution once blockers are removed.

### `BLOCKED`
- **Definition:** Execution cannot proceed due to missing prerequisite hardware, tooling, or upstream platform defect.
- **Usage:** Explicitly identifies halting conditions (e.g., Physical ESP32-S3 not connected).

---

## 3. Subsystem Evidence Mapping Table

| Subsystem | Host Unit Tests | Cross-Compile | Device Manager Enum | Physical Input Effect | Accurate Taxonomy Label |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Android Unit Tests** | PASS (147/147) | N/A | N/A | N/A | `HOST-UNIT-TESTED` / `BUILD-VERIFIED` |
| **Firmware Parser/Safety**| PASS (99/99) | N/A | N/A | N/A | `HOST-UNIT-TESTED` |
| **Firmware ELF/BIN** | N/A | PASS (ESP-IDF v5.3.1)| N/A | N/A | `CROSS-COMPILED` |
| **USB Descriptor Match** | PASS (313 B) | PASS | N/A | N/A | `HOST-UNIT-TESTED` / `CROSS-COMPILED` |
| **Physical ESP32-S3** | N/A | N/A | None (0 boards) | None | `NOT CONNECTED` / `BLOCKED` |
| **Windows HID** | N/A | N/A | Awaiting Board | Awaiting Board | `PENDING` |
| **BLE Physical** | N/A | N/A | Awaiting Board | Awaiting Board | `PENDING` |
| **iOS BridgeTransport** | None | N/A | N/A | N/A | `NOT IMPLEMENTED` |

---
*End of Evidence Taxonomy Standard — PocketHID*
