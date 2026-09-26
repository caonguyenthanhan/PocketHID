package dev.aleian.pockethid.action

import android.bluetooth.BluetoothDevice
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDispatcherTest {

    private class FakeInputTransport : InputTransport {
        override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        override val isSupported: Boolean = true
        override val connectedDevice: BluetoothDevice? = null
        override val isConnected: Boolean = true

        var lastKeyClickCode: Byte? = null
        var lastKeyClickMods: Byte? = null
        var lastMouseClickButton: Byte? = null
        var lastConsumerCode: Int? = null

        override fun register() {}
        override fun unregister() {}
        override fun syncConnectionState() {}
        override fun connect(device: BluetoothDevice): Boolean = true
        override fun disconnect(): Boolean = true

        override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean = true

        override suspend fun sendMouseClick(buttons: Byte) {
            lastMouseClickButton = buttons
        }

        override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean = true
        override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte): Boolean = true
        override fun sendKeyRelease(): Boolean = true

        override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {
            lastKeyClickCode = keyCode
            lastKeyClickMods = modifiers
        }

        var lastGamepadButtons: Int? = null

        override fun sendConsumerClick(usageCode: Int): Boolean {
            lastConsumerCode = usageCode
            return true
        }

        override fun sendConsumerPress(usageCode: Int): Boolean {
            lastConsumerCode = usageCode
            return true
        }

        override fun sendConsumerRelease(): Boolean = true

        override fun sendGamepadReport(
            buttons: Int,
            hat: Byte,
            leftX: Short,
            leftY: Short,
            rightX: Short,
            rightY: Short,
            leftTrigger: Byte,
            rightTrigger: Byte
        ): Boolean {
            lastGamepadButtons = buttons
            return true
        }

        override fun sendGamepadNeutral(): Boolean {
            lastGamepadButtons = 0
            return true
        }
    }

    @Test
    fun testDispatchKeystrokeOnWindows() = runBlocking {
        val transport = FakeInputTransport()
        val success = ActionDispatcher.dispatch(PocketAction.EditAction.Copy, transport, HostOs.WINDOWS)
        assertTrue(success)
        assertEquals(HidConstants.KEY_C, transport.lastKeyClickCode)
        assertEquals(HidConstants.MOD_LEFT_CTRL, transport.lastKeyClickMods)
    }

    @Test
    fun testDispatchKeystrokeOnMacOs() = runBlocking {
        val transport = FakeInputTransport()
        val success = ActionDispatcher.dispatch(PocketAction.EditAction.Copy, transport, HostOs.MACOS)
        assertTrue(success)
        assertEquals(HidConstants.KEY_C, transport.lastKeyClickCode)
        assertEquals(HidConstants.MOD_LEFT_GUI, transport.lastKeyClickMods)
    }

    @Test
    fun testDispatchPointerAction() = runBlocking {
        val transport = FakeInputTransport()
        val success = ActionDispatcher.dispatch(PocketAction.PointerAction.MiddleClick, transport)
        assertTrue(success)
        assertEquals(HidConstants.MOUSE_BUTTON_MIDDLE, transport.lastMouseClickButton)
    }

    @Test
    fun testDispatchConsumerAction() = runBlocking {
        val transport = FakeInputTransport()
        val success = ActionDispatcher.dispatch(PocketAction.MediaAction.PlayPause, transport)
        assertTrue(success)
        assertEquals(HidConstants.CONSUMER_PLAY_PAUSE, transport.lastConsumerCode)
    }

    @Test
    fun testDispatchGamepadAction() = runBlocking {
        val transport = FakeInputTransport()
        val success = ActionDispatcher.dispatch(PocketAction.GamepadAction.ButtonA, transport)
        assertTrue(success)
        // After dispatch and neutral reset, neutral was sent
        assertEquals(0, transport.lastGamepadButtons)
    }
}
