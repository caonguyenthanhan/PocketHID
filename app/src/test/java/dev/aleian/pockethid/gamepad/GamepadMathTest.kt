package dev.aleian.pockethid.gamepad

import dev.aleian.pockethid.action.ActionExecutionPlan
import dev.aleian.pockethid.action.ActionResolver
import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.action.PocketAction
import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GamepadMathTest {

    @Test
    fun testDeadzoneSuppressesSmallMovement() {
        // Small deflection well within 15% deadzone (5% deflection on radius 100)
        val (x, y) = GamepadMath.processStickInput(
            dx = 5f,
            dy = 0f,
            maxRadius = 100f,
            deadzone = 0.15f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f
        )
        assertEquals(0.toShort(), x)
        assertEquals(0.toShort(), y)
    }

    @Test
    fun testFullDeflectionClampsProperly() {
        // Full right deflection
        val (rightX, rightY) = GamepadMath.processStickInput(
            dx = 100f,
            dy = 0f,
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f
        )
        assertEquals(32767.toShort(), rightX)
        assertEquals(0.toShort(), rightY)

        // Full left deflection
        val (leftX, leftY) = GamepadMath.processStickInput(
            dx = -100f,
            dy = 0f,
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f
        )
        assertEquals((-32767).toShort(), leftX)
        assertEquals(0.toShort(), leftY)
    }

    @Test
    fun testInvertYFlipsVerticalAxis() {
        val (_, normalY) = GamepadMath.processStickInput(
            dx = 0f,
            dy = -100f, // Upward movement
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f,
            invertY = false
        )

        val (_, invertedY) = GamepadMath.processStickInput(
            dx = 0f,
            dy = -100f, // Upward movement
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f,
            invertY = true
        )

        assertTrue(normalY < 0)
        assertTrue(invertedY > 0)
        assertEquals(abs(normalY.toInt()), abs(invertedY.toInt()))
    }

    @Test
    fun testResponseCurvesComparison() {
        val midDisplacement = 55f // half travel beyond 10% deadzone
        val (_, linearY) = GamepadMath.processStickInput(
            dx = 0f,
            dy = midDisplacement,
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.LINEAR,
            sensitivity = 1.0f
        )

        val (_, precisionY) = GamepadMath.processStickInput(
            dx = 0f,
            dy = midDisplacement,
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.PRECISION,
            sensitivity = 1.0f
        )

        val (_, aggressiveY) = GamepadMath.processStickInput(
            dx = 0f,
            dy = midDisplacement,
            maxRadius = 100f,
            deadzone = 0.10f,
            curve = StickResponseCurve.AGGRESSIVE,
            sensitivity = 1.0f
        )

        // Precision curve has lower output at mid deflection for finer control
        // Aggressive curve has higher output at mid deflection for instant twitch response
        assertTrue("Precision should be lower than linear", precisionY < linearY)
        assertTrue("Aggressive should be higher than linear", aggressiveY > linearY)
    }

    @Test
    fun testTriggerConversion() {
        assertEquals(0.toByte(), GamepadMath.processTriggerInput(0.0f))
        val halfByte = GamepadMath.processTriggerInput(0.5f).toInt() and 0xFF
        assertTrue("Half trigger should be near 128", halfByte in 126..130)
        val fullByte = GamepadMath.processTriggerInput(1.0f).toInt() and 0xFF
        assertEquals(255, fullByte)
    }

    @Test
    fun testHatSwitchCalculation() {
        assertEquals(HidConstants.GAMEPAD_HAT_CENTERED, HidConstants.calculateHatSwitch(up = false, down = false, left = false, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_UP, HidConstants.calculateHatSwitch(up = true, down = false, left = false, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_UP_RIGHT, HidConstants.calculateHatSwitch(up = true, down = false, left = false, right = true))
        assertEquals(HidConstants.GAMEPAD_HAT_RIGHT, HidConstants.calculateHatSwitch(up = false, down = false, left = false, right = true))
        assertEquals(HidConstants.GAMEPAD_HAT_DOWN_RIGHT, HidConstants.calculateHatSwitch(up = false, down = true, left = false, right = true))
        assertEquals(HidConstants.GAMEPAD_HAT_DOWN, HidConstants.calculateHatSwitch(up = false, down = true, left = false, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_DOWN_LEFT, HidConstants.calculateHatSwitch(up = false, down = true, left = true, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_LEFT, HidConstants.calculateHatSwitch(up = false, down = false, left = true, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_UP_LEFT, HidConstants.calculateHatSwitch(up = true, down = false, left = true, right = false))

        // Opposite directions cancel out
        assertEquals(HidConstants.GAMEPAD_HAT_CENTERED, HidConstants.calculateHatSwitch(up = true, down = true, left = false, right = false))
        assertEquals(HidConstants.GAMEPAD_HAT_CENTERED, HidConstants.calculateHatSwitch(up = false, down = false, left = true, right = true))
    }

    @Test
    fun testSemanticGamepadActionsResolveToExecutionPlan() {
        val planA = ActionResolver.resolve(PocketAction.GamepadAction.ButtonA, HostOs.WINDOWS)
        assertTrue(planA is ActionExecutionPlan.GamepadButtonClick)
        assertEquals(HidConstants.GAMEPAD_BTN_A, (planA as ActionExecutionPlan.GamepadButtonClick).buttonMask)

        val planStart = ActionResolver.resolve(PocketAction.GamepadAction.Start, HostOs.LINUX)
        assertTrue(planStart is ActionExecutionPlan.GamepadButtonClick)
        assertEquals(HidConstants.GAMEPAD_BTN_START, (planStart as ActionExecutionPlan.GamepadButtonClick).buttonMask)
    }
}
