/**
 * @file hid_reports.h
 * @brief PocketHID Validated Report Builders & State Encoders
 */

#pragma once

#include "hid_descriptors.h"
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Clamping bounds for Tablet coordinates */
#define TABLET_COORD_MIN 0
#define TABLET_COORD_MAX 32767

void hid_build_keyboard_report(uint8_t modifiers, const uint8_t *keycodes, size_t key_count, uint8_t *out_report);
void hid_build_keyboard_neutral(uint8_t *out_report);

void hid_build_mouse_report(uint8_t buttons, int8_t dx, int8_t dy, int8_t wheel, uint8_t *out_report);
void hid_build_mouse_neutral(uint8_t *out_report);

void hid_build_consumer_report(uint16_t usage_code, uint8_t *out_report);
void hid_build_consumer_neutral(uint8_t *out_report);

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
);
void hid_build_gamepad_neutral(uint8_t *out_report);

void hid_build_tablet_report(uint8_t status, uint16_t x, uint16_t y, uint8_t *out_report);
void hid_build_tablet_neutral(uint8_t *out_report);

#ifdef __cplusplus
}
#endif
