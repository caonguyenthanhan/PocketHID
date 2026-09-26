package dev.aleian.pockethid.gamepad

import android.view.HapticFeedbackConstants
import android.view.View
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport

/**
 * Coordinates Gamepad input states and dispatches low-latency HID Gamepad reports (Report ID 4).
 * Ensures thread-safe state synchronization and prevents stuck controls.
 */
class GamepadController(
    private val transport: InputTransport?
) {
    private val lock = Any()

    @Volatile private var buttons: Int = 0
    @Volatile private var hat: Byte = HidConstants.GAMEPAD_HAT_CENTERED

    @Volatile private var dpadUp: Boolean = false
    @Volatile private var dpadDown: Boolean = false
    @Volatile private var dpadLeft: Boolean = false
    @Volatile private var dpadRight: Boolean = false

    @Volatile private var leftX: Short = 0
    @Volatile private var leftY: Short = 0
    @Volatile private var rightX: Short = 0
    @Volatile private var rightY: Short = 0

    @Volatile private var leftTrigger: Byte = 0
    @Volatile private var rightTrigger: Byte = 0

    fun triggerHaptic(view: View?) {
        val hapticEnabled = SettingsRepository.settings.value.keyboardHaptics
        if (hapticEnabled && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun pressButton(buttonMask: Int, view: View? = null) {
        triggerHaptic(view)
        synchronized(lock) {
            buttons = buttons or buttonMask
        }
        dispatchReport()
    }

    fun releaseButton(buttonMask: Int) {
        synchronized(lock) {
            buttons = buttons and buttonMask.inv()
        }
        dispatchReport()
    }

    fun setDpadDirection(up: Boolean, down: Boolean, left: Boolean, right: Boolean, view: View? = null) {
        var changed = false
        synchronized(lock) {
            if (dpadUp != up || dpadDown != down || dpadLeft != left || dpadRight != right) {
                dpadUp = up
                dpadDown = down
                dpadLeft = left
                dpadRight = right
                hat = HidConstants.calculateHatSwitch(up, down, left, right)

                // Also sync D-pad button bits for dual compatibility
                var b = buttons
                b = if (up) b or HidConstants.GAMEPAD_BTN_DPAD_UP else b and HidConstants.GAMEPAD_BTN_DPAD_UP.inv()
                b = if (down) b or HidConstants.GAMEPAD_BTN_DPAD_DOWN else b and HidConstants.GAMEPAD_BTN_DPAD_DOWN.inv()
                b = if (left) b or HidConstants.GAMEPAD_BTN_DPAD_LEFT else b and HidConstants.GAMEPAD_BTN_DPAD_LEFT.inv()
                b = if (right) b or HidConstants.GAMEPAD_BTN_DPAD_RIGHT else b and HidConstants.GAMEPAD_BTN_DPAD_RIGHT.inv()
                buttons = b
                changed = true
            }
        }
        if (changed) {
            if (up || down || left || right) {
                triggerHaptic(view)
            }
            dispatchReport()
        }
    }

    fun updateLeftStick(x: Short, y: Short) {
        synchronized(lock) {
            if (leftX == x && leftY == y) return
            leftX = x
            leftY = y
        }
        dispatchReport()
    }

    fun updateRightStick(x: Short, y: Short) {
        synchronized(lock) {
            if (rightX == x && rightY == y) return
            rightX = x
            rightY = y
        }
        dispatchReport()
    }

    fun updateLeftTrigger(value: Byte) {
        synchronized(lock) {
            if (leftTrigger == value) return
            leftTrigger = value
        }
        dispatchReport()
    }

    fun updateRightTrigger(value: Byte) {
        synchronized(lock) {
            if (rightTrigger == value) return
            rightTrigger = value
        }
        dispatchReport()
    }

    fun resetNeutral() {
        synchronized(lock) {
            buttons = 0
            hat = HidConstants.GAMEPAD_HAT_CENTERED
            dpadUp = false
            dpadDown = false
            dpadLeft = false
            dpadRight = false
            leftX = 0
            leftY = 0
            rightX = 0
            rightY = 0
            leftTrigger = 0
            rightTrigger = 0
        }
        GamepadDiagnosticsHub.reset()
        transport?.sendGamepadNeutral()
    }

    private fun dispatchReport() {
        val b: Int
        val h: Byte
        val lx: Short
        val ly: Short
        val rx: Short
        val ry: Short
        val lt: Byte
        val rt: Byte

        synchronized(lock) {
            b = buttons
            h = hat
            lx = leftX
            ly = leftY
            rx = rightX
            ry = rightY
            lt = leftTrigger
            rt = rightTrigger
        }

        GamepadDiagnosticsHub.update(b, h, lx, ly, rx, ry, lt, rt)
        transport?.sendGamepadReport(b, h, lx, ly, rx, ry, lt, rt)
    }
}
