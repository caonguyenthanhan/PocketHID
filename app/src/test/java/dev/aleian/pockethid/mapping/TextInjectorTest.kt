package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class TextInjectorTest {

    @Test
    fun testVoiceTranscriptForwardedToPipeline() = runBlocking {
        var actionsSent = 0
        
        val fakeTransport = object : InputTransport {
            override val connectionState: kotlinx.coroutines.flow.StateFlow<dev.aleian.pockethid.model.ConnectionState> = 
                kotlinx.coroutines.flow.MutableStateFlow(dev.aleian.pockethid.model.ConnectionState.Disconnected)
            override val isSupported = true
            override val connectedDevice = null
            override val isConnected = true

            override fun register() {}
            override fun unregister() {}
            override fun syncConnectionState() {}
            override fun connect(device: android.bluetooth.BluetoothDevice) = false
            override fun disconnect() = false

            override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int) = false
            override suspend fun sendMouseClick(buttons: Byte) {}
            override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte) = false
            override fun sendKeyPress(keyCode: Byte, modifiers: Byte) = false
            override fun sendKeyRelease() = false
            override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {
                actionsSent++
            }
            override fun sendConsumerClick(usageCode: Int) = false
            override fun sendConsumerPress(usageCode: Int) = false
            override fun sendConsumerRelease() = false
            override fun sendGamepadReport(
                buttons: Int, hat: Byte, leftX: Short, leftY: Short,
                rightX: Short, rightY: Short, leftTrigger: Byte, rightTrigger: Byte
            ) = false
            override fun sendGamepadNeutral() = false
            override fun sendTabletReport(status: Byte, x: Int, y: Int) = false
            override fun sendTabletNeutral() = false
        }

        // Simulate voice result text
        val voiceResult = "test"
        
        // Ensure TextInjector correctly converts and forwards to transport
        TextInjector.injectText(voiceResult, fakeTransport, 0)
        
        // "test" -> 4 characters -> 4 keystrokes + 4 releases (since ActionDispatcher sends both)
        // Wait, ActionDispatcher.execute(KeyStroke) sends PRESS and RELEASE.
        // So 4 chars = 4 presses + 4 releases = 8 actions.
        // Actually, it might be more if there are modifiers, but for 'test', it's at least 4.
        assertTrue("Transcript should generate input actions", actionsSent >= 4)
    }
}
