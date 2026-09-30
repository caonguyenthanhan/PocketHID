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

    private let transportQueue = DispatchQueue(label: "PocketHID.BridgeTransport")

    private var _status: ConnectionStatus = .notConnected {
        didSet {
            let newStatus = _status
            DispatchQueue.main.async { [weak self] in
                self?.status = newStatus
            }
        }
    }

    // CoreBluetooth state
    private var centralManager: CBCentralManager?
    private var connectedPeripheral: CBPeripheral?
    private var txCharacteristic: CBCharacteristic?

    // BLE Write Queue
    private var writeQueue: [Data] = []

    // Configuration placeholders (Hardware-dependent, currently PENDING)
    private let targetDeviceName = "PocketHID-Bridge" // PENDING

    // UUIDs from PocketHID Bridge Specification
    private let bridgeServiceUUID = CBUUID(string: "A55A0001-E234-4B56-8A78-9ABCDEF01234")
    private let bridgeCharacteristicRxUUID = CBUUID(string: "A55A0002-E234-4B56-8A78-9ABCDEF01234") // WriteWithoutResponse

    public init() {
        self.capability = .externalBridgeGATT
        self.status = .notConnected
        self._status = .notConnected
        super.init()
    }

    // MARK: - Lifecycle API

    public func connect() {
        transportQueue.async {
            self.connectOnQueue()
        }
    }

    private func connectOnQueue() {
        guard _status == .notConnected || isErrorStatus(_status) else { return }

        _status = .connecting(deviceName: targetDeviceName)

        if centralManager == nil {
            centralManager = CBCentralManager(delegate: self, queue: transportQueue)
        } else if centralManager?.state == .poweredOn {
            centralManager?.scanForPeripherals(withServices: [bridgeServiceUUID], options: nil)
        }
    }

    private func isErrorStatus(_ status: ConnectionStatus) -> Bool {
        if case .error = status { return true }
        return false
    }

    public func disconnect() -> Bool {
        return transportQueue.sync {
            return disconnectOnQueue()
        }
    }

    private func disconnectOnQueue() -> Bool {
        // Only valid if we were connected
        guard case .connected = _status else {
            _status = .notConnected
            return true
        }

        // Transition to DISCONNECTING
        _status = .disconnecting

        // Clear outbound queue to prevent normal sends from delaying disconnect
        writeQueue.removeAll()

        // ATTEMPT flushing neutral reports locally.
        // We cannot guarantee these leave the radio before cancelPeripheralConnection,
        // but the Bridge firmware's BLE_GAP_EVENT_DISCONNECT hook guarantees host neutralization.
        let kbNeutral = HIDReportBuilder.buildKeyboardReport(keyCodes: [], modifiers: 0)
        _ = dispatchToBLEOnQueue(msgType: .kbReport, payload: kbNeutral.payload)

        let mouseNeutral = HIDReportBuilder.buildMouseMove(dx: 0, dy: 0, buttons: 0, wheel: 0)
        _ = dispatchToBLEOnQueue(msgType: .mouseReport, payload: mouseNeutral.payload)

        let gamepadNeutral = HIDReportBuilder.buildGamepadReport(buttons: 0, leftStickX: 0, leftStickY: 0, rightStickX: 0, rightStickY: 0, leftTrigger: 0, rightTrigger: 0)
        _ = dispatchToBLEOnQueue(msgType: .gamepadReport, payload: gamepadNeutral.payload)

        let tabletNeutral = HIDReportBuilder.buildTabletNeutral()
        _ = dispatchToBLEOnQueue(msgType: .tabletReport, payload: tabletNeutral.payload)

        flushQueueOnQueue()

        // Cancel peripheral connection
        if let peripheral = connectedPeripheral {
            centralManager?.cancelPeripheralConnection(peripheral)
        } else {
            _status = .notConnected
        }

        // Note: We don't set _status = .notConnected here if we successfully called cancelPeripheralConnection,
        // because the didDisconnectPeripheral delegate callback will handle the final transition.
        return true
    }

    private func disconnectCleanupOnQueue() {
        if let peripheral = connectedPeripheral {
            centralManager?.cancelPeripheralConnection(peripheral)
        }
        connectedPeripheral = nil
        txCharacteristic = nil
        writeQueue.removeAll()
    }

    // MARK: - HIDTransport Protocol Implementation

    public func sendRawReport(endpoint: UInt8, payload: [UInt8]) -> Bool {
        return transportQueue.sync {
            let msgType: WireEncoder.MessageType
            switch endpoint {
            case 1: msgType = .kbReport
            case 2: msgType = .mouseReport
            case 3: msgType = .consumerClick
            case 4: msgType = .gamepadReport
            case 5: msgType = .tabletReport
            default: return false
            }
            return dispatchToBLEOnQueue(msgType: msgType, payload: payload)
        }
    }

    // MARK: - Core Dispatch Boundary

    private var sequenceNumber: UInt16 = 1

    private let maxQueueDepth = 50
    
    /// Encapsulates the opaque HID payload into the bridge wire envelope (without content inspection)
    /// and writes it to the GATT TX characteristic.
    private func dispatchToBLEOnQueue(msgType: WireEncoder.MessageType, payload: [UInt8]) -> Bool {
        let isConnected: Bool
        if case .connected = _status { isConnected = true } else { isConnected = false }

        // Allow dispatch ONLY if CONNECTED or DISCONNECTING (for safety flush)
        guard isConnected || _status == .disconnecting else {
            return false
        }

        // Validate queue bounds BEFORE accepting and encoding
        if writeQueue.count >= maxQueueDepth {
            _status = .error(message: "BLE write queue overflow")
            disconnectCleanupOnQueue()
            return false
        }

        guard let wireEnvelope = WireEncoder.encode(msgType: msgType, sequenceNo: sequenceNumber, payload: payload) else {
            return false
        }
        sequenceNumber = sequenceNumber &+ 1

        let data = Data(wireEnvelope)
        writeQueue.append(data)
        flushQueueOnQueue()

        return true
    }

    private func flushQueueOnQueue() {
        guard let peripheral = connectedPeripheral, let tx = txCharacteristic else {
            writeQueue.removeAll()
            return
        }

        while !writeQueue.isEmpty && peripheral.canSendWriteWithoutResponse {
            let packet = writeQueue.removeFirst()
            peripheral.writeValue(packet, for: tx, type: .withoutResponse)
        }
    }

    // MARK: - CoreBluetooth Delegates

    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            if case .connecting = _status {
                central.scanForPeripherals(withServices: [bridgeServiceUUID], options: nil)
            }
        case .poweredOff:
            _status = .error(message: "Bluetooth is powered off")
            disconnectCleanupOnQueue()
        case .unauthorized:
            _status = .error(message: "Bluetooth is unauthorized")
            disconnectCleanupOnQueue()
        case .unsupported:
            _status = .error(message: "Bluetooth is unsupported")
            disconnectCleanupOnQueue()
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
        writeQueue.removeAll()

        if case .disconnecting = _status {
            _status = .notConnected
        } else {
            _status = .error(message: error?.localizedDescription ?? "Disconnected")
        }
    }

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard error == nil else {
            _status = .error(message: error!.localizedDescription)
            return
        }

        guard let services = peripheral.services, let service = services.first(where: { $0.uuid == bridgeServiceUUID }) else {
            _status = .error(message: "Bridge service not found")
            return
        }

        peripheral.discoverCharacteristics([bridgeCharacteristicRxUUID], for: service)
    }

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard error == nil else {
            _status = .error(message: error!.localizedDescription)
            return
        }

        guard let characteristics = service.characteristics, let characteristic = characteristics.first(where: { $0.uuid == bridgeCharacteristicRxUUID }) else {
            _status = .error(message: "Bridge RX characteristic not found")
            return
        }

        guard characteristic.properties.contains(.writeWithoutResponse) else {
            _status = .error(message: "Bridge RX characteristic lacks writeWithoutResponse property")
            disconnectCleanupOnQueue()
            return
        }

        txCharacteristic = characteristic
        _status = .connected(deviceName: peripheral.name ?? targetDeviceName)
    }

    public func peripheralIsReady(toSendWriteWithoutResponse peripheral: CBPeripheral) {
        flushQueueOnQueue()
    }
}
