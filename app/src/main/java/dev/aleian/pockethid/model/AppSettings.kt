package dev.aleian.pockethid.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val pollingRate: Float = 125f,
    val deadZonePx: Int = 1,
    val accelerationFactor: Float = 1.5f,
    val hapticsTrackpad: Boolean = true,
    val hapticIntensity: String = "Vừa",
    val naturalScroll: Boolean = false,
    val dragLock: Boolean = true,
    val pasteDelayMs: Long = 15L,
    val keyboardHaptics: Boolean = true,
    val keepAwakeTimeout: String = "5 Phút"
)

object SettingsRepository {
    private const val PREFS_NAME = "pockethid_settings"
    private const val KEY_POLLING_RATE = "polling_rate"
    private const val KEY_DEADZONE = "dead_zone"
    private const val KEY_ACCEL = "accel_factor"
    private const val KEY_HAPTIC_TRACKPAD = "haptic_trackpad"
    private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    private const val KEY_NATURAL_SCROLL = "natural_scroll"
    private const val KEY_DRAG_LOCK = "drag_lock"
    private const val KEY_PASTE_DELAY = "paste_delay"
    private const val KEY_KEYBOARD_HAPTICS = "keyboard_haptics"
    private const val KEY_KEEP_AWAKE = "keep_awake"

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _settings.value = AppSettings(
            pollingRate = sp.getFloat(KEY_POLLING_RATE, 125f),
            deadZonePx = sp.getInt(KEY_DEADZONE, 1),
            accelerationFactor = sp.getFloat(KEY_ACCEL, 1.5f),
            hapticsTrackpad = sp.getBoolean(KEY_HAPTIC_TRACKPAD, true),
            hapticIntensity = sp.getString(KEY_HAPTIC_INTENSITY, "Vừa") ?: "Vừa",
            naturalScroll = sp.getBoolean(KEY_NATURAL_SCROLL, false),
            dragLock = sp.getBoolean(KEY_DRAG_LOCK, true),
            pasteDelayMs = sp.getLong(KEY_PASTE_DELAY, 15L),
            keyboardHaptics = sp.getBoolean(KEY_KEYBOARD_HAPTICS, true),
            keepAwakeTimeout = sp.getString(KEY_KEEP_AWAKE, "5 Phút") ?: "5 Phút"
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs?.edit()?.apply {
            putFloat(KEY_POLLING_RATE, newSettings.pollingRate)
            putInt(KEY_DEADZONE, newSettings.deadZonePx)
            putFloat(KEY_ACCEL, newSettings.accelerationFactor)
            putBoolean(KEY_HAPTIC_TRACKPAD, newSettings.hapticsTrackpad)
            putString(KEY_HAPTIC_INTENSITY, newSettings.hapticIntensity)
            putBoolean(KEY_NATURAL_SCROLL, newSettings.naturalScroll)
            putBoolean(KEY_DRAG_LOCK, newSettings.dragLock)
            putLong(KEY_PASTE_DELAY, newSettings.pasteDelayMs)
            putBoolean(KEY_KEYBOARD_HAPTICS, newSettings.keyboardHaptics)
            putString(KEY_KEEP_AWAKE, newSettings.keepAwakeTimeout)
            apply()
        }
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        updateSettings(defaults)
    }
}
