/**
 * @file safety_manager.h
 * @brief PocketHID Fail-Safe Watchdog & Input Neutralization Engine
 */

#pragma once

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define DEFAULT_WATCHDOG_TIMEOUT_MS     250
#define MAX_CONSECUTIVE_MALFORMED_FAILS 3

typedef enum {
    SAFETY_STATE_IDLE = 0,
    SAFETY_STATE_ACTIVE,
    SAFETY_STATE_TIMED_OUT,
    SAFETY_STATE_DISCONNECTED,
    SAFETY_STATE_FAULT
} safety_state_t;

typedef void (*neutral_flush_cb_t)(void);

typedef struct {
    uint32_t timeout_ms;
    uint32_t last_packet_time_ms;
    uint32_t consecutive_malformed_count;
    safety_state_t state;
    bool is_neutral;
    neutral_flush_cb_t flush_cb;
} safety_manager_t;

/**
 * @brief Initializes safety manager with a configurable timeout and neutral flush callback.
 */
void safety_manager_init(safety_manager_t *mgr, uint32_t timeout_ms, neutral_flush_cb_t flush_cb);

/**
 * @brief Feeds the watchdog when a valid packet arrives.
 */
void safety_manager_feed(safety_manager_t *mgr, uint32_t current_time_ms);

/**
 * @brief Periodic tick to inspect watchdog timeout.
 */
void safety_manager_tick(safety_manager_t *mgr, uint32_t current_time_ms);

/**
 * @brief Handles incoming packet parse or validation error.
 */
void safety_manager_on_malformed_packet(safety_manager_t *mgr);

/**
 * @brief Immediately triggers fail-safe on BLE disconnection.
 */
void safety_manager_on_disconnect(safety_manager_t *mgr);

/**
 * @brief Forces all active HID endpoints to neutral release state.
 * Idempotent: Can be safely called repeatedly without redundant transmissions.
 */
void safety_manager_force_all_neutral(safety_manager_t *mgr);

#ifdef __cplusplus
}
#endif
