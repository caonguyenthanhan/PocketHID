package dev.aleian.pockethid.model

import android.bluetooth.BluetoothDevice

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val device: BluetoothDevice? = null) : ConnectionState
    data class Connected(val device: BluetoothDevice) : ConnectionState
    data object Disconnecting : ConnectionState
    data class Error(val message: String) : ConnectionState

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting
    val isDisconnected: Boolean get() = this is Disconnected
    val isDisconnecting: Boolean get() = this is Disconnecting
}
