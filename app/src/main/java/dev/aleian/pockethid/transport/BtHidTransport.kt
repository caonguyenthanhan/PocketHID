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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

class BtHidTransport(
    private val context: Context
) : InputTransport {

    companion object {
        private const val TAG = "BtHidTransport"
        private const val KEY_PRESS_DELAY_MS = 12L
        private const val CONSUMER_PRESS_DELAY_MS = 85L
        private const val CONSUMER_RELEASE_SETTLE_MS = 20L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()
    private val consumerMutex = kotlinx.coroutines.sync.Mutex()

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
            } else if (id == HidConstants.REPORT_ID_CONSUMER) {
                hidDevice?.replyReport(device, type, id, ByteArray(2))
            } else if (id == HidConstants.REPORT_ID_GAMEPAD) {
                hidDevice?.replyReport(device, type, id, ByteArray(13))
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
                "PocketHID Composite Controller",
                "Aleian",
                (BluetoothHidDevice.SUBCLASS1_COMBO.toInt() or BluetoothHidDevice.SUBCLASS2_GAMEPAD.toInt()).toByte(),
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

    override suspend fun sendMouseClick(buttons: Byte) {
        sendMouseMove(0, 0, buttons, 0)
        delay(16)
        sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
        delay(16)
    }

    @SuppressLint("MissingPermission")
    override fun sendKeyReport(keyCodes: ByteArray, modifiers: Byte): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val keyboardReport = ByteArray(8)
        keyboardReport[0] = modifiers
        keyboardReport[1] = 0x00 // reserved

        val count = minOf(keyCodes.size, 6)
        for (i in 0 until count) {
            keyboardReport[2 + i] = keyCodes[i]
        }

        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), keyboardReport)
            if (sent && _connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(device)
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendKeyReport failed", e)
            false
        }
    }

    override fun sendKeyPress(keyCode: Byte, modifiers: Byte): Boolean {
        return sendKeyReport(byteArrayOf(keyCode), modifiers)
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

    private fun formatConsumerPayload(usageCode: Int): String {
        return String.format("%02X %02X", usageCode and 0xFF, (usageCode shr 8) and 0xFF)
    }

    @SuppressLint("MissingPermission")
    override fun sendConsumerClick(usageCode: Int): Boolean {
        val hid = hidDevice ?: run {
            dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                actionName = dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.resolveActionName(usageCode),
                usageCode = usageCode,
                pressSent = false,
                releaseSent = false,
                status = "FAILED (no hidDevice)",
                pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                payloadHex = formatConsumerPayload(usageCode),
                releaseHex = "00 00",
                deviceInfo = "None"
            )
            return false
        }
        val device = resolveActiveDevice() ?: run {
            dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                actionName = dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.resolveActionName(usageCode),
                usageCode = usageCode,
                pressSent = false,
                releaseSent = false,
                status = "FAILED (no active device)",
                pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                payloadHex = formatConsumerPayload(usageCode),
                releaseHex = "00 00",
                deviceInfo = "None"
            )
            return false
        }
        if (!isAppRegistered) {
            dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                actionName = dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.resolveActionName(usageCode),
                usageCode = usageCode,
                pressSent = false,
                releaseSent = false,
                status = "FAILED (not registered)",
                pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                payloadHex = formatConsumerPayload(usageCode),
                releaseHex = "00 00",
                deviceInfo = device.name ?: device.address
            )
            return false
        }

        val actionName = dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.resolveActionName(usageCode)
        val payloadHex = formatConsumerPayload(usageCode)
        val deviceDesc = "${device.name ?: "Bluetooth Device"} (${device.address})"

        Log.d(TAG, """
            |--- CONSUMER CONTROL DISPATCH ---
            |ACTION:    $actionName
            |USAGE:     0x${usageCode.toString(16).padStart(4, '0').uppercase()}
            |REPORT ID: 3
            |PAYLOAD:   $payloadHex
            |RELEASE:   00 00
            |TIMING:    ${CONSUMER_PRESS_DELAY_MS} ms
            |DEVICE:    $deviceDesc
            |---------------------------------
        """.trimMargin())

        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        val emptyReport = ByteArray(2)

        scope.launch {
            consumerMutex.withLock {
                try {
                    // Step 1: Send PRESS
                    val pressSent = hid.sendReport(device, HidConstants.REPORT_ID_CONSUMER.toInt(), report)
                    if (pressSent && _connectionState.value !is ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Connected(device)
                    }

                    dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                        actionName = actionName,
                        usageCode = usageCode,
                        pressSent = pressSent,
                        releaseSent = false,
                        status = if (pressSent) "PRESS_SENT" else "PRESS_FAILED",
                        pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                        payloadHex = payloadHex,
                        releaseHex = "00 00",
                        deviceInfo = deviceDesc
                    )

                    // Step 2: Hold for pulse duration
                    delay(CONSUMER_PRESS_DELAY_MS)

                    // Step 3: Send RELEASE
                    val releaseSent = hid.sendReport(device, HidConstants.REPORT_ID_CONSUMER.toInt(), emptyReport)

                    dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                        actionName = actionName,
                        usageCode = usageCode,
                        pressSent = pressSent,
                        releaseSent = releaseSent,
                        status = if (pressSent && releaseSent) "SUCCESS" else if (releaseSent) "PRESS_FAILED" else "RELEASE_FAILED",
                        pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                        payloadHex = payloadHex,
                        releaseHex = "00 00",
                        deviceInfo = deviceDesc
                    )

                    // Step 4: Settle delay to ensure host processes release before any subsequent press
                    delay(CONSUMER_RELEASE_SETTLE_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Consumer dispatch sequence failed", e)
                    dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                        actionName = actionName,
                        usageCode = usageCode,
                        pressSent = false,
                        releaseSent = false,
                        status = "EXCEPTION: ${e.message}",
                        pulseDurationMs = CONSUMER_PRESS_DELAY_MS,
                        payloadHex = payloadHex,
                        releaseHex = "00 00",
                        deviceInfo = deviceDesc
                    )
                }
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    override fun sendConsumerPress(usageCode: Int): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val actionName = dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.resolveActionName(usageCode)
        val payloadHex = formatConsumerPayload(usageCode)
        val deviceDesc = "${device.name ?: "Bluetooth Device"} (${device.address})"

        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_CONSUMER.toInt(), report)
            if (sent && _connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(device)
            }
            dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                actionName = actionName,
                usageCode = usageCode,
                pressSent = sent,
                releaseSent = false,
                status = if (sent) "HELD_DOWN" else "FAILED",
                pulseDurationMs = 0L,
                payloadHex = payloadHex,
                releaseHex = "00 00",
                deviceInfo = deviceDesc
            )
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendConsumerPress failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendConsumerRelease(): Boolean {
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val emptyReport = ByteArray(2)
        val deviceDesc = "${device.name ?: "Bluetooth Device"} (${device.address})"
        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_CONSUMER.toInt(), emptyReport)
            dev.aleian.pockethid.gamepad.ConsumerDiagnosticsHub.onEvent(
                actionName = "RELEASE",
                usageCode = 0,
                pressSent = false,
                releaseSent = sent,
                status = if (sent) "RELEASE_OK" else "FAILED",
                pulseDurationMs = 0L,
                payloadHex = "00 00",
                releaseHex = "00 00",
                deviceInfo = deviceDesc
            )
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendConsumerRelease failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
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
        val hid = hidDevice ?: return false
        val device = resolveActiveDevice() ?: return false
        if (!isAppRegistered) return false

        val report = ByteArray(13)
        // Buttons (16-bit bitmask, little endian)
        report[0] = (buttons and 0xFF).toByte()
        report[1] = ((buttons shr 8) and 0xFF).toByte()
        // Hat Switch (lower 4 bits)
        report[2] = (hat.toInt() and 0x0F).toByte()
        // Left Stick X
        report[3] = (leftX.toInt() and 0xFF).toByte()
        report[4] = ((leftX.toInt() shr 8) and 0xFF).toByte()
        // Left Stick Y
        report[5] = (leftY.toInt() and 0xFF).toByte()
        report[6] = ((leftY.toInt() shr 8) and 0xFF).toByte()
        // Right Stick X
        report[7] = (rightX.toInt() and 0xFF).toByte()
        report[8] = ((rightX.toInt() shr 8) and 0xFF).toByte()
        // Right Stick Y
        report[9] = (rightY.toInt() and 0xFF).toByte()
        report[10] = ((rightY.toInt() shr 8) and 0xFF).toByte()
        // Analog Triggers (0..255)
        report[11] = leftTrigger
        report[12] = rightTrigger

        return try {
            val sent = hid.sendReport(device, HidConstants.REPORT_ID_GAMEPAD.toInt(), report)
            if (sent && _connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Connected(device)
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "sendGamepadReport failed", e)
            false
        }
    }

    override fun sendGamepadNeutral(): Boolean {
        return sendGamepadReport(0, HidConstants.GAMEPAD_HAT_CENTERED, 0, 0, 0, 0, 0, 0)
    }
}

