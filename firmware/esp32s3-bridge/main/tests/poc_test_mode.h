/**
 * @file poc_test_mode.h
 * @brief Development-Only Deterministic USB HID Sequence Generator
 * 
 * WARNING: POC TEST MODE ONLY. NOT FOR PRODUCTION USE.
 */

#pragma once

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Runs the 15-step deterministic USB HID test sequence directly against USB.
 * Can be triggered via physical boot button or serial command.
 */
void poc_test_mode_run_sequence(void);

#ifdef __cplusplus
}
#endif
