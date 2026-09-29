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

/* 128-bit UUIDs in little-endian order for NimBLE:
 * a55a0001-e234-4b56-8a78-9abcdef01234 ->
 * {0x34, 0x12, 0xf0, 0xde, 0xbc, 0x9a, 0x78, 0x8a, 0x56, 0x4b, 0x34, 0xe2, 0x01, 0x00, 0x5a, 0xa5}
 */
static const ble_uuid128_t s_pockethid_svc_uuid =
    BLE_UUID128_INIT(0x34, 0x12, 0xf0, 0xde, 0xbc, 0x9a, 0x78, 0x8a,
                     0x56, 0x4b, 0x34, 0xe2, 0x01, 0x00, 0x5a, 0xa5);

static const ble_uuid128_t s_pockethid_chr_rx_uuid =
    BLE_UUID128_INIT(0x34, 0x12, 0xf0, 0xde, 0xbc, 0x9a, 0x78, 0x8a,
                     0x56, 0x4b, 0x34, 0xe2, 0x02, 0x00, 0x5a, 0xa5);

static const ble_uuid128_t s_pockethid_chr_tx_uuid =
    BLE_UUID128_INIT(0x34, 0x12, 0xf0, 0xde, 0xbc, 0x9a, 0x78, 0x8a,
                     0x56, 0x4b, 0x34, 0xe2, 0x03, 0x00, 0x5a, 0xa5);

static uint16_t s_rx_val_handle;
static uint16_t s_tx_val_handle;
static uint16_t s_conn_handle;
#endif

static ble_callbacks_t s_cbs;
static bool s_is_connected = false;

#ifdef ESP_PLATFORM
static int gatt_svr_chr_access(uint16_t conn_handle, uint16_t attr_handle,
                               struct ble_gatt_access_ctxt *ctxt, void *arg) {
    (void)conn_handle;
    (void)attr_handle;
    (void)arg;
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

static const struct ble_gatt_svc_def s_gatt_svcs[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &s_pockethid_svc_uuid.u,
        .characteristics = (struct ble_gatt_chr_def[]) {
            {
                // RX Characteristic: Write Without Response / Write
                .uuid = &s_pockethid_chr_rx_uuid.u,
                .access_cb = gatt_svr_chr_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
                .val_handle = &s_rx_val_handle,
            },
            {
                // TX Characteristic: Notify
                .uuid = &s_pockethid_chr_tx_uuid.u,
                .access_cb = gatt_svr_chr_access,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &s_tx_val_handle,
            },
            {
                0, // No more characteristics
            }
        },
    },
    {
        0, // No more services
    },
};

static void ble_advertise(void);

static int ble_gap_event(struct ble_gap_event *event, void *arg) {
    (void)arg;
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
            ble_advertise();
            break;

        default:
            break;
    }
    return 0;
}

static void ble_advertise(void) {
    struct ble_gap_adv_params adv_params;
    struct ble_hs_adv_fields fields;
    int rc;

    memset(&fields, 0, sizeof(fields));
    fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
    fields.tx_pwr_lvl_is_present = 1;
    fields.tx_pwr_lvl = BLE_HS_ADV_TX_PWR_LVL_AUTO;

    const char *name = "PocketHID-Bridge";
    fields.name = (uint8_t *)name;
    fields.name_len = (uint8_t)strlen(name);
    fields.name_is_complete = 1;

    fields.uuids128 = (ble_uuid128_t *)&s_pockethid_svc_uuid;
    fields.num_uuids128 = 1;
    fields.uuids128_is_complete = 1;

    rc = ble_gap_adv_set_fields(&fields);
    if (rc != 0) {
        ESP_LOGE(TAG, "Error setting adv fields: %d", rc);
        return;
    }

    memset(&adv_params, 0, sizeof(adv_params));
    adv_params.conn_mode = BLE_GAP_CONN_MODE_UND;
    adv_params.disc_mode = BLE_GAP_DISC_MODE_GEN;

    rc = ble_gap_adv_start(BLE_OWN_ADDR_PUBLIC, NULL, BLE_HS_FOREVER,
                           &adv_params, ble_gap_event, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "Error starting adv: %d", rc);
    } else {
        ESP_LOGI(TAG, "BLE Advertising started for PocketHID-Bridge");
    }
}

static void ble_on_sync(void) {
    ble_advertise();
}

static void ble_host_task(void *param) {
    (void)param;
    ESP_LOGI(TAG, "BLE Host Task Started");
    nimble_port_run();
    nimble_port_freertos_deinit();
}
#endif

bool bridge_ble_transport_init(const ble_callbacks_t *cbs) {
    if (cbs) {
        s_cbs = *cbs;
    }
    s_is_connected = false;

#ifdef ESP_PLATFORM
    ESP_LOGI(TAG, "Initializing NimBLE Stack for PocketHID Bridge");
    esp_err_t ret = nimble_port_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to init nimble: %d", ret);
        return false;
    }

    ble_svc_gap_init();
    ble_svc_gatt_init();

    ble_hs_cfg.sync_cb = ble_on_sync;

    int rc = ble_gatts_count_cfg(s_gatt_svcs);
    if (rc != 0) {
        ESP_LOGE(TAG, "ble_gatts_count_cfg failed: %d", rc);
        return false;
    }

    rc = ble_gatts_add_svcs(s_gatt_svcs);
    if (rc != 0) {
        ESP_LOGE(TAG, "ble_gatts_add_svcs failed: %d", rc);
        return false;
    }

    ble_svc_gap_device_name_set("PocketHID-Bridge");

    nimble_port_freertos_init(ble_host_task);
    return true;
#else
    return true;
#endif
}

bool bridge_ble_transport_send_notify(const uint8_t *data, size_t length) {
    if (!s_is_connected || !data || length == 0) return false;

#ifdef ESP_PLATFORM
    struct os_mbuf *om = ble_hs_mbuf_from_flat(data, length);
    if (!om) return false;
    return ble_gatts_notify_custom(s_conn_handle, s_tx_val_handle, om) == 0;
#else
    return true;
#endif
}

bool bridge_ble_transport_is_connected(void) {
    return s_is_connected;
}
