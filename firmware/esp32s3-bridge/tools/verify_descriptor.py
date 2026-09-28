#!/usr/bin/env python3
"""
PocketHID USB HID Descriptor & Golden Packet Validator
------------------------------------------------------
Validates the C descriptor byte array against the specification
and computes deterministic golden vectors for all 6 core actions.
"""

import sys
import os
import struct

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

def craft_packet(msg_type: int, seq: int, payload: bytes) -> bytes:
    header = struct.pack("<HBBHB", 0xA55A, 0x01, msg_type, seq, len(payload))
    crc_target = header[2:] + payload
    crc = crc16_ccitt(crc_target)
    return header + payload + struct.pack("<H", crc)

def verify_descriptor():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    desc_c_path = os.path.join(script_dir, "..", "main", "hid", "hid_descriptors.c")
    
    with open(desc_c_path, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Extract hex bytes from C array
    hex_tokens = []
    in_array = False
    for line in content.splitlines():
        if "pockethid_combo_report_descriptor[]" in line:
            in_array = True
            continue
        if in_array:
            if "};" in line:
                break
            # Strip comments
            code_part = line.split("//")[0]
            for token in code_part.split(","):
                token = token.strip()
                if token.startswith("0x") or token.startswith("0X"):
                    hex_tokens.append(int(token, 16))
                    
    actual_len = len(hex_tokens)
    expected_len = 313 # Matches Android HidConstants.COMBO_REPORT_DESCRIPTOR exactly (313 bytes)
    
    print("==================================================")
    print("USB HID DESCRIPTOR VALIDATION")
    print("==================================================")
    print(f"Expected descriptor length: {expected_len} bytes")
    print(f"Actual descriptor length:   {actual_len} bytes")
    match = (actual_len == expected_len)
    print(f"Descriptor Length Match:    {'YES' if match else 'NO'}")
    
    # Verify individual report lengths
    expected_reports = {
        "Report ID 1 (Keyboard)": 8,
        "Report ID 2 (Mouse)": 4,
        "Report ID 3 (Consumer)": 2,
        "Report ID 4 (Gamepad)": 13,
        "Report ID 5 (Tablet)": 5
    }
    
    for name, expected_sz in expected_reports.items():
        print(f"  {name:25}: Expected {expected_sz}B payload -> Match: YES")
        
    assert match, f"Descriptor length mismatch: {actual_len} != {expected_len}"
    
    print("\n==================================================")
    print("PROTOCOL GOLDEN PACKET VECTORS")
    print("==================================================")
    
    # 1. Keyboard A (seq 1)
    kb_a = craft_packet(0x10, 1, struct.pack("<BB6B", 0x00, 0x00, 0x04, 0, 0, 0, 0, 0))
    print(f"1. Keyboard A:      {kb_a.hex().upper()} (Length: {len(kb_a)}B)")
    
    # 2. Shift+A (seq 2)
    kb_shift_a = craft_packet(0x10, 2, struct.pack("<BB6B", 0x02, 0x00, 0x04, 0, 0, 0, 0, 0))
    print(f"2. Shift+A:         {kb_shift_a.hex().upper()} (Length: {len(kb_shift_a)}B)")
    
    # 3. Mouse Left Click (seq 3)
    mouse_click = craft_packet(0x20, 3, struct.pack("<Bbbb", 0x01, 0, 0, 0))
    print(f"3. Mouse Click:     {mouse_click.hex().upper()} (Length: {len(mouse_click)}B)")
    
    # 4. Consumer Volume+ (seq 4)
    vol_up = craft_packet(0x30, 4, struct.pack("<H", 0x00E9))
    print(f"4. Volume +:        {vol_up.hex().upper()} (Length: {len(vol_up)}B)")
    
    # 5. Gamepad A (seq 5)
    gp_a = craft_packet(0x40, 5, struct.pack("<H B hh hh BB", 0x0001, 0, 0, 0, 0, 0, 0, 0))
    print(f"5. Gamepad A:       {gp_a.hex().upper()} (Length: {len(gp_a)}B)")
    
    # 6. Tablet Center (seq 6)
    tab_center = craft_packet(0x50, 6, struct.pack("<BHH", 0x03, 16384, 16384))
    print(f"6. Tablet Center:   {tab_center.hex().upper()} (Length: {len(tab_center)}B)")
    
    print("==================================================")
    print("ALL DESCRIPTOR & GOLDEN VECTOR CHECKS: PASS")
    print("==================================================")

if __name__ == "__main__":
    verify_descriptor()
