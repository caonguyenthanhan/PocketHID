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
    USB_STATUS_INIT_FAILED,
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
 * @brief Initiates serialized neutral release sequence across all 5 HID endpoints.
 * Non-blocking, safe to call from multiple contexts.
 */
void usb_hid_send_all_neutral(void);

/**
 * @brief Returns true if a neutral report sequence is currently pending or in progress.
 */
bool usb_hid_is_neutral_in_progress(void);

/**
 * @brief Returns true if all 5 endpoints are confirmed in neutral state.
 */
bool usb_hid_is_neutral_complete(void);

/**
 * @brief Advances neutral serialization if pending and endpoint is ready.
 * Non-blocking, called periodically from main loop.
 */
void usb_hid_tick(void);

/**
 * @brief Called by TinyUSB tud_hid_report_complete_cb() when an IN report completes.
 */
void usb_hid_on_report_complete(void);

#ifndef ESP_PLATFORM
/**
 * @brief Host-side mock controls for unit testing neutral serialization.
 */
void usb_hid_mock_reset(void);
void usb_hid_mock_set_ready(bool ready);
void usb_hid_mock_complete_report(void);
uint8_t usb_hid_mock_get_completed_count(void);
uint8_t usb_hid_mock_get_last_report_id(void);
uint8_t usb_hid_mock_get_step(void);
#endif

#ifdef __cplusplus
}
#endif
