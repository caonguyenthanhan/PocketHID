//
//  iOSTransport.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine

/// Native iOS transport implementation.
/// Formally adheres to the Hard Architecture Gate:
/// Exposes capability state as unsupported for direct driverless PC Bluetooth HID,
/// while providing an interactive telemetry pipeline for local UI control and demonstration.
public final class iOSTransport: ObservableObject, HIDTransport {
    
    @Published public private(set) var status: ConnectionStatus
    @Published public private(set) var capability: TransportCapability
    
    // Internal Telemetry & Diagnostics for local UI validation
    @Published public private(set) var lastDispatchedLabel: String = "IDLE"
    @Published public private(set) var lastScancodeHex: String = "0x00"
    @Published public private(set) var totalLocalActionsDispatched: UInt64 = 0
    @Published public private(set) var lastTabletPoint: (status: UInt8, x: UInt16, y: UInt16)? = nil
    
    public init(capability: TransportCapability = .iOSPublicAPIBaseline) {
        self.capability = capability
        if case .unsupportedDirectHID(let reason) = capability.level {
            self.status = .unavailable(reason: reason)
        } else {
            self.status = .notConnected
        }
    }
    
    // MARK: - HIDTransport Operations
    
    public func sendKeyboardReport(keyCodes: [UInt8], modifiers: UInt8) -> Bool {
        totalLocalActionsDispatched += 1
        let primaryCode = keyCodes.first ?? 0
        lastScancodeHex = String(format: "0x%02X", primaryCode)
        lastDispatchedLabel = primaryCode == 0 ? "LOCAL_KEY_RELEASE" : String(format: "LOCAL_KEY 0x%02X (mod: 0x%02X)", primaryCode, modifiers)
        
        // Direct transport is not available via public iOS APIs
        return capability.level == .mockTesting
    }
    
    public func sendMouseMove(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) -> Bool {
        totalLocalActionsDispatched += 1
        lastDispatchedLabel = "LOCAL_MOUSE dx:\(dx) dy:\(dy) btn:\(buttons) whl:\(wheel)"
        return capability.level == .mockTesting
    }
    
    public func sendConsumerClick(usageCode: UInt16) -> Bool {
        totalLocalActionsDispatched += 1
        lastScancodeHex = String(format: "0x%04X", usageCode)
        lastDispatchedLabel = String(format: "LOCAL_CONSUMER 0x%04X", usageCode)
        return capability.level == .mockTesting
    }
    
    public func sendGamepadReport(
        buttons: UInt16,
        leftStickX: Int16,
        leftStickY: Int16,
        rightStickX: Int16,
        rightStickY: Int16,
        leftTrigger: UInt8,
        rightTrigger: UInt8
    ) -> Bool {
        totalLocalActionsDispatched += 1
        lastDispatchedLabel = String(format: "LOCAL_GAMEPAD btn:0x%04X LT:%d RT:%d", buttons, leftTrigger, rightTrigger)
        return capability.level == .mockTesting
    }
    
    public func sendTabletReport(status: UInt8, x: UInt16, y: UInt16) -> Bool {
        totalLocalActionsDispatched += 1
        lastTabletPoint = (status, x, y)
        lastDispatchedLabel = String(format: "LOCAL_TABLET st:0x%02X X:%d Y:%d", status, x, y)
        return capability.level == .mockTesting
    }
    
    public func sendTabletNeutral() -> Bool {
        totalLocalActionsDispatched += 1
        lastTabletPoint = (0, 0, 0)
        lastDispatchedLabel = "LOCAL_TABLET NEUTRAL"
        return capability.level == .mockTesting
    }
    
    public func disconnect() -> Bool {
        status = .notConnected
        lastDispatchedLabel = "LOCAL_DISCONNECTED"
        return true
    }
    
    public func setTestingMode(_ isTesting: Bool) {
        capability = isTesting ? .localPreview : .iOSPublicAPIBaseline
        if isTesting {
            status = .notConnected
        } else {
            if case .unsupportedDirectHID(let reason) = capability.level {
                status = .unavailable(reason: reason)
            }
        }
    }
}
