//
//  FocusLockTests.swift
//  PocketHIDTests
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import XCTest
@testable import PocketHID

final class FocusLockTests: XCTestCase {

    override func setUp() {
        super.setUp()
        FocusLockController.shared.reset()
    }

    func testInitialStateIsUnlocked() {
        XCTAssertFalse(FocusLockController.shared.isLocked)
        XCTAssertTrue(FocusLockController.shared.canSwitchMode())
        XCTAssertTrue(FocusLockController.shared.allowNavigationGesture())
        XCTAssertTrue(FocusLockController.shared.allowsInternalAction())
    }

    func testToggleLocksAndUnlocks() {
        let locked = FocusLockController.shared.toggle()
        XCTAssertTrue(locked)
        XCTAssertTrue(FocusLockController.shared.isLocked)
        XCTAssertFalse(FocusLockController.shared.canSwitchMode())

        let unlocked = FocusLockController.shared.toggle()
        XCTAssertFalse(unlocked)
        XCTAssertFalse(FocusLockController.shared.isLocked)
        XCTAssertTrue(FocusLockController.shared.canSwitchMode())
    }

    func testModeSwitchingGatingWhenLocked() {
        FocusLockController.shared.setLocked(true)

        // When locked, any mode switch returns current mode
        let resolved = FocusLockController.shared.resolveModeSwitch(currentMode: .gamepad, targetMode: .keyboard)
        XCTAssertEqual(resolved, .gamepad)

        let intResolved = FocusLockController.shared.resolveModeSwitch(currentMode: 2, targetMode: 0)
        XCTAssertEqual(intResolved, 2)

        // Internal actions remain fully permitted
        XCTAssertTrue(FocusLockController.shared.allowsInternalAction())
    }

    func testModeSwitchingAllowedWhenUnlocked() {
        FocusLockController.shared.setLocked(false)

        let resolved = FocusLockController.shared.resolveModeSwitch(currentMode: .gamepad, targetMode: .keyboard)
        XCTAssertEqual(resolved, .keyboard)

        let intResolved = FocusLockController.shared.resolveModeSwitch(currentMode: 2, targetMode: 0)
        XCTAssertEqual(intResolved, 0)
    }

    func testResetUnlocks() {
        FocusLockController.shared.setLocked(true)
        XCTAssertTrue(FocusLockController.shared.isLocked)

        FocusLockController.shared.reset()
        XCTAssertFalse(FocusLockController.shared.isLocked)
        XCTAssertTrue(FocusLockController.shared.canSwitchMode())
    }
}
