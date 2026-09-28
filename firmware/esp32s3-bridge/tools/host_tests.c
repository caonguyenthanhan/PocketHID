/**
 * @file host_tests.c
 * @brief Host-side unit test suite for PocketHID Bridge firmware components
 * Compilable with native GCC on Windows/Linux/macOS.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "../main/protocol/wire_protocol.h"
#include "../main/protocol/packet_parser.h"
#include "../main/safety/safety_manager.h"
#include "../main/hid/hid_descriptors.h"
#include "../main/hid/hid_reports.h"

static int s_tests_run = 0;
static int s_tests_passed = 0;

#define TEST_ASSERT(cond, msg) do { \
    s_tests_run++; \
    if (!(cond)) { \
        printf("[FAIL] Line %d: %s (%s)\n", __LINE__, #cond, msg); \
    } else { \
        s_tests_passed++; \
    } \
} while(0)

// Helper to build a raw wire packet
static size_t build_raw_test_packet(uint8_t msg_type, uint16_t seq, const uint8_t *payload, uint8_t payload_len, uint8_t *out_buf) {
    pockethid_header_t hdr;
    hdr.magic = POCKETHID_MAGIC;
    hdr.version = POCKETHID_PROTOCOL_VERSION;
    hdr.msg_type = msg_type;
    hdr.sequence_no = seq;
    hdr.payload_len = payload_len;

    memcpy(out_buf, &hdr, POCKETHID_HEADER_SIZE);
    if (payload && payload_len > 0) {
        memcpy(&out_buf[POCKETHID_HEADER_SIZE], payload, payload_len);
    }

    size_t crc_len = 1 + 1 + 2 + 1 + payload_len;
    uint16_t crc = crc16_ccitt(&out_buf[2], crc_len);

    out_buf[POCKETHID_HEADER_SIZE + payload_len] = (uint8_t)(crc & 0xFF);
    out_buf[POCKETHID_HEADER_SIZE + payload_len + 1] = (uint8_t)((crc >> 8) & 0xFF);

    return POCKETHID_HEADER_SIZE + payload_len + POCKETHID_CHECKSUM_SIZE;
}

static void test_packet_parser_valid(void) {
    printf("Running test_packet_parser_valid...\n");
    uint8_t packet_buf[64];
    payload_kb_report_t kb = { .modifiers = 0x02, .reserved = 0x00, .keycodes = {0x04, 0, 0, 0, 0, 0} };
    size_t len = build_raw_test_packet(MSG_KB_REPORT, 1, (const uint8_t*)&kb, sizeof(kb), packet_buf);

    parsed_packet_t parsed;
    parse_result_t res = parse_packet(packet_buf, len, &parsed);
    TEST_ASSERT(res == PARSE_OK, "Valid keyboard packet should parse cleanly");
    TEST_ASSERT(parsed.header.magic == POCKETHID_MAGIC, "Magic must match 0xA55A");
    TEST_ASSERT(parsed.header.version == 1, "Version must be 1");
    TEST_ASSERT(parsed.header.msg_type == MSG_KB_REPORT, "Msg type must be MSG_KB_REPORT");
    TEST_ASSERT(parsed.header.sequence_no == 1, "Seq must be 1");
    TEST_ASSERT(parsed.header.payload_len == 8, "Payload len must be 8");
    TEST_ASSERT(parsed.payload != NULL, "Payload pointer must not be null");
}

static void test_packet_parser_invalid_magic(void) {
    printf("Running test_packet_parser_invalid_magic...\n");
    uint8_t packet_buf[64];
    payload_kb_report_t kb = {0};
    size_t len = build_raw_test_packet(MSG_KB_REPORT, 1, (const uint8_t*)&kb, sizeof(kb), packet_buf);
    packet_buf[0] = 0x12; // Corrupt magic

    parsed_packet_t parsed;
    parse_result_t res = parse_packet(packet_buf, len, &parsed);
    TEST_ASSERT(res == PARSE_ERR_INVALID_MAGIC, "Corrupt magic must return PARSE_ERR_INVALID_MAGIC");
}

static void test_packet_parser_crc_failure(void) {
    printf("Running test_packet_parser_crc_failure...\n");
    uint8_t packet_buf[64];
    payload_mouse_report_t m = { .buttons = 1, .dx = 10, .dy = -5, .wheel = 0 };
    size_t len = build_raw_test_packet(MSG_MOUSE_REPORT, 5, (const uint8_t*)&m, sizeof(m), packet_buf);
    packet_buf[POCKETHID_HEADER_SIZE] ^= 0xFF; // Corrupt payload byte without updating CRC

    parsed_packet_t parsed;
    parse_result_t res = parse_packet(packet_buf, len, &parsed);
    TEST_ASSERT(res == PARSE_ERR_CRC_MISMATCH, "Payload tamper must return PARSE_ERR_CRC_MISMATCH");
}

static void test_packet_parser_payload_length_mismatch(void) {
    printf("Running test_packet_parser_payload_length_mismatch...\n");
    uint8_t packet_buf[64];
    // Tablet requires 5 bytes, supply 3 bytes
    uint8_t short_payload[3] = {1, 2, 3};
    size_t len = build_raw_test_packet(MSG_TABLET_REPORT, 10, short_payload, sizeof(short_payload), packet_buf);

    parsed_packet_t parsed;
    parse_result_t res = parse_packet(packet_buf, len, &parsed);
    TEST_ASSERT(res == PARSE_ERR_PAYLOAD_LEN_INVALID, "Short tablet payload must return PARSE_ERR_PAYLOAD_LEN_INVALID");
}

static void test_sequence_progression_and_wraparound(void) {
    printf("Running test_sequence_progression_and_wraparound...\n");
    uint16_t last_seq = 0;
    TEST_ASSERT(validate_sequence(100, &last_seq, true), "Initial seq should be accepted");
    TEST_ASSERT(last_seq == 100, "last_seq updated to 100");

    TEST_ASSERT(validate_sequence(101, &last_seq, false), "Monotonic seq 101 accepted");
    TEST_ASSERT(!validate_sequence(101, &last_seq, false), "Duplicate seq 101 rejected (replay)");
    TEST_ASSERT(!validate_sequence(99, &last_seq, false), "Old seq 99 rejected (regression)");

    // Test 16-bit wraparound
    last_seq = 65534;
    TEST_ASSERT(validate_sequence(65535, &last_seq, false), "Seq 65535 accepted");
    TEST_ASSERT(validate_sequence(0, &last_seq, false), "Seq 0 (wraparound) accepted");
    TEST_ASSERT(validate_sequence(1, &last_seq, false), "Seq 1 after wraparound accepted");
    TEST_ASSERT(!validate_sequence(65535, &last_seq, false), "Seq 65535 after wrap rejected");
}

static bool s_mock_neutral_called = false;
static void mock_neutral_flush(void) {
    s_mock_neutral_called = true;
}

static void test_safety_manager_watchdog_timeout(void) {
    printf("Running test_safety_manager_watchdog_timeout...\n");
    safety_manager_t mgr;
    s_mock_neutral_called = false;
    safety_manager_init(&mgr, 250, mock_neutral_flush);

    safety_manager_feed(&mgr, 1000);
    TEST_ASSERT(mgr.state == SAFETY_STATE_ACTIVE, "State is ACTIVE after feed");
    TEST_ASSERT(!mgr.is_neutral, "is_neutral is false");

    // Tick at 1100ms (100ms elapsed < 250ms timeout)
    safety_manager_tick(&mgr, 1100);
    TEST_ASSERT(mgr.state == SAFETY_STATE_ACTIVE, "State remains ACTIVE within timeout");
    TEST_ASSERT(!s_mock_neutral_called, "Neutral flush not called yet");

    // Tick at 1251ms (251ms elapsed >= 250ms timeout)
    safety_manager_tick(&mgr, 1251);
    TEST_ASSERT(mgr.state == SAFETY_STATE_TIMED_OUT, "State transitions to TIMED_OUT");
    TEST_ASSERT(s_mock_neutral_called, "Neutral flush callback executed on timeout");
    TEST_ASSERT(mgr.is_neutral, "is_neutral reset to true");

    // Test idempotency
    s_mock_neutral_called = false;
    safety_manager_force_all_neutral(&mgr);
    TEST_ASSERT(!s_mock_neutral_called, "Redundant neutral call must be idempotent");
}

static void test_safety_manager_disconnect(void) {
    printf("Running test_safety_manager_disconnect...\n");
    safety_manager_t mgr;
    s_mock_neutral_called = false;
    safety_manager_init(&mgr, 250, mock_neutral_flush);

    safety_manager_feed(&mgr, 500);
    safety_manager_on_disconnect(&mgr);
    TEST_ASSERT(mgr.state == SAFETY_STATE_DISCONNECTED, "State transitions to DISCONNECTED");
    TEST_ASSERT(s_mock_neutral_called, "Neutral flush triggered on disconnect");
}

static void test_hid_report_builders_match_android(void) {
    printf("Running test_hid_report_builders_match_android...\n");

    // 1. Keyboard
    uint8_t kb_buf[8];
    uint8_t keys[6] = {0x04, 0x05, 0, 0, 0, 0};
    hid_build_keyboard_report(0x02 /* LShift */, keys, 2, kb_buf);
    TEST_ASSERT(kb_buf[0] == 0x02, "KB Modifiers must match");
    TEST_ASSERT(kb_buf[1] == 0x00, "KB Reserved byte must be 0");
    TEST_ASSERT(kb_buf[2] == 0x04 && kb_buf[3] == 0x05, "KB keycodes match 6KRO slots");

    // 2. Mouse
    uint8_t mouse_buf[4];
    hid_build_mouse_report(0x01 /* Left */ | 0x02 /* Right */, -10, 20, -1, mouse_buf);
    TEST_ASSERT((mouse_buf[0] & 0x03) == 0x03, "Mouse buttons match");
    TEST_ASSERT((int8_t)mouse_buf[1] == -10, "Mouse dx matches signed int8");
    TEST_ASSERT((int8_t)mouse_buf[2] == 20, "Mouse dy matches signed int8");
    TEST_ASSERT((int8_t)mouse_buf[3] == -1, "Mouse wheel matches signed int8");

    // 3. Consumer
    uint8_t consumer_buf[2];
    hid_build_consumer_report(0x00E9 /* Vol+ */, consumer_buf);
    TEST_ASSERT(consumer_buf[0] == 0xE9 && consumer_buf[1] == 0x00, "Consumer Vol+ Little-Endian");

    // 4. Gamepad
    uint8_t gamepad_buf[13];
    hid_build_gamepad_report(0x0005 /* A + X */, 1 /* Hat Up */, -32768, 32767, 100, -200, 255, 128, gamepad_buf);
    TEST_ASSERT(gamepad_buf[0] == 0x05 && gamepad_buf[1] == 0x00, "Gamepad buttons match");
    TEST_ASSERT(gamepad_buf[2] == 0x01, "Gamepad hat matches Up (1)");
    int16_t lx = (int16_t)((uint16_t)gamepad_buf[3] | ((uint16_t)gamepad_buf[4] << 8));
    int16_t ly = (int16_t)((uint16_t)gamepad_buf[5] | ((uint16_t)gamepad_buf[6] << 8));
    TEST_ASSERT(lx == -32768, "Left stick X matches -32768");
    TEST_ASSERT(ly == 32767, "Left stick Y matches 32767");
    TEST_ASSERT(gamepad_buf[11] == 255, "Left trigger matches 255");
    TEST_ASSERT(gamepad_buf[12] == 128, "Right trigger matches 128");

    // 5. Tablet Clamping
    uint8_t tablet_buf[5];
    hid_build_tablet_report(0x03 /* Tip + InRange */, 40000 /* Over max */, 16384, tablet_buf);
    TEST_ASSERT(tablet_buf[0] == 0x03, "Tablet status matches");
    uint16_t tx = (uint16_t)tablet_buf[1] | ((uint16_t)tablet_buf[2] << 8);
    uint16_t ty = (uint16_t)tablet_buf[3] | ((uint16_t)tablet_buf[4] << 8);
    TEST_ASSERT(tx == TABLET_COORD_MAX, "Tablet X clamped to 32767");
    TEST_ASSERT(ty == 16384, "Tablet Y remains 16384");
}

int main(void) {
    printf("==================================================\n");
    printf("POCKETHID BRIDGE FIRMWARE HOST UNIT TESTS\n");
    printf("==================================================\n");

    test_packet_parser_valid();
    test_packet_parser_invalid_magic();
    test_packet_parser_crc_failure();
    test_packet_parser_payload_length_mismatch();
    test_sequence_progression_and_wraparound();
    test_safety_manager_watchdog_timeout();
    test_safety_manager_disconnect();
    test_hid_report_builders_match_android();

    printf("==================================================\n");
    printf("TEST RESULTS: %d / %d PASSED\n", s_tests_passed, s_tests_run);
    printf("==================================================\n");

    return (s_tests_passed == s_tests_run) ? 0 : 1;
}
