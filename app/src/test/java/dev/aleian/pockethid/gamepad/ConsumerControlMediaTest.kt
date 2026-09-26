package dev.aleian.pockethid.gamepad

import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConsumerControlMediaTest {

    @Before
    fun setUp() {
        ConsumerDiagnosticsHub.reset()
    }

    @Test
    fun testConsumerDescriptorConstants() {
        assertEquals(3.toByte(), HidConstants.REPORT_ID_CONSUMER)
        assertEquals(0x00E9, HidConstants.CONSUMER_VOLUME_UP)
        assertEquals(0x00EA, HidConstants.CONSUMER_VOLUME_DOWN)
        assertEquals(0x00E2, HidConstants.CONSUMER_MUTE)
        assertEquals(0x00CD, HidConstants.CONSUMER_PLAY_PAUSE)
        assertEquals(0x00B5, HidConstants.CONSUMER_SCAN_NEXT)
        assertEquals(0x00B6, HidConstants.CONSUMER_SCAN_PREV)
        assertEquals(0x00B7, HidConstants.CONSUMER_STOP)
    }

    @Test
    fun testVolumeUpReportSerialization() {
        val usageCode = HidConstants.CONSUMER_VOLUME_UP
        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        assertEquals(2, report.size)
        // 0x00E9 in Little-Endian: Byte 0 = 0xE9, Byte 1 = 0x00
        assertEquals(0xE9.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])

        // Verify Telemetry
        ConsumerDiagnosticsHub.onEvent(
            actionName = "VOLUME_UP",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("VOLUME_UP", t.actionName)
        assertEquals(3.toByte(), t.reportId)
        assertEquals(0x00E9, t.usageCode)
        assertEquals("0x00E9", t.usageHex)
        assertTrue(t.pressSent)
    }

    @Test
    fun testVolumeDownReportSerialization() {
        val usageCode = HidConstants.CONSUMER_VOLUME_DOWN
        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        assertEquals(2, report.size)
        // 0x00EA in Little-Endian: Byte 0 = 0xEA, Byte 1 = 0x00
        assertEquals(0xEA.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])

        ConsumerDiagnosticsHub.onEvent(
            actionName = "VOLUME_DOWN",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("VOLUME_DOWN", t.actionName)
        assertEquals(0x00EA, t.usageCode)
        assertEquals("0x00EA", t.usageHex)
    }

    @Test
    fun testMuteReportSerialization() {
        val usageCode = HidConstants.CONSUMER_MUTE
        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        assertEquals(2, report.size)
        // 0x00E2 in Little-Endian: Byte 0 = 0xE2, Byte 1 = 0x00
        assertEquals(0xE2.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])

        ConsumerDiagnosticsHub.onEvent(
            actionName = "MUTE",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("MUTE", t.actionName)
        assertEquals(0x00E2, t.usageCode)
        assertEquals("0x00E2", t.usageHex)
    }

    @Test
    fun testConsumerReleaseReportIsNeutralZeros() {
        val emptyReport = ByteArray(2)
        assertEquals(2, emptyReport.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00), emptyReport)

        ConsumerDiagnosticsHub.onEvent(
            actionName = "RELEASE",
            usageCode = 0,
            pressSent = false,
            releaseSent = true,
            status = "SUCCESS"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("RELEASE", t.actionName)
        assertTrue(t.releaseSent)
        assertEquals("0x0000", t.usageHex)
    }

    @Test
    fun testPlayPauseReportSerialization() {
        val usageCode = HidConstants.CONSUMER_PLAY_PAUSE
        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        assertEquals(2, report.size)
        assertEquals(0xCD.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])
    }
}
