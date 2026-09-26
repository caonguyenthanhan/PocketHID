# PocketHID — Development Todo

## Phase 0: Project Setup & Spike
- [x] Tạo `todo.md` và kiểm tra bối cảnh dự án
- [x] Thiết lập Gradle build Android (Gradle 8.11.1, AGP 8.9.2, Kotlin 2.0.21, Compose)
- [x] Tạo cấu trúc thư mục source `dev.aleian.pockethid`
- [x] Khai báo `AndroidManifest.xml` (Permissions: BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE, FOREGROUND_SERVICE_CONNECTED_DEVICE)
- [x] Định nghĩa `HidConstants.kt` chứa Report Descriptor combo (Keyboard ID 1, Mouse ID 2)
- [x] Xây dựng `InputTransport` interface & `BtHidTransport.kt` (`BluetoothHidDevice`)
- [x] Xây dựng `HidDeviceService.kt` (Foreground Service `connectedDevice`)

## Phase 1: Core Features (P0-1 -> P0-9)
- [x] P0-2: Kiểm tra khả năng HID Device role trên máy khi khởi động (`UnsupportedScreen`)
- [x] P0-9: Luồng xin quyền runtime Bluetooth (API 31+) có giải thích trước (`PermissionScreen`)
- [x] P0-3 & P0-6: UI Quản lý kết nối (`ConnectionBar`, trạng thái Connected/Connecting/Disconnected, bật discoverable, hướng dẫn ghép đôi từ host)
- [x] P0-7: Tự động kết nối lại (Auto-reconnect) với host đã ghép đôi
- [x] P0-4: Tab Mouse (`MouseScreen`, `GestureInterpreter` relative dx/dy, tap=left click, 2-finger tap=right click, 2-finger drag=scroll, dead zone, acceleration curve, nút bấm Left/Middle/Right)
- [x] P0-5: Tab Keyboard (`KeyboardScreen`, `KeyMapper` US-QWERTY -> HID usage codes, transparent EditText nhận IME, sticky modifier bar Ctrl/Shift/Alt/Win/Fn, special keys bar Esc/Tab/Arrows/F1-F12)
- [x] P0-8: Giữ kết nối ở nền qua Foreground Service + Notification
- [x] Build & Test compile dự án (`./gradlew.bat assembleDebug` -> `app-debug.apk` thành công)

## Phase 2: Advanced Design System & Screens (from stitch_new_build_project)
- [x] Thiết lập Design Tokens và bảng màu theo `pockethid_engineering_system` (`Color.kt`)
- [x] Tạo `AppSettings.kt` & `SettingsRepository` quản lý cấu hình chuột, bàn phím, độ trễ và haptics
- [x] Xây dựng màn hình Cài đặt nâng cao `SettingsScreen.kt` (`c_i_t_n_ng_cao`) với Live Tuner, Polling Rate, Deadzone, Acceleration Curve, Haptic Toggle/Level, Natural Scroll, Drag Lock, Safe Paste Throttle, Keep Awake Selector
- [x] Xây dựng bàn phím cơ ảo xoay ngang `LandscapeDeckScreen.kt` (`b_n_ph_m_o_xoay_ngang_landscape_mode`) chia 3 vùng (Zone 1: Left Deck Macros & D-Pad; Zone 2: Main 75% QWERTY; Zone 3: Right Deck Numpad/Trackpad Switcher) cùng Realtime Scancode / Terminal Command Buffer
- [x] Nâng cấp `PairingSheet.kt` (`host_manager`) với thẻ Host đang kết nối, Telemetry, xác thực HID Device Role trên AOSP, danh sách Paired Devices và nút mở Settings
- [x] Tích hợp `MainScreen.kt` tự động chuyển sang `LandscapeDeckScreen` khi xoay ngang và mở `SettingsScreen` từ `ConnectionBar`
- [x] Bổ sung Live HID Stream Buffer và nút Safe Paste (rate limit) vào `KeyboardScreen.kt`
- [x] Biên dịch thành công APK Debug (`./gradlew.bat assembleDebug` -> BUILD SUCCESSFUL)

## Phase 3: Feedback, Alerts & Connection Notifications
- [x] Bổ sung thông báo nổi `SnackbarHost` tự động hiển thị khi kết nối thành công, thất bại, ngắt kết nối hoặc có lỗi
- [x] Thêm Alert Banner ngữ cảnh ngay dưới thanh trạng thái khi chưa kết nối máy tính, đang kết nối hoặc lỗi kết nối (kèm nút "Kết nối" / "Thử lại")
- [x] Tích hợp cảnh báo Toast chống gõ phím / rê chuột khi chưa kết nối thiết bị trong `MouseScreen.kt` và `KeyboardScreen.kt`
- [x] Bổ sung logic kiểm tra Bluetooth của điện thoại (bật Bluetooth prompt nếu đang tắt) trong `MainActivity.kt` và `BtHidTransport.kt`
- [x] Cải thiện xử lý lỗi chi tiết khi máy tính từ chối kết nối hoặc bị ngắt kết nối đột ngột
- [x] Biên dịch kiểm tra thành công (`./gradlew.bat assembleDebug` -> PASS 100%)


