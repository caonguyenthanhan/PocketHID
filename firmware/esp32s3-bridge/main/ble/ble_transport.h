/**
 * @file ble_transport.h
 * @brief PocketHID Custom BLE GATT Service & Peripheral Interface
 * 
 * Note: Uses custom 128-bit vendor service UUID. Does NOT use Bluetooth SIG 0x1812.
 */

#pragma once

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Custom 128-bit Vendor Service & Characteristic UUIDs */
#define POCKETHID_SERVICE_UUID          "a55a0001-e234-4b56-8a78-9abcdef01234"
#define POCKETHID_CHAR_RX_UUID          "a55a0002-e234-4b56-8a78-9abcdef01234" // Phone -> Bridge (Write Without Response)
#define POCKETHID_CHAR_TX_UUID          "a55a0003-e234-4b56-8a78-9abcdef01234" // Bridge -> Phone (Notify)

typedef void (*ble_packet_rx_cb_t)(const uint8_t *data, size_t length);
typedef void (*ble_connection_cb_t)(bool connected);

typedef struct {
    ble_packet_rx_cb_t on_packet;
    ble_connection_cb_t on_connection;
} ble_callbacks_t;

/**
 * @brief Initializes BLE GATT server and begins advertising.
 */
bool ble_transport_init(const ble_callbacks_t *cbs);

/**
 * @brief Sends notification packet back to connected central (e.g. MSG_SYS_READY).
 */
bool ble_transport_send_notify(const uint8_t *data, size_t length);

/**
 * @brief Checks if an iPhone central is currently connected.
 */
bool ble_transport_is_connected(void);

#ifdef __cplusplus
}
#endif
