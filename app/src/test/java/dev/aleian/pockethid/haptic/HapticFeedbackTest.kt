package dev.aleian.pockethid.haptic

import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HapticFeedbackTest {

    private lateinit var fakeVibrator: FakeVibrator
    private lateinit var controller: IHapticFeedbackController

    @Before
    fun setup() {
        fakeVibrator = FakeVibrator()
        controller = FakeHapticFeedbackController(fakeVibrator)
    }

    private fun setHapticsEnabled(enabled: Boolean) {
        SettingsRepository.updateSettings(
            SettingsRepository.settings.value.copy(hapticsEnabled = enabled)
        )
    }

    @Test
    fun testHapticsEnabledProducesPlaybackRequest() {
        setHapticsEnabled(true)
        controller.play(HapticEvent.ACTION_ACCEPTED)
        
        assertEquals(1, fakeVibrator.vibrateCount)
        assertEquals(0, fakeVibrator.lastEffectId) // EFFECT_CLICK
    }

    @Test
    fun testHapticsDisabledSuppressesPlayback() {
        setHapticsEnabled(false)
        controller.play(HapticEvent.ACTION_ACCEPTED)
        
        assertEquals(0, fakeVibrator.vibrateCount)
    }

    @Test
    fun testEachEventMapsCorrectly() {
        setHapticsEnabled(true)
        
        controller.play(HapticEvent.MODE_CHANGED)
        assertEquals(2, fakeVibrator.lastEffectId) // EFFECT_TICK
        
        controller.play(HapticEvent.CONNECTED)
        assertEquals(1, fakeVibrator.lastEffectId) // EFFECT_DOUBLE_CLICK
        
        controller.play(HapticEvent.DISCONNECTED)
        assertEquals(5, fakeVibrator.lastEffectId) // EFFECT_HEAVY_CLICK
        
        controller.play(HapticEvent.ERROR)
        assertEquals(1, fakeVibrator.lastEffectId) // EFFECT_DOUBLE_CLICK
    }

    @Test
    fun testRepeatedEventsDoNotCrash() {
        setHapticsEnabled(true)
        
        for (i in 0 until 100) {
            controller.play(HapticEvent.ACTION_ACCEPTED)
        }
        
        assertEquals(100, fakeVibrator.vibrateCount)
    }

    @Test
    fun testDisposalPreventsFurtherRequests() {
        setHapticsEnabled(true)
        controller.play(HapticEvent.ACTION_ACCEPTED)
        assertEquals(1, fakeVibrator.vibrateCount)
        
        controller.release()
        
        controller.play(HapticEvent.ACTION_ACCEPTED)
        assertEquals(1, fakeVibrator.vibrateCount)
    }

    @Test
    fun testDispatchWithPlayFeedbackFalseDoesNotPlayHaptic() = kotlinx.coroutines.runBlocking {
        setHapticsEnabled(true)
        // Inject fake controller into manager
        HapticFeedbackManager.setController(controller)
        
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

        // Discrete action with playFeedback = true should play haptic
        dev.aleian.pockethid.action.ActionDispatcher.dispatch(
            dev.aleian.pockethid.action.PocketAction.EditAction.Copy, 
            dummyTransport, 
            dev.aleian.pockethid.action.HostOs.WINDOWS, 
            playFeedback = true
        )
        assertEquals(1, fakeVibrator.vibrateCount)

        // Continuous action with playFeedback = false should NOT play haptic
        dev.aleian.pockethid.action.ActionDispatcher.dispatch(
            dev.aleian.pockethid.action.PocketAction.VideoAction.VolumeUp, 
            dummyTransport, 
            dev.aleian.pockethid.action.HostOs.WINDOWS, 
            playFeedback = false
        )
        assertEquals(1, fakeVibrator.vibrateCount) // Count remains 1
    }
}

class FakeVibrator {
    var vibrateCount = 0
    var lastEffectId = -1
    
    fun vibrate(effectType: Int) {
        vibrateCount++
        lastEffectId = effectType
    }
}

class FakeHapticFeedbackController(private var fakeVibrator: FakeVibrator?) : IHapticFeedbackController {
    override fun play(event: HapticEvent) {
        val settings = SettingsRepository.settings.value
        if (!settings.hapticsEnabled) return

        val effectId = when (event) {
            HapticEvent.ACTION_ACCEPTED -> 0 // EFFECT_CLICK
            HapticEvent.MODE_CHANGED -> 2 // EFFECT_TICK
            HapticEvent.CONNECTED -> 1 // EFFECT_DOUBLE_CLICK
            HapticEvent.DISCONNECTED -> 5 // EFFECT_HEAVY_CLICK
            HapticEvent.ERROR -> 1 // EFFECT_DOUBLE_CLICK
        }
        fakeVibrator?.vibrate(effectId)
    }

    override fun release() {
        fakeVibrator = null
    }
}
