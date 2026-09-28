//
//  KeyboardMappingTests.swift
//  PocketHIDTests
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import XCTest
@testable import PocketHID

final class KeyboardMappingTests: XCTestCase {

    func testNumberRowDualLegends() {
        let expectedPairs: [(String, String, UInt8)] = [
            ("`", "~", HidConstants.KEY_GRAVE),
            ("1", "!", HidConstants.KEY_1),
            ("2", "@", HidConstants.KEY_2),
            ("3", "#", HidConstants.KEY_3),
            ("4", "$", HidConstants.KEY_4),
            ("5", "%", HidConstants.KEY_5),
            ("6", "^", HidConstants.KEY_6),
            ("7", "&", HidConstants.KEY_7),
            ("8", "*", HidConstants.KEY_8),
            ("9", "(", HidConstants.KEY_9),
            ("0", ")", HidConstants.KEY_0),
            ("-", "_", HidConstants.KEY_MINUS),
            ("=", "+", HidConstants.KEY_EQUAL)
        ]

        XCTAssertEqual(KeyLegends.numberRow.count, 13)

        for (index, expected) in expectedPairs.enumerated() {
            let item = KeyLegends.numberRow[index]
            XCTAssertEqual(item.primary, expected.0)
            XCTAssertEqual(item.shifted, expected.1)
            XCTAssertEqual(item.keyCode, expected.2)

            // Shift OFF -> Primary
            XCTAssertEqual(item.resolveActiveLabel(isShiftActive: false, isCapsLockActive: false), expected.0)
            // Shift ON -> Shifted
            XCTAssertEqual(item.resolveActiveLabel(isShiftActive: true, isCapsLockActive: false), expected.1)
            // CapsLock does not alter symbols
            XCTAssertEqual(item.resolveActiveLabel(isShiftActive: false, isCapsLockActive: true), expected.0)
            XCTAssertEqual(item.resolveActiveLabel(isShiftActive: true, isCapsLockActive: true), expected.1)
        }
    }

    func testCapsLockAndShiftSemanticsForLetters() {
        let keyA = KeyLegend(primary: "A", keyCode: HidConstants.KEY_A, isAlphabetic: true)

        // 1. CAPS OFF + SHIFT OFF -> lowercase 'a'
        XCTAssertEqual(keyA.resolveActiveLabel(isShiftActive: false, isCapsLockActive: false), "a")

        // 2. CAPS ON + SHIFT OFF -> uppercase 'A'
        XCTAssertEqual(keyA.resolveActiveLabel(isShiftActive: false, isCapsLockActive: true), "A")

        // 3. CAPS OFF + SHIFT ON -> uppercase 'A'
        XCTAssertEqual(keyA.resolveActiveLabel(isShiftActive: true, isCapsLockActive: false), "A")

        // 4. CAPS ON + SHIFT ON -> lowercase 'a' (standard keyboard XOR inversion)
        XCTAssertEqual(keyA.resolveActiveLabel(isShiftActive: true, isCapsLockActive: true), "a")
    }
}
