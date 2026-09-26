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
        // In Android 9+ (API 28+), HID_DEVICE is standard
        isSupported = true
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
                    _connectionState.value = ConnectionState.Connecting(null)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnecting = _connectionState.value is ConnectionState.Connecting
                    if (_connectedDevice?.address == device.address) {
                        _connectedDevice = null
                    }
                    if (wasConnecting) {
                        val name = device.name ?: device.address
                        _connectionState.value = ConnectionState.Error("Không thể kết nối với $name. Hãy kiểm tra xem máy tính đã bật Bluetooth chưa.")
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
            // Output report: host sending LED indicators (Caps Lock, Num Lock)
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
            if (!success) {
                Log.w(TAG, "registerApp returned false (possibly already registered or firmware limitation)")
            }
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
            _connectionState.value = ConnectionState.Error("Bluetooth trên điện thoại đang tắt. Vui lòng bật Bluetooth.")
            return false
        }
        val hid = hidDevice
        if (hid == null || !isAppRegistered) {
            _connectionState.value = ConnectionState.Error("Hệ thống HID đang khởi tạo. Vui lòng thử lại sau vài giây.")
            return false
        }
        _connectionState.value = ConnectionState.Connecting(device)
        return try {
            val result = hid.connect(device)
            if (!result) {
                _connectionState.value = ConnectionState.Error("Yêu cầu kết nối tới ${device.name ?: device.address} bị từ chối.")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device ${device.address}", e)
            _connectionState.value = ConnectionState.Error("Lỗi kết nối: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect(): Boolean {
        val hid = hidDevice ?: return false
        val device = _connectedDevice ?: return false
        return try {
            hid.disconnect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendMouseMove(dx: Int, dy: Int, buttons: Byte, wheel: Int): Boolean {
        val hid = hidDevice ?: return false
        val device = _connectedDevice ?: return false
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
            hid.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), mouseReport)
        } catch (e: Exception) {
            Log.e(TAG, "sendMouseMove failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean {
        val hid = hidDevice ?: return false
        val device = _connectedDevice ?: return false
        if (!isAppRegistered) return false

        val keyboardReport = byteArrayOf(
            modifiers,
            0x00, // reserved
            keyCode,
            0x00, 0x00, 0x00, 0x00, 0x00
        )

        return try {
            hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyboardReport)
        } catch (e: Exception) {
            Log.e(TAG, "sendKeyPress failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendKeyRelease(): Boolean {
        val hid = hidDevice ?: return false
        val device = _connectedDevice ?: return false
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
