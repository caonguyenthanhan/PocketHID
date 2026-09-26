package dev.aleian.pockethid.gamepad

import dev.aleian.pockethid.model.HidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Snapshot of active gamepad report state for diagnostics and UI indicators.
 */
data class GamepadTelemetry(
    val buttons: Int = 0,
    val hat: Byte = HidConstants.GAMEPAD_HAT_CENTERED,
    val leftX: Short = 0,
    val leftY: Short = 0,
    val rightX: Short = 0,
    val rightY: Short = 0,
    val leftTrigger: Byte = 0,
    val rightTrigger: Byte = 0,
    val downCount: Long = 0L,
    val upCount: Long = 0L
) {
    val a: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_A) != 0
    val b: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_B) != 0
    val x: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_X) != 0
    val y: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_Y) != 0
    val lb: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_LB) != 0
    val rb: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_RB) != 0
    val back: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_BACK) != 0
    val start: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_START) != 0
    val guide: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_GUIDE) != 0
    val l3: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_L3) != 0
    val r3: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_R3) != 0

    val dpadUp: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_DPAD_UP) != 0 || hat == HidConstants.GAMEPAD_HAT_UP || hat == HidConstants.GAMEPAD_HAT_UP_LEFT || hat == HidConstants.GAMEPAD_HAT_UP_RIGHT
    val dpadDown: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_DPAD_DOWN) != 0 || hat == HidConstants.GAMEPAD_HAT_DOWN || hat == HidConstants.GAMEPAD_HAT_DOWN_LEFT || hat == HidConstants.GAMEPAD_HAT_DOWN_RIGHT
    val dpadLeft: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_DPAD_LEFT) != 0 || hat == HidConstants.GAMEPAD_HAT_LEFT || hat == HidConstants.GAMEPAD_HAT_UP_LEFT || hat == HidConstants.GAMEPAD_HAT_DOWN_LEFT
    val dpadRight: Boolean get() = (buttons and HidConstants.GAMEPAD_BTN_DPAD_RIGHT) != 0 || hat == HidConstants.GAMEPAD_HAT_RIGHT || hat == HidConstants.GAMEPAD_HAT_UP_RIGHT || hat == HidConstants.GAMEPAD_HAT_DOWN_RIGHT

    val lxNormalized: Float get() = leftX.toFloat() / 32767f
    val lyNormalized: Float get() = leftY.toFloat() / 32767f
    val rxNormalized: Float get() = rightX.toFloat() / 32767f
    val ryNormalized: Float get() = rightY.toFloat() / 32767f

    val ltNormalized: Float get() = (leftTrigger.toInt() and 0xFF).toFloat() / 255f
    val rtNormalized: Float get() = (rightTrigger.toInt() and 0xFF).toFloat() / 255f

    val isNeutral: Boolean
        get() = buttons == 0 &&
                hat == HidConstants.GAMEPAD_HAT_CENTERED &&
                leftX == 0.toShort() &&
                leftY == 0.toShort() &&
                rightX == 0.toShort() &&
                rightY == 0.toShort() &&
                leftTrigger == 0.toByte() &&
                rightTrigger == 0.toByte()

    val rawBytes: ByteArray
        get() {
            val report = ByteArray(13)
            report[0] = (buttons and 0xFF).toByte()
            report[1] = ((buttons shr 8) and 0xFF).toByte()
            report[2] = (hat.toInt() and 0x0F).toByte()
            report[3] = (leftX.toInt() and 0xFF).toByte()
            report[4] = ((leftX.toInt() shr 8) and 0xFF).toByte()
            report[5] = (leftY.toInt() and 0xFF).toByte()
            report[6] = ((leftY.toInt() shr 8) and 0xFF).toByte()
            report[7] = (rightX.toInt() and 0xFF).toByte()
            report[8] = ((rightX.toInt() shr 8) and 0xFF).toByte()
            report[9] = (rightY.toInt() and 0xFF).toByte()
            report[10] = ((rightY.toInt() shr 8) and 0xFF).toByte()
            report[11] = leftTrigger
            report[12] = rightTrigger
            return report
        }

    val rawReportHex: String
        get() = rawBytes.joinToString(" ") { String.format(Locale.US, "%02X", it) }

    val activeButtonsList: List<String>
        get() {
            val list = mutableListOf<String>()
            if (a) list.add("A")
            if (b) list.add("B")
            if (x) list.add("X")
            if (y) list.add("Y")
            if (lb) list.add("LB")
            if (rb) list.add("RB")
            if (back) list.add("BACK")
            if (start) list.add("START")
            if (guide) list.add("GUIDE")
            if (l3) list.add("L3")
            if (r3) list.add("R3")
            if (dpadUp) list.add("DPAD_UP")
            if (dpadDown) list.add("DPAD_DOWN")
            if (dpadLeft) list.add("DPAD_LEFT")
            if (dpadRight) list.add("DPAD_RIGHT")
            return list
        }

    val dpadLabel: String get() = when (hat) {
        HidConstants.GAMEPAD_HAT_UP -> "UP"
        HidConstants.GAMEPAD_HAT_UP_RIGHT -> "UP-RIGHT"
        HidConstants.GAMEPAD_HAT_RIGHT -> "RIGHT"
        HidConstants.GAMEPAD_HAT_DOWN_RIGHT -> "DOWN-RIGHT"
        HidConstants.GAMEPAD_HAT_DOWN -> "DOWN"
        HidConstants.GAMEPAD_HAT_DOWN_LEFT -> "DOWN-LEFT"
        HidConstants.GAMEPAD_HAT_LEFT -> "LEFT"
        HidConstants.GAMEPAD_HAT_UP_LEFT -> "UP-LEFT"
        else -> if (buttons and (HidConstants.GAMEPAD_BTN_DPAD_UP or HidConstants.GAMEPAD_BTN_DPAD_DOWN or
                    HidConstants.GAMEPAD_BTN_DPAD_LEFT or HidConstants.GAMEPAD_BTN_DPAD_RIGHT) != 0) {
            "ACTIVE"
        } else {
            "CENTER"
        }
    }

    fun formatAxis(value: Float): String {
        return String.format(Locale.US, "%+0.2f", value)
    }

    fun formatTrigger(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }
}

/**
 * Singleton hub for real-time telemetry observation across UI and Diagnostics.
 */
object GamepadDiagnosticsHub {
    private val _telemetry = MutableStateFlow(GamepadTelemetry())
    val telemetry: StateFlow<GamepadTelemetry> = _telemetry.asStateFlow()

    private var previousButtons: Int = 0
    private var totalDown: Long = 0L
    private var totalUp: Long = 0L

    fun update(
        buttons: Int,
        hat: Byte,
        leftX: Short,
        leftY: Short,
        rightX: Short,
        rightY: Short,
        leftTrigger: Byte,
        rightTrigger: Byte
    ) {
        // Calculate bit transitions
        val newlyPressed = buttons and previousButtons.inv()
        val newlyReleased = previousButtons and buttons.inv()
        totalDown += Integer.bitCount(newlyPressed)
        totalUp += Integer.bitCount(newlyReleased)
        previousButtons = buttons

        _telemetry.value = GamepadTelemetry(
            buttons = buttons,
            hat = hat,
            leftX = leftX,
            leftY = leftY,
            rightX = rightX,
            rightY = rightY,
            leftTrigger = leftTrigger,
            rightTrigger = rightTrigger,
            downCount = totalDown,
            upCount = totalUp
        )
    }

    fun reset() {
        val newlyReleased = previousButtons
        totalUp += Integer.bitCount(newlyReleased)
        previousButtons = 0

        _telemetry.value = GamepadTelemetry(
            downCount = totalDown,
            upCount = totalUp
        )
    }

    fun resetCounters() {
        totalDown = 0L
        totalUp = 0L
        previousButtons = _telemetry.value.buttons
        _telemetry.value = _telemetry.value.copy(downCount = 0L, upCount = 0L)
    }
}
