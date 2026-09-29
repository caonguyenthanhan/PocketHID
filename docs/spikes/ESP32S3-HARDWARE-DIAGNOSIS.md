# ESP32-S3 Hardware Diagnosis
**Document ID:** `docs/spikes/ESP32S3-HARDWARE-DIAGNOSIS.md`  
**Diagnostic Wave:** Wave 12.2  
**Baseline Git Commit:** `db9374bcf46a53d78ec448ab43876c470b2588c3`  
**Classification System:** `OBSERVED` | `INFERRED` | `HYPOTHESIS` | `UNKNOWN`  

---

## 1. Observed Environment

- **Host Operating System:** Windows 10/11 x64 [`OBSERVED`]
- **Host USB Controller:** Intel(R) USB 3.10 eXtensible Host Controller - 1.20 (Microsoft) [`OBSERVED`]
- **Serial Ports Detected by OS:**
  - `COM6` (`BTHENUM\{00001101-0000-1000-8000-00805F9B34FB}_LOCALMFG&0000\...`) [`OBSERVED`]
  - `COM7` (`BTHENUM\{00001101-0000-1000-8000-00805F9B34FB}_VID&000105D6_PID&000A\...`) [`OBSERVED`]
- **Classification of COM6 / COM7:** Both ports are Bluetooth Serial Port Profile (SPP) virtual links, explicitly rejected by `flash.ps1` and project safety rules [`INFERRED`].
- **ESP-IDF Python Serial Tool:** `serial.tools.list_ports` detected only `COM6` and `COM7` [`OBSERVED`].

---

## 2. Windows USB Enumeration

- **Espressif USB VID Search (`VID_303A`):** 0 devices present [`OBSERVED`].
- **Silicon Labs CP210x Search (`VID_10C4`):** 0 devices present [`OBSERVED`].
- **WCH CH34x Search (`VID_1A86`):** 0 devices present [`OBSERVED`].
- **FTDI Search (`VID_0403`):** 0 devices present [`OBSERVED`].
- **String Keyword Match (`USB JTAG`, `Espressif`, `ESP32`, `CP210`, `CH340`, `FTDI`):** 0 devices present [`OBSERVED`].
- **Active Physical USB Devices Present:**
  - `USB\VID_062A&PID_5918` (Wireless Mouse/Keyboard 2.4GHz USB Dongle) [`OBSERVED`]
  - `USB\VID_04F2&PID_B72B` (Integrated HD Webcam) [`OBSERVED`]
  - `USB\VID_04CA&PID_3802` (Integrated MediaTek Bluetooth Adapter) [`OBSERVED`]
  - `USB\ROOT_HUB30` (Intel Root Hub) [`OBSERVED`]
- **Summary:** Windows PnP device tree contains zero references to any ESP32-S3 or USB-UART interface [`OBSERVED`].

---

## 3. PnP Events

- **Log Inspected:** `Microsoft-Windows-Kernel-PnP/Device Management` [`OBSERVED`].
- **Query Window:** Events leading up to current session [`OBSERVED`].
- **Findings:**
  - Event ID 1010 indicates a device reported as missing on the bus / surprise removal.
  - No relevant Event ID 1010 entry was observed for an ESP32-S3-class device during the inspected interval [`OBSERVED`].
  - No failed USB descriptor requests (`USB\VID_0000&PID_0002` / `Device Descriptor Request Failed`) recorded [`OBSERVED`].
  - No Port Reset failures or Unknown USB Device events present [`OBSERVED`].
- **Diagnosis Category:** **Category A: No new USB device at all detected by Windows USB host controller** [`INFERRED`].
- **OS Conclusion:** Windows currently shows no present ESP32-S3-class USB device and no corresponding serial/JTAG interface. [`OBSERVED`]

---

## 4. Board Identity

- **Specific Board Model:** ESP32-S3 DevKit, ESP32-S3-Zero, or custom variant [`UNKNOWN`].
- **Microcontroller Chip:** Espressif ESP32-S3 (Dual-core Xtensa LX7) [`INFERRED by user request`].
- **USB Interface Hardware:** Native USB OTG (GPIO 19/20) vs External USB-to-UART Bridge (CP2102/CH340) [`UNKNOWN`].
- **Hardware Status:** Hardware identity unconfirmed over USB [`INFERRED`].

---

## 5. USB Ports

Many ESP32-S3 development boards feature multiple physical USB Type-C ports:
- **Port A (Native USB / "USB"):**
  - Connected directly to ESP32-S3 internal USB OTG PHY (GPIO 19: D-, GPIO 20: D+) [`INFERRED by ESP32-S3 datasheet`].
  - Exposes USB JTAG/serial debug unit (`VID_303A&PID_1001`) or TinyUSB composite HID in runtime [`INFERRED by ESP-IDF docs`].
- **Port B (UART / "COM"):**
  - Connected to an on-board USB-to-UART bridge IC (e.g. CP2102N, CH340K, or CH343) or dedicated CP2102 bridge [`INFERRED by ESP32-S3-DevKitC-1 schematic`].
  - Routes directly to ESP32-S3 UART0 (TXD0/RXD0) for serial bootloader programming [`INFERRED`].
- **Current Port Connected:** Physical board port in use is [`UNKNOWN`].

---

## 6. Cable Tests

- **Cable A (Currently connected cable):**
  - Physical data conductivity: [`UNKNOWN`].
  - Confirmation status: Needs verification by testing with a known USB data device (e.g. smartphone data transfer, USB thumbdrive) or replacing with a certified data cable [`PENDING`].
- **Cable B (Alternative known-good data cable):**
  - Connection status: [`PENDING`].

---

## 7. Power State vs. Data Enumeration

- **Board Power State:**
  - Whether VBUS delivers 5V power and whether board power LED is illuminated: [`UNKNOWN to host software`].
- **USB Data Enumeration:**
  - `ABSENT` [`OBSERVED`].
- **Axiom:** Physical power delivery (LED ON) does not establish that the device is communicating with the host OS [`INFERRED`].

---

## 8. Boot/Download Mode

Standard ESP32-S3 ROM bootloader strapping sequence:
1. Hold down the **BOOT** button (GPIO0 pulled LOW).
2. Press and release the **RESET** (EN) button.
3. Release the **BOOT** button.
- **Expected Behavior (Native USB port):** ROM bootloader enables internal USB PHY pull-up, causing Windows to enumerate `USB JTAG/serial debug unit` (`VID_303A&PID_1001`) [`INFERRED by Espressif TRM`].
- **Expected Behavior (UART bridge port):** UART bridge remains enumerated (`VID_10C4` or `VID_1A86`) regardless of MCU boot mode; MCU enters download wait state [`INFERRED`].
- **Execution Status:** Strapping sequence on physical board: [`UNKNOWN / PENDING physical test`].

---

## 9. Driver Status

- **Status:** Driver evaluation is **NOT APPLICABLE** at this stage [`INFERRED`].
- **Technical Justification:** Device driver matching (VCP or WinUSB) occurs only after successful USB device enumeration. Windows currently shows no present ESP32-S3-class USB device and no corresponding serial/JTAG interface. [`INFERRED`]
- **Policy:** Do not install third-party drivers or modify Windows driver store without device presence [`INFERRED`].

---

## 10. Observed & Inferred Facts

1. Baseline git working tree is clean at commit `db9374bcf46a53d78ec448ab43876c470b2588c3` [`OBSERVED`].
2. Operating system exposes only `COM6` and `COM7`, which are Bluetooth SPP virtual serial links (`BTHENUM`) [`OBSERVED`].
3. Zero USB devices matching Espressif (`VID_303A`), Silicon Labs (`VID_10C4`), WCH (`VID_1A86`), or FTDI (`VID_0403`) are present [`OBSERVED`].
4. Windows Kernel-PnP event logs record zero USB arrival or descriptor failure events during connection attempts [`OBSERVED`].
5. Firmware artifacts (`merged-binary.bin`, `pockethid-esp32s3-bridge.bin`, `bootloader.bin`, `partition-table.bin`) in `firmware/esp32s3-bridge/build/` match `FLASH-MANIFEST.md` verified SHA-256 hashes 100% [`OBSERVED`].

---

## 11. Hypotheses

1. **`HYPOTHESIS 1` (Port Selection):** On a dual-port board, the cable may be connected to a native USB port requiring ROM bootloader strapping (GPIO0 LOW during reset) to initiate USB enumeration.

---

## 12. Next Physical Action

To resolve the hardware gate without speculative software changes:

1. **Cable Verification:**
   - Swap the current USB cable with a verified **USB Data Cable** (e.g., a cable confirmed to transfer files from an Android phone or external drive to this PC).
2. **Port Selection:**
   - If the board has two USB Type-C connectors:
     - Try connecting to the **UART** port (often labeled `UART` or `COM`).
     - Try connecting to the **USB** port (often labeled `USB`).
3. **ROM Bootloader Strapping:**
   - While plugged into the native `USB` port:
     - Hold down the **BOOT** button.
     - Press and release the **RESET** button.
     - Release the **BOOT** button.
   - Listen for Windows device arrival chime and check if `VID_303A` appears.
4. **Direct PC Port:**
   - Plug directly into a motherboard USB Type-A or Type-C port, avoiding passive hubs.

---
*End of Hardware Diagnosis Report — PocketHID Wave 12.2*
