package dev.aleian.pockethid.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class BtHidTransport(
    private val context: Context
) : InputTransport {

    companion object {
        private const val TAG = "BtHidTransport"
        private const val KEY_PRESS_DELAY_MS = 12L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var isAppRegistered = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var _connectedDevice: BluetoothDevice? = null
    override val connectedDevice: BluetoothDevice?
        get() = _connectedDevice

    override val isConnected: Boolean
        get() {
            if (_connectionState.value is ConnectionState.Connected) return true
            if (_connectedDevice != null) return true
            return queryActualConnectedDevice() != null
        }

    override var isSupported: Boolean = false
        private set

    init {
        checkSupport()
    }

    private fun checkSupport() {
        if (bluetoothAdapter == null) {
            isSupported = false
            _connectionState.value = ConnectionState.Error("Bluetooth not available on this device")
            return
        }
        isSupported = true
    }

    @SuppressLint("MissingPermission")
    private fun queryActualConnectedDevice(): BluetoothDevice? {
        val hid = hidDevice ?: return null
        return try {
            val connectedDevices = hid.getDevicesMatchingConnectionStates(
                intArrayOf(BluetoothProfile.STATE_CONNECTED)
            )
            if (!connectedDevices.isNullOrEmpty()) {
                connectedDevices.first()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying actual connected devices", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun syncConnectionState() {
        val hid = hidDevice ?: return
        try {
            val activeDevice = queryActualConnectedDevice()
            if (activeDevice != null) {
                _connectedDevice = activeDevice
                if (_connectionState.value !is ConnectionState.Connected ||
                    (_connectionState.value as ConnectionState.Connected).device.address != activeDevice.address) {
                    Log.d(TAG, "syncConnectionState: Active connected host: ${activeDevice.name ?: activeDevice.address}")
                    _connectionState.value = ConnectionState.Connected(activeDevice)
                }
                return
            }

            val connectingDevices = hid.getDevicesMatchingConnectionStates(
                intArrayOf(BluetoothProfile.STATE_CONNECTING)
            )
            if (!connectingDevices.isNullOrEmpty()) {
                val connectingDevice = connectingDevices.first()
                if (_connectionState.value !is ConnectionState.Connecting) {
                    _connectionState.value = ConnectionState.Connecting(connectingDevice)
                }
                return
            }

            // No host active
            if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
                _connectedDevice = null
                _connectionState.value = ConnectionState.Disconnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during syncConnectionState", e)
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID Device Proxy Connected")
                hidDevice = proxy as? BluetoothHidDevice
                if (hidDevice == null) {
                    isSupported = false
                    _connectionState.value = ConnectionState.Error("Device firmware lacks Bluetooth HID Device role")
                    return
                }
                registerAppInternal()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID Device Proxy Disconnected")
                hidDevice = null
                isAppRegistered = false
                _connectedDevice = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered, device=${pluggedDevice?.name}")
            isAppRegistered = registered
            if (!registered) {
                _connectedDevice = null
                _connectionState.value = ConnectionState.Disconnected
            } else if (pluggedDevice != null) {
                _connectedDevice = pluggedDevice
                _connectionState.value = ConnectionState.Connected(pluggedDevice)
            } else {
                // Check if a host was already connected before registration finished
                syncConnectionState()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: device=${device.address}, state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice = device
                    _connectionState.value = ConnectionState.Connected(device)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.Connecting(device)
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.value = ConnectionState.Disconnecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnecting = _connectionState.value is ConnectionState.Connecting
                    if (_connectedDevice?.address == device.address) {
                        _connectedDevice = null
                    }
                    if (wasConnecting) {
                        val name = device.name ?: device.address
                        _connectionState.value = ConnectionState.Error("Failed to connect to $name")
                    } else {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id")
            if (id == HidConstants.REPORT_ID_KEYBOARD) {
                hidDevice?.replyReport(device, type, id, ByteArray(8))
            } else if (id == HidConstants.REPORT_ID_MOUSE) {
                hidDevice?.replyReport(device, type, id, ByteArray(4))
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            Log.d(TAG, "onSetReport: id=$id, data=${data.joinToString { it.toString() }}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun register() {
        if (bluetoothAdapter == null) return
        try {
            bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining HID_DEVICE proxy", e)
            isSupported = false
            _connectionState.value = ConnectionState.Error("HID profile not supported: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerAppInternal() {
        val hid = hidDevice ?: return
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "PocketHID",
                "PocketHID Keyboard & Mouse",
                "Aleian",
                0xC0.toByte(), // Combo (Keyboard + Mouse)
                HidConstants.COMBO_REPORT_DESCRIPTOR
            )
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                9,
                0,
                11250,
                BluetoothHidDeviceAppQosSettings.MAX
            )
            val success = hid.registerApp(sdp, null, qos, executor, hidCallback)
            Log.d(TAG, "registerApp result: $success")
            syncConnectionState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to registerApp", e)
            _connectionState.value = ConnectionState.Error("Register HID app failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun unregister() {
        try {
            if (isAppRegistered) {
                hidDevice?.unregisterApp()
                isAppRegistered = false
            }
            if (hidDevice != null) {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
                hidDevice = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering HID", e)
        }
        _connectedDevice = null
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    override fun connect(device: BluetoothDevice): Boolean {
        if (bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled. Please enable Bluetooth.")
            return false
        }
        val hid = hidDevice
        if (hid == null || !isAppRegistered) {
            _connectionState.value = ConnectionState.Error("HID subsystem is initializing. Please retry in a moment.")
            return false
        }

        // If the device is ALREADY connected in the HID stack, mark it immediately
        val activeDev = queryActualConnectedDevice()
        if (activeDev?.address == device.address) {
            _connectedDevice = activeDev
            _connectionState.value = ConnectionState.Connected(activeDev)
            return true
        }

        _connectionState.value = ConnectionState.Connecting(device)
        return try {
            val result = hid.connect(device)
            if (!result) {
                // If connect returned false, verify if it was already connected
                val recheck = queryActualConnectedDevice()
                if (recheck?.address == device.address) {
                    _connectedDevice = recheck
                    _connectionState.value = ConnectionState.Connected(recheck)
                    return true
                }
                _connectionState.value = ConnectionState.Error("Connection request to ${device.name ?: device.address} was rejected.")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device ${device.address}", e)
            _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect(): Boolean {
        val hid = hidDevice ?: return false
        val device = _connectedDevice ?: queryActualConnectedDevice() ?: return false
        _connectionState.value = ConnectionState.Disconnecting
        return try {
            hid.disconnect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
            false
        }
    }

    private fun resolveActiveDevice(): BluetoothDevice? {
        if (_connectedDevice != null) return _connectedDevice
        val dev = queryActualConnectedDevice()
        if (dev != null) {
            _connectedDevice = dev
            if (_connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(dev)
            }
        }
        return dev
    }

    @SuppressLint("MissingPermission")
    override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()

        val mouseReport = byteArrayOf(
            buttons,
            clampedDx,
            clampedDy,
            clampedWheel
        )

        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), mouseReport)
            if (sent && _connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(device)
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendMouseMove failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val keyboardReport = byteArrayOf(
            modifiers,
            0x00, // reserved
            keyCode,
            0x00, 0x00, 0x00, 0x00, 0x00
        )

        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyboardReport)
            if (sent && _connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(device)
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendKeyPress failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendKeyRelease(): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val emptyReport = ByteArray(8)
        return try {
            hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), emptyReport)
        } catch (e: Exception) {
            Log.e(TAG, "sendKeyRelease failed", e)
            false
        }
    }

    override suspend fun sendKeyClick(keyCode: Byte, modifiers: Byte) {
        sendKeyPress(keyCode, modifiers)
        delay(KEY_PRESS_DELAY_MS)
        sendKeyRelease()
        delay(KEY_PRESS_DELAY_MS)
    }
}
