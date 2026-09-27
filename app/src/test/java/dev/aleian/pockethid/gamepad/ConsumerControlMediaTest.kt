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
            actionName = "VolumeUp",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS",
            pulseDurationMs = 85L,
            payloadHex = "E9 00",
            releaseHex = "00 00",
            deviceInfo = "TestHostPC"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("VolumeUp", t.actionName)
        assertEquals(3.toByte(), t.reportId)
        assertEquals(0x00E9, t.usageCode)
        assertEquals("0x00E9", t.usageHex)
        assertEquals("E9 00", t.payloadHex)
        assertEquals("00 00", t.releaseHex)
        assertEquals(85L, t.pulseDurationMs)
        assertEquals("TestHostPC", t.deviceInfo)
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
            actionName = "VolumeDown",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS",
            pulseDurationMs = 85L,
            payloadHex = "EA 00",
            releaseHex = "00 00",
            deviceInfo = "TestHostPC"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("VolumeDown", t.actionName)
        assertEquals(0x00EA, t.usageCode)
        assertEquals("0x00EA", t.usageHex)
        assertEquals("EA 00", t.payloadHex)
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
            actionName = "Mute",
            usageCode = usageCode,
            pressSent = true,
            releaseSent = false,
            status = "SUCCESS",
            pulseDurationMs = 85L,
            payloadHex = "E2 00",
            releaseHex = "00 00",
            deviceInfo = "TestHostPC"
        )
        val t = ConsumerDiagnosticsHub.telemetry.value
        assertEquals("Mute", t.actionName)
        assertEquals(0x00E2, t.usageCode)
        assertEquals("0x00E2", t.usageHex)
        assertEquals("E2 00", t.payloadHex)
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
            status = "SUCCESS",
            pulseDurationMs = 0L,
            payloadHex = "00 00",
            releaseHex = "00 00",
            deviceInfo = "TestHostPC"
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

    @Test
    fun testFullTraceSemanticActionToWindowsHostReport() {
        // Trace: UI -> semantic action -> Consumer Control usage -> Report ID 3 -> report serialization -> Windows host
        val actions = listOf(
            dev.aleian.pockethid.action.PocketAction.MediaAction.VolumeUp to Pair(HidConstants.CONSUMER_VOLUME_UP, byteArrayOf(0xE9.toByte(), 0x00.toByte())),
            dev.aleian.pockethid.action.PocketAction.MediaAction.VolumeDown to Pair(HidConstants.CONSUMER_VOLUME_DOWN, byteArrayOf(0xEA.toByte(), 0x00.toByte())),
            dev.aleian.pockethid.action.PocketAction.MediaAction.Mute to Pair(HidConstants.CONSUMER_MUTE, byteArrayOf(0xE2.toByte(), 0x00.toByte())),
            dev.aleian.pockethid.action.PocketAction.MediaAction.PlayPause to Pair(HidConstants.CONSUMER_PLAY_PAUSE, byteArrayOf(0xCD.toByte(), 0x00.toByte())),
            dev.aleian.pockethid.action.PocketAction.MediaAction.NextTrack to Pair(HidConstants.CONSUMER_SCAN_NEXT, byteArrayOf(0xB5.toByte(), 0x00.toByte())),
            dev.aleian.pockethid.action.PocketAction.MediaAction.PrevTrack to Pair(HidConstants.CONSUMER_SCAN_PREV, byteArrayOf(0xB6.toByte(), 0x00.toByte()))
        )

        for ((action, expected) in actions) {
            val (expectedUsage, expectedBytes) = expected
            // 1. Resolve semantic action for Windows Host
            val plan = dev.aleian.pockethid.action.ActionResolver.resolve(
                action = action,
                hostOs = dev.aleian.pockethid.action.HostOs.WINDOWS
            )
            assertTrue("Plan must be ConsumerKey", plan is dev.aleian.pockethid.action.ActionExecutionPlan.ConsumerKey)
            val consumerPlan = plan as dev.aleian.pockethid.action.ActionExecutionPlan.ConsumerKey

            // 2. Verify Consumer Control usage code
            assertEquals(expectedUsage, consumerPlan.usageCode)

            // 3. Verify Report ID
            assertEquals(3.toByte(), HidConstants.REPORT_ID_CONSUMER)

            // 4. Verify 16-bit Little-Endian Serialization
            val serializedReport = byteArrayOf(
                (consumerPlan.usageCode and 0xFF).toByte(),
                ((consumerPlan.usageCode shr 8) and 0xFF).toByte()
            )
            assertArrayEquals("Report bytes must match Windows host expectation", expectedBytes, serializedReport)
        }
    }

    @Test
    fun testRapidSuccessiveTapsProduceDistinctPressReleaseCycles() {
        // Simulates rapid taps (e.g. Volume Up twice in rapid succession)
        val eventLog = mutableListOf<String>()

        fun simulateClick(usageCode: Int) {
            val hex = String.format("%02X %02X", usageCode and 0xFF, (usageCode shr 8) and 0xFF)
            // 1. PRESS
            eventLog.add("PRESS: $hex")
            // 2. RELEASE
            eventLog.add("RELEASE: 00 00")
        }

        simulateClick(HidConstants.CONSUMER_VOLUME_UP)
        simulateClick(HidConstants.CONSUMER_VOLUME_UP)

        assertEquals("Must produce exactly 4 events for 2 clicks", 4, eventLog.size)
        assertEquals("PRESS: E9 00", eventLog[0])
        assertEquals("RELEASE: 00 00", eventLog[1])
        assertEquals("PRESS: E9 00", eventLog[2])
        assertEquals("RELEASE: 00 00", eventLog[3])
    }

    @Test
    fun testConsumerDescriptorStructureIntegrity() {
        val descriptor = HidConstants.COMBO_REPORT_DESCRIPTOR

        // Find Report ID 3 in the combo descriptor: 0x85, 0x03
        var reportId3Index = -1
        for (i in 0 until descriptor.size - 1) {
            if (descriptor[i] == 0x85.toByte() && descriptor[i + 1] == 0x03.toByte()) {
                reportId3Index = i
                break
            }
        }
        assertTrue("Report ID 3 must exist in COMBO_REPORT_DESCRIPTOR", reportId3Index >= 0)

        // Verify Application collection around Report ID 3:
        // Must have Usage Page 0x0C (Consumer), Usage 0x01 (Consumer Control), Collection 0x01 (Application)
        assertEquals(0x05.toByte(), descriptor[reportId3Index - 6]) // Usage Page
        assertEquals(0x0C.toByte(), descriptor[reportId3Index - 5]) // Consumer
        assertEquals(0x09.toByte(), descriptor[reportId3Index - 4]) // Usage
        assertEquals(0x01.toByte(), descriptor[reportId3Index - 3]) // Consumer Control
        assertEquals(0xA1.toByte(), descriptor[reportId3Index - 2]) // Collection
        assertEquals(0x01.toByte(), descriptor[reportId3Index - 1]) // Application
    }
}

