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
#include "../main/hid/usb_hid.h"

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

static void test_golden_vector_packets(void) {
    printf("Running test_golden_vector_packets...\n");
    parsed_packet_t parsed;

    // 1. Keyboard A (seq 1): 5AA50110010008000004000000000028AD
    const uint8_t golden_kb_a[] = {
        0x5A, 0xA5, 0x01, 0x10, 0x01, 0x00, 0x08,
        0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x28, 0xAD
    };
    TEST_ASSERT(parse_packet(golden_kb_a, sizeof(golden_kb_a), &parsed) == PARSE_OK, "Golden KB A parses cleanly");
    TEST_ASSERT(parsed.header.msg_type == MSG_KB_REPORT && parsed.header.sequence_no == 1, "Golden KB A header correct");
    payload_kb_report_t *kb = (payload_kb_report_t*)parsed.payload;
    TEST_ASSERT(kb->modifiers == 0 && kb->keycodes[0] == 0x04, "Golden KB A payload matches key A");

    // 2. Shift+A (seq 2): 5AA501100200080200040000000000745A
    const uint8_t golden_shift_a[] = {
        0x5A, 0xA5, 0x01, 0x10, 0x02, 0x00, 0x08,
        0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x74, 0x5A
    };
    TEST_ASSERT(parse_packet(golden_shift_a, sizeof(golden_shift_a), &parsed) == PARSE_OK, "Golden Shift+A parses cleanly");
    payload_kb_report_t *shift_kb = (payload_kb_report_t*)parsed.payload;
    TEST_ASSERT(shift_kb->modifiers == 0x02 && shift_kb->keycodes[0] == 0x04, "Golden Shift+A payload matches");

    // 3. Mouse Click (seq 3): 5AA501200300040100000009AF
    const uint8_t golden_mouse_click[] = {
        0x5A, 0xA5, 0x01, 0x20, 0x03, 0x00, 0x04,
        0x01, 0x00, 0x00, 0x00,
        0x09, 0xAF
    };
    TEST_ASSERT(parse_packet(golden_mouse_click, sizeof(golden_mouse_click), &parsed) == PARSE_OK, "Golden Mouse click parses cleanly");
    payload_mouse_report_t *mouse = (payload_mouse_report_t*)parsed.payload;
    TEST_ASSERT(mouse->buttons == 0x01 && mouse->dx == 0 && mouse->dy == 0, "Golden Mouse Left Click matches");

    // 4. Volume + (seq 4): 5AA50130040002E9006F2B
    const uint8_t golden_vol_up[] = {
        0x5A, 0xA5, 0x01, 0x30, 0x04, 0x00, 0x02,
        0xE9, 0x00,
        0x6F, 0x2B
    };
    TEST_ASSERT(parse_packet(golden_vol_up, sizeof(golden_vol_up), &parsed) == PARSE_OK, "Golden Vol+ parses cleanly");
    payload_consumer_click_t *cons = (payload_consumer_click_t*)parsed.payload;
    TEST_ASSERT(cons->usage_code == 0x00E9, "Golden Vol+ usage code 0x00E9 matches");

    // 5. Gamepad A (seq 5): 5AA5014005000D01000000000000000000000000A4CD
    const uint8_t golden_gamepad_a[] = {
        0x5A, 0xA5, 0x01, 0x40, 0x05, 0x00, 0x0D,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0xA4, 0xCD
    };
    TEST_ASSERT(parse_packet(golden_gamepad_a, sizeof(golden_gamepad_a), &parsed) == PARSE_OK, "Golden Gamepad A parses cleanly");
    payload_gamepad_report_t *gp = (payload_gamepad_report_t*)parsed.payload;
    TEST_ASSERT(gp->buttons == 0x0001 && gp->hat_switch == 0, "Golden Gamepad A buttons match");

    // 6. Tablet Center (seq 6): 5AA5015006000503004000402AFA
    const uint8_t golden_tablet_center[] = {
        0x5A, 0xA5, 0x01, 0x50, 0x06, 0x00, 0x05,
        0x03, 0x00, 0x40, 0x00, 0x40,
        0x2A, 0xFA
    };
    TEST_ASSERT(parse_packet(golden_tablet_center, sizeof(golden_tablet_center), &parsed) == PARSE_OK, "Golden Tablet Center parses cleanly");
    payload_tablet_report_t *tab = (payload_tablet_report_t*)parsed.payload;
    TEST_ASSERT(tab->status == 0x03 && tab->x == 16384 && tab->y == 16384, "Golden Tablet Center coordinates match");
}

static void test_neutral_sequence_starts(void) {
    printf("Running test_neutral_sequence_starts...\n");
    usb_hid_mock_reset();

    // Send a normal report so device is active
    uint8_t kb[8] = {0x02, 0, 0x04, 0, 0, 0, 0, 0};
    TEST_ASSERT(usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb)) == true, "Normal report sent");
    TEST_ASSERT(usb_hid_is_neutral_complete() == false, "Device has active report");

    // Initiate neutral sequence
    usb_hid_send_all_neutral();
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Neutral sequence is in progress");
    TEST_ASSERT(usb_hid_mock_get_step() == 1, "Step 0 (Keyboard) sent; step 1 (Mouse) pending");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_KEYBOARD, "Last report is Keyboard neutral");
}

static void test_neutral_sequence_advances(void) {
    printf("Running test_neutral_sequence_advances...\n");
    // Continuing from Step 0 completion:
    usb_hid_mock_complete_report(); // Step 0 (Keyboard) completes -> Step 1 (Mouse) sent
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Neutral still in progress");
    TEST_ASSERT(usb_hid_mock_get_step() == 2, "Step 1 (Mouse) sent; step 2 (Consumer) pending");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_MOUSE, "Last report is Mouse neutral");

    usb_hid_mock_complete_report(); // Step 1 (Mouse) completes -> Step 2 (Consumer) sent
    TEST_ASSERT(usb_hid_mock_get_step() == 3, "Step 2 (Consumer) sent; step 3 (Gamepad) pending");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_CONSUMER, "Last report is Consumer neutral");
}

static void test_neutral_sequence_completes(void) {
    printf("Running test_neutral_sequence_completes...\n");
    // Continuing from Step 2 completion:
    usb_hid_mock_complete_report(); // Step 2 (Consumer) completes -> Step 3 (Gamepad) sent
    TEST_ASSERT(usb_hid_mock_get_step() == 4, "Step 3 (Gamepad) sent; step 4 (Tablet) pending");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_GAMEPAD, "Last report is Gamepad neutral");

    usb_hid_mock_complete_report(); // Step 3 (Gamepad) completes -> Step 4 (Tablet) sent
    TEST_ASSERT(usb_hid_mock_get_step() == 5, "Step 4 (Tablet) sent; step 5 pending");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_TABLET, "Last report is Tablet neutral");

    usb_hid_mock_complete_report(); // Step 4 (Tablet) completes
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == false, "Neutral sequence finished");
    TEST_ASSERT(usb_hid_is_neutral_complete() == true, "All 5 endpoints confirmed neutral");
    TEST_ASSERT(usb_hid_mock_get_completed_count() == 5, "Exactly 5 neutral reports completed");
}

static void test_neutral_sequence_when_endpoint_busy(void) {
    printf("Running test_neutral_sequence_when_endpoint_busy...\n");
    usb_hid_mock_reset();

    // Mark active report
    uint8_t kb[8] = {0x02, 0, 0x04, 0, 0, 0, 0, 0};
    usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb));

    // Simulate endpoint busy (e.g. previous report in-flight)
    usb_hid_mock_set_ready(false);

    usb_hid_send_all_neutral();
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Neutral sequence queued");
    TEST_ASSERT(usb_hid_mock_get_step() == 0, "Step remains 0 because endpoint was busy");

    // Normal reports rejected while neutral is pending
    TEST_ASSERT(usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb)) == false, "Normal report rejected while neutral pending");

    // Endpoint becomes ready (previous report completed)
    usb_hid_mock_set_ready(true);
    usb_hid_mock_complete_report(); // Fires completion hook, which drives pending step 0
    TEST_ASSERT(usb_hid_mock_get_step() == 1, "Step 0 sent upon endpoint becoming free");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_KEYBOARD, "Keyboard neutral sent");
}

static void test_repeated_neutral_is_idempotent(void) {
    printf("Running test_repeated_neutral_is_idempotent...\n");
    usb_hid_mock_reset();
    TEST_ASSERT(usb_hid_is_neutral_complete() == true, "Initially neutral");

    // Repeated call while already neutral must not start sequence or send traffic
    usb_hid_send_all_neutral();
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == false, "No sequence started when already neutral");
    TEST_ASSERT(usb_hid_mock_get_completed_count() == 0, "No reports transmitted");

    // Start a sequence, then request neutral again mid-sequence
    uint8_t kb[8] = {0x02, 0, 0x04, 0, 0, 0, 0, 0};
    usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb));
    usb_hid_send_all_neutral();
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Neutral sequence active");
    TEST_ASSERT(usb_hid_mock_get_step() == 1, "Step 0 sent");

    // Advance to step 2
    usb_hid_mock_complete_report();
    TEST_ASSERT(usb_hid_mock_get_step() == 2, "Step 1 sent, now on step 2");

    // Re-assert neutral while in progress: must reset to step 0 to ensure full coverage without duplicate sequences
    usb_hid_send_all_neutral();
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Still in progress");
    TEST_ASSERT(usb_hid_mock_get_step() == 1, "Reset to step 0 and resent, now on step 1");
}

static void test_disconnect_forces_neutral_sequence(void) {
    printf("Running test_disconnect_forces_neutral_sequence...\n");
    usb_hid_mock_reset();
    safety_manager_t mgr;
    safety_manager_init(&mgr, 250, usb_hid_send_all_neutral);

    // Active session
    safety_manager_feed(&mgr, 100);
    uint8_t m[4] = {1, 10, -5, 0};
    usb_hid_send_report(REPORT_ID_MOUSE, m, sizeof(m));
    TEST_ASSERT(usb_hid_is_neutral_complete() == false, "Device has active mouse state");

    // BLE disconnect
    safety_manager_on_disconnect(&mgr);
    TEST_ASSERT(mgr.state == SAFETY_STATE_DISCONNECTED, "Safety manager in DISCONNECTED state");
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Disconnect triggered neutral sequence");
    TEST_ASSERT(usb_hid_mock_get_last_report_id() == REPORT_ID_KEYBOARD, "First neutral report is Keyboard");

    // Normal input blocked during disconnect neutralization
    TEST_ASSERT(usb_hid_send_report(REPORT_ID_MOUSE, m, sizeof(m)) == false, "Input rejected during disconnect neutral");
}

static void test_timeout_forces_neutral_sequence(void) {
    printf("Running test_timeout_forces_neutral_sequence...\n");
    usb_hid_mock_reset();
    safety_manager_t mgr;
    safety_manager_init(&mgr, 250, usb_hid_send_all_neutral);

    // Feed packet at time 100
    safety_manager_feed(&mgr, 100);
    uint8_t kb[8] = {0, 0, 0x04, 0, 0, 0, 0, 0};
    usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb));

    // Tick before timeout (elapsed 200ms < 250ms)
    safety_manager_tick(&mgr, 300);
    TEST_ASSERT(mgr.state == SAFETY_STATE_ACTIVE, "Still active at 200ms");
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == false, "No neutral triggered yet");

    // Tick after timeout (elapsed 260ms >= 250ms)
    safety_manager_tick(&mgr, 360);
    TEST_ASSERT(mgr.state == SAFETY_STATE_TIMED_OUT, "State transitions to TIMED_OUT");
    TEST_ASSERT(usb_hid_is_neutral_in_progress() == true, "Timeout triggered neutral sequence");
    TEST_ASSERT(usb_hid_mock_get_step() == 1, "Keyboard neutral sent immediately");
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
    test_golden_vector_packets();

    // Wave 11.1 Neutral Serialization Tests
    test_neutral_sequence_starts();
    test_neutral_sequence_advances();
    test_neutral_sequence_completes();
    test_neutral_sequence_when_endpoint_busy();
    test_repeated_neutral_is_idempotent();
    test_disconnect_forces_neutral_sequence();
    test_timeout_forces_neutral_sequence();

    printf("==================================================\n");
    printf("TEST RESULTS: %d / %d PASSED\n", s_tests_passed, s_tests_run);
    printf("==================================================\n");

    return (s_tests_passed == s_tests_run) ? 0 : 1;
}
