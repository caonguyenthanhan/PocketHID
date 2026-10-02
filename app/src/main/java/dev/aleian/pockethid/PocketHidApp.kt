package dev.aleian.pockethid

import android.app.Application
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.audio.AudioFeedbackManager

class PocketHidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        dev.aleian.pockethid.audio.AudioFeedbackManager.init(this)
        dev.aleian.pockethid.haptic.HapticFeedbackManager.init(this)
    }
}
