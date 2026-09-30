import XCTest
@testable import PocketHID

final class WireEncoderTests: XCTestCase {

    func testCRC16CCITT() {
        // Golden vector test for CRC
        // Payload: 01 10 01 00 08 00 00 04 00 00 00 00 00
        let data: [UInt8] = [0x01, 0x10, 0x01, 0x00, 0x08, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00]
        let crc = WireEncoder.calculateCRC(data: data)
        XCTAssertEqual(crc, 0xAD28, "CRC calculation should exactly match Golden Vectors")
    }

    func testVector1_KeyboardA() {
        // Seq: 1  MSG_TYPE: 0x10 (MSG_KB_REPORT)  PAYLOAD_LEN: 8
        // Hex: 5A A5 01 10 01 00 08 00 00 04 00 00 00 00 00 28 AD
        let payload: [UInt8] = [0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x10, 0x01, 0x00, 0x08, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x28, 0xAD]
        
        let packet = WireEncoder.encode(msgType: .kbReport, sequenceNo: 1, payload: payload)
        XCTAssertEqual(packet, expected)
    }

    func testVector2_ShiftA() {
        // Seq: 2  MSG_TYPE: 0x10 (MSG_KB_REPORT)  PAYLOAD_LEN: 8
        // Hex: 5A A5 01 10 02 00 08 02 00 04 00 00 00 00 00 74 5A
        let payload: [UInt8] = [0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x10, 0x02, 0x00, 0x08, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x74, 0x5A]
        
        let packet = WireEncoder.encode(msgType: .kbReport, sequenceNo: 2, payload: payload)
        XCTAssertEqual(packet, expected)
    }

    func testVector3_MouseLeftClick() {
        // Seq: 3  MSG_TYPE: 0x20 (MSG_MOUSE_REPORT)  PAYLOAD_LEN: 4
        // Hex: 5A A5 01 20 03 00 04 01 00 00 00 09 AF
        let payload: [UInt8] = [0x01, 0x00, 0x00, 0x00]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x20, 0x03, 0x00, 0x04, 0x01, 0x00, 0x00, 0x00, 0x09, 0xAF]
        
        let packet = WireEncoder.encode(msgType: .mouseReport, sequenceNo: 3, payload: payload)
        XCTAssertEqual(packet, expected)
    }

    func testVector4_VolumeUp() {
        // Seq: 4  MSG_TYPE: 0x30 (MSG_CONSUMER_CLICK)  PAYLOAD_LEN: 2
        // Hex: 5A A5 01 30 04 00 02 E9 00 6F 2B
        let payload: [UInt8] = [0xE9, 0x00]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x30, 0x04, 0x00, 0x02, 0xE9, 0x00, 0x6F, 0x2B]
        
        let packet = WireEncoder.encode(msgType: .consumerClick, sequenceNo: 4, payload: payload)
        XCTAssertEqual(packet, expected)
    }

    func testVector5_GamepadA() {
        // Seq: 5  MSG_TYPE: 0x40 (MSG_GAMEPAD_REPORT)  PAYLOAD_LEN: 13
        // Hex: 5A A5 01 40 05 00 0D 01 00 00 00 00 00 00 00 00 00 00 00 00 00 A4 CD
        let payload: [UInt8] = [0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x40, 0x05, 0x00, 0x0D, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xA4, 0xCD]
        
        let packet = WireEncoder.encode(msgType: .gamepadReport, sequenceNo: 5, payload: payload)
        XCTAssertEqual(packet, expected)
    }

    func testVector6_TabletCenter() {
        // Seq: 6  MSG_TYPE: 0x50 (MSG_TABLET_REPORT)  PAYLOAD_LEN: 5
        // Hex: 5A A5 01 50 06 00 05 03 00 40 00 40 2A FA
        let payload: [UInt8] = [0x03, 0x00, 0x40, 0x00, 0x40]
        let expected: [UInt8] = [0x5A, 0xA5, 0x01, 0x50, 0x06, 0x00, 0x05, 0x03, 0x00, 0x40, 0x00, 0x40, 0x2A, 0xFA]
        
        let packet = WireEncoder.encode(msgType: .tabletReport, sequenceNo: 6, payload: payload)
        XCTAssertEqual(packet, expected)
    }
    
    func testVector_EmergencyNeutral() {
        // Seq: 7, MSG_TYPE: 0x04 (MSG_SYS_EMERGENCY_NEUTRAL), PAYLOAD_LEN: 0
        let payload: [UInt8] = []
        // Expected payload len 0, packet: 5A A5 01 04 07 00 00 [CRC]
        let packet = WireEncoder.encode(msgType: .sysEmergencyNeutral, sequenceNo: 7, payload: payload)
        
        XCTAssertNotNil(packet)
        XCTAssertEqual(packet?.count, 9)
        XCTAssertEqual(packet?[6], 0x00) // length
    }
    
    func testVector_Heartbeat() {
        // Seq: 8, MSG_TYPE: 0x03, len: 4
        let payload: [UInt8] = [0x01, 0x02, 0x03, 0x04] // 4 bytes timestamp
        let packet = WireEncoder.encode(msgType: .sysHeartbeat, sequenceNo: 8, payload: payload)
        XCTAssertNotNil(packet)
        XCTAssertEqual(packet?.count, 13) // 7 + 4 + 2
    }
    
    func testVector_SysHello() {
        // Hello is 12 bytes
        let payload: [UInt8] = Array(repeating: 0x00, count: 12)
        let packet = WireEncoder.encode(msgType: .sysHello, sequenceNo: 9, payload: payload)
        XCTAssertNotNil(packet)
        XCTAssertEqual(packet?.count, 21)
    }

    func testInvalidInput_PayloadTooLarge() {
        let payload: [UInt8] = Array(repeating: 0x00, count: 65)
        let packet = WireEncoder.encode(msgType: .kbReport, sequenceNo: 1, payload: payload)
        XCTAssertNil(packet, "Should return nil if payload length > 64")
    }

    func testSequenceWraparound() {
        // Let's test that 65535 is encoded correctly and wraparound happens at caller level usually, but here we can just test 0xFFFF
        let payload: [UInt8] = [0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00]
        let packet = WireEncoder.encode(msgType: .kbReport, sequenceNo: 65535, payload: payload)
        
        XCTAssertEqual(packet?[4], 0xFF)
        XCTAssertEqual(packet?[5], 0xFF)
    }
}
