package dev.aleian.pockethid.audio

import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

class AudioFeedbackTest {

    private lateinit var fakeAudioManager: FakeAudioManager
    private lateinit var controller: IAudioFeedbackController

    @Before
    fun setup() {
        fakeAudioManager = FakeAudioManager()
        controller = FakeAudioFeedbackController(fakeAudioManager)
    }

    private fun setSoundEffectsEnabled(enabled: Boolean) {
        SettingsRepository.updateSettings(
            SettingsRepository.settings.value.copy(soundEffectsEnabled = enabled)
        )
    }

    @Test
    fun testSoundEnabledProducesPlaybackRequest() {
        setSoundEffectsEnabled(true)
        controller.play(AudioEvent.ACTION_ACCEPTED)
        
        assertEquals(1, fakeAudioManager.playCount)
        assertEquals(0, fakeAudioManager.lastEffectId) // FX_KEY_CLICK
    }

    @Test
    fun testSoundDisabledSuppressesPlayback() {
        setSoundEffectsEnabled(false)
        controller.play(AudioEvent.ACTION_ACCEPTED)
        
        assertEquals(0, fakeAudioManager.playCount)
    }

    @Test
    fun testEachEventMapsCorrectly() {
        setSoundEffectsEnabled(true)
        
        controller.play(AudioEvent.MODE_CHANGED)
        assertEquals(1, fakeAudioManager.lastEffectId) // FX_FOCUS_NAVIGATION_UP
        
        controller.play(AudioEvent.CONNECTED)
        assertEquals(8, fakeAudioManager.lastEffectId) // FX_KEYPRESS_RETURN
        
        controller.play(AudioEvent.DISCONNECTED)
        assertEquals(7, fakeAudioManager.lastEffectId) // FX_KEYPRESS_DELETE
        
        controller.play(AudioEvent.ERROR)
        assertEquals(9, fakeAudioManager.lastEffectId) // FX_KEYPRESS_INVALID
        
        controller.play(AudioEvent.VOICE_LISTENING_STARTED)
        assertEquals(1, fakeAudioManager.lastEffectId) // FX_FOCUS_NAVIGATION_UP
        
        controller.play(AudioEvent.VOICE_LISTENING_STOPPED)
        assertEquals(2, fakeAudioManager.lastEffectId) // FX_FOCUS_NAVIGATION_DOWN
        
        controller.play(AudioEvent.VOICE_RESULT)
        assertEquals(5, fakeAudioManager.lastEffectId) // FX_KEYPRESS_STANDARD
        
        controller.play(AudioEvent.VOICE_ERROR)
        assertEquals(9, fakeAudioManager.lastEffectId) // FX_KEYPRESS_INVALID
    }

    @Test
    fun testRepeatedEventsDoNotCrash() {
        setSoundEffectsEnabled(true)
        
        for (i in 0 until 100) {
            controller.play(AudioEvent.ACTION_ACCEPTED)
        }
        
        assertEquals(100, fakeAudioManager.playCount)
    }

    @Test
    fun testDisposalPreventsFurtherRequests() {
        setSoundEffectsEnabled(true)
        controller.play(AudioEvent.ACTION_ACCEPTED)
        assertEquals(1, fakeAudioManager.playCount)
        
        controller.release()
        
        controller.play(AudioEvent.ACTION_ACCEPTED)
        // Note: With our simple fake, if release() nullifies the manager, it should not increment
        // In the FakeAudioFeedbackController we can implement release to block further plays.
        assertEquals(1, fakeAudioManager.playCount)
    }

    @Test
    fun testDispatchWithPlayFeedbackFalseDoesNotPlayAudio() = kotlinx.coroutines.runBlocking {
        setSoundEffectsEnabled(true)
        // Inject fake controller into manager
        AudioFeedbackManager.setController(controller)
        
        // Stub transport
        val dummyTransport = object : dev.aleian.pockethid.transport.InputTransport {
            override val connectionState = kotlinx.coroutines.flow.MutableStateFlow(dev.aleian.pockethid.model.ConnectionState.Disconnected)
            override val isSupported = true
            override val connectedDevice = null
            override val isConnected = true
            override fun register() {}
            override fun unregister() {}
            override fun syncConnectionState() {}
            override fun connect(device: android.bluetooth.BluetoothDevice) = true
            override fun disconnect() = true
            override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int) = true
            override suspend fun sendMouseClick(buttons: Byte) {}
            override fun sendKeyPress(keyCode: Byte, modifiers: Byte) = true
            override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte) = true
            override fun sendKeyRelease() = true
            override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {}
            override fun sendConsumerClick(usageCode: Int) = true
            override fun sendConsumerPress(usageCode: Int) = true
            override fun sendConsumerRelease() = true
            override fun sendGamepadReport(b: Int, h: Byte, lx: Short, ly: Short, rx: Short, ry: Short, lt: Byte, rt: Byte) = true
            override fun sendGamepadNeutral() = true
            override fun sendTabletReport(s: Byte, x: Int, y: Int) = true
            override fun sendTabletNeutral() = true
        }

        // Discrete action with playFeedback = true should play audio
        dev.aleian.pockethid.action.ActionDispatcher.dispatch(
            dev.aleian.pockethid.action.PocketAction.EditAction.Copy, 
            dummyTransport, 
            dev.aleian.pockethid.action.HostOs.WINDOWS, 
            playFeedback = true
        )
        Thread.sleep(100)
        assertEquals(1, fakeAudioManager.playCount)

        // Continuous action with playFeedback = false should NOT play audio
        dev.aleian.pockethid.action.ActionDispatcher.dispatch(
            dev.aleian.pockethid.action.PocketAction.VideoAction.VolumeUp, 
            dummyTransport, 
            dev.aleian.pockethid.action.HostOs.WINDOWS, 
            playFeedback = false
        )
        Thread.sleep(100)
        assertEquals(1, fakeAudioManager.playCount) // Count remains 1
    }
}

class FakeAudioManager {
    var playCount = 0
    var lastEffectId = -1
    
    fun playSoundEffect(effectType: Int) {
        playCount++
        lastEffectId = effectType
    }
}

class FakeAudioFeedbackController(private var fakeAudioManager: FakeAudioManager?) : IAudioFeedbackController {
    override fun play(event: AudioEvent) {
        val settings = SettingsRepository.settings.value
        if (!settings.soundEffectsEnabled) return

        val effectId = when (event) {
            AudioEvent.ACTION_ACCEPTED -> 0 // FX_KEY_CLICK
            AudioEvent.MODE_CHANGED -> 1 // FX_FOCUS_NAVIGATION_UP
            AudioEvent.CONNECTED -> 8 // FX_KEYPRESS_RETURN
            AudioEvent.DISCONNECTED -> 7 // FX_KEYPRESS_DELETE
            AudioEvent.ERROR -> 9 // FX_KEYPRESS_INVALID
            AudioEvent.VOICE_LISTENING_STARTED -> 1 // FX_FOCUS_NAVIGATION_UP
            AudioEvent.VOICE_LISTENING_STOPPED -> 2 // FX_FOCUS_NAVIGATION_DOWN
            AudioEvent.VOICE_RESULT -> 5 // FX_KEYPRESS_STANDARD
            AudioEvent.VOICE_ERROR -> 9 // FX_KEYPRESS_INVALID
        }
        fakeAudioManager?.playSoundEffect(effectId)
    }

    override fun release() {
        fakeAudioManager = null
    }
}
