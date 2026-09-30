//
//  HIDReportBuilder.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Pure-data builder for serializing semantic HID arguments into opaque binary payloads.
/// Conforms to POCKETHID-GOLDEN-VECTORS.md and preserves exactly the standard report formats.
public struct HIDReportBuilder {
    
    public static func buildKeyboardReport(keyCodes: [UInt8], modifiers: UInt8) -> (endpoint: UInt8, payload: [UInt8]) {
        var payload: [UInt8] = [modifiers, 0]
        payload.append(contentsOf: keyCodes)
        while payload.count < 8 { payload.append(0) }
        return (endpoint: 1, payload: Array(payload.prefix(8)))
    }
    
    public static func buildMouseMove(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) -> (endpoint: UInt8, payload: [UInt8]) {
        let payload: [UInt8] = [
            buttons,
            UInt8(bitPattern: dx),
            UInt8(bitPattern: dy),
            UInt8(bitPattern: wheel)
        ]
        return (endpoint: 2, payload: payload)
    }
    
    public static func buildConsumerClick(usageCode: UInt16) -> (endpoint: UInt8, payload: [UInt8]) {
        let payload: [UInt8] = [
            UInt8(usageCode & 0xFF),
            UInt8((usageCode >> 8) & 0xFF)
        ]
        return (endpoint: 3, payload: payload)
    }
    
    public static func buildGamepadReport(
        buttons: UInt16, leftStickX: Int16, leftStickY: Int16,
        rightStickX: Int16, rightStickY: Int16, leftTrigger: UInt8, rightTrigger: UInt8
    ) -> (endpoint: UInt8, payload: [UInt8]) {
        let payload: [UInt8] = [
            UInt8(buttons & 0xFF), UInt8((buttons >> 8) & 0xFF),
            UInt8(bitPattern: Int8(clamping: leftStickX >> 8)),
            UInt8(bitPattern: Int8(clamping: leftStickY >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickX >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickY >> 8)),
            leftTrigger, rightTrigger
        ]
        return (endpoint: 4, payload: payload)
    }
    
    public static func buildTabletReport(status: UInt8, x: UInt16, y: UInt16) -> (endpoint: UInt8, payload: [UInt8]) {
        let payload: [UInt8] = [
            status,
            UInt8(x & 0xFF), UInt8((x >> 8) & 0xFF),
            UInt8(y & 0xFF), UInt8((y >> 8) & 0xFF)
        ]
        return (endpoint: 5, payload: payload)
    }
    
    public static func buildTabletNeutral() -> (endpoint: UInt8, payload: [UInt8]) {
        return buildTabletReport(status: 0, x: 0, y: 0)
    }
}
