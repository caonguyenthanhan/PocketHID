/**
 * @file hid_descriptors.h
 * @brief PocketHID USB HID Composite Report Descriptor Definition
 * 
 * Matches Android HidConstants.COMBO_REPORT_DESCRIPTOR 100% byte-for-byte.
 */

#pragma once

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define REPORT_ID_KEYBOARD          1
#define REPORT_ID_MOUSE             2
#define REPORT_ID_CONSUMER          3
#define REPORT_ID_GAMEPAD           4
#define REPORT_ID_TABLET            5

#define REPORT_LEN_KEYBOARD         8
#define REPORT_LEN_MOUSE            4
#define REPORT_LEN_CONSUMER         2
#define REPORT_LEN_GAMEPAD          13
#define REPORT_LEN_TABLET           5

extern const uint8_t pockethid_combo_report_descriptor[];
extern const size_t  pockethid_combo_report_descriptor_len;

#ifdef __cplusplus
}
#endif
