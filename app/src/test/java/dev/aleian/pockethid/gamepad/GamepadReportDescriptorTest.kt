package dev.aleian.pockethid.gamepad

import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GamepadReportDescriptorTest {

    @Before
    fun setUp() {
        GamepadDiagnosticsHub.reset()
        GamepadDiagnosticsHub.resetCounters()
        ConsumerDiagnosticsHub.reset()
    }

    @Test
    fun testGamepadDescriptorConstants() {
        assertEquals(4.toByte(), HidConstants.REPORT_ID_GAMEPAD)
        assertEquals(13, HidConstants.GAMEPAD_REPORT_LENGTH)
        assertEquals(16, HidConstants.GAMEPAD_BUTTON_COUNT)
        assertEquals(6, HidConstants.GAMEPAD_AXES_COUNT)
    }

    @Test
    fun testGamepadReportSerializationExact13Bytes() {
        val buttons = HidConstants.GAMEPAD_BTN_A or HidConstants.GAMEPAD_BTN_X or HidConstants.GAMEPAD_BTN_RB
        val hat = HidConstants.GAMEPAD_HAT_UP_RIGHT // 2
        val leftX: Short = 16384 // 0x4000
        val leftY: Short = -16384 // 0xC000
        val rightX: Short = 32767 // 0x7FFF
        val rightY: Short = -32768 // 0x8000
        val leftTrigger: Byte = 128.toByte() // 0x80
        val rightTrigger: Byte = 255.toByte() // 0xFF

        val telemetry = GamepadTelemetry(
            buttons = buttons,
            hat = hat,
            leftX = leftX,
            leftY = leftY,
            rightX = rightX,
            rightY = rightY,
            leftTrigger = leftTrigger,
            rightTrigger = rightTrigger
        )

        val report = telemetry.rawBytes
        assertEquals(13, report.size)

        // Byte 0-1: 16-bit Button bitmask
        val expectedButtons = (HidConstants.GAMEPAD_BTN_A or HidConstants.GAMEPAD_BTN_X or HidConstants.GAMEPAD_BTN_RB)
        assertEquals((expectedButtons and 0xFF).toByte(), report[0])
        assertEquals(((expectedButtons shr 8) and 0xFF).toByte(), report[1])

        // Byte 2: Hat Switch (lower 4 bits, upper 4 bits zero padding)
        assertEquals(0x02.toByte(), report[2])

        // Byte 3-4: Left Stick X (16384 -> 0x00, 0x40)
        assertEquals(0x00.toByte(), report[3])
        assertEquals(0x40.toByte(), report[4])

        // Byte 5-6: Left Stick Y (-16384 -> 0x00, 0xC0)
        assertEquals(0x00.toByte(), report[5])
        assertEquals(0xC0.toByte(), report[6])

        // Byte 7-8: Right Stick X (32767 -> 0xFF, 0x7F)
        assertEquals(0xFF.toByte(), report[7])
        assertEquals(0x7F.toByte(), report[8])

        // Byte 9-10: Right Stick Y (-32768 -> 0x00, 0x80)
        assertEquals(0x00.toByte(), report[9])
        assertEquals(0x80.toByte(), report[10])

        // Byte 11-12: Triggers (128, 255)
        assertEquals(128.toByte(), report[11])
        assertEquals(255.toByte(), report[12])

        // Verify Hex formatting
        assertEquals("25 00 02 00 40 00 C0 FF 7F 00 80 80 FF", telemetry.rawReportHex)
    }

    @Test
    fun testNeutralReportState() {
        val neutral = GamepadTelemetry()
        assertTrue(neutral.isNeutral)
        assertEquals(13, neutral.rawBytes.size)
        assertArrayEquals(ByteArray(13), neutral.rawBytes)

        // Non-neutral button
        val withBtn = neutral.copy(buttons = HidConstants.GAMEPAD_BTN_A)
        assertFalse(withBtn.isNeutral)

        // Non-neutral hat
        val withHat = neutral.copy(hat = HidConstants.GAMEPAD_HAT_UP)
        assertFalse(withHat.isNeutral)

        // Non-neutral axis
        val withStick = neutral.copy(leftX = 1)
        assertFalse(withStick.isNeutral)

        // Non-neutral trigger
        val withTrigger = neutral.copy(leftTrigger = 1)
        assertFalse(withTrigger.isNeutral)
    }

    @Test
    fun testButtonLatchAndTransitions() {
        // Press A and B
        GamepadDiagnosticsHub.update(
            buttons = HidConstants.GAMEPAD_BTN_A or HidConstants.GAMEPAD_BTN_B,
            hat = 0, leftX = 0, leftY = 0, rightX = 0, rightY = 0, leftTrigger = 0, rightTrigger = 0
        )
        val s1 = GamepadDiagnosticsHub.telemetry.value
        assertEquals(2L, s1.downCount)
        assertEquals(0L, s1.upCount)
        assertTrue(s1.a)
        assertTrue(s1.b)
        assertFalse(s1.x)

        // Release A, keep B, press X
        GamepadDiagnosticsHub.update(
            buttons = HidConstants.GAMEPAD_BTN_B or HidConstants.GAMEPAD_BTN_X,
            hat = 0, leftX = 0, leftY = 0, rightX = 0, rightY = 0, leftTrigger = 0, rightTrigger = 0
        )
        val s2 = GamepadDiagnosticsHub.telemetry.value
        assertEquals(3L, s2.downCount) // A, B, and now X
        assertEquals(1L, s2.upCount)   // A released

        // Release all
        GamepadDiagnosticsHub.reset()
        val s3 = GamepadDiagnosticsHub.telemetry.value
        assertEquals(3L, s3.downCount)
        assertEquals(3L, s3.upCount)
        assertTrue(s3.isNeutral)
    }

    @Test
    fun testConsumerControlDiagnosticsHub() {
        ConsumerDiagnosticsHub.onEvent(
            actionName = "VOLUME_UP",
            usageCode = HidConstants.CONSUMER_VOLUME_UP,
            pressSent = true,
            releaseSent = true,
            status = "SUCCESS"
        )
        val telemetry = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("VOLUME_UP", telemetry.actionName)
        assertEquals(3.toByte(), telemetry.reportId)
        assertEquals(HidConstants.CONSUMER_VOLUME_UP, telemetry.usageCode)
        assertEquals("0x00E9", telemetry.usageHex)
        assertTrue(telemetry.pressSent)
        assertTrue(telemetry.releaseSent)
        assertEquals("SUCCESS", telemetry.transportStatus)
    }
}
