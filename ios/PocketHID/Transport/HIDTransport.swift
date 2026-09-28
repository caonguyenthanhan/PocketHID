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
    
    /// Sends a standard 8-byte Keyboard input report (Report ID 1)
    /// - Parameters:
    ///   - keyCodes: Array of up to 6 simultaneous HID usage codes (6KRO)
    ///   - modifiers: Bitmask of modifier keys (Ctrl, Shift, Alt, GUI)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendKeyboardReport(keyCodes: [UInt8], modifiers: UInt8) -> Bool
    
    /// Sends a standard Relative Mouse input report (Report ID 2)
    /// - Parameters:
    ///   - dx: Relative X delta (-127...127)
    ///   - dy: Relative Y delta (-127...127)
    ///   - buttons: Bitmask of buttons (Left: 0x01, Right: 0x02, Middle: 0x04)
    ///   - wheel: Scroll wheel delta (-127...127)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendMouseMove(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) -> Bool
    
    /// Sends a Consumer Control usage click (Report ID 3)
    /// - Parameter usageCode: 16-bit consumer usage (e.g. 0x00E9 for Vol+, 0x00CD for Play/Pause)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendConsumerClick(usageCode: UInt16) -> Bool
    
    /// Sends a Gamepad input report (Report ID 4)
    /// - Parameters:
    ///   - buttons: 16-bit button mask (ABXY, Triggers, Bumpers, D-pad, Thumbsticks)
    ///   - leftStickX: Signed 16-bit horizontal axis (-32767...32767)
    ///   - leftStickY: Signed 16-bit vertical axis (-32767...32767)
    ///   - rightStickX: Signed 16-bit horizontal axis (-32767...32767)
    ///   - rightStickY: Signed 16-bit vertical axis (-32767...32767)
    ///   - leftTrigger: Unsigned 8-bit analog trigger (0...255)
    ///   - rightTrigger: Unsigned 8-bit analog trigger (0...255)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendGamepadReport(
        buttons: UInt16,
        leftStickX: Int16,
        leftStickY: Int16,
        rightStickX: Int16,
        rightStickY: Int16,
        leftTrigger: UInt8,
        rightTrigger: UInt8
    ) -> Bool
    
    /// Sends an Absolute Digitizer / Tablet report (Report ID 5)
    /// - Parameters:
    ///   - status: Bitmask (Tip Switch: 0x01, In Range: 0x02, Invert/Eraser: 0x10)
    ///   - x: Absolute X coordinate (0...32767)
    ///   - y: Absolute Y coordinate (0...32767)
    /// - Returns: True if dispatched to host successfully, false otherwise
    func sendTabletReport(status: UInt8, x: UInt16, y: UInt16) -> Bool
    
    /// Resets the tablet report to neutral (Tip Switch = 0, In Range = 0)
    func sendTabletNeutral() -> Bool
    
    /// Disconnects the active session
    func disconnect() -> Bool
}
