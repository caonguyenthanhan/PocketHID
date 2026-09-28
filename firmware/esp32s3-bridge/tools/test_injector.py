#!/usr/bin/env python3
"""
PocketHID Bridge Development Test Injector
------------------------------------------
WARNING: DEVELOPMENT TEST TOOL ONLY.
This is NOT a companion application and NOT for production use.
Used to craft and inject deterministic binary wire packets over BLE/Serial for firmware validation.
"""

import struct
import sys

POCKETHID_MAGIC = 0xA55A
POCKETHID_PROTOCOL_VERSION = 0x01

MSG_SYS_HELLO = 0x01
MSG_SYS_READY = 0x02
MSG_SYS_HEARTBEAT = 0x03
MSG_SYS_EMERGENCY_NEUTRAL = 0x04

MSG_KB_REPORT = 0x10
MSG_KB_RELEASE_ALL = 0x11
MSG_MOUSE_REPORT = 0x20
MSG_CONSUMER_CLICK = 0x30
MSG_GAMEPAD_REPORT = 0x40
MSG_TABLET_REPORT = 0x50

def crc16_ccitt(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= (byte << 8)
        for _ in range(8):
            if crc & 0x8000:
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF
            else:
                crc = (crc << 1) & 0xFFFF
    return crc

def craft_packet(msg_type: int, seq: int, payload: bytes = b"") -> bytes:
    payload_len = len(payload)
    # Header: magic (2B), version (1B), msg_type (1B), sequence_no (2B), payload_len (1B)
    header = struct.pack("<HB B H B", POCKETHID_MAGIC, POCKETHID_PROTOCOL_VERSION, msg_type, seq, payload_len)
    
    # CRC calculated over VERSION through end of PAYLOAD
    crc_target = header[2:] + payload
    crc = crc16_ccitt(crc_target)
    
    return header + payload + struct.pack("<H", crc)

def build_keyboard_packet(seq: int, modifiers: int, keycodes: list) -> bytes:
    # 8 bytes: modifiers (1B), reserved (1B), 6 keycodes (6B)
    padded_keys = (keycodes + [0]*6)[:6]
    payload = struct.pack("<BB6B", modifiers, 0, *padded_keys)
    return craft_packet(MSG_KB_REPORT, seq, payload)

def build_mouse_packet(seq: int, buttons: int, dx: int, dy: int, wheel: int) -> bytes:
    # 4 bytes: buttons (1B), dx (1B signed), dy (1B signed), wheel (1B signed)
    payload = struct.pack("<Bbbb", buttons, dx, dy, wheel)
    return craft_packet(MSG_MOUSE_REPORT, seq, payload)

def build_consumer_packet(seq: int, usage_code: int) -> bytes:
    # 2 bytes: usage_code (2B uint16)
    payload = struct.pack("<H", usage_code)
    return craft_packet(MSG_CONSUMER_CLICK, seq, payload)

def build_gamepad_packet(seq: int, buttons: int, hat: int, lx: int, ly: int, rx: int, ry: int, lt: int, rt: int) -> bytes:
    # 13 bytes: buttons (2B), hat (1B), lx (2B signed), ly (2B signed), rx (2B signed), ry (2B signed), lt (1B), rt (1B)
    payload = struct.pack("<H B hh hh BB", buttons, hat, lx, ly, rx, ry, lt, rt)
    return craft_packet(MSG_GAMEPAD_REPORT, seq, payload)

def build_tablet_packet(seq: int, status: int, x: int, y: int) -> bytes:
    # 5 bytes: status (1B), x (2B uint16), y (2B uint16)
    payload = struct.pack("<BHH", status, x, y)
    return craft_packet(MSG_TABLET_REPORT, seq, payload)

def main():
    print("==================================================")
    print("PocketHID Development Test Injector (Packets Test)")
    print("==================================================")
    
    kb_pkt = build_keyboard_packet(1, 0x02, [0x04]) # Shift + 'a'
    print(f"Shift+A Packet ({len(kb_pkt)} bytes): {kb_pkt.hex().upper()}")
    assert len(kb_pkt) == 17 # 7 hdr + 8 payload + 2 crc
    
    mouse_pkt = build_mouse_packet(2, 0x01, 10, -5, 0)
    print(f"Mouse Click Packet ({len(mouse_pkt)} bytes): {mouse_pkt.hex().upper()}")
    assert len(mouse_pkt) == 13 # 7 hdr + 4 payload + 2 crc
    
    tablet_pkt = build_tablet_packet(3, 0x03, 16384, 16384)
    print(f"Tablet Center Packet ({len(tablet_pkt)} bytes): {tablet_pkt.hex().upper()}")
    assert len(tablet_pkt) == 14 # 7 hdr + 5 payload + 2 crc
    
    print("All packet crafting assertions PASSED.")

if __name__ == "__main__":
    main()
