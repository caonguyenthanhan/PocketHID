/**
 * @file hid_reports.c
 * @brief Implementation of report builders matching Android contract
 */

#include "hid_reports.h"
#include <string.h>

void hid_build_keyboard_report(uint8_t modifiers, const uint8_t *keycodes, size_t key_count, uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_KEYBOARD);
    out_report[0] = modifiers;
    out_report[1] = 0x00; // Reserved
    if (keycodes && key_count > 0) {
        size_t count = (key_count > 6) ? 6 : key_count;
        for (size_t i = 0; i < count; i++) {
            out_report[2 + i] = keycodes[i];
        }
    }
}

void hid_build_keyboard_neutral(uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_KEYBOARD);
}

void hid_build_mouse_report(uint8_t buttons, int8_t dx, int8_t dy, int8_t wheel, uint8_t *out_report) {
    if (!out_report) return;
    out_report[0] = buttons & 0x07; // 3 buttons: Left (0x01), Right (0x02), Middle (0x04)
    out_report[1] = (uint8_t)dx;
    out_report[2] = (uint8_t)dy;
    out_report[3] = (uint8_t)wheel;
}

void hid_build_mouse_neutral(uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_MOUSE);
}

void hid_build_consumer_report(uint16_t usage_code, uint8_t *out_report) {
    if (!out_report) return;
    out_report[0] = (uint8_t)(usage_code & 0xFF);
    out_report[1] = (uint8_t)((usage_code >> 8) & 0xFF);
}

void hid_build_consumer_neutral(uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_CONSUMER);
}

void hid_build_gamepad_report(
    uint16_t buttons,
    uint8_t hat_switch,
    int16_t left_stick_x,
    int16_t left_stick_y,
    int16_t right_stick_x,
    int16_t right_stick_y,
    uint8_t left_trigger,
    uint8_t right_trigger,
    uint8_t *out_report
) {
    if (!out_report) return;
    // Bytes 0-1: 16 buttons (Little-Endian)
    out_report[0] = (uint8_t)(buttons & 0xFF);
    out_report[1] = (uint8_t)((buttons >> 8) & 0xFF);
    // Byte 2: Hat switch (0..8)
    out_report[2] = hat_switch & 0x0F;
    // Bytes 3-4: Left stick X
    out_report[3] = (uint8_t)(left_stick_x & 0xFF);
    out_report[4] = (uint8_t)((left_stick_x >> 8) & 0xFF);
    // Bytes 5-6: Left stick Y
    out_report[5] = (uint8_t)(left_stick_y & 0xFF);
    out_report[6] = (uint8_t)((left_stick_y >> 8) & 0xFF);
    // Bytes 7-8: Right stick X
    out_report[7] = (uint8_t)(right_stick_x & 0xFF);
    out_report[8] = (uint8_t)((right_stick_x >> 8) & 0xFF);
    // Bytes 9-10: Right stick Y
    out_report[9] = (uint8_t)(right_stick_y & 0xFF);
    out_report[10] = (uint8_t)((right_stick_y >> 8) & 0xFF);
    // Bytes 11-12: Triggers (0..255)
    out_report[11] = left_trigger;
    out_report[12] = right_trigger;
}

void hid_build_gamepad_neutral(uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_GAMEPAD);
}

void hid_build_tablet_report(uint8_t status, uint16_t x, uint16_t y, uint8_t *out_report) {
    if (!out_report) return;
    uint16_t clamped_x = (x > TABLET_COORD_MAX) ? TABLET_COORD_MAX : x;
    uint16_t clamped_y = (y > TABLET_COORD_MAX) ? TABLET_COORD_MAX : y;

    out_report[0] = status & 0x1F; // Tip switch (0x01), In-Range (0x02), Barrel (0x04), Invert (0x08), Eraser (0x10)
    out_report[1] = (uint8_t)(clamped_x & 0xFF);
    out_report[2] = (uint8_t)((clamped_x >> 8) & 0xFF);
    out_report[3] = (uint8_t)(clamped_y & 0xFF);
    out_report[4] = (uint8_t)((clamped_y >> 8) & 0xFF);
}

void hid_build_tablet_neutral(uint8_t *out_report) {
    if (!out_report) return;
    memset(out_report, 0, REPORT_LEN_TABLET);
}
