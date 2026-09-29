/**
 * @file app_main.c
 * @brief PocketHID ESP32-S3 External HID Bridge POC Entry Point
 */

#include <stdio.h>
#include <string.h>

#include "protocol/packet_parser.h"
#include "safety/safety_manager.h"
#include "hid/usb_hid.h"
#include "hid/hid_reports.h"
#include "ble/ble_transport.h"
#include "diagnostics/diagnostics.h"
#include "tests/poc_test_mode.h"

#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "esp_timer.h"
#include "nvs_flash.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
static const char *TAG = "POCKETHID_MAIN";
static inline uint32_t get_time_ms(void) {
    return (uint32_t)(esp_timer_get_time() / 1000ULL);
}
#else
#include <time.h>
static inline uint32_t get_time_ms(void) {
    return (uint32_t)(clock() * 1000 / CLOCKS_PER_SEC);
}
#endif

static safety_manager_t s_safety_mgr;
static uint16_t s_last_sequence = 0;
static bool s_is_first_packet = true;

static void handle_rx_packet(const uint8_t *data, size_t length) {
    diagnostics_inc_rx();

    parsed_packet_t pkt;
    parse_result_t res = parse_packet(data, length, &pkt);
    if (res != PARSE_OK) {
        diagnostics_inc_malformed((int32_t)res);
        safety_manager_on_malformed_packet(&s_safety_mgr);
#ifdef ESP_PLATFORM
        ESP_LOGW(TAG, "Packet parse error: %d", res);
#endif
        return;
    }

    if (!validate_sequence(pkt.header.sequence_no, &s_last_sequence, s_is_first_packet)) {
        diagnostics_inc_malformed((int32_t)PARSE_ERR_SEQUENCE_REGRESSION);
        safety_manager_on_malformed_packet(&s_safety_mgr);
#ifdef ESP_PLATFORM
        ESP_LOGW(TAG, "Sequence regression/replay: %u (last %u)", pkt.header.sequence_no, s_last_sequence);
#endif
        return;
    }
    s_is_first_packet = false;

    // Packet is valid: feed watchdog
    safety_manager_feed(&s_safety_mgr, get_time_ms());
    diagnostics_inc_valid(pkt.header.sequence_no);

    // Dispatch semantic packet to USB HID report builder
    uint8_t hid_buf[16];
    switch (pkt.header.msg_type) {
        case MSG_SYS_HELLO: {
            // Reply with MSG_SYS_READY
            payload_sys_ready_t ready;
            ready.bridge_random_nonce = 0x5A5A1234;
            ready.bridge_firmware_ver = BRIDGE_FIRMWARE_VERSION;
            ready.usb_host_status = (usb_hid_get_status() == USB_STATUS_ENUMERATED) ? 1 : 0;
            ready.active_hid_endpoints = 0x1F; // All 5 endpoints active
            bridge_ble_transport_send_notify((const uint8_t*)&ready, sizeof(ready));
            break;
        }

        case MSG_SYS_HEARTBEAT:
            // Handled by watchdog feed above
            break;

        case MSG_SYS_EMERGENCY_NEUTRAL:
            safety_manager_force_all_neutral(&s_safety_mgr);
            break;

        case MSG_KB_REPORT: {
            payload_kb_report_t *kb = (payload_kb_report_t*)pkt.payload;
            hid_build_keyboard_report(kb->modifiers, kb->keycodes, 6, hid_buf);
            usb_hid_send_report(REPORT_ID_KEYBOARD, hid_buf, REPORT_LEN_KEYBOARD);
            diagnostics_inc_dispatched();
            break;
        }

        case MSG_KB_RELEASE_ALL:
            hid_build_keyboard_neutral(hid_buf);
            usb_hid_send_report(REPORT_ID_KEYBOARD, hid_buf, REPORT_LEN_KEYBOARD);
            diagnostics_inc_dispatched();
            break;

        case MSG_MOUSE_REPORT: {
            payload_mouse_report_t *m = (payload_mouse_report_t*)pkt.payload;
            hid_build_mouse_report(m->buttons, m->dx, m->dy, m->wheel, hid_buf);
            usb_hid_send_report(REPORT_ID_MOUSE, hid_buf, REPORT_LEN_MOUSE);
            diagnostics_inc_dispatched();
            break;
        }

        case MSG_CONSUMER_CLICK: {
            payload_consumer_click_t *c = (payload_consumer_click_t*)pkt.payload;
            hid_build_consumer_report(c->usage_code, hid_buf);
            usb_hid_send_report(REPORT_ID_CONSUMER, hid_buf, REPORT_LEN_CONSUMER);
            diagnostics_inc_dispatched();
            break;
        }

        case MSG_GAMEPAD_REPORT: {
            payload_gamepad_report_t *g = (payload_gamepad_report_t*)pkt.payload;
            hid_build_gamepad_report(
                g->buttons, g->hat_switch,
                g->left_stick_x, g->left_stick_y,
                g->right_stick_x, g->right_stick_y,
                g->left_trigger, g->right_trigger,
                hid_buf
            );
            usb_hid_send_report(REPORT_ID_GAMEPAD, hid_buf, REPORT_LEN_GAMEPAD);
            diagnostics_inc_dispatched();
            break;
        }

        case MSG_TABLET_REPORT: {
            payload_tablet_report_t *t = (payload_tablet_report_t*)pkt.payload;
            hid_build_tablet_report(t->status, t->x, t->y, hid_buf);
            usb_hid_send_report(REPORT_ID_TABLET, hid_buf, REPORT_LEN_TABLET);
            diagnostics_inc_dispatched();
            break;
        }

        default:
            break;
    }
}

static void handle_ble_connection(bool connected) {
    if (connected) {
        s_is_first_packet = true;
    } else {
        safety_manager_on_disconnect(&s_safety_mgr);
    }
}

void app_main(void) {
#ifdef ESP_PLATFORM
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
#endif

    diagnostics_init();
    safety_manager_init(&s_safety_mgr, DEFAULT_WATCHDOG_TIMEOUT_MS, usb_hid_send_all_neutral);

    // Phase 3: USB HID Composite
    bool usb_ok = usb_hid_init();
#ifdef ESP_PLATFORM
    if (!usb_ok) {
        ESP_LOGE(TAG, "USB: INIT_FAILED");
    } else {
        ESP_LOGI(TAG, "USB: INIT_OK (USB STACK READY, USB HOST ENUMERATION PENDING)");
    }
#endif

    // Phase 5: BLE GATT Server
    ble_callbacks_t ble_cbs = {
        .on_packet = handle_rx_packet,
        .on_connection = handle_ble_connection,
    };
    bool ble_ok = bridge_ble_transport_init(&ble_cbs);
#ifdef ESP_PLATFORM
    if (!ble_ok) {
        ESP_LOGE(TAG, "BLE: INIT_FAILED");
    } else {
        ESP_LOGI(TAG, "BLE: INIT_OK");
    }

    ESP_LOGI(TAG, "SAFETY: READY (CONFIGURED TIMEOUT=%u ms)", DEFAULT_WATCHDOG_TIMEOUT_MS);
    ESP_LOGI(TAG, "PocketHID ESP32-S3 Bridge POC Initialized (Firmware v%04x)", BRIDGE_FIRMWARE_VERSION);

    // Periodic safety watchdog loop
    while (1) {
        vTaskDelay(pdMS_TO_TICKS(10));
        safety_manager_tick(&s_safety_mgr, get_time_ms());
        usb_hid_tick();
    }
#endif
}
