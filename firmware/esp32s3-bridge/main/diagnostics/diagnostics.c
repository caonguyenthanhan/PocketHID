/**
 * @file diagnostics.c
 * @brief Diagnostics implementation
 */

#include "diagnostics.h"
#include <string.h>

static bridge_diagnostics_t s_diag;

void diagnostics_init(void) {
    memset(&s_diag, 0, sizeof(s_diag));
    s_diag.firmware_version = BRIDGE_FIRMWARE_VERSION;
}

void diagnostics_inc_rx(void) {
    s_diag.total_packets_received++;
}

void diagnostics_inc_valid(uint16_t seq) {
    s_diag.total_packets_valid++;
    s_diag.last_valid_seq = seq;
}

void diagnostics_inc_malformed(int32_t error_code) {
    s_diag.total_packets_malformed++;
    s_diag.last_error_code = error_code;
}

void diagnostics_inc_dispatched(void) {
    s_diag.total_reports_dispatched++;
}

void diagnostics_inc_timeout(void) {
    s_diag.total_watchdog_timeouts++;
}

const bridge_diagnostics_t* diagnostics_get(void) {
    return &s_diag;
}
