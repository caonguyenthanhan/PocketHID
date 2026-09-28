//
//  PocketStrings.swift
//  PocketHID
//
//  Created for PocketHID iOS Port.
//  Copyright © 2026 Aleian. All rights reserved.
//

import Foundation

public struct PocketStrings {
    // Navigation
    public static func navKeyboard(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Bàn phím" : "Keyboard"
    }
    public static func navMouse(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Chuột" : "Mouse"
    }
    public static func navGamepad(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Tay cầm" : "Gamepad"
    }
    public static func navPresenter(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Thuyết trình" : "Presenter"
    }
    public static func navOneHand(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "1-Tay" : "1-Hand"
    }
    public static func navDraw(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Bảng vẽ" : "Draw"
    }
    
    // Status
    public static func statusConnected(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Đã kết nối" : "Connected"
    }
    public static func statusConnecting(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Đang kết nối…" : "Connecting…"
    }
    public static func statusDisconnected(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Chưa kết nối" : "Disconnected"
    }
    
    // Focus Lock
    public static func focusActive(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "ĐÃ KHÓA FOCUS" : "FOCUS LOCKED"
    }
    public static func focusPrompt(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Chạm vào FOCUS để mở khóa chuyển chế độ" : "Tap FOCUS to unlock mode switching"
    }
    
    // Presenter
    public static func presenterStart(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "BẮT ĐẦU" : "START"
    }
    public static func presenterResume(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "TIẾP TỤC" : "RESUME"
    }
    public static func presenterSafeExitPrompt(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "GIỮ 1.5s!" : "HOLD 1.5s!"
    }
    
    // Gamepad
    public static func gamepadLandscapeHint(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Tay cầm tối ưu khi xoay ngang" : "Gamepad works best in landscape"
    }
    
    // Settings
    public static func settingsTitle(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Cài đặt" : "Settings"
    }
    public static func settingsSave(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Lưu" : "Save"
    }
    public static func settingsReset(_ lang: AppLanguage) -> String {
        lang == .vietnamese ? "Đặt lại mặc định" : "Reset Defaults"
    }
}
