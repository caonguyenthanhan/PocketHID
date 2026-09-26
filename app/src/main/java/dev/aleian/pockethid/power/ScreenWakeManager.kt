package dev.aleian.pockethid.power

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coordinates screen wakefulness and inactivity timeout for PocketHID.
 *
 * Guarantees:
 * 1. NEVER calls any forced device-locking API (e.g. lockNow()).
 * 2. While actively used, keeps the display usable.
 * 3. After 10 minutes of inactivity (default), allows the display to dim and sleep naturally.
 * 4. Android's own lock screen and security policy remains completely authoritative.
 * 5. Background Bluetooth HID service continues running independently of screen state.
 */
class ScreenWakeManager(
    private val activity: Activity,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ScreenWakeManager"
    }

    private var lastInteractionTime = System.currentTimeMillis()
    private var checkJob: Job? = null
    private var isFlagKeepScreenOnSet = false
    private var isResumed = false

    /**
     * Called by Activity.onUserInteraction() whenever any touch or key interaction occurs.
     */
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (isResumed) {
            applyWakePolicy()
        }
    }

    fun onResume() {
        isResumed = true
        lastInteractionTime = System.currentTimeMillis()
        applyWakePolicy()
    }

    fun onPause() {
        isResumed = false
        checkJob?.cancel()
        checkJob = null
        // Clear flag when leaving foreground so device can sleep naturally
        setWindowKeepScreenOn(false)
    }

    fun onSettingsChanged(newSettings: AppSettings) {
        if (isResumed) {
            applyWakePolicy(newSettings)
        }
    }

    fun applyWakePolicy(settings: AppSettings = SettingsRepository.settings.value) {
        if (!isResumed) return

        if (settings.keepScreenAwake) {
            // Option: Keep screen awake permanently while PocketHID is in foreground
            setWindowKeepScreenOn(true)
            checkJob?.cancel()
            checkJob = null
        } else {
            // Option: Allow screen to sleep after inactivity timeout (default 10 minutes)
            val now = System.currentTimeMillis()
            val elapsed = now - lastInteractionTime
            val timeoutMs = settings.screenSleepTimeoutMinutes * 60 * 1000L

            if (elapsed < timeoutMs) {
                // Still active: maintain screen-on flag during interaction
                setWindowKeepScreenOn(true)

                val remainingMs = timeoutMs - elapsed
                checkJob?.cancel()
                checkJob = scope.launch {
                    delay(maxOf(500L, remainingMs))
                    // Re-evaluate when timeout expires
                    applyWakePolicy()
                }
            } else {
                // Inactivity threshold reached: release flag to let Android naturally dim and sleep
                setWindowKeepScreenOn(false)
                checkJob?.cancel()
                checkJob = null
                Log.d(TAG, "Inactivity timeout reached (${settings.screenSleepTimeoutMinutes}m). Screen released to sleep naturally.")
            }
        }
    }

    private fun setWindowKeepScreenOn(keepOn: Boolean) {
        if (isFlagKeepScreenOnSet != keepOn) {
            isFlagKeepScreenOnSet = keepOn
            activity.runOnUiThread {
                try {
                    if (keepOn) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d(TAG, "Window FLAG_KEEP_SCREEN_ON added")
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d(TAG, "Window FLAG_KEEP_SCREEN_ON cleared")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating FLAG_KEEP_SCREEN_ON", e)
                }
            }
        }
    }

    fun onDestroy() {
        checkJob?.cancel()
        setWindowKeepScreenOn(false)
    }
}
