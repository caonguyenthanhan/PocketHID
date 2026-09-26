package dev.aleian.pockethid.transport

import android.bluetooth.BluetoothDevice
import dev.aleian.pockethid.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface InputTransport {
    val connectionState: StateFlow<ConnectionState>
    val isSupported: Boolean
    val connectedDevice: BluetoothDevice?

    fun register()
    fun unregister()
    fun connect(device: BluetoothDevice): Boolean
    fun disconnect(): Boolean

    fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean
    fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean
    fun sendKeyRelease(): Boolean
    suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte = 0)
}
