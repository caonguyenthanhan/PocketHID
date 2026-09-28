/**
 * @file packet_parser.c
 * @brief Implementation of packet parser and validation engine
 */

#include "packet_parser.h"
#include <string.h>

uint16_t crc16_ccitt(const uint8_t *data, size_t length) {
    uint16_t crc = 0xFFFF;
    for (size_t i = 0; i < length; i++) {
        crc ^= (uint16_t)data[i] << 8;
        for (int bit = 0; bit < 8; bit++) {
            if (crc & 0x8000) {
                crc = (crc << 1) ^ 0x1021;
            } else {
                crc = crc << 1;
            }
        }
    }
    return crc;
}

static uint8_t get_expected_payload_len(uint8_t msg_type, bool *valid_type) {
    *valid_type = true;
    switch (msg_type) {
        case MSG_SYS_HELLO:             return 12;
        case MSG_SYS_READY:             return 8;
        case MSG_SYS_HEARTBEAT:         return 4;
        case MSG_SYS_EMERGENCY_NEUTRAL: return 0;
        case MSG_KB_REPORT:             return 8;
        case MSG_KB_RELEASE_ALL:        return 0;
        case MSG_MOUSE_REPORT:          return 4;
        case MSG_CONSUMER_CLICK:        return 2;
        case MSG_GAMEPAD_REPORT:        return 13;
        case MSG_TABLET_REPORT:         return 5;
        default:
            *valid_type = false;
            return 0;
    }
}

parse_result_t parse_packet(const uint8_t *buffer, size_t length, parsed_packet_t *out_packet) {
    if (!buffer || !out_packet) {
        return PARSE_ERR_TOO_SHORT;
    }

    if (length < (POCKETHID_HEADER_SIZE + POCKETHID_CHECKSUM_SIZE)) {
        return PARSE_ERR_TOO_SHORT;
    }

    // Read header (Little-Endian)
    uint16_t magic = (uint16_t)buffer[0] | ((uint16_t)buffer[1] << 8);
    if (magic != POCKETHID_MAGIC) {
        return PARSE_ERR_INVALID_MAGIC;
    }

    uint8_t version = buffer[2];
    if (version != POCKETHID_PROTOCOL_VERSION) {
        return PARSE_ERR_UNSUPPORTED_VERSION;
    }

    uint8_t msg_type = buffer[3];
    uint16_t sequence_no = (uint16_t)buffer[4] | ((uint16_t)buffer[5] << 8);
    uint8_t payload_len = buffer[6];

    if (payload_len > POCKETHID_MAX_PAYLOAD_SIZE) {
        return PARSE_ERR_PAYLOAD_TOO_LARGE;
    }

    size_t expected_total = POCKETHID_HEADER_SIZE + payload_len + POCKETHID_CHECKSUM_SIZE;
    if (length != expected_total) {
        return PARSE_ERR_LENGTH_MISMATCH;
    }

    bool valid_type = false;
    uint8_t expected_payload_len = get_expected_payload_len(msg_type, &valid_type);
    if (!valid_type) {
        return PARSE_ERR_UNKNOWN_MSG_TYPE;
    }
    if (payload_len != expected_payload_len) {
        return PARSE_ERR_PAYLOAD_LEN_INVALID;
    }

    // CRC is computed over bytes [2 .. 7 + payload_len - 1] (VERSION through end of PAYLOAD)
    size_t crc_calc_len = 1 + 1 + 2 + 1 + payload_len; // version + msg_type + seq + payload_len + payload
    uint16_t calculated_crc = crc16_ccitt(&buffer[2], crc_calc_len);

    size_t checksum_offset = POCKETHID_HEADER_SIZE + payload_len;
    uint16_t received_checksum = (uint16_t)buffer[checksum_offset] | ((uint16_t)buffer[checksum_offset + 1] << 8);

    if (calculated_crc != received_checksum) {
        return PARSE_ERR_CRC_MISMATCH;
    }

    out_packet->header.magic = magic;
    out_packet->header.version = version;
    out_packet->header.msg_type = msg_type;
    out_packet->header.sequence_no = sequence_no;
    out_packet->header.payload_len = payload_len;
    out_packet->payload = (payload_len > 0) ? &buffer[POCKETHID_HEADER_SIZE] : NULL;
    out_packet->checksum = received_checksum;

    return PARSE_OK;
}

bool validate_sequence(uint16_t current_seq, uint16_t *last_seq, bool is_initial) {
    if (!last_seq) return false;

    if (is_initial) {
        *last_seq = current_seq;
        return true;
    }

    // 16-bit modular distance: delta must be strictly positive and < 32768
    uint16_t delta = (uint16_t)(current_seq - *last_seq);
    if (delta > 0 && delta < 32768) {
        *last_seq = current_seq;
        return true;
    }

    return false; // Replay or sequence regression
}
