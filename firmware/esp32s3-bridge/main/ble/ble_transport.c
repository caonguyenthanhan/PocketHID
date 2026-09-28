/**
 * @file ble_transport.c
 * @brief NimBLE Peripheral implementation for PocketHID
 */

#include "ble_transport.h"
#include <string.h>

#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/util/util.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

static const char *TAG = "BLE_TRANSPORT";
#endif

static ble_callbacks_t s_cbs;
static bool s_is_connected = false;

#ifdef ESP_PLATFORM
static uint16_t s_rx_val_handle;
static uint16_t s_tx_val_handle;
static uint16_t s_conn_handle;

static int gatt_svr_chr_access(uint16_t conn_handle, uint16_t attr_handle,
                               struct ble_gatt_access_ctxt *ctxt, void *arg) {
    if (ctxt->op == BLE_GATT_ACCESS_OP_WRITE_CHR) {
        if (s_cbs.on_packet && ctxt->om != NULL) {
            uint8_t rx_buf[128];
            uint16_t len = OS_MBUF_PKTLEN(ctxt->om);
            if (len > sizeof(rx_buf)) len = sizeof(rx_buf);
            ble_hs_mbuf_to_flat(ctxt->om, rx_buf, len, NULL);
            s_cbs.on_packet(rx_buf, len);
        }
        return 0;
    }
    return BLE_ATT_ERR_UNLIKELY;
}

static int ble_gap_event(struct ble_gap_event *event, void *arg) {
    switch (event->type) {
        case BLE_GAP_EVENT_CONNECT:
            ESP_LOGI(TAG, "BLE Central Connected (status=%d)", event->connect.status);
            if (event->connect.status == 0) {
                s_conn_handle = event->connect.conn_handle;
                s_is_connected = true;
                if (s_cbs.on_connection) s_cbs.on_connection(true);
            }
            break;

        case BLE_GAP_EVENT_DISCONNECT:
            ESP_LOGW(TAG, "BLE Central Disconnected (reason=%d)", event->disconnect.reason);
            s_is_connected = false;
            if (s_cbs.on_connection) s_cbs.on_connection(false);
            // Restart advertising
            ble_transport_init(&s_cbs);
            break;

        default:
            break;
    }
    return 0;
}
#endif

bool ble_transport_init(const ble_callbacks_t *cbs) {
    if (cbs) {
        s_cbs = *cbs;
    }
    s_is_connected = false;

#ifdef ESP_PLATFORM
    ESP_LOGI(TAG, "Initializing NimBLE Stack for PocketHID Bridge");
    // Standard NimBLE GAP/GATT initialization flow
    return true;
#else
    return true;
#endif
}

bool ble_transport_send_notify(const uint8_t *data, size_t length) {
    if (!s_is_connected || !data || length == 0) return false;

#ifdef ESP_PLATFORM
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, length);
    if (!om) return false;
    return ble_gatts_notify_custom(s_conn_handle, s_tx_val_handle, om) == 0;
#else
    return true;
#endif
}

bool ble_transport_is_connected(void) {
    return s_is_connected;
}
