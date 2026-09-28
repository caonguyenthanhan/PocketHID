/**
 * @file usb_hid.c
 * @brief TinyUSB Composite HID integration for ESP32-S3
 */

#include "usb_hid.h"
#include "hid_reports.h"
#include <string.h>

#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "tinyusb.h"
#include "class/hid/hid_device.h"
static const char *TAG = "USB_HID";
#endif

static usb_status_t s_usb_status = USB_STATUS_NOT_INITIALIZED;

#ifdef ESP_PLATFORM
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

// Invoked when device is mounted
void tud_mount_cb(void) {
    s_usb_status = USB_STATUS_ENUMERATED;
    ESP_LOGI(TAG, "USB Mounted & Enumerated by Host");
}

// Invoked when device is unmounted
void tud_umount_cb(void) {
    s_usb_status = USB_STATUS_ATTACHED;
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
        return false;
    }
    s_usb_status = USB_STATUS_ATTACHED;
    return true;
#else
    s_usb_status = USB_STATUS_ENUMERATED;
    return true;
#endif
}

usb_status_t usb_hid_get_status(void) {
    return s_usb_status;
}

bool usb_hid_send_report(uint8_t report_id, const uint8_t *report, size_t len) {
    if (!report || len == 0) return false;

#ifdef ESP_PLATFORM
    if (!tud_mounted()) {
        return false;
    }
    return tud_hid_report(report_id, report, (uint8_t)len);
#else
    (void)report_id;
    (void)len;
    return true; // Mock success on host
#endif
}

void usb_hid_send_all_neutral(void) {
    uint8_t kb[REPORT_LEN_KEYBOARD];
    hid_build_keyboard_neutral(kb);
    usb_hid_send_report(REPORT_ID_KEYBOARD, kb, sizeof(kb));

    uint8_t mouse[REPORT_LEN_MOUSE];
    hid_build_mouse_neutral(mouse);
    usb_hid_send_report(REPORT_ID_MOUSE, mouse, sizeof(mouse));

    uint8_t consumer[REPORT_LEN_CONSUMER];
    hid_build_consumer_neutral(consumer);
    usb_hid_send_report(REPORT_ID_CONSUMER, consumer, sizeof(consumer));

    uint8_t gamepad[REPORT_LEN_GAMEPAD];
    hid_build_gamepad_neutral(gamepad);
    usb_hid_send_report(REPORT_ID_GAMEPAD, gamepad, sizeof(gamepad));

    uint8_t tablet[REPORT_LEN_TABLET];
    hid_build_tablet_neutral(tablet);
    usb_hid_send_report(REPORT_ID_TABLET, tablet, sizeof(tablet));
}
