//
//  HIDTransport.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Defines connection states for HID transports.
/// Distinguishes between unavailable capability, uncoupled local state, and actual host connections.
public enum ConnectionStatus: Equatable {
    case unavailable(reason: String)
    case notConnected
    case connecting(deviceName: String?)
    case connected(deviceName: String)
    case disconnecting
    case error(message: String)
}

/// Abstract contract for HID communication.
/// Decoupled from platform implementations (Android BluetoothHidDevice vs iOS Transport).
public protocol HIDTransport: AnyObject {
    /// Current connection status of the transport
    var status: ConnectionStatus { get }
    
    /// Hardware & platform capability descriptor
    var capability: TransportCapability { get }
    
    /// Sends a raw, pre-serialized HID report payload.
    /// The transport must treat this payload as opaque bytes.
    /// - Parameters:
    ///   - endpoint: The Endpoint ID / Report ID for routing (e.g. 1=Keyboard, 2=Mouse, etc.)
    ///   - payload: The serialized binary HID report payload (matching POCKETHID-GOLDEN-VECTORS.md)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendRawReport(endpoint: UInt8, payload: [UInt8]) -> Bool
    
    /// Disconnects the active session
    func disconnect() -> Bool
}

public extension HIDTransport {
    func sendKeyboardReport(keyCodes: [UInt8], modifiers: UInt8) -> Bool {
        var payload: [UInt8] = [modifiers, 0]
        payload.append(contentsOf: keyCodes)
        while payload.count < 8 { payload.append(0) }
        return sendRawReport(endpoint: 1, payload: Array(payload.prefix(8)))
    }
    
    func sendMouseMove(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) -> Bool {
        let payload: [UInt8] = [
            buttons,
            UInt8(bitPattern: dx),
            UInt8(bitPattern: dy),
            UInt8(bitPattern: wheel)
        ]
        return sendRawReport(endpoint: 2, payload: payload)
    }
    
    func sendConsumerClick(usageCode: UInt16) -> Bool {
        let payload: [UInt8] = [
            UInt8(usageCode & 0xFF),
            UInt8((usageCode >> 8) & 0xFF)
        ]
        return sendRawReport(endpoint: 3, payload: payload)
    }
    
    func sendGamepadReport(
        buttons: UInt16,
        leftStickX: Int16,
        leftStickY: Int16,
        rightStickX: Int16,
        rightStickY: Int16,
        leftTrigger: UInt8,
        rightTrigger: UInt8
    ) -> Bool {
        let payload: [UInt8] = [
            UInt8(buttons & 0xFF), UInt8((buttons >> 8) & 0xFF),
            UInt8(bitPattern: Int8(clamping: leftStickX >> 8)),
            UInt8(bitPattern: Int8(clamping: leftStickY >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickX >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickY >> 8)),
            leftTrigger,
            rightTrigger
        ]
        return sendRawReport(endpoint: 4, payload: payload)
    }
    
    func sendTabletReport(status: UInt8, x: UInt16, y: UInt16) -> Bool {
        let payload: [UInt8] = [
            status,
            UInt8(x & 0xFF), UInt8((x >> 8) & 0xFF),
            UInt8(y & 0xFF), UInt8((y >> 8) & 0xFF)
        ]
        return sendRawReport(endpoint: 5, payload: payload)
    }
    
    func sendTabletNeutral() -> Bool {
        return sendTabletReport(status: 0, x: 0, y: 0)
    }
}
