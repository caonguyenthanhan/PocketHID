# PocketHID — Hardware Evidence Audit & Verification Matrix
**Audit Date:** 2026-09-28  
**Component Under Audit:** Graphics Tablet (True Absolute Digitizer / Pen & Eraser, Report ID 5)  
**Audit Standard:** Zero-Hallucination Hardware Verification Policy  

---

## 1. Executive Summary & Status Classification

Per the PocketHID Engineering Audit Policy, physical test claims cannot be inferred or synthesized from coordinate math, unit test execution, or APK build success. Strict hardware proof requires an active physical session pairing a real Android handset with a real Windows host over Bluetooth HID.

### System Verification Status

| Layer | Status | Evidence / Verification Method |
|---|---|---|
| **Code Implementation** | **COMPLETE** | `DrawingCoordinatePipeline.kt`, `DrawingTabletController.kt`, `BtHidTransport.kt` |
| **Unit Test Suite** | **PASS** | `testDebugUnitTest` (22/22 suites passing in Gradle) |
| **Gradle / APK Build** | **PASS** | `assembleDebug` BUILD SUCCESSFUL (debug APK packaged) |
| **Physical Device Pairing** | **PENDING** | No physical Android device connected via ADB or Bluetooth |
| **Windows HID Enumeration** | **PENDING** | Awaiting physical pairing to inspect Windows Device Manager |
| **PC Application Drawing** | **PENDING** | Awaiting physical contact testing in Microsoft Paint / OneNote |

> [!IMPORTANT]
> All physical interaction benchmarks previously marked `PASS` are formally rectified to **`NOT VERIFIED`** (Expected from implementation, pending physical validation).

---

## 2. Windows HID Digitizer Collection Verification Protocol

### Host Enumeration Audit Criteria
Windows does not automatically treat every HID collection as a Windows Ink digitizer. When paired over Bluetooth, Windows must recognize Report ID 5 under:
- **Device Manager** $\rightarrow$ **Human Interface Devices**
  - Expected node: `HID-compliant pen` or `HID-compliant digitizer` / `Bluetooth HID Device`
  - Expected Hardware IDs: Matching PocketHID Bluetooth HID descriptor (`Usage Page: Digitizers (0x0D)`, `Usage: Touch Screen (0x04)` or `Pen (0x02)`)

### Current Hardware Verification
- **ADB Device Probe:** Executed `adb devices` $\rightarrow$ `List of devices attached: [Empty]`.
- **Windows PnP Device Probe:** Executed `Get-PnpDevice -Class "Bluetooth", "HIDClass"` $\rightarrow$ No paired PocketHID device detected on host.
- **Enumeration Status:** **NOT VERIFIED** (Pending physical connection).

---

## 3. Physical Drawing Verification Matrix (Rectified)

*Evaluation against Microsoft Paint (Canvas at 100% zoom, single display)*

| Test Case | Touch Input (Phone) | Expected Coordinate (PC) | Actual Position | Error (X, Y) | Physical Status |
|---|---|---|---|---|---|
| **Top-Left Corner** | $X = 0, Y = 0$ | $(0, 0)$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Top-Right Corner** | $X = W, Y = 0$ | $(32767, 0)$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Center** | $X = W/2, Y = H/2$ | $(16383, 16383)$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Bottom-Left Corner** | $X = 0, Y = H$ | $(0, 32767)$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Bottom-Right Corner**| $X = W, Y = H$ | $(32767, 32767)$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Horizontal Line** | $(0.2, 0.5) \rightarrow (0.8, 0.5)$ | Strict horizontal stroke | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Vertical Line** | $(0.5, 0.2) \rightarrow (0.5, 0.8)$ | Strict vertical stroke | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Diagonal Line** | $(0.1, 0.1) \rightarrow (0.9, 0.9)$ | Linear diagonal $45^\circ$ | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Geometry: Circle** | Continuous circular stroke | Smooth, non-skewed ellipse/circle | Pending test | — | **NOT VERIFIED** *(Expected from math)* |
| **Geometry: Box** | 4-point rectangle perimeter | Square vertices with closed loop | Pending test | — | **NOT VERIFIED** *(Expected from math)* |

---

## 4. Eraser Behavioral Verification

### Protocol & Mechanism
- **HID Implementation:** Bit `0x10` (Invert) / Secondary tip switch (`TABLET_STATUS_ERASER = 0x02` or `TABLET_STATUS_INVERT = 0x10`) inside Report ID 5.
- **Verification Rule:** Bit setting does not guarantee Windows Ink eraser recognition. Different Windows applications interpret eraser flags differently (e.g., standard eraser mode vs eraser button toggle vs right-click erase).
- **Physical Test Status:** **NOT VERIFIED** *(Pending live testing in Paint, OneNote, and PowerPoint)*.

---

## 5. Application Compatibility Matrix (Rectified)

| Target Application | Windows Enumeration | Pen Contact (Tip Switch) | Absolute Positioning | Continuous Stroke | Eraser Action | Overall Status |
|---|---|---|---|---|---|---|
| **Microsoft Paint** | PENDING | PENDING | PENDING | PENDING | PENDING | **NOT VERIFIED** |
| **Microsoft OneNote** | PENDING | PENDING | PENDING | PENDING | PENDING | **NOT VERIFIED** |
| **Microsoft PowerPoint** (Slide Draw) | PENDING | PENDING | PENDING | PENDING | PENDING | **NOT VERIFIED** |
| **Microsoft Whiteboard** | PENDING | PENDING | PENDING | PENDING | PENDING | **NOT VERIFIED** |

*(Status across all target apps: Expected from implementation, pending physical validation.)*

---

## 6. Multi-Monitor Coordinate Mapping Policy

In multi-monitor environments on Windows:
- Windows Virtual Desktop spans across displays: $(X_{\min}, Y_{\min}) \rightarrow (X_{\max}, Y_{\max})$.
- Digitizers without monitor association may default to:
  1. Spanning the entire virtual desktop (causing aspect ratio distortion if phone is 20:9 and desktop is 32:9).
  2. Mapping strictly to the Primary Monitor (preferred for drawing apps).
  3. Windows Tablet PC Settings configuration (`Control Panel` $\rightarrow$ `Tablet PC Settings` $\rightarrow$ `Configure display`).
- **Physical Test Status:** **NOT VERIFIED** *(To be measured and documented once paired to a dual-monitor setup)*.

---

## 7. Hardware Test Acceptance Criteria

Final acceptance requires every item in the following checklist to be physically verified:

- [ ] Real Android phone connected via Bluetooth HID.
- [ ] Real Windows host paired and displaying active connection.
- [ ] Windows Device Manager confirms HID digitizer / pen enumeration.
- [ ] Top-left corner coordinates confirmed within $\pm 2$px error.
- [ ] Top-right corner coordinates confirmed within $\pm 2$px error.
- [ ] Bottom-left corner coordinates confirmed within $\pm 2$px error.
- [ ] Bottom-right corner coordinates confirmed within $\pm 2$px error.
- [ ] Center coordinates confirmed within $\pm 2$px error.
- [ ] Continuous line drawing verified with zero jagged jitter or lost packets.
- [ ] Circle stroke verified without aspect-ratio skewing.
- [ ] Eraser tool verified to remove strokes in target applications.
- [ ] Zero coordinate offset or stuck contacts upon touch release.

**Current Overall Physical Verdict:** **PENDING PHYSICAL TEST**
