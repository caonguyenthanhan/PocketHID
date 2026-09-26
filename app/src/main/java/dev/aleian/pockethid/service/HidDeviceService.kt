package dev.aleian.pockethid.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.aleian.pockethid.MainActivity
import dev.aleian.pockethid.R
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.transport.BtHidTransport
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HidDeviceService : Service() {

    companion object {
        private const val TAG = "HidDeviceService"
        const val CHANNEL_ID = "pockethid_service_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "dev.aleian.pockethid.action.START"
        const val ACTION_STOP = "dev.aleian.pockethid.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, HidDeviceService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HidDeviceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        val service: HidDeviceService
            get() = this@HidDeviceService
        val transport: InputTransport
            get() = this@HidDeviceService.transport
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var transport: BtHidTransport
        private set

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HidDeviceService onCreate")
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PocketHID::ServiceWakeLock"
        )

        transport = BtHidTransport(applicationContext)
        transport.register()

        observeConnectionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground(buildNotification("Initializing PocketHID..."))
        return START_STICKY
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeConnectionState() {
        scope.launch {
            transport.connectionState.collectLatest { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        @SuppressLint("MissingPermission")
                        val deviceName = state.device.name ?: state.device.address
                        updateNotification("Connected to $deviceName")
                        acquireWakeLock()
                    }
                    is ConnectionState.Connecting -> {
                        @SuppressLint("MissingPermission")
                        val target = state.device?.name ?: "host"
                        updateNotification("Connecting to $target...")
                        acquireWakeLock()
                    }
                    is ConnectionState.Disconnected -> {
                        updateNotification("Ready for Bluetooth connection")
                        releaseWakeLock()
                    }
                    is ConnectionState.Disconnecting -> {
                        updateNotification("Disconnecting…")
                        releaseWakeLock()
                    }
                    is ConnectionState.Error -> {
                        updateNotification("Error: ${state.message}")
                        releaseWakeLock()
                    }
                }
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketHID")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HidDeviceService onDestroy")
        scope.cancel()
        releaseWakeLock()
        transport.unregister()
    }
}
