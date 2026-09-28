/**
 * @file diagnostics.h
 * @brief PocketHID Firmware Diagnostics & Telemetry Counters
 */

#pragma once

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define BRIDGE_FIRMWARE_VERSION 0x0100 // v1.0.0

typedef struct {
    uint32_t total_packets_received;
    uint32_t total_packets_valid;
    uint32_t total_packets_malformed;
    uint32_t total_reports_dispatched;
    uint32_t total_watchdog_timeouts;
    uint16_t last_valid_seq;
    int32_t  last_error_code;
    uint16_t firmware_version;
} bridge_diagnostics_t;

void diagnostics_init(void);
void diagnostics_inc_rx(void);
void diagnostics_inc_valid(uint16_t seq);
void diagnostics_inc_malformed(int32_t error_code);
void diagnostics_inc_dispatched(void);
void diagnostics_inc_timeout(void);
const bridge_diagnostics_t* diagnostics_get(void);

#ifdef __cplusplus
}
#endif
