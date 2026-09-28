/**
 * @file poc_test_mode.c
 * @brief POC Test Mode Implementation
 */

#include "poc_test_mode.h"
#include "../hid/usb_hid.h"
#include "../hid/hid_reports.h"

#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
static const char *TAG = "POC_TEST_MODE";
#define DELAY_MS(ms) vTaskDelay(pdMS_TO_TICKS(ms))
#else
#include <stdio.h>
#define DELAY_MS(ms) do {} while(0)
#endif

void poc_test_mode_run_sequence(void) {
#ifdef ESP_PLATFORM
    ESP_LOGW(TAG, "=== STARTING POC DETERMINISTIC USB HID TEST SEQUENCE ===");
#else
    printf("=== STARTING POC DETERMINISTIC USB HID TEST SEQUENCE ===\n");
#endif

    uint8_t report_buf[16];

    // 1. Keyboard A press/release
    uint8_t key_a = 0x04; // HID 'a'
    hid_build_keyboard_report(0, &key_a, 1, report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(50);
    hid_build_keyboard_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(100);

    // 2. Shift+A
    hid_build_keyboard_report(0x02 /* LShift */, &key_a, 1, report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(50);
    hid_build_keyboard_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(100);

    // 3. Ctrl+C
    uint8_t key_c = 0x06; // HID 'c'
    hid_build_keyboard_report(0x01 /* LCtrl */, &key_c, 1, report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(50);
    hid_build_keyboard_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(100);

    // 4. Win+D
    uint8_t key_d = 0x07; // HID 'd'
    hid_build_keyboard_report(0x08 /* LGUI */, &key_d, 1, report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(50);
    hid_build_keyboard_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_KEYBOARD, report_buf, REPORT_LEN_KEYBOARD);
    DELAY_MS(100);

    // 5. Mouse move (+50, +50)
    hid_build_mouse_report(0, 50, 50, 0, report_buf);
    usb_hid_send_report(REPORT_ID_MOUSE, report_buf, REPORT_LEN_MOUSE);
    DELAY_MS(50);

    // 6. Left click
    hid_build_mouse_report(0x01 /* Left */, 0, 0, 0, report_buf);
    usb_hid_send_report(REPORT_ID_MOUSE, report_buf, REPORT_LEN_MOUSE);
    DELAY_MS(50);
    hid_build_mouse_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_MOUSE, report_buf, REPORT_LEN_MOUSE);
    DELAY_MS(100);

    // 7. Scroll (+10)
    hid_build_mouse_report(0, 0, 0, 10, report_buf);
    usb_hid_send_report(REPORT_ID_MOUSE, report_buf, REPORT_LEN_MOUSE);
    DELAY_MS(100);

    // 8. Consumer Volume + (0x00E9)
    hid_build_consumer_report(0x00E9, report_buf);
    usb_hid_send_report(REPORT_ID_CONSUMER, report_buf, REPORT_LEN_CONSUMER);
    DELAY_MS(50);
    hid_build_consumer_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_CONSUMER, report_buf, REPORT_LEN_CONSUMER);
    DELAY_MS(100);

    // 9. Consumer Volume - (0x00EA)
    hid_build_consumer_report(0x00EA, report_buf);
    usb_hid_send_report(REPORT_ID_CONSUMER, report_buf, REPORT_LEN_CONSUMER);
    DELAY_MS(50);
    hid_build_consumer_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_CONSUMER, report_buf, REPORT_LEN_CONSUMER);
    DELAY_MS(100);

    // 10. Gamepad A (Bit 0)
    hid_build_gamepad_report(0x0001, 0, 0, 0, 0, 0, 0, 0, report_buf);
    usb_hid_send_report(REPORT_ID_GAMEPAD, report_buf, REPORT_LEN_GAMEPAD);
    DELAY_MS(50);
    hid_build_gamepad_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_GAMEPAD, report_buf, REPORT_LEN_GAMEPAD);
    DELAY_MS(100);

    // 11. Gamepad analog axis (+32767 on Left X)
    hid_build_gamepad_report(0, 0, 32767, 0, 0, 0, 0, 0, report_buf);
    usb_hid_send_report(REPORT_ID_GAMEPAD, report_buf, REPORT_LEN_GAMEPAD);
    DELAY_MS(50);
    hid_build_gamepad_neutral(report_buf);
    usb_hid_send_report(REPORT_ID_GAMEPAD, report_buf, REPORT_LEN_GAMEPAD);
    DELAY_MS(100);

    // 12. Tablet top-left (0, 0)
    hid_build_tablet_report(0x03 /* Tip + InRange */, 0, 0, report_buf);
    usb_hid_send_report(REPORT_ID_TABLET, report_buf, REPORT_LEN_TABLET);
    DELAY_MS(50);

    // 13. Tablet center (16383, 16383)
    hid_build_tablet_report(0x03, 16383, 16383, report_buf);
    usb_hid_send_report(REPORT_ID_TABLET, report_buf, REPORT_LEN_TABLET);
    DELAY_MS(50);

    // 14. Tablet bottom-right (32767, 32767)
    hid_build_tablet_report(0x03, 32767, 32767, report_buf);
    usb_hid_send_report(REPORT_ID_TABLET, report_buf, REPORT_LEN_TABLET);
    DELAY_MS(50);

    // 15. Neutral / release all
    usb_hid_send_all_neutral();

#ifdef ESP_PLATFORM
    ESP_LOGI(TAG, "=== POC TEST SEQUENCE FINISHED: ALL ENDPOINTS NEUTRAL ===");
#else
    printf("=== POC TEST SEQUENCE FINISHED: ALL ENDPOINTS NEUTRAL ===\n");
#endif
}
