package dev.aleian.pockethid

import android.app.Application
import dev.aleian.pockethid.model.SettingsRepository

class PocketHidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
    }
}
