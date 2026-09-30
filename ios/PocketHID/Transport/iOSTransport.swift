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
    
    public func sendRawReport(endpoint: UInt8, payload: [UInt8]) -> Bool {
        totalLocalActionsDispatched += 1
        switch endpoint {
        case 1:
            let modifiers = payload.count > 0 ? payload[0] : 0
            let primaryCode = payload.count > 2 ? payload[2] : 0
            lastScancodeHex = String(format: "0x%02X", primaryCode)
            lastDispatchedLabel = primaryCode == 0 ? "LOCAL_KEY_RELEASE" : String(format: "LOCAL_KEY 0x%02X (mod: 0x%02X)", primaryCode, modifiers)
        case 2:
            let buttons = payload.count > 0 ? payload[0] : 0
            let dx = payload.count > 1 ? Int8(bitPattern: payload[1]) : 0
            let dy = payload.count > 2 ? Int8(bitPattern: payload[2]) : 0
            let wheel = payload.count > 3 ? Int8(bitPattern: payload[3]) : 0
            lastDispatchedLabel = "LOCAL_MOUSE dx:\(dx) dy:\(dy) btn:\(buttons) whl:\(wheel)"
        case 3:
            let usageCode = payload.count > 1 ? (UInt16(payload[1]) << 8) | UInt16(payload[0]) : 0
            lastScancodeHex = String(format: "0x%04X", usageCode)
            lastDispatchedLabel = String(format: "LOCAL_CONSUMER 0x%04X", usageCode)
        case 4:
            let buttons = payload.count > 1 ? (UInt16(payload[1]) << 8) | UInt16(payload[0]) : 0
            let lt = payload.count > 6 ? payload[6] : 0
            let rt = payload.count > 7 ? payload[7] : 0
            lastDispatchedLabel = String(format: "LOCAL_GAMEPAD btn:0x%04X LT:%d RT:%d", buttons, lt, rt)
        case 5:
            let status = payload.count > 0 ? payload[0] : 0
            let x = payload.count > 2 ? (UInt16(payload[2]) << 8) | UInt16(payload[1]) : 0
            let y = payload.count > 4 ? (UInt16(payload[4]) << 8) | UInt16(payload[3]) : 0
            lastTabletPoint = (status, x, y)
            if status == 0 && x == 0 && y == 0 {
                lastDispatchedLabel = "LOCAL_TABLET NEUTRAL"
            } else {
                lastDispatchedLabel = String(format: "LOCAL_TABLET st:0x%02X X:%d Y:%d", status, x, y)
            }
        default:
            lastDispatchedLabel = "LOCAL_UNKNOWN ep:\(endpoint)"
        }
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
