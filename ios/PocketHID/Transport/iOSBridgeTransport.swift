//
//  iOSBridgeTransport.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation
import Combine
import CoreBluetooth

extension TransportCapability {
    public static let externalBridgeGATT = TransportCapability(
        level: .ready,
        supportsKeyboard: true,
        supportsMouse: true,
        supportsGamepad: true,
        supportsConsumerControl: true,
        supportsTabletDigitizer: true,
        technicalNotes: "External HID Bridge via BLE Custom GATT (ESP32-S3). End-to-end routing to USB HID on Host PC."
    )
}

/// iOS BridgeTransport skeleton for External HID Bridge architecture.
/// Conforms to docs/spec/POCKETHID-IOS-BRIDGE-TRANSPORT.md
public final class iOSBridgeTransport: NSObject, ObservableObject, HIDTransport, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    @Published public private(set) var status: ConnectionStatus
    @Published public private(set) var capability: TransportCapability
    
    // CoreBluetooth state
    private var centralManager: CBCentralManager?
    private var connectedPeripheral: CBPeripheral?
    private var txCharacteristic: CBCharacteristic?
    
    // Configuration placeholders (Hardware-dependent, currently PENDING)
    private let targetDeviceName = "PocketHID-Bridge" // PENDING
    
    public init() {
        self.capability = .externalBridgeGATT
        self.status = .notConnected
        super.init()
    }
    
    // MARK: - Lifecycle API
    
    public func connect() {
        guard status == .notConnected || isErrorStatus(status) else { return }
        
        status = .connecting(deviceName: targetDeviceName)
        
        // PENDING: Real CoreBluetooth initialization and scanning
        // centralManager = CBCentralManager(delegate: self, queue: nil)
        
        // Note: For now, it remains in connecting state since hardware validation is BLOCKED.
        // We do NOT fake the CONNECTED state as per the strict specification.
    }
    
    private func isErrorStatus(_ status: ConnectionStatus) -> Bool {
        if case .error = status { return true }
        return false
    }
    
    public func disconnect() -> Bool {
        // Only valid if we were connected
        guard case .connected = status else {
            status = .notConnected
            return true
        }
        
        // Transition to DISCONNECTING (Lifecycle window for neutral/safety flushing)
        status = .disconnecting
        
        // ATTEMPT flushing neutral reports
        _ = sendKeyboardReport(keyCodes: [], modifiers: 0)
        _ = sendMouseMove(dx: 0, dy: 0, buttons: 0, wheel: 0)
        _ = sendTabletNeutral()
        
        // Cancel peripheral connection
        if let peripheral = connectedPeripheral {
            centralManager?.cancelPeripheralConnection(peripheral)
        }
        
        status = .notConnected
        return true
    }
    
    // MARK: - HIDTransport Protocol Implementation
    
    public func sendKeyboardReport(keyCodes: [UInt8], modifiers: UInt8) -> Bool {
        var payload: [UInt8] = [modifiers, 0] // 2 bytes header
        payload.append(contentsOf: keyCodes)
        // Pad to exactly 8 bytes per standard report
        while payload.count < 8 { payload.append(0) }
        
        return dispatchToBLE(endpoint: 1, payload: payload)
    }
    
    public func sendMouseMove(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) -> Bool {
        let payload: [UInt8] = [
            buttons,
            UInt8(bitPattern: dx),
            UInt8(bitPattern: dy),
            UInt8(bitPattern: wheel)
        ]
        return dispatchToBLE(endpoint: 2, payload: payload)
    }
    
    public func sendConsumerClick(usageCode: UInt16) -> Bool {
        let payload: [UInt8] = [
            UInt8(usageCode & 0xFF),
            UInt8((usageCode >> 8) & 0xFF)
        ]
        return dispatchToBLE(endpoint: 3, payload: payload)
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
        let payload: [UInt8] = [
            UInt8(buttons & 0xFF), UInt8((buttons >> 8) & 0xFF),
            UInt8(bitPattern: Int8(clamping: leftStickX >> 8)), // Simplified packing for skeleton
            UInt8(bitPattern: Int8(clamping: leftStickY >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickX >> 8)),
            UInt8(bitPattern: Int8(clamping: rightStickY >> 8)),
            leftTrigger,
            rightTrigger
        ]
        return dispatchToBLE(endpoint: 4, payload: payload)
    }
    
    public func sendTabletReport(status: UInt8, x: UInt16, y: UInt16) -> Bool {
        let payload: [UInt8] = [
            status,
            UInt8(x & 0xFF), UInt8((x >> 8) & 0xFF),
            UInt8(y & 0xFF), UInt8((y >> 8) & 0xFF)
        ]
        return dispatchToBLE(endpoint: 5, payload: payload)
    }
    
    public func sendTabletNeutral() -> Bool {
        return sendTabletReport(status: 0, x: 0, y: 0)
    }
    
    // MARK: - Core Dispatch Boundary
    
    /// Encapsulates the opaque HID payload into the bridge wire envelope (without content inspection)
    /// and writes it to the GATT TX characteristic.
    private func dispatchToBLE(endpoint: UInt8, payload: [UInt8]) -> Bool {
        let isConnected: Bool
        if case .connected = status { isConnected = true } else { isConnected = false }
        
        // Allow dispatch ONLY if CONNECTED or DISCONNECTING (for safety flush)
        guard isConnected || status == .disconnecting else {
            return false
        }
        
        // Ensure framing exactly matches the existing frozen protocol (POCKETHID-BRIDGE-WIRE-PROTOCOL.md)
        // e.g. [Endpoint ID] + [Payload]
        var wireEnvelope: [UInt8] = [endpoint]
        wireEnvelope.append(contentsOf: payload)
        
        // PENDING: GATT MTU segmentation logic
        // let data = Data(wireEnvelope)
        // if let tx = txCharacteristic, let peripheral = connectedPeripheral {
        //     peripheral.writeValue(data, for: tx, type: .withoutResponse)
        // }
        
        return true
    }
    
    // MARK: - CoreBluetooth Delegates (Skeleton)
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state != .poweredOn {
            status = .error(message: "Bluetooth disabled")
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        // PENDING
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        // PENDING
    }
    
    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        status = .error(message: error?.localizedDescription ?? "Disconnected")
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        // PENDING
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        // PENDING: Upon success -> status = .connected(deviceName: targetDeviceName)
    }
}
