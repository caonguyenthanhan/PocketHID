package dev.aleian.pockethid.model

import android.content.Context
import android.content.SharedPreferences
import dev.aleian.pockethid.action.HostOs
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
    val keepAwakeTimeout: String = "10 Phút",
    // Screen Behavior & Timeout Settings
    val keepScreenAwake: Boolean = false, // false = Allow screen to sleep (Default), true = Keep screen awake while PocketHID is open
    val screenSleepTimeoutMinutes: Int = 10, // 10 minutes inactivity timeout (Default)
    val hostOs: HostOs = HostOs.WINDOWS,
    // Gamepad Settings
    val gamepadLeftDeadzone: Float = 0.10f,
    val gamepadLeftSensitivity: Float = 1.0f,
    val gamepadLeftCurve: String = "Linear",
    val gamepadRightDeadzone: Float = 0.10f,
    val gamepadRightSensitivity: Float = 1.0f,
    val gamepadRightCurve: String = "Linear",
    val gamepadTriggerSensitivity: Float = 1.0f,
    val gamepadInvertY: Boolean = false,
    // Multi-Touch Pinch Zoom Settings
    val pinchZoomEnabled: Boolean = true,
    val pinchThresholdPx: Float = 35.0f,
    val zoomMode: String = "Wheel", // "Wheel" (Ctrl/Cmd + Wheel) or "Keys" (Ctrl/Cmd + Plus/Minus)
    // One-Hand Remote Control Settings
    val oneHandHandedness: String = "Right", // "Right" (Thumb on right) or "Left" (Thumb on left)
    val oneHandSeekStepSeconds: Int = 10, // 5, 10, or 30 seconds
    val oneHandScrollSensitivity: Float = 1.0f, // 0.5f to 2.0f
    val oneHandEdgeGestures: Boolean = true,
    // Right Edge Fast Scroll Settings
    val fastScrollEnabled: Boolean = true,
    val fastScrollMultiplier: Float = 2.5f,
    val fastScrollWidthPercent: Float = 0.10f
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
    private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
    private const val KEY_SCREEN_SLEEP_TIMEOUT = "screen_sleep_timeout"
    private const val KEY_HOST_OS = "host_os"
    private const val KEY_GAMEPAD_LEFT_DEADZONE = "gp_left_deadzone"
    private const val KEY_GAMEPAD_LEFT_SENS = "gp_left_sens"
    private const val KEY_GAMEPAD_LEFT_CURVE = "gp_left_curve"
    private const val KEY_GAMEPAD_RIGHT_DEADZONE = "gp_right_deadzone"
    private const val KEY_GAMEPAD_RIGHT_SENS = "gp_right_sens"
    private const val KEY_GAMEPAD_RIGHT_CURVE = "gp_right_curve"
    private const val KEY_GAMEPAD_TRIGGER_SENS = "gp_trigger_sens"
    private const val KEY_GAMEPAD_INVERT_Y = "gp_invert_y"
    private const val KEY_PINCH_ZOOM_ENABLED = "pinch_zoom_enabled"
    private const val KEY_PINCH_THRESHOLD = "pinch_threshold"
    private const val KEY_ZOOM_MODE = "zoom_mode"
    private const val KEY_ONE_HAND_HANDEDNESS = "one_hand_handedness"
    private const val KEY_ONE_HAND_SEEK_STEP = "one_hand_seek_step"
    private const val KEY_ONE_HAND_SCROLL_SENS = "one_hand_scroll_sens"
    private const val KEY_ONE_HAND_EDGE_GESTURES = "one_hand_edge_gestures"
    private const val KEY_FAST_SCROLL_ENABLED = "fast_scroll_enabled"
    private const val KEY_FAST_SCROLL_MULTIPLIER = "fast_scroll_multiplier"
    private const val KEY_FAST_SCROLL_WIDTH = "fast_scroll_width"

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
            keepAwakeTimeout = sp.getString(KEY_KEEP_AWAKE, "10 Phút") ?: "10 Phút",
            keepScreenAwake = sp.getBoolean(KEY_KEEP_SCREEN_AWAKE, false),
            screenSleepTimeoutMinutes = sp.getInt(KEY_SCREEN_SLEEP_TIMEOUT, 10),
            hostOs = HostOs.fromString(sp.getString(KEY_HOST_OS, "WINDOWS") ?: "WINDOWS"),
            gamepadLeftDeadzone = sp.getFloat(KEY_GAMEPAD_LEFT_DEADZONE, 0.10f),
            gamepadLeftSensitivity = sp.getFloat(KEY_GAMEPAD_LEFT_SENS, 1.0f),
            gamepadLeftCurve = sp.getString(KEY_GAMEPAD_LEFT_CURVE, "Linear") ?: "Linear",
            gamepadRightDeadzone = sp.getFloat(KEY_GAMEPAD_RIGHT_DEADZONE, 0.10f),
            gamepadRightSensitivity = sp.getFloat(KEY_GAMEPAD_RIGHT_SENS, 1.0f),
            gamepadRightCurve = sp.getString(KEY_GAMEPAD_RIGHT_CURVE, "Linear") ?: "Linear",
            gamepadTriggerSensitivity = sp.getFloat(KEY_GAMEPAD_TRIGGER_SENS, 1.0f),
            gamepadInvertY = sp.getBoolean(KEY_GAMEPAD_INVERT_Y, false),
            pinchZoomEnabled = sp.getBoolean(KEY_PINCH_ZOOM_ENABLED, true),
            pinchThresholdPx = sp.getFloat(KEY_PINCH_THRESHOLD, 35.0f),
            zoomMode = sp.getString(KEY_ZOOM_MODE, "Wheel") ?: "Wheel",
            oneHandHandedness = sp.getString(KEY_ONE_HAND_HANDEDNESS, "Right") ?: "Right",
            oneHandSeekStepSeconds = sp.getInt(KEY_ONE_HAND_SEEK_STEP, 10),
            oneHandScrollSensitivity = sp.getFloat(KEY_ONE_HAND_SCROLL_SENS, 1.0f),
            oneHandEdgeGestures = sp.getBoolean(KEY_ONE_HAND_EDGE_GESTURES, true),
            fastScrollEnabled = sp.getBoolean(KEY_FAST_SCROLL_ENABLED, true),
            fastScrollMultiplier = sp.getFloat(KEY_FAST_SCROLL_MULTIPLIER, 2.5f),
            fastScrollWidthPercent = sp.getFloat(KEY_FAST_SCROLL_WIDTH, 0.10f)
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
            putBoolean(KEY_KEEP_SCREEN_AWAKE, newSettings.keepScreenAwake)
            putInt(KEY_SCREEN_SLEEP_TIMEOUT, newSettings.screenSleepTimeoutMinutes)
            putString(KEY_HOST_OS, newSettings.hostOs.name)
            putFloat(KEY_GAMEPAD_LEFT_DEADZONE, newSettings.gamepadLeftDeadzone)
            putFloat(KEY_GAMEPAD_LEFT_SENS, newSettings.gamepadLeftSensitivity)
            putString(KEY_GAMEPAD_LEFT_CURVE, newSettings.gamepadLeftCurve)
            putFloat(KEY_GAMEPAD_RIGHT_DEADZONE, newSettings.gamepadRightDeadzone)
            putFloat(KEY_GAMEPAD_RIGHT_SENS, newSettings.gamepadRightSensitivity)
            putString(KEY_GAMEPAD_RIGHT_CURVE, newSettings.gamepadRightCurve)
            putFloat(KEY_GAMEPAD_TRIGGER_SENS, newSettings.gamepadTriggerSensitivity)
            putBoolean(KEY_GAMEPAD_INVERT_Y, newSettings.gamepadInvertY)
            putBoolean(KEY_PINCH_ZOOM_ENABLED, newSettings.pinchZoomEnabled)
            putFloat(KEY_PINCH_THRESHOLD, newSettings.pinchThresholdPx)
            putString(KEY_ZOOM_MODE, newSettings.zoomMode)
            putString(KEY_ONE_HAND_HANDEDNESS, newSettings.oneHandHandedness)
            putInt(KEY_ONE_HAND_SEEK_STEP, newSettings.oneHandSeekStepSeconds)
            putFloat(KEY_ONE_HAND_SCROLL_SENS, newSettings.oneHandScrollSensitivity)
            putBoolean(KEY_ONE_HAND_EDGE_GESTURES, newSettings.oneHandEdgeGestures)
            putBoolean(KEY_FAST_SCROLL_ENABLED, newSettings.fastScrollEnabled)
            putFloat(KEY_FAST_SCROLL_MULTIPLIER, newSettings.fastScrollMultiplier)
            putFloat(KEY_FAST_SCROLL_WIDTH, newSettings.fastScrollWidthPercent)
            apply()
        }
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        updateSettings(defaults)
    }
}
