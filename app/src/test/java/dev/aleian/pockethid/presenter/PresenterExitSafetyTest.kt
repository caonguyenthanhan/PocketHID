package dev.aleian.pockethid.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenterExitSafetyTest {

    @Test
    fun `initial state is idle`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        assertFalse(safety.isHoldActive)
        assertEquals(0f, safety.computeProgress(1000L), 0.001f)
        assertFalse(safety.isHoldComplete(1000L))
    }

    @Test
    fun `short accidental tap does not trigger exit`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        val startTime = 10000L
        safety.onPointerDown(startTime)
        assertTrue(safety.isHoldActive)

        // User releases after 250ms
        val tapReleaseTime = startTime + 250L
        val progress = safety.computeProgress(tapReleaseTime)
        assertEquals(250f / 1500f, progress, 0.01f)
        assertFalse(safety.isHoldComplete(tapReleaseTime))

        val exitTriggered = safety.onPointerUp(tapReleaseTime)
        assertFalse("Accidental short tap must never trigger presenter exit", exitTriggered)
        assertFalse(safety.isHoldActive)
    }

    @Test
    fun `intermediate hold calculates correct progress`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        val startTime = 5000L
        safety.onPointerDown(startTime)

        // 750ms is exactly 50%
        val halfProgress = safety.computeProgress(startTime + 750L)
        assertEquals(0.5f, halfProgress, 0.001f)
        assertFalse(safety.isHoldComplete(startTime + 750L))
    }

    @Test
    fun `continuous 1500ms hold triggers exit`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        val startTime = 20000L
        safety.onPointerDown(startTime)

        val completedTime = startTime + 1500L
        assertEquals(1.0f, safety.computeProgress(completedTime), 0.001f)
        assertTrue(safety.isHoldComplete(completedTime))

        val exitTriggered = safety.onPointerUp(completedTime)
        assertTrue("Continuous 1500ms hold must confirm presenter exit", exitTriggered)
    }

    @Test
    fun `hold beyond 1500ms clamps progress to 1`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        val startTime = 30000L
        safety.onPointerDown(startTime)

        val prolongedTime = startTime + 3000L
        assertEquals(1.0f, safety.computeProgress(prolongedTime), 0.001f)
        assertTrue(safety.isHoldComplete(prolongedTime))
    }

    @Test
    fun `reset clears all hold state`() {
        val safety = PresenterExitSafety(requiredHoldMs = 1500L)
        safety.onPointerDown(1000L)
        safety.reset()

        assertFalse(safety.isHoldActive)
        assertEquals(0f, safety.computeProgress(2000L), 0.001f)
        assertFalse(safety.isHoldComplete(2000L))
    }
}
