package dev.aleian.pockethid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.power.ScreenWakeManager
import dev.aleian.pockethid.service.HidDeviceService
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.screens.MainScreen
import dev.aleian.pockethid.ui.screens.PermissionScreen
import dev.aleian.pockethid.ui.screens.UnsupportedScreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.PocketHIDTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "pockethid_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_device_mac"
    }

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var screenWakeManager: ScreenWakeManager

    private var transport: InputTransport? by mutableStateOf(null)
    private var hasPermissions by mutableStateOf(false)
    private var isBound = false

    private val bluetoothManager: BluetoothManager? by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasPermissions = allGranted
        if (allGranted) {
            bindHidService()
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Discoverable result code: ${result.resultCode}")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? HidDeviceService.LocalBinder
            transport = binder?.transport
            isBound = true
            Log.d(TAG, "Connected to HidDeviceService")
            attemptAutoReconnect()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            transport = null
            isBound = false
            Log.d(TAG, "Disconnected from HidDeviceService")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenWakeManager = ScreenWakeManager(this, activityScope)
        activityScope.launch {
            SettingsRepository.settings.collect { settings ->
                screenWakeManager.onSettingsChanged(settings)
            }
        }

        checkAndRequestPermissions()

        setContent {
            PocketHIDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    if (!hasPermissions) {
                        PermissionScreen(onRequestPermission = { requestRequiredPermissions() })
                    } else {
                        val currentTransport = transport
                        if (currentTransport != null && !currentTransport.isSupported) {
                            UnsupportedScreen(onRetry = {
                                unbindHidService()
                                bindHidService()
                            })
                        } else {
                            MainScreen(
                                transport = currentTransport,
                                pairedDevices = getPairedDevices(),
                                onConnectToDevice = { device ->
                                    if (bluetoothAdapter?.isEnabled != true) {
                                        try {
                                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                            startActivity(enableBtIntent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(this, "Vui lòng bật Bluetooth trên điện thoại!", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        saveLastConnectedDevice(device.address)
                                        val success = currentTransport?.connect(device) ?: false
                                        if (!success) {
                                            android.widget.Toast.makeText(this, "Không thể gửi yêu cầu kết nối tới ${device.name ?: device.address}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onMakeDiscoverable = { makeDiscoverable() },
                                onToggleOrientation = { toggleScreenOrientation() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val required = getRequiredPermissions()
        val allGranted = required.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        hasPermissions = allGranted
        if (allGranted) {
            bindHidService()
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            list.add(Manifest.permission.BLUETOOTH)
            list.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun requestRequiredPermissions() {
        permissionLauncher.launch(getRequiredPermissions())
    }

    private fun bindHidService() {
        if (!isBound) {
            HidDeviceService.startService(this)
            val intent = Intent(this, HidDeviceService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindHidService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasPermissions) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun makeDiscoverable() {
        try {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            discoverableLauncher.launch(discoverableIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting discoverable mode", e)
        }
    }

    private fun saveLastConnectedDevice(macAddress: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_DEVICE_ADDRESS, macAddress).apply()
    }

    @SuppressLint("MissingPermission")
    private fun attemptAutoReconnect() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastMac = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null) ?: return
        val currentTransport = transport ?: return

        try {
            val device = bluetoothAdapter?.getRemoteDevice(lastMac)
            if (device != null && bluetoothAdapter?.bondedDevices?.contains(device) == true) {
                Log.d(TAG, "Auto-reconnecting to previously paired host: ${device.name ?: lastMac}")
                currentTransport.connect(device)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-reconnect failed for $lastMac", e)
        }
    }

    fun toggleScreenOrientation() {
        val currentOrientation = resources.configuration.orientation
        requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    override fun onResume() {
        super.onResume()
        screenWakeManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        screenWakeManager.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        screenWakeManager.onUserInteraction()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenWakeManager.onDestroy()
        unbindHidService()
        activityScope.cancel()
    }
}
