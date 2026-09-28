/**
 * @file wire_protocol.h
 * @brief PocketHID Bridge Wire Protocol Definition (Binary Frame Structures)
 * 
 * Target: ESP32-S3 Firmware & Native Unit Test Harness
 * Byte Order: Little-Endian (<stdint.h> standard)
 */

#pragma once

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define POCKETHID_MAGIC                 0xA55A
#define POCKETHID_PROTOCOL_VERSION      0x01
#define POCKETHID_HEADER_SIZE           7
#define POCKETHID_CHECKSUM_SIZE         2
#define POCKETHID_MAX_PAYLOAD_SIZE      64
#define POCKETHID_MAX_PACKET_SIZE       (POCKETHID_HEADER_SIZE + POCKETHID_MAX_PAYLOAD_SIZE + POCKETHID_CHECKSUM_SIZE)

/* Message Type Categories & Identifiers */
#define MSG_SYS_HELLO                   0x01
#define MSG_SYS_READY                   0x02
#define MSG_SYS_HEARTBEAT               0x03
#define MSG_SYS_EMERGENCY_NEUTRAL       0x04

#define MSG_KB_REPORT                   0x10
#define MSG_KB_RELEASE_ALL              0x11

#define MSG_MOUSE_REPORT                0x20

#define MSG_CONSUMER_CLICK              0x30

#define MSG_GAMEPAD_REPORT              0x40

#define MSG_TABLET_REPORT               0x50

/* Fixed Packet Header (7 bytes) */
#pragma pack(push, 1)
typedef struct {
    uint16_t magic;         /**< 0xA55A (Little-Endian: 0x5A, 0xA5) */
    uint8_t  version;       /**< 0x01 */
    uint8_t  msg_type;      /**< Message type identifier */
    uint16_t sequence_no;   /**< Monotonically increasing sequence number */
    uint8_t  payload_len;   /**< Length of payload (0..64) */
} pockethid_header_t;

/* System Hello Payload (12 bytes) */
typedef struct {
    uint32_t client_random_nonce;
    uint16_t client_protocol_ver;
    uint16_t client_capabilities;
    uint32_t client_uptime_ms;
} payload_sys_hello_t;

/* System Ready Payload (8 bytes) */
typedef struct {
    uint32_t bridge_random_nonce;
    uint16_t bridge_firmware_ver;
    uint8_t  usb_host_status;
    uint8_t  active_hid_endpoints;
} payload_sys_ready_t;

/* System Heartbeat Payload (4 bytes) */
typedef struct {
    uint32_t timestamp_ms;
} payload_sys_heartbeat_t;

/* Keyboard Report Payload (8 bytes) */
typedef struct {
    uint8_t modifiers;      /**< Bitmask of modifier keys (Ctrl, Shift, Alt, GUI) */
    uint8_t reserved;       /**< 0x00 */
    uint8_t keycodes[6];    /**< Up to 6 simultaneous HID usage codes (6KRO) */
} payload_kb_report_t;

/* Mouse Report Payload (4 bytes) */
typedef struct {
    uint8_t buttons;        /**< Bit 0: Left, Bit 1: Right, Bit 2: Middle */
    int8_t  dx;             /**< Relative X delta (-127..127) */
    int8_t  dy;             /**< Relative Y delta (-127..127) */
    int8_t  wheel;          /**< Relative wheel scroll delta (-127..127) */
} payload_mouse_report_t;

/* Consumer Click Payload (2 bytes) */
typedef struct {
    uint16_t usage_code;    /**< 16-bit consumer usage (e.g. 0x00E9 for Vol+) */
} payload_consumer_click_t;

/* Gamepad Report Payload (13 bytes) */
typedef struct {
    uint16_t buttons;       /**< 16 digital buttons */
    uint8_t  hat_switch;    /**< 4-bit hat (0..8) + 4-bit padding */
    int16_t  left_stick_x;  /**< Signed 16-bit (-32768..32767) */
    int16_t  left_stick_y;  /**< Signed 16-bit (-32768..32767) */
    int16_t  right_stick_x; /**< Signed 16-bit (-32768..32767) */
    int16_t  right_stick_y; /**< Signed 16-bit (-32768..32767) */
    uint8_t  left_trigger;  /**< 0..255 */
    uint8_t  right_trigger; /**< 0..255 */
} payload_gamepad_report_t;

/* Tablet Report Payload (5 bytes) */
typedef struct {
    uint8_t  status;        /**< Bit 0: Tip Switch, Bit 1: In Range, Bit 4: Eraser */
    uint16_t x;             /**< Absolute X (0..32767) */
    uint16_t y;             /**< Absolute Y (0..32767) */
} payload_tablet_report_t;

#pragma pack(pop)

#ifdef __cplusplus
}
#endif
