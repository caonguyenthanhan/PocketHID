package dev.aleian.pockethid.gamepad

import dev.aleian.pockethid.model.HidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Diagnostics telemetry model for HID Consumer Control (Media keys, Report ID 3).
 */
data class ConsumerTelemetry(
    val actionName: String = "IDLE",
    val reportId: Byte = HidConstants.REPORT_ID_CONSUMER,
    val usageCode: Int = 0,
    val pressSent: Boolean = false,
    val releaseSent: Boolean = false,
    val transportStatus: String = "IDLE",
    val timestamp: Long = 0L
) {
    val usageHex: String get() = "0x" + usageCode.toString(16).padStart(4, '0').uppercase()
}

/**
 * Singleton hub for monitoring and testing Consumer Control HID dispatches in real-time.
 */
object ConsumerDiagnosticsHub {
    private val _telemetry = MutableStateFlow(ConsumerTelemetry())
    val telemetry: StateFlow<ConsumerTelemetry> = _telemetry.asStateFlow()

    fun onEvent(
        actionName: String,
        usageCode: Int,
        pressSent: Boolean,
        releaseSent: Boolean,
        status: String
    ) {
        _telemetry.value = ConsumerTelemetry(
            actionName = actionName,
            reportId = HidConstants.REPORT_ID_CONSUMER,
            usageCode = usageCode,
            pressSent = pressSent,
            releaseSent = releaseSent,
            transportStatus = status,
            timestamp = System.currentTimeMillis()
        )
    }

    fun resolveActionName(usageCode: Int): String {
        return when (usageCode) {
            HidConstants.CONSUMER_PLAY_PAUSE -> "PLAY_PAUSE"
            HidConstants.CONSUMER_STOP -> "STOP"
            HidConstants.CONSUMER_SCAN_NEXT -> "NEXT"
            HidConstants.CONSUMER_SCAN_PREV -> "PREV"
            HidConstants.CONSUMER_VOLUME_UP -> "VOLUME_UP"
            HidConstants.CONSUMER_VOLUME_DOWN -> "VOLUME_DOWN"
            HidConstants.CONSUMER_MUTE -> "MUTE"
            else -> "USAGE_0x" + usageCode.toString(16).uppercase()
        }
    }

    fun reset() {
        _telemetry.value = ConsumerTelemetry()
    }
}
