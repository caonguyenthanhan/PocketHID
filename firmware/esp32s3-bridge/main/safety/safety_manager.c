/**
 * @file safety_manager.c
 * @brief Safety manager implementation
 */

#include "safety_manager.h"

void safety_manager_init(safety_manager_t *mgr, uint32_t timeout_ms, neutral_flush_cb_t flush_cb) {
    if (!mgr) return;
    mgr->timeout_ms = (timeout_ms > 0) ? timeout_ms : DEFAULT_WATCHDOG_TIMEOUT_MS;
    mgr->last_packet_time_ms = 0;
    mgr->consecutive_malformed_count = 0;
    mgr->state = SAFETY_STATE_IDLE;
    mgr->is_neutral = true;
    mgr->flush_cb = flush_cb;
}

void safety_manager_feed(safety_manager_t *mgr, uint32_t current_time_ms) {
    if (!mgr) return;
    mgr->last_packet_time_ms = current_time_ms;
    mgr->consecutive_malformed_count = 0;
    mgr->state = SAFETY_STATE_ACTIVE;
    mgr->is_neutral = false;
}

void safety_manager_tick(safety_manager_t *mgr, uint32_t current_time_ms) {
    if (!mgr) return;

    if (mgr->state == SAFETY_STATE_ACTIVE) {
        uint32_t elapsed = current_time_ms - mgr->last_packet_time_ms;
        if (elapsed >= mgr->timeout_ms) {
            mgr->state = SAFETY_STATE_TIMED_OUT;
            safety_manager_force_all_neutral(mgr);
        }
    }
}

void safety_manager_on_malformed_packet(safety_manager_t *mgr) {
    if (!mgr) return;
    mgr->consecutive_malformed_count++;
    if (mgr->consecutive_malformed_count >= MAX_CONSECUTIVE_MALFORMED_FAILS) {
        mgr->state = SAFETY_STATE_FAULT;
        safety_manager_force_all_neutral(mgr);
    }
}

void safety_manager_on_disconnect(safety_manager_t *mgr) {
    if (!mgr) return;
    mgr->state = SAFETY_STATE_DISCONNECTED;
    safety_manager_force_all_neutral(mgr);
}

void safety_manager_force_all_neutral(safety_manager_t *mgr) {
    if (!mgr) return;
    if (mgr->is_neutral) return; // Idempotent: already in neutral state

    if (mgr->flush_cb) {
        mgr->flush_cb();
    }
    mgr->is_neutral = true;
}
