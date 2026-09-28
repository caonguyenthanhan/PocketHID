package dev.aleian.pockethid.model

/**
 * Supported UI display languages.
 */
enum class AppLanguage {
    ENGLISH,
    VIETNAMESE;

    companion object {
        fun fromString(value: String): AppLanguage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Centralized string resources for PocketHID.
 * Guarantees zero language mixing across screens and provides instant EN/VI switching.
 */
object PocketStrings {

    // --- Navigation Labels ---
    fun navKeyboard(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Keyboard"
        AppLanguage.VIETNAMESE -> "Bàn phím"
    }

    fun navMouse(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Mouse"
        AppLanguage.VIETNAMESE -> "Chuột"
    }

    fun navGamepad(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Gamepad"
        AppLanguage.VIETNAMESE -> "Tay cầm"
    }

    fun navPresenter(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Presenter"
        AppLanguage.VIETNAMESE -> "Thuyết trình"
    }

    fun navOneHand(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "1-Hand"
        AppLanguage.VIETNAMESE -> "1-Tay"
    }

    fun navDraw(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Draw"
        AppLanguage.VIETNAMESE -> "Bảng vẽ"
    }

    // --- Connection Status ---
    fun statusConnected(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "CONNECTED"
        AppLanguage.VIETNAMESE -> "ĐÃ KẾT NỐI"
    }

    fun statusConnecting(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "CONNECTING..."
        AppLanguage.VIETNAMESE -> "ĐANG KẾT NỐI..."
    }

    fun statusDisconnected(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "DISCONNECTED"
        AppLanguage.VIETNAMESE -> "CHƯA KẾT NỐI"
    }

    fun noHostConnectedPrompt(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No host connected. Tap to select a host."
        AppLanguage.VIETNAMESE -> "Chưa kết nối máy tính. Chạm để chọn máy."
    }

    fun btnConnect(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Connect"
        AppLanguage.VIETNAMESE -> "Kết nối"
    }

    // --- Presenter Safety ---
    fun presenterStart(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "START F5"
        AppLanguage.VIETNAMESE -> "BẮT ĐẦU F5"
    }

    fun presenterResume(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "RESUME ⇧F5"
        AppLanguage.VIETNAMESE -> "TIẾP TỤC ⇧F5"
    }

    fun presenterExitSafe(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "HOLD 1.5s EXIT"
        AppLanguage.VIETNAMESE -> "GIỮ 1.5s THOÁT"
    }

    fun presenterExitHolding(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "RELEASE TO CANCEL"
        AppLanguage.VIETNAMESE -> "THẢ ĐỂ HỦY"
    }

    fun presenterBlack(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "BLACK (B)"
        AppLanguage.VIETNAMESE -> "MÀN ĐEN (B)"
    }

    fun presenterWhite(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "WHITE (W)"
        AppLanguage.VIETNAMESE -> "MÀN TRẮNG (W)"
    }

    fun presenterPointer(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "POINTER"
        AppLanguage.VIETNAMESE -> "CON TRỎ"
    }

    // --- Gamepad Guidance ---
    fun gamepadLandscapeHint(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "GAMEPAD works best in landscape"
        AppLanguage.VIETNAMESE -> "GAMEPAD tối ưu nhất khi xoay ngang"
    }

    fun gamepadRotate(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Rotate"
        AppLanguage.VIETNAMESE -> "Xoay"
    }

    // --- One-Hand Remote Strings ---
    fun oneHandRightHand(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Right Hand"
        AppLanguage.VIETNAMESE -> "Tay Phải"
    }

    fun oneHandLeftHand(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Left Hand"
        AppLanguage.VIETNAMESE -> "Tay Trái"
    }

    fun oneHandWebMode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Web Browsing"
        AppLanguage.VIETNAMESE -> "Duyệt Web"
    }

    fun oneHandVideoMode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Video Player"
        AppLanguage.VIETNAMESE -> "Xem Video"
    }

    fun oneHandSwipeUpScroll(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "▲ Swipe: Scroll Page"
        AppLanguage.VIETNAMESE -> "▲ Vuốt: Cuộn trang"
    }

    fun oneHandSwipeUpVolume(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "▲ Swipe: Volume"
        AppLanguage.VIETNAMESE -> "▲ Vuốt: Âm lượng"
    }

    fun oneHandTapClick(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "TAP\nCLICK"
        AppLanguage.VIETNAMESE -> "CHẠM\nCLICK"
    }

    fun oneHandTapPlay(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "TAP\nPLAY"
        AppLanguage.VIETNAMESE -> "CHẠM\nPLAY"
    }

    fun oneHandBack(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "◀ Back"
        AppLanguage.VIETNAMESE -> "◀ Lùi"
    }

    fun oneHandForward(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Forward ▶"
        AppLanguage.VIETNAMESE -> "Tiến ▶"
    }

    fun oneHandSeekBack(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "◀ Seek -10s"
        AppLanguage.VIETNAMESE -> "◀ Tua -10s"
    }

    fun oneHandSeekForward(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Seek +10s ▶"
        AppLanguage.VIETNAMESE -> "Tua +10s ▶"
    }

    fun oneHandFeedbackScrollUp(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scroll Up ↑"
        AppLanguage.VIETNAMESE -> "Cuộn lên ↑"
    }

    fun oneHandFeedbackScrollDown(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scroll Down ↓"
        AppLanguage.VIETNAMESE -> "Cuộn xuống ↓"
    }

    fun oneHandFeedbackBack(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "← Back"
        AppLanguage.VIETNAMESE -> "← Quay lại"
    }

    fun oneHandFeedbackForward(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Forward →"
        AppLanguage.VIETNAMESE -> "Tiến tới →"
    }

    fun oneHandFeedbackVolumeUp(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Volume Up ▲"
        AppLanguage.VIETNAMESE -> "Âm lượng ▲"
    }

    fun oneHandFeedbackVolumeDown(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Volume Down ▼"
        AppLanguage.VIETNAMESE -> "Âm lượng ▼"
    }

    fun oneHandFeedbackLeftClick(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Left Click"
        AppLanguage.VIETNAMESE -> "Chuột Trái"
    }

    fun oneHandFeedbackPlayPause(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Play / Pause ⏯"
        AppLanguage.VIETNAMESE -> "Phát / Tạm dừng ⏯"
    }

    fun oneHandSwipeDownScroll(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "▼ Swipe: Scroll Page"
        AppLanguage.VIETNAMESE -> "▼ Vuốt: Cuộn trang"
    }

    fun oneHandSwipeDownVolume(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "▼ Swipe: Volume"
        AppLanguage.VIETNAMESE -> "▼ Vuốt: Âm lượng"
    }

    fun oneHandWebReload(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Reload"
        AppLanguage.VIETNAMESE -> "Tải lại"
    }

    fun oneHandWebHome(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.VIETNAMESE -> "Trang chủ"
    }

    fun oneHandVideoPrev(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Prev"
        AppLanguage.VIETNAMESE -> "Trước"
    }

    fun oneHandVideoPlayPause(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Play/Pause"
        AppLanguage.VIETNAMESE -> "Phát/Dừng"
    }

    fun oneHandVideoNext(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.VIETNAMESE -> "Tiếp"
    }

    fun oneHandVideoMute(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Mute"
        AppLanguage.VIETNAMESE -> "Tắt tiếng"
    }

    // --- Settings Labels ---
    fun settingsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.VIETNAMESE -> "Cài đặt"
    }

    fun settingsLanguage(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Language / Ngôn ngữ"
        AppLanguage.VIETNAMESE -> "Ngôn ngữ / Language"
    }

    fun settingsSave(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save Settings"
        AppLanguage.VIETNAMESE -> "Lưu cài đặt"
    }

    fun settingsReset(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Reset Defaults"
        AppLanguage.VIETNAMESE -> "Khôi phục mặc định"
    }
}
