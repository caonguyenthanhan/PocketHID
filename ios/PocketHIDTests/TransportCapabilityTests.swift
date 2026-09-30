//
//  TransportCapabilityTests.swift
//  PocketHIDTests
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import XCTest
@testable import PocketHID

final class TransportCapabilityTests: XCTestCase {

    func testIOSBaselineDoesNotClaimDirectHID() {
        let baseline = TransportCapability.iOSPublicAPIBaseline

        // Must explicitly declare unsupported direct HID
        switch baseline.level {
        case .unsupportedDirectHID(let reason):
            XCTAssertTrue(reason.contains("Apple iOS does not expose Bluetooth HID Device peripheral APIs"))
        default:
            XCTFail("iOS baseline capability must be .unsupportedDirectHID")
        }

        // Direct PC HID channels must be false to avoid false claims
        XCTAssertFalse(baseline.supportsKeyboard)
        XCTAssertFalse(baseline.supportsMouse)
        XCTAssertFalse(baseline.supportsGamepad)
        XCTAssertFalse(baseline.supportsTabletDigitizer)
    }

    func testIOSTransportDefaultStatusIsUnavailable() {
        let transport = iOSTransport(capability: .iOSPublicAPIBaseline)
        
        switch transport.status {
        case .unavailable(let reason):
            XCTAssertTrue(reason.contains("Direct PC Bluetooth HID peripheral role is not available"))
        default:
            XCTFail("Default iOS status must be .unavailable under public baseline")
        }
    }

    func testIOSTransportSafeDispatchesWithoutCrashing() {
        let transport = iOSTransport(capability: .iOSPublicAPIBaseline)

        // Dispatching local actions while unsupported returns false cleanly without crashing
        let kbReport = HIDReportBuilder.buildKeyboardReport(keyCodes: [HidConstants.KEY_A], modifiers: 0)
        let kbResult = transport.sendRawReport(endpoint: kbReport.endpoint, payload: kbReport.payload)
        XCTAssertFalse(kbResult)
        XCTAssertEqual(transport.lastScancodeHex, "0x04")
        XCTAssertEqual(transport.totalLocalActionsDispatched, 1)

        let mouseReport = HIDReportBuilder.buildMouseMove(dx: 10, dy: 20, buttons: 1, wheel: 0)
        let mouseResult = transport.sendRawReport(endpoint: mouseReport.endpoint, payload: mouseReport.payload)
        XCTAssertFalse(mouseResult)
        XCTAssertEqual(transport.totalLocalActionsDispatched, 2)

        let tabletReport = HIDReportBuilder.buildTabletReport(status: 1, x: 1000, y: 2000)
        let tabletResult = transport.sendRawReport(endpoint: tabletReport.endpoint, payload: tabletReport.payload)
        XCTAssertFalse(tabletResult)
        XCTAssertEqual(transport.totalLocalActionsDispatched, 3)
    }
}
