/**
 * @file usb_hid.h
 * @brief PocketHID USB Composite HID Driver Interface (TinyUSB Integration)
 */

#pragma once

#include "hid_descriptors.h"
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    USB_STATUS_NOT_INITIALIZED = 0,
    USB_STATUS_ATTACHED,
    USB_STATUS_ENUMERATED,
    USB_STATUS_SUSPENDED
} usb_status_t;

/**
 * @brief Initializes TinyUSB composite HID stack.
 * Exposes Report IDs 1 to 5.
 */
bool usb_hid_init(void);

/**
 * @brief Returns current USB host connection status.
 */
usb_status_t usb_hid_get_status(void);

/**
 * @brief Sends an arbitrary HID report to the USB host.
 * 
 * @param report_id 1 (KB), 2 (Mouse), 3 (Consumer), 4 (Gamepad), 5 (Tablet)
 * @param report Data bytes matching report length
 * @param len Exact length of report
 * @return true if sent or queued, false if host not ready or buffer full
 */
bool usb_hid_send_report(uint8_t report_id, const uint8_t *report, size_t len);

/**
 * @brief Flushes neutral release reports across all 5 HID endpoints.
 */
void usb_hid_send_all_neutral(void);

#ifdef __cplusplus
}
#endif
