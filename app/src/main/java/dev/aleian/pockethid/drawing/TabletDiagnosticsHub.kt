package dev.aleian.pockethid.drawing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Real-time Telemetry and Diagnostics Hub for PocketHID Graphics Tablet.
 *
 * Exposes developer diagnostics:
 * - RAW (x, y)
 * - CANVAS (x, y)
 * - NORMALIZED (x, y)
 * - HID (x, y)
 * - ORIENTATION (LANDSCAPE / PORTRAIT)
 * - CANVAS SIZE (W x H)
 * - HID RANGE (X 0..32767, Y 0..32767)
 * - TARGET MODE (ABSOLUTE_TABLET / RELATIVE_MOUSE)
 * - ASPECT POLICY (FIT / STRETCH)
 * - CONTACT STATE (Tip Switch, In Range)
 */
object TabletDiagnosticsHub {

    data class TabletTelemetry(
        val rawX: Float = 0f,
        val rawY: Float = 0f,
        val canvasX: Float = 0f,
        val canvasY: Float = 0f,
        val safeX: Float = 0f,
        val safeY: Float = 0f,
        val normalizedX: Float = 0f,
        val normalizedY: Float = 0f,
        val hidX: Int = 0,
        val hidY: Int = 0,
        val orientation: String = "LANDSCAPE",
        val canvasWidth: Float = 0f,
        val canvasHeight: Float = 0f,
        val insetsDesc: String = "L0 T0 R0 B0",
        val hidRangeX: String = "0..32767",
        val hidRangeY: String = "0..32767",
        val targetMode: String = "ABSOLUTE_TABLET",
        val aspectPolicy: String = "FIT",
        val isTipDown: Boolean = false,
        val isInRange: Boolean = false,
        val activeTool: String = "PEN",
        val reportsSent: Long = 0L,
        val lastReportHex: String = "00 00 00 00 00"
    )

    private val _telemetry = MutableStateFlow(TabletTelemetry())
    val telemetry: StateFlow<TabletTelemetry> = _telemetry.asStateFlow()

    fun update(
        rawX: Float,
        rawY: Float,
        canvasX: Float,
        canvasY: Float,
        safeX: Float,
        safeY: Float,
        normalizedX: Float,
        normalizedY: Float,
        hidX: Int,
        hidY: Int,
        orientation: String,
        canvasWidth: Float,
        canvasHeight: Float,
        insetsDesc: String,
        targetMode: String,
        aspectPolicy: String,
        isTipDown: Boolean,
        isInRange: Boolean,
        activeTool: String,
        lastReportHex: String
    ) {
        _telemetry.update { current ->
            current.copy(
                rawX = rawX,
                rawY = rawY,
                canvasX = canvasX,
                canvasY = canvasY,
                safeX = safeX,
                safeY = safeY,
                normalizedX = normalizedX,
                normalizedY = normalizedY,
                hidX = hidX,
                hidY = hidY,
                orientation = orientation,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                insetsDesc = insetsDesc,
                targetMode = targetMode,
                aspectPolicy = aspectPolicy,
                isTipDown = isTipDown,
                isInRange = isInRange,
                activeTool = activeTool,
                reportsSent = current.reportsSent + 1,
                lastReportHex = lastReportHex
            )
        }
    }

    fun onContactRelease() {
        _telemetry.update { current ->
            current.copy(
                isTipDown = false,
                isInRange = false,
                lastReportHex = "00 00 00 00 00"
            )
        }
    }

    fun reset() {
        _telemetry.value = TabletTelemetry()
    }
}
