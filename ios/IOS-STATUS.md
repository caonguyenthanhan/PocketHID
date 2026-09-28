# PocketHID iOS Status

## Implemented
- **Native SwiftUI Shell**: `MainView`, `TopCommandBarView`, `ModeNavigationBar`, `DeckModeSwitcherView`
- **Domain Action Architecture**: `PocketAction`, `ActionResolver`, `ActionDispatcher`, `HostOs`
- **Global Focus Lock**: `FocusLockController` (single source of truth for global mode lock across all 6 modes)
- **Keyboard UI**: Dedicated dual-legend number row, full QWERTY, modifier states, persistent LED CapsLock
- **Mouse UI**: Precision trackpad, multi-touch gestures, physical buttons, fast scroll
- **Gamepad UI**: Symmetrical lower-half thumb cluster, dual sticks, ABXY, D-Pad, bumpers, triggers
- **Presenter UI**: Large PREV/NEXT navigation targets, `PresenterSafeExitButtonView` (1.5s press safety)
- **One-Hand UI**: WEB & VIDEO segmented modes, Left/Right hand reachability orientation
- **Graphics Tablet Local Canvas**: Full-bleed `Canvas` (90–95%), collapsed floating toolbar (5–10%), smooth Bezier rendering, undo/redo (max 50), eraser, 4 sizes, 5 colors
- **Coordinate Pipeline**: Canonical 8-stage `DrawingCoordinatePipeline` (raw touch to 16-bit 0...32767)
- **Localization**: Centralized dictionary in `PocketStrings` supporting English and Vietnamese
- **Transport Abstraction**: `HIDTransport` protocol, `TransportCapability` descriptor, `iOSTransport` implementation
- **Project Structure**: SPM `Package.swift` and native `PocketHID.xcodeproj` referencing all 32 source files and 5 test suites

## Transport
**Direct iPhone → PC Bluetooth HID:**  
`NOT AVAILABLE THROUGH CURRENT PUBLIC IOS API BASELINE`
- Public iOS APIs do not support registering an iPhone as a standard driverless Bluetooth HID peripheral to a PC.
- `iOSTransport` status is set to `.unavailable`.
- Telemetry explicitly measures local UI action dispatch only; no network or HID transmission to a host is falsely reported.
- No unapproved proprietary network protocols (WebSocket/TCP/UDP) or companion bridges have been introduced.

## Verified
- **Source-Level Implementation**: PASS (clean Swift types, decoupled domain, SwiftUI views)
- **Project Structure & Target Membership**: PASS (all 32 source files + 5 test suites referenced in `project.pbxproj` and `Package.swift`)
- **API Availability & Deployment Target**: PASS (iOS 15.0+ baseline compatibility verified)
- **Honest Status & Telemetry Audit**: PASS (zero false "Connected" claims; decoupled capability from connection state)

## Not Verified
- **Xcode Build / XCTest Execution**: NOT RUN — Current development environment is Windows 10 (macOS & Xcode CLI unavailable)
- **Physical iPhone**: PENDING (No physical iPhone paired or tested)
- **Windows HID Enumeration**: NOT APPLICABLE / PENDING (Direct Bluetooth HID peripheral role not exposed by iOS)
- **PC Application Delivery**: NOT APPLICABLE / PENDING (Microsoft Paint, OneNote, PowerPoint, Whiteboard input delivery pending an approved future transport backend)
