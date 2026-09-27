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
    val pulseDurationMs: Long = 85L,
    val payloadHex: String = "00 00",
    val releaseHex: String = "00 00",
    val deviceInfo: String = "None",
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
        status: String,
        pulseDurationMs: Long = 85L,
        payloadHex: String = "00 00",
        releaseHex: String = "00 00",
        deviceInfo: String = "None"
    ) {
        _telemetry.value = ConsumerTelemetry(
            actionName = actionName,
            reportId = HidConstants.REPORT_ID_CONSUMER,
            usageCode = usageCode,
            pressSent = pressSent,
            releaseSent = releaseSent,
            transportStatus = status,
            pulseDurationMs = pulseDurationMs,
            payloadHex = payloadHex,
            releaseHex = releaseHex,
            deviceInfo = deviceInfo,
            timestamp = System.currentTimeMillis()
        )
    }

    fun resolveActionName(usageCode: Int): String {
        return when (usageCode) {
            HidConstants.CONSUMER_PLAY_PAUSE -> "PlayPause"
            HidConstants.CONSUMER_STOP -> "Stop"
            HidConstants.CONSUMER_SCAN_NEXT -> "NextTrack"
            HidConstants.CONSUMER_SCAN_PREV -> "PreviousTrack"
            HidConstants.CONSUMER_VOLUME_UP -> "VolumeUp"
            HidConstants.CONSUMER_VOLUME_DOWN -> "VolumeDown"
            HidConstants.CONSUMER_MUTE -> "Mute"
            else -> "Usage_0x" + usageCode.toString(16).uppercase()
        }
    }

    fun reset() {
        _telemetry.value = ConsumerTelemetry()
    }
}
