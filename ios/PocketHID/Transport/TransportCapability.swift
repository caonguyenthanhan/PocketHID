//
//  TransportCapability.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

/// Technical capability descriptor explaining the exact availability of HID transports on iOS.
public struct TransportCapability: Equatable {
    public enum Level: Equatable {
        case ready
        case unsupportedDirectHID(reason: String)
        case mockTesting
    }
    
    public let level: Level
    public let supportsKeyboard: Bool
    public let supportsMouse: Bool
    public let supportsGamepad: Bool
    public let supportsConsumerControl: Bool
    public let supportsTabletDigitizer: Bool
    public let technicalNotes: String
    
    /// Official baseline for iOS public SDK:
    /// Apple does not provide a public API for an iPhone to act as a driverless Bluetooth HID peripheral to a PC.
    public static let iOSPublicAPIBaseline = TransportCapability(
        level: .unsupportedDirectHID(reason: "Apple iOS does not expose Bluetooth HID Device peripheral APIs (unlike Android BluetoothHidDevice). CoreBluetooth peripheral role rejects standard HID GATT 0x1812 service."),
        supportsKeyboard: false,
        supportsMouse: false,
        supportsGamepad: false,
        supportsConsumerControl: false,
        supportsTabletDigitizer: false,
        technicalNotes: "iOS app operates in High-Fidelity Local Controller & Graphics Tablet mode. Direct PC Bluetooth HID requires private Apple entitlements or a companion receiver."
    )
    
    public static let localPreview = TransportCapability(
        level: .mockTesting,
        supportsKeyboard: true,
        supportsMouse: true,
        supportsGamepad: true,
        supportsConsumerControl: true,
        supportsTabletDigitizer: true,
        technicalNotes: "Simulated local transport for UI testing, latency measurement, and offline interaction validation."
    )
}
