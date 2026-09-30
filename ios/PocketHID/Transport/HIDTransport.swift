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
@MainActor
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


