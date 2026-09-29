/**
 * @file usb_hid.c
 * @brief TinyUSB Composite HID integration for ESP32-S3 with Serialized Neutral Flush
 */

#include "usb_hid.h"
#include "hid_reports.h"
#include "hid_descriptors.h"
#include <string.h>

#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "tinyusb.h"
#include "class/hid/hid_device.h"
static const char *TAG = "USB_HID";
#endif

static usb_status_t s_usb_status = USB_STATUS_NOT_INITIALIZED;

/* Neutral Report Serialization State Machine */
static volatile bool s_neutral_in_progress = false;
static volatile bool s_neutral_complete = true;
static volatile uint8_t s_neutral_step = 5; // 0: KB, 1: Mouse, 2: Consumer, 3: Gamepad, 4: Tablet, 5: Done

#ifndef ESP_PLATFORM
static bool s_mock_ready = true;
static uint8_t s_mock_completed_count = 0;
static uint8_t s_mock_last_report_id = 0;

void usb_hid_mock_reset(void) {
    s_mock_ready = true;
    s_mock_completed_count = 0;
    s_mock_last_report_id = 0;
    s_neutral_in_progress = false;
    s_neutral_complete = true;
    s_neutral_step = 5;
    s_usb_status = USB_STATUS_ENUMERATED;
}

void usb_hid_mock_set_ready(bool ready) {
    s_mock_ready = ready;
}

void usb_hid_mock_complete_report(void) {
    usb_hid_on_report_complete();
}

uint8_t usb_hid_mock_get_completed_count(void) {
    return s_mock_completed_count;
}

uint8_t usb_hid_mock_get_last_report_id(void) {
    return s_mock_last_report_id;
}

uint8_t usb_hid_mock_get_step(void) {
    return s_neutral_step;
}
#endif

#ifdef ESP_PLATFORM
// Invoked when received GET HID REPORT DESCRIPTOR request
uint8_t const *tud_hid_descriptor_report_cb(uint8_t instance) {
    (void)instance;
    return pockethid_combo_report_descriptor;
}

// Invoked when received GET_REPORT control request
uint16_t tud_hid_get_report_cb(uint8_t itf, uint8_t report_id, hid_report_type_t report_type, uint8_t* buffer, uint16_t reqlen) {
    (void) itf;
    (void) report_type;
    memset(buffer, 0, reqlen);
    return reqlen;
}

// Invoked when received SET_REPORT control request or output report over OUT endpoint
void tud_hid_set_report_cb(uint8_t itf, uint8_t report_id, hid_report_type_t report_type, uint8_t const* buffer, uint16_t bufsize) {
    (void) itf;
    (void) report_id;
    (void) report_type;
    (void) buffer;
    (void) bufsize;
}

// Invoked when report transfer is completed
void tud_hid_report_complete_cb(uint8_t instance, uint8_t const* report, uint16_t len) {
    (void)instance;
    (void)report;
    (void)len;
    usb_hid_on_report_complete();
}

// Invoked when device is mounted
void tud_mount_cb(void) {
    s_usb_status = USB_STATUS_ENUMERATED;
    s_neutral_in_progress = false;
    s_neutral_complete = true;
    s_neutral_step = 5;
    ESP_LOGI(TAG, "USB Mounted & Enumerated by Host");
}

// Invoked when device is unmounted
void tud_umount_cb(void) {
    s_usb_status = USB_STATUS_ATTACHED;
    s_neutral_in_progress = false;
    s_neutral_complete = true;
    s_neutral_step = 5;
    ESP_LOGW(TAG, "USB Unmounted");
}

// Invoked when usb bus is suspended
void tud_suspend_cb(bool remote_wakeup_en) {
    (void) remote_wakeup_en;
    s_usb_status = USB_STATUS_SUSPENDED;
    ESP_LOGW(TAG, "USB Suspended");
}

// Invoked when usb bus is resumed
void tud_resume_cb(void) {
    s_usb_status = USB_STATUS_ENUMERATED;
    ESP_LOGI(TAG, "USB Resumed");
}
#endif

bool usb_hid_init(void) {
#ifdef ESP_PLATFORM
    ESP_LOGI(TAG, "Initializing TinyUSB Composite HID Driver");
    const tinyusb_config_t tusb_cfg = {
        .device_descriptor = NULL,
        .string_descriptor = NULL,
        .external_phy = false,
        .configuration_descriptor = NULL,
    };
    esp_err_t err = tinyusb_driver_install(&tusb_cfg);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "TinyUSB install failed: %d", err);
        s_usb_status = USB_STATUS_INIT_FAILED;
        return false;
    }
    s_usb_status = USB_STATUS_ATTACHED;
    s_neutral_complete = true;
    s_neutral_in_progress = false;
    s_neutral_step = 5;
    return true;
#else
    s_usb_status = USB_STATUS_ENUMERATED;
    s_neutral_complete = true;
    s_neutral_in_progress = false;
    s_neutral_step = 5;
    return true;
#endif
}

usb_status_t usb_hid_get_status(void) {
    return s_usb_status;
}

static bool send_neutral_report_step(uint8_t step) {
    uint8_t buf[16];
    uint8_t report_id = 0;
    size_t len = 0;

    switch (step) {
        case 0:
            report_id = REPORT_ID_KEYBOARD;
            len = REPORT_LEN_KEYBOARD;
            hid_build_keyboard_neutral(buf);
            break;
        case 1:
            report_id = REPORT_ID_MOUSE;
            len = REPORT_LEN_MOUSE;
            hid_build_mouse_neutral(buf);
            break;
        case 2:
            report_id = REPORT_ID_CONSUMER;
            len = REPORT_LEN_CONSUMER;
            hid_build_consumer_neutral(buf);
            break;
        case 3:
            report_id = REPORT_ID_GAMEPAD;
            len = REPORT_LEN_GAMEPAD;
            hid_build_gamepad_neutral(buf);
            break;
        case 4:
            report_id = REPORT_ID_TABLET;
            len = REPORT_LEN_TABLET;
            hid_build_tablet_neutral(buf);
            break;
        default:
            return false;
    }

#ifdef ESP_PLATFORM
    if (!tud_mounted() || !tud_hid_ready()) {
        return false;
    }
    return tud_hid_report(report_id, buf, (uint16_t)len);
#else
    if (!s_mock_ready) {
        return false;
    }
    s_mock_last_report_id = report_id;
    return true;
#endif
}

bool usb_hid_send_report(uint8_t report_id, const uint8_t *report, size_t len) {
    if (!report || len == 0) return false;

    // Safety Priority: If neutralization is in progress, normal reports must not
    // interrupt or starve the neutral sequence.
    if (s_neutral_in_progress) {
        return false;
    }

#ifdef ESP_PLATFORM
    if (!tud_mounted() || !tud_hid_ready()) {
        return false;
    }
    bool ok = tud_hid_report(report_id, report, (uint16_t)len);
    if (ok) {
        s_neutral_complete = false;
    }
    return ok;
#else
    if (!s_mock_ready) {
        return false;
    }
    s_mock_last_report_id = report_id;
    s_neutral_complete = false;
    return true;
#endif
}

void usb_hid_send_all_neutral(void) {
    // Idempotency: If neutral is already achieved and no sequence is in progress, do nothing
    if (s_neutral_complete && !s_neutral_in_progress) {
        return;
    }

    // Start or reset sequence
    s_neutral_in_progress = true;
    s_neutral_complete = false;
    s_neutral_step = 0;

    // Attempt to send step 0 (Keyboard) immediately
    if (send_neutral_report_step(0)) {
        s_neutral_step = 1;
    }
}

bool usb_hid_is_neutral_in_progress(void) {
    return s_neutral_in_progress;
}

bool usb_hid_is_neutral_complete(void) {
    return s_neutral_complete;
}

void usb_hid_tick(void) {
    if (!s_neutral_in_progress) {
        return;
    }

    if (s_neutral_step < 5) {
        if (send_neutral_report_step(s_neutral_step)) {
            s_neutral_step++;
        }
    }
}

void usb_hid_on_report_complete(void) {
#ifndef ESP_PLATFORM
    s_mock_completed_count++;
#endif

    if (!s_neutral_in_progress) {
        return;
    }

    if (s_neutral_step < 5) {
        if (send_neutral_report_step(s_neutral_step)) {
            s_neutral_step++;
        }
    } else {
        // Step 4 (Tablet) has completed
        s_neutral_in_progress = false;
        s_neutral_complete = true;
    }
}
