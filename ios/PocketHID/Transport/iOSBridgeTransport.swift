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

    // BLE Write Queue
    private var writeQueue: [Data] = []
    private let maxQueueChunks = 200

    // Configuration placeholders (Hardware-dependent, currently PENDING)
    private let targetDeviceName = "PocketHID-Bridge" // PENDING

    // UUIDs from PocketHID Bridge Specification
    private let bridgeServiceUUID = CBUUID(string: "A55A0001-E234-4B56-8A78-9ABCDEF01234")
    private let bridgeCharacteristicRxUUID = CBUUID(string: "A55A0002-E234-4B56-8A78-9ABCDEF01234") // WriteWithoutResponse

    public init() {
        self.capability = .externalBridgeGATT
        self.status = .notConnected
        super.init()
    }

    // MARK: - Lifecycle API

    public func connect() {
        guard status == .notConnected || isErrorStatus(status) else { return }

        status = .connecting(deviceName: targetDeviceName)

        if centralManager == nil {
            centralManager = CBCentralManager(delegate: self, queue: nil)
        } else if centralManager?.state == .poweredOn {
            centralManager?.scanForPeripherals(withServices: [bridgeServiceUUID], options: nil)
        }
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
        let kbNeutral = HIDReportBuilder.buildKeyboardReport(keyCodes: [], modifiers: 0)
        _ = sendRawReport(endpoint: kbNeutral.endpoint, payload: kbNeutral.payload)
        let mouseNeutral = HIDReportBuilder.buildMouseMove(dx: 0, dy: 0, buttons: 0, wheel: 0)
        _ = sendRawReport(endpoint: mouseNeutral.endpoint, payload: mouseNeutral.payload)
        let tabletNeutral = HIDReportBuilder.buildTabletNeutral()
        _ = sendRawReport(endpoint: tabletNeutral.endpoint, payload: tabletNeutral.payload)

        flushQueue()

        // Cancel peripheral connection
        if let peripheral = connectedPeripheral {
            centralManager?.cancelPeripheralConnection(peripheral)
        } else {
            status = .notConnected
        }

        // Note: We don't set status = .notConnected here if we successfully called cancelPeripheralConnection,
        // because the didDisconnectPeripheral delegate callback will handle the final transition.
        return true
    }

    private func disconnectCleanup() {
        if let peripheral = connectedPeripheral {
            centralManager?.cancelPeripheralConnection(peripheral)
        }
        connectedPeripheral = nil
        txCharacteristic = nil
        writeQueue.removeAll()
    }

    // MARK: - HIDTransport Protocol Implementation

    public func sendRawReport(endpoint: UInt8, payload: [UInt8]) -> Bool {
        let msgType: WireEncoder.MessageType
        switch endpoint {
        case 1: msgType = .kbReport
        case 2: msgType = .mouseReport
        case 3: msgType = .consumerClick
        case 4: msgType = .gamepadReport
        case 5: msgType = .tabletReport
        default: return false
        }
        return dispatchToBLE(msgType: msgType, payload: payload)
    }

    // MARK: - Core Dispatch Boundary

    private var sequenceNumber: UInt16 = 1

    /// Encapsulates the opaque HID payload into the bridge wire envelope (without content inspection)
    /// and writes it to the GATT TX characteristic.
    private func dispatchToBLE(msgType: WireEncoder.MessageType, payload: [UInt8]) -> Bool {
        let isConnected: Bool
        if case .connected = status { isConnected = true } else { isConnected = false }

        // Allow dispatch ONLY if CONNECTED or DISCONNECTING (for safety flush)
        guard isConnected || status == .disconnecting else {
            return false
        }

        guard let peripheral = connectedPeripheral else {
            return false
        }

        guard let wireEnvelope = WireEncoder.encode(msgType: msgType, sequenceNo: sequenceNumber, payload: payload) else {
            return false
        }
        sequenceNumber = sequenceNumber &+ 1

        let maxWrite = peripheral.maximumWriteValueLength(for: .withoutResponse)
        guard maxWrite > 0 else {
            status = .error(message: "Invalid BLE maximumWriteValueLength (0)")
            disconnectCleanup()
            return false
        }

        // Calculate chunks
        var chunks: [Data] = []
        var offset = 0
        while offset < wireEnvelope.count {
            let length = min(maxWrite, wireEnvelope.count - offset)
            let chunk = Data(wireEnvelope[offset..<(offset + length)])
            chunks.append(chunk)
            offset += length
        }

        // Enqueue if there is space
        if writeQueue.count + chunks.count > maxQueueChunks {
            status = .error(message: "BLE write queue overflow")
            disconnectCleanup()
            return false
        }

        writeQueue.append(contentsOf: chunks)
        flushQueue()

        return true
    }

    private func flushQueue() {
        guard let peripheral = connectedPeripheral, let tx = txCharacteristic else {
            writeQueue.removeAll()
            return
        }

        while !writeQueue.isEmpty && peripheral.canSendWriteWithoutResponse {
            let chunk = writeQueue.removeFirst()
            peripheral.writeValue(chunk, for: tx, type: .withoutResponse)
        }
    }

    // MARK: - CoreBluetooth Delegates

    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            if case .connecting = status {
                central.scanForPeripherals(withServices: [bridgeServiceUUID], options: nil)
            }
        case .poweredOff:
            status = .error(message: "Bluetooth is powered off")
            disconnectCleanup()
        case .unauthorized:
            status = .error(message: "Bluetooth is unauthorized")
            disconnectCleanup()
        case .unsupported:
            status = .error(message: "Bluetooth is unsupported")
            disconnectCleanup()
        default:
            break
        }
    }

    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        centralManager?.stopScan()
        connectedPeripheral = peripheral
        peripheral.delegate = self
        centralManager?.connect(peripheral, options: nil)
    }

    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.discoverServices([bridgeServiceUUID])
    }

    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        connectedPeripheral = nil
        txCharacteristic = nil

        if case .disconnecting = status {
            status = .notConnected
        } else {
            status = .error(message: error?.localizedDescription ?? "Disconnected")
        }
    }

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard error == nil else {
            status = .error(message: error!.localizedDescription)
            return
        }

        guard let services = peripheral.services, let service = services.first(where: { $0.uuid == bridgeServiceUUID }) else {
            status = .error(message: "Bridge service not found")
            return
        }

        peripheral.discoverCharacteristics([bridgeCharacteristicRxUUID], for: service)
    }

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard error == nil else {
            status = .error(message: error!.localizedDescription)
            return
        }

        guard let characteristics = service.characteristics, let characteristic = characteristics.first(where: { $0.uuid == bridgeCharacteristicRxUUID }) else {
            status = .error(message: "Bridge RX characteristic not found")
            return
        }

        guard characteristic.properties.contains(.writeWithoutResponse) else {
            status = .error(message: "Bridge RX characteristic lacks writeWithoutResponse property")
            disconnectCleanup()
            return
        }

        txCharacteristic = characteristic
        status = .connected(deviceName: peripheral.name ?? targetDeviceName)
    }

    public func peripheralIsReady(toSendWriteWithoutResponse peripheral: CBPeripheral) {
        flushQueue()
    }
}
