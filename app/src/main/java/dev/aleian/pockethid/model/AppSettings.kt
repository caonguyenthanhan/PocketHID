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
    val hapticIntensity: String = "Medium",
    val naturalScroll: Boolean = false,
    val dragLock: Boolean = true,
    val pasteDelayMs: Long = 15L,
    val keyboardHaptics: Boolean = true,
    val keepAwakeTimeout: String = "10 Minutes",
    val language: AppLanguage = AppLanguage.ENGLISH,
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
    val fastScrollWidthPercent: Float = 0.10f,
    // Visible Modes
    val modeKeyboardVisible: Boolean = true,
    val modeMouseVisible: Boolean = true,
    val modeGamepadVisible: Boolean = false,
    val modePresenterVisible: Boolean = true,
    val modeOneHandVisible: Boolean = true,
    val modeDrawVisible: Boolean = false
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
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_MODE_KEYBOARD = "mode_keyboard_visible"
    private const val KEY_MODE_MOUSE = "mode_mouse_visible"
    private const val KEY_MODE_GAMEPAD = "mode_gamepad_visible"
    private const val KEY_MODE_PRESENTER = "mode_presenter_visible"
    private const val KEY_MODE_ONE_HAND = "mode_one_hand_visible"
    private const val KEY_MODE_DRAW = "mode_draw_visible"

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        init(sp)
    }

    fun init(sp: SharedPreferences) {
        prefs = sp
        _settings.value = AppSettings(
            pollingRate = sp.getFloat(KEY_POLLING_RATE, 125f),
            deadZonePx = sp.getInt(KEY_DEADZONE, 1),
            accelerationFactor = sp.getFloat(KEY_ACCEL, 1.5f),
            hapticsTrackpad = sp.getBoolean(KEY_HAPTIC_TRACKPAD, true),
            hapticIntensity = sp.getString(KEY_HAPTIC_INTENSITY, "Medium") ?: "Medium",
            naturalScroll = sp.getBoolean(KEY_NATURAL_SCROLL, false),
            dragLock = sp.getBoolean(KEY_DRAG_LOCK, true),
            pasteDelayMs = sp.getLong(KEY_PASTE_DELAY, 15L),
            keyboardHaptics = sp.getBoolean(KEY_KEYBOARD_HAPTICS, true),
            keepAwakeTimeout = sp.getString(KEY_KEEP_AWAKE, "10 Minutes") ?: "10 Minutes",
            language = AppLanguage.fromString(sp.getString(KEY_LANGUAGE, "ENGLISH") ?: "ENGLISH"),
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
            fastScrollWidthPercent = sp.getFloat(KEY_FAST_SCROLL_WIDTH, 0.10f),
            modeKeyboardVisible = sp.getBoolean(KEY_MODE_KEYBOARD, true),
            modeMouseVisible = sp.getBoolean(KEY_MODE_MOUSE, true),
            modeGamepadVisible = sp.getBoolean(KEY_MODE_GAMEPAD, false),
            modePresenterVisible = sp.getBoolean(KEY_MODE_PRESENTER, true),
            modeOneHandVisible = sp.getBoolean(KEY_MODE_ONE_HAND, true),
            modeDrawVisible = sp.getBoolean(KEY_MODE_DRAW, false)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        val visibleCount = listOf(
            newSettings.modeKeyboardVisible,
            newSettings.modeMouseVisible,
            newSettings.modeGamepadVisible,
            newSettings.modePresenterVisible,
            newSettings.modeOneHandVisible,
            newSettings.modeDrawVisible
        ).count { it }

        val finalSettings = if (visibleCount < 3) {
            newSettings.copy(
                modeKeyboardVisible = _settings.value.modeKeyboardVisible,
                modeMouseVisible = _settings.value.modeMouseVisible,
                modeGamepadVisible = _settings.value.modeGamepadVisible,
                modePresenterVisible = _settings.value.modePresenterVisible,
                modeOneHandVisible = _settings.value.modeOneHandVisible,
                modeDrawVisible = _settings.value.modeDrawVisible
            )
        } else {
            newSettings
        }

        _settings.value = finalSettings
        prefs?.edit()?.apply {
            putFloat(KEY_POLLING_RATE, finalSettings.pollingRate)
            putInt(KEY_DEADZONE, finalSettings.deadZonePx)
            putFloat(KEY_ACCEL, finalSettings.accelerationFactor)
            putBoolean(KEY_HAPTIC_TRACKPAD, finalSettings.hapticsTrackpad)
            putString(KEY_HAPTIC_INTENSITY, finalSettings.hapticIntensity)
            putBoolean(KEY_NATURAL_SCROLL, finalSettings.naturalScroll)
            putBoolean(KEY_DRAG_LOCK, finalSettings.dragLock)
            putLong(KEY_PASTE_DELAY, finalSettings.pasteDelayMs)
            putBoolean(KEY_KEYBOARD_HAPTICS, finalSettings.keyboardHaptics)
            putString(KEY_KEEP_AWAKE, finalSettings.keepAwakeTimeout)
            putString(KEY_LANGUAGE, finalSettings.language.name)
            putBoolean(KEY_KEEP_SCREEN_AWAKE, finalSettings.keepScreenAwake)
            putInt(KEY_SCREEN_SLEEP_TIMEOUT, finalSettings.screenSleepTimeoutMinutes)
            putString(KEY_HOST_OS, finalSettings.hostOs.name)
            putFloat(KEY_GAMEPAD_LEFT_DEADZONE, finalSettings.gamepadLeftDeadzone)
            putFloat(KEY_GAMEPAD_LEFT_SENS, finalSettings.gamepadLeftSensitivity)
            putString(KEY_GAMEPAD_LEFT_CURVE, finalSettings.gamepadLeftCurve)
            putFloat(KEY_GAMEPAD_RIGHT_DEADZONE, finalSettings.gamepadRightDeadzone)
            putFloat(KEY_GAMEPAD_RIGHT_SENS, finalSettings.gamepadRightSensitivity)
            putString(KEY_GAMEPAD_RIGHT_CURVE, finalSettings.gamepadRightCurve)
            putFloat(KEY_GAMEPAD_TRIGGER_SENS, finalSettings.gamepadTriggerSensitivity)
            putBoolean(KEY_GAMEPAD_INVERT_Y, finalSettings.gamepadInvertY)
            putBoolean(KEY_PINCH_ZOOM_ENABLED, finalSettings.pinchZoomEnabled)
            putFloat(KEY_PINCH_THRESHOLD, finalSettings.pinchThresholdPx)
            putString(KEY_ZOOM_MODE, finalSettings.zoomMode)
            putString(KEY_ONE_HAND_HANDEDNESS, finalSettings.oneHandHandedness)
            putInt(KEY_ONE_HAND_SEEK_STEP, finalSettings.oneHandSeekStepSeconds)
            putFloat(KEY_ONE_HAND_SCROLL_SENS, finalSettings.oneHandScrollSensitivity)
            putBoolean(KEY_ONE_HAND_EDGE_GESTURES, finalSettings.oneHandEdgeGestures)
            putBoolean(KEY_FAST_SCROLL_ENABLED, finalSettings.fastScrollEnabled)
            putFloat(KEY_FAST_SCROLL_MULTIPLIER, finalSettings.fastScrollMultiplier)
            putFloat(KEY_FAST_SCROLL_WIDTH, finalSettings.fastScrollWidthPercent)
            putBoolean(KEY_MODE_KEYBOARD, finalSettings.modeKeyboardVisible)
            putBoolean(KEY_MODE_MOUSE, finalSettings.modeMouseVisible)
            putBoolean(KEY_MODE_GAMEPAD, finalSettings.modeGamepadVisible)
            putBoolean(KEY_MODE_PRESENTER, finalSettings.modePresenterVisible)
            putBoolean(KEY_MODE_ONE_HAND, finalSettings.modeOneHandVisible)
            putBoolean(KEY_MODE_DRAW, finalSettings.modeDrawVisible)
            apply()
        }
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        updateSettings(defaults)
    }
}
