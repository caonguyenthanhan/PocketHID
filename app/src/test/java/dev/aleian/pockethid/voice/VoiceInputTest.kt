package dev.aleian.pockethid.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputTest {

    @Test
    fun testUnavailable() {
        val platform = FakeVoicePlatform(available = false)
        val controller = VoiceInputController(platform)
        
        assertEquals(VoiceState.UNAVAILABLE, controller.state.value)
        
        // Attempt to start
        controller.startListening()
        assertEquals(VoiceState.UNAVAILABLE, controller.state.value)
    }

    @Test
    fun testPermissionDenied() {
        val platform = FakeVoicePlatform(available = true, hasPermission = false)
        val controller = VoiceInputController(platform)
        
        assertEquals(VoiceState.IDLE, controller.state.value)
        
        controller.startListening()
        assertEquals(VoiceState.ERROR, controller.state.value)
    }

    @Test
    fun testStateTransitions() {
        val platform = FakeVoicePlatform(available = true, hasPermission = true)
        val controller = VoiceInputController(platform)
        
        assertEquals(VoiceState.IDLE, controller.state.value)
        
        controller.startListening()
        assertEquals(VoiceState.LISTENING, controller.state.value)
        assertTrue(platform.started)
        
        // End of speech
        controller.onEndOfSpeech()
        assertEquals(VoiceState.PROCESSING, controller.state.value)
        
        // Results
        controller.processResultText("hello world")
        assertEquals(VoiceState.RESULT, controller.state.value)
        assertEquals("hello world", controller.transcript.value)
    }

    @Test
    fun testEmptyResult() {
        val platform = FakeVoicePlatform(available = true, hasPermission = true)
        val controller = VoiceInputController(platform)
        
        controller.startListening()
        controller.onEndOfSpeech()
        
        // Empty text should yield error
        controller.processResultText("")
        assertEquals(VoiceState.ERROR, controller.state.value)
    }

    @Test
    fun testVietnameseUnicode() {
        val platform = FakeVoicePlatform(available = true, hasPermission = true)
        val controller = VoiceInputController(platform)
        
        controller.startListening("vi-VN")
        // We can't easily assert the Intent extra here without more mocking, but we can verify text
        controller.processResultText("Xin chào Việt Nam")
        assertEquals(VoiceState.RESULT, controller.state.value)
        assertEquals("Xin chào Việt Nam", controller.transcript.value)
    }

    @Test
    fun testCancel() {
        val platform = FakeVoicePlatform(available = true, hasPermission = true)
        val controller = VoiceInputController(platform)
        
        controller.startListening()
        assertEquals(VoiceState.LISTENING, controller.state.value)
        
        controller.cancel()
        assertEquals(VoiceState.IDLE, controller.state.value)
        assertTrue(platform.cancelled)
    }

    @Test
    fun testCleanup() {
        val platform = FakeVoicePlatform(available = true, hasPermission = true)
        val controller = VoiceInputController(platform)
        
        controller.startListening()
        controller.cleanup()
        assertEquals(VoiceState.IDLE, controller.state.value)
        assertTrue(platform.destroyed)
    }
}
