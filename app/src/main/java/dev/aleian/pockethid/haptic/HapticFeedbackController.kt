package dev.aleian.pockethid.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.aleian.pockethid.model.SettingsRepository

interface IHapticFeedbackController {
    fun play(event: HapticEvent)
    fun release()
}

class HapticFeedbackController(private val context: Context) : IHapticFeedbackController {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun play(event: HapticEvent) {
        val settings = SettingsRepository.settings.value
        if (!settings.hapticsEnabled) return

        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when (event) {
                    HapticEvent.ACTION_ACCEPTED -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticEvent.MODE_CHANGED -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticEvent.CONNECTED -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    HapticEvent.DISCONNECTED -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    HapticEvent.ERROR -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                }
                vibrator.vibrate(effect)
            } else {
                // Fallback for older devices
                val pattern = when (event) {
                    HapticEvent.ACTION_ACCEPTED -> longArrayOf(0, 10)
                    HapticEvent.MODE_CHANGED -> longArrayOf(0, 5)
                    HapticEvent.CONNECTED -> longArrayOf(0, 15, 50, 15)
                    HapticEvent.DISCONNECTED -> longArrayOf(0, 30)
                    HapticEvent.ERROR -> longArrayOf(0, 20, 50, 20)
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Fail silently as per rules
        }
    }

    override fun release() {
        // Nothing to release explicitly
    }
}
