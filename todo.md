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

## Phase 4: Multi-Finger Touch Gestures & Dedicated Number Row
- [x] Nâng cấp `GestureInterpreter.kt` với State Machine nhận diện cử chỉ 1, 2, 3, 4 ngón và Gesture Priority Lock (ngăn xung đột)
- [x] Triển khai cử chỉ 1 ngón: Di chuột (Gia tốc + Deadzone), Tap (Left click), Double Tap (Double click), Tap+Hold (Drag)
- [x] Triển khai cử chỉ 2 ngón: Cuộn dọc, Cuộn ngang (Pan H), 2-Finger Tap (Right click), 2-Finger Tap+Hold
- [x] Triển khai cử chỉ 3 ngón: 3-Finger Tap (Middle click), Swipe Trái/Phải (Chuyển Desktop ảo), Swipe Lên (Task View), Swipe Xuống (Show Desktop)
- [x] Triển khai cử chỉ 4 ngón: 4-Finger Tap (Mở Command Deck / Quick Actions), Swipe Trái/Phải (Chuyển App Alt+Tab), Swipe Lên (Task Overview), Swipe Xuống (Thu nhỏ về Desktop)
- [x] Xây dựng `MultiTouchTrackpad.kt` dùng chung cho cả Landscape và Portrait, có subtle hint và Dialog hướng dẫn cử chỉ
- [x] Giữ nguyên cụm phím chuột vật lý bên dưới (LEFT, MIDDLE, RIGHT, LOCK) hỗ trợ vừa ấn giữ nút vừa rê chuột để kéo thả
- [x] Xây dựng `DedicatedNumberRow.kt` với hàng phím số cố định (` 1 2 3 4 5 6 7 8 9 0 - = BKSP), hỗ trợ Shift ra ký tự đặc biệt và Backspace hold-to-repeat
- [x] Tích hợp hàng phím số cố định vào `LandscapeDeckScreen.kt` và `KeyboardScreen.kt` (TYPE mode)
- [x] Bổ sung Gesture Telemetry HUD vào `DiagnosticsSheet.kt`
- [x] Viết bộ Unit Test tự động cho `GestureInterpreterTest` và `KeyMapperTest` (`testDebugUnitTest` PASS 100%)
- [x] Tạo `PROJECT_STRUCTURE.md` chuẩn hóa kiến trúc thư mục theo checklist scaffold
- [x] Biên dịch APK Debug (`./gradlew.bat assembleDebug` -> BUILD SUCCESSFUL in 5s)

## Phase 8: One-Hand Web & Video Control Mode & Settings Modularization
- [x] Bổ sung primary mode thứ 5: `KEYBOARD | MOUSE | GAMEPAD | PRESENTER | ONE-HAND` trên Portrait & Landscape
- [x] Sub-modes `WEB` và `VIDEO` với segmented control chuyển đổi nhanh
- [x] Thumb surface công thái học hỗ trợ tùy chọn tay thuận Trái / Phải
- [x] Báo cáo chuột thật Mouse Wheel HID cho cuộn trang web
- [x] Consumer Control HID cho Video: Play/Pause, Volume Up/Down, Mute, Seek
- [x] Mở rộng Semantic Action Engine với `PocketAction.WebAction` & `VideoAction`, phân giải OS-aware
- [x] Tái cấu trúc module `SettingsSections.kt` giải quyết triệt để JVM 64KB `MethodTooLargeException`

## Phase 9: Right Edge Fast Scroll Zone for Trackpad
- [x] Tạo `FastScrollController.kt` chuyên trách nhận diện activation zone (8–12% chiều rộng mép phải, min 32dp, max 64dp)
- [x] Tính toán delta Y, vận tốc (velocity) và đường cong gia tốc phi tuyến riêng biệt cho cuộn nhanh
- [x] Giới hạn tốc độ cuộn tối đa (clamping max wheel per report) chống văng trang
- [x] Gửi báo cáo chuột thật Mouse Wheel HID, đồng bộ 100% với cài đặt `naturalScroll`
- [x] Khóa cử chỉ chỉ khi `ACTION_DOWN` bắt đầu tại mép phải, ngăn chặn hoàn toàn việc chạm từ giữa rê sang
- [x] Ngăn ngừa xung đột cử chỉ: khi cuộn nhanh không di chuột, không click, không tap, không pinch zoom
- [x] Tuân thủ thứ bậc đa ngón (4 > 3 > 2 > 1): Tự động nhường quyền khi có thêm ngón tay hạ xuống
- [x] Dải giao diện mép phải tinh tế với viền phân cách, grip markers `⋮`, icon `↕`, và nhãn `FAST`
- [x] Hiển thị highlight viền và banner thoáng qua `FAST SCROLL ×2.5` khi chạm kích hoạt
- [x] Rung nhẹ 1 xung haptic khi kích hoạt (không rung liên tục khi đang cuộn)
- [x] Gắn nhãn trợ năng (Accessibility) "Fast Scroll Area. Swipe vertically to quickly scroll."
- [x] Cài đặt bật/tắt và thanh trượt tốc độ cuộn nhanh (1.5x–5.0x, mặc định 2.5x) trong `SettingsSections.kt` & `AppSettings.kt`
## Phase 10: Gamepad Windows Enumeration (joy.cpl) & Consumer Control Diagnostics
- [x] Phân tích và phát hiện nguyên nhân gốc rễ: SDP Subclass 0xC0 thiếu Gamepad, ô nhiễm Global items trong HID descriptor, padding 0x01 thay vì 0x03, thiếu Pointer Physical Collection, và chu kỳ nhả Consumer Key 12ms
- [x] Sửa lỗi HID Report Descriptor trong `HidConstants.kt`: Bọc Pointer Physical Collection cho Left X/Y và Right Z/Rz, reset Physical Min/Max/Unit sau Hat switch, dùng 0x03 cho đệm Hat
- [x] Cập nhật SDP Subclass trong `BtHidTransport.kt`: `(SUBCLASS1_COMBO or SUBCLASS2_GAMEPAD).toByte()` (0xC8)
- [x] Sửa lỗi Media Consumer Control: Tăng thời gian giữ phím lên 75ms (`CONSUMER_PRESS_DELAY_MS`), thêm `sendConsumerPress` & `sendConsumerRelease`
- [x] Xây dựng `ConsumerDiagnosticsHub.kt` thu thập telemetry chi tiết: actionName, usageCode, pressSent, releaseSent, transportStatus
- [x] Nâng cấp `GamepadState.kt` / `GamepadDiagnosticsHub`: rawBytes 13 byte, rawReportHex, isNeutral flag, và bộ đếm latch/down/up transition chống kẹt phím
- [x] Module hóa giao diện chẩn đoán `dev.aleian.pockethid.ui.screens.diagnostics`:
  - `EnumerationStatusCard.kt`: Kiểm tra SDP 0xC8, 4 collections, độ dài report 13 bytes, Gamepad Host Status
  - `GamepadTestPanel.kt`: Ma trận 11 nút bấm + D-pad, 2D crosshair sticks, thanh áp lực cò (0..1), soi byte thô HEX, cờ NEUTRAL REPORT, bộ đếm latch
  - `MediaConsumerTestPanel.kt`: 7 nút bấm test âm lượng/media với trạng thái phản hồi trực tiếp
  - `HostChecklistPanel.kt`: Quy trình hướng dẫn dev quên thiết bị cũ, pair lại và test `joy.cpl`
- [x] Cập nhật `DiagnosticsSheet.kt` với tab chuyển đổi 4 mục `[ SYSTEM ] [ GAMEPAD ] [ MEDIA ] [ CHECKLIST ]` và scroll dọc mượt mà
- [x] Viết bộ unit test toàn diện `GamepadReportDescriptorTest` kiểm tra Report ID 4, độ dài 13 byte, bitmask, hat, axes, triggers, neutral state, và latch transitions
- [x] Chạy `./gradlew.bat testDebugUnitTest` pass 100% và `./gradlew.bat assembleDebug` thành công

## Phase 11: Landscape Keyboard UX Rework & Media Volume/Mute Functional Fix
- [x] Loại bỏ phím trùng lặp khỏi Landscape Deck:
  - Gỡ bỏ CTRL, ALT, SHIFT khỏi Left Deck; duy trì duy nhất một dải modifier canonical ở hàng dưới cùng (Bottom Row)
  - Gỡ bỏ phím PASTE lớn khỏi bàn phím chính (chuyển sang Shortcut layer & Command Palette)
  - Gỡ bỏ phím BKSP và ENTER khổng lồ khỏi Right Deck
  - Duy trì duy nhất phím Backspace tại góc phải hàng số cố định (Dedicated Number Row)
  - Duy trì duy nhất phím Enter chính tại góc phải hàng dưới cùng (Bottom Row)
  - Gỡ bỏ nhãn "TOUCH OPTIMIZED" khỏi thanh tiêu đề phím
- [x] Tái phân bổ tỷ lệ màn hình Landscape:
  - Left Thumb Zone: ~13% (`weight(1.3f)`) - Chứa cụm phím ngón cái gọn gàng (`ESC`, `TAB`, `SUPER`, `⌘ QUICK`)
  - Primary Keyboard Bay: ~74% (`weight(7.4f)`) - Chiếm vị trí áp đảo, tăng kích thước touch target của tất cả các phím QWERTY, hàng số, Space (weight 3.8f) và Enter
  - Right Thumb Zone: ~13% (`weight(1.3f)`) - Chứa cụm điều hướng 4 chiều tự nhiên (`↑`, `←`, `↓`, `→`), `DEL`, và nút chuyển `123#`
- [x] Khắc phục chức năng Media & Consumer Control:
  - Bàn phím Media thiết kế lại dạng 2 hàng tinh gọn: Row 1 (`PREV` | `PLAY / PAUSE` | `NEXT`), Row 2 (`VOL −` | `MUTE` | `VOL ＋`)
  - Điều khiển âm lượng remote PC thông qua Bluetooth HID Consumer Control (Report ID 3), không tác động AudioManager điện thoại
  - Cơ chế `DeckRepeatKey`:
    - Volume Up (`0x00E9`) & Volume Down (`0x00EA`): Chạm đơn gửi 1 bước (+2% trên host PC); giữ ngón tay lặp lại chu kỳ 100ms sau 350ms ban đầu
    - Mute (`0x00E2`): Kích hoạt 1 lần toggle duy nhất khi chạm, vô hiệu hóa lặp khi giữ (`enableRepeat = false`) để tránh bật tắt âm thanh mất kiểm soát
  - Bổ sung thanh trạng thái Telemetry thời gian thực hiển thị Action Name, Report ID 3, Usage Code HEX, và Transport Status (SUCCESS/ERROR)
- [x] Viết unit test `ConsumerControlMediaTest.kt` kiểm tra Report ID 3, Usage codes, gói tin release `[0x03, 0x00, 0x00]`, và telemetry
- [x] Chạy `./gradlew.bat testDebugUnitTest` pass 100% và `./gradlew.bat assembleDebug` thành công


