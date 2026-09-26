package dev.aleian.pockethid.model

import android.bluetooth.BluetoothDevice

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val device: BluetoothDevice? = null) : ConnectionState
    data class Connected(val device: BluetoothDevice) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
