import Foundation

public struct WireEncoder {
    
    public enum MessageType: UInt8 {
        case sysHello = 0x01
        case sysReady = 0x02
        case sysHeartbeat = 0x03
        case sysEmergencyNeutral = 0x04
        
        case kbReport = 0x10
        case kbReleaseAll = 0x11
        
        case mouseReport = 0x20
        case consumerClick = 0x30
        case gamepadReport = 0x40
        case tabletReport = 0x50
    }
    
    private static let magic: UInt16 = 0xA55A
    private static let version: UInt8 = 0x01
    
    // Calculates CRC-16-CCITT-False
    public static func calculateCRC(data: [UInt8]) -> UInt16 {
        var crc: UInt16 = 0xFFFF
        for byte in data {
            crc ^= (UInt16(byte) << 8)
            for _ in 0..<8 {
                if (crc & 0x8000) != 0 {
                    crc = (crc << 1) ^ 0x1021
                } else {
                    crc = crc << 1
                }
            }
        }
        return crc
    }
    
    public static func encode(msgType: MessageType, sequenceNo: UInt16, payload: [UInt8]) -> [UInt8]? {
        guard payload.count <= 64 else { return nil }
        
        var packet: [UInt8] = []
        
        // 1. MAGIC
        packet.append(contentsOf: withUnsafeBytes(of: magic.littleEndian) { Array($0) })
        
        // 2. VERSION
        packet.append(version)
        
        // 3. MSG_TYPE
        packet.append(msgType.rawValue)
        
        // 4. SEQUENCE_NO
        packet.append(contentsOf: withUnsafeBytes(of: sequenceNo.littleEndian) { Array($0) })
        
        // 5. PAYLOAD_LEN
        packet.append(UInt8(payload.count))
        
        // 6. PAYLOAD
        packet.append(contentsOf: payload)
        
        // 7. CRC
        // CRC is calculated over bytes 2..N (VERSION through end of PAYLOAD)
        let crcData = Array(packet[2...])
        let crc = calculateCRC(data: crcData)
        packet.append(contentsOf: withUnsafeBytes(of: crc.littleEndian) { Array($0) })
        
        return packet
    }
}
