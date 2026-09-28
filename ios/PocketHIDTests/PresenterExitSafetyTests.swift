//
//  PresenterExitSafetyTests.swift
//  PocketHIDTests
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import XCTest
@testable import PocketHID

final class PresenterExitSafetyTests: XCTestCase {

    func testPrematureReleaseRejection() {
        let safety = PresenterExitSafety()
        let t0: Int64 = 10000

        safety.onPressDown(currentTimeMs: t0)
        XCTAssertTrue(safety.isHolding)
        XCTAssertFalse(safety.wasTriggered)

        // Released after only 500ms (< 1500ms)
        let didComplete = safety.onPressUp(currentTimeMs: t0 + 500)
        XCTAssertFalse(didComplete, "500ms hold must be rejected")
        XCTAssertFalse(safety.isHolding)
        XCTAssertFalse(safety.wasTriggered)
    }

    func testFullHoldTriggersExit() {
        let safety = PresenterExitSafety()
        let t0: Int64 = 10000

        safety.onPressDown(currentTimeMs: t0)
        let progressHalf = safety.progress(currentTimeMs: t0 + 750)
        XCTAssertEqual(progressHalf, 0.5, accuracy: 0.01)

        // Released after 1500ms
        let didComplete = safety.onPressUp(currentTimeMs: t0 + 1500)
        XCTAssertTrue(didComplete, "1500ms hold must trigger exit")
        XCTAssertTrue(safety.wasTriggered)
    }

    func testCancelResetsState() {
        let safety = PresenterExitSafety()
        safety.onPressDown(currentTimeMs: 1000)
        XCTAssertTrue(safety.isHolding)

        safety.cancel()
        XCTAssertFalse(safety.isHolding)
        XCTAssertEqual(safety.progress(currentTimeMs: 2000), 0.0)
    }
}
