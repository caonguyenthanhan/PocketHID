/**
 * @file packet_parser.h
 * @brief PocketHID Wire Protocol Packet Parsing & Validation
 */

#pragma once

#include "wire_protocol.h"
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    PARSE_OK = 0,
    PARSE_ERR_TOO_SHORT,
    PARSE_ERR_INVALID_MAGIC,
    PARSE_ERR_UNSUPPORTED_VERSION,
    PARSE_ERR_LENGTH_MISMATCH,
    PARSE_ERR_PAYLOAD_TOO_LARGE,
    PARSE_ERR_CRC_MISMATCH,
    PARSE_ERR_UNKNOWN_MSG_TYPE,
    PARSE_ERR_PAYLOAD_LEN_INVALID,
    PARSE_ERR_SEQUENCE_REGRESSION
} parse_result_t;

typedef struct {
    pockethid_header_t header;
    const uint8_t* payload;
    uint16_t checksum;
} parsed_packet_t;

/**
 * @brief Computes standard CRC-16-CCITT (poly 0x1021, init 0xFFFF).
 */
uint16_t crc16_ccitt(const uint8_t *data, size_t length);

/**
 * @brief Parses and validates an incoming raw buffer.
 * 
 * @param buffer Raw byte stream received over BLE
 * @param length Length of buffer in bytes
 * @param out_packet Output parsed packet structure
 * @return parse_result_t PARSE_OK if completely valid
 */
parse_result_t parse_packet(const uint8_t *buffer, size_t length, parsed_packet_t *out_packet);

/**
 * @brief Validates sequence number against last seen sequence number.
 * Allows 16-bit wraparound while rejecting replays.
 * 
 * @param current_seq Sequence number from received packet
 * @param last_seq Pointer to last recorded sequence number (updated on success)
 * @param is_initial True if this is the first packet of a session
 * @return true if valid sequence progression, false if regression/replay
 */
bool validate_sequence(uint16_t current_seq, uint16_t *last_seq, bool is_initial);

#ifdef __cplusplus
}
#endif
