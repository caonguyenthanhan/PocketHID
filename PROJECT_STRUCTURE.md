# PROJECT STRUCTURE — PocketHID

Kiến trúc thư mục và quy chuẩn tổ chức mã nguồn của ứng dụng **PocketHID** (Universal Bluetooth HID Command Deck).
Tuân thủ tiêu chuẩn `project-structure` trong hệ sinh thái đa agent.

---

## 1. Cây Thư Mục Tổng Quan

```text
PocketHID/
├── .agent/                             # Quản trị đa agent (governance, sessions, reports, envelopes)
│   ├── envelopes/                      # Task envelopes độc lập (template v1.1)
│   ├── feedback-src/                   # Báo cáo phản hồi từ người dùng
│   ├── reports/                        # Báo cáo phân tích (codemap, review, digest)
│   └── sessions/                       # File trạng thái phiên (master-state, worker-state)
├── app/
│   ├── build.gradle.kts                # Cấu hình build Android (AGP 8.9.2, Compose, Dependencies)
│   ├── proguard-rules.pro              # Quy tắc obfuscation & keep rules cho Bluetooth HID
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # Khai báo permissions (Bluetooth, Foreground Service)
│       │   └── java/dev/aleian/pockethid/
│       │       ├── MainActivity.kt     # UI Entrypoint, xin quyền, Service binding
│       │       ├── action/             # Semantic Action Engine đa nền tảng (OS-Aware)
│       │       │   ├── ActionDispatcher.kt     # Bộ điều phối thực thi kế hoạch lên InputTransport
│       │       │   ├── ActionExecutionPlan.kt  # Kế hoạch thực thi cấp thấp (Key, Consumer, Mouse, Pad)
│       │       │   ├── ActionRegistry.kt       # Danh mục các hành động ngữ nghĩa kèm metadata
│       │       │   ├── ActionResolver.kt       # Bộ chuyển đổi ánh xạ scancode theo OS (Win/Mac/Linux)
│       │       │   ├── HostOs.kt               # Enum hệ điều hành đích (WINDOWS, MACOS, LINUX)
│       │       │   └── PocketAction.kt         # Cây hành động ngữ nghĩa có thứ bậc
│       │       ├── gamepad/            # Module điều khiển tay cầm chơi game chuẩn HID
│       │       │   ├── ConsumerDiagnosticsHub.kt # Telemetry giám sát phím Media Consumer Control
│       │       │   ├── GamepadController.kt    # Bộ điều phối trạng thái phím, haptic và report 13 bytes
│       │       │   ├── GamepadMath.kt          # Thuật toán Deadzone, Response Curves, Sensitivity
│       │       │   └── GamepadState.kt         # GamepadTelemetry & GamepadDiagnosticsHub (Latch counters)
│       │       ├── mapping/            # Logic chuyển đổi ký tự & cử chỉ chuột
│       │       │   ├── FastScrollController.kt # Bộ điều khiển vùng cuộn nhanh mép phải trackpad
│       │       │   ├── GestureInterpreter.kt   # Bộ máy nhận diện multi-touch 1-4 ngón, gia tốc, cuộn, zoom
│       │       │   ├── KeyMapper.kt            # Ánh xạ Char/KeyCode -> HID Scancode & Modifiers
│       │       │   └── TextInputResolver.kt    # Giải mã văn bản từ Android IME sang chuỗi HID keys
│       │       ├── model/              # Trạng thái dữ liệu, hằng số HID, cấu hình
│       │       │   ├── AppSettings.kt          # Data class cấu hình hệ thống
│       │       │   ├── ConnectionState.kt      # Sealed class trạng thái kết nối Bluetooth
│       │       │   ├── HidConstants.kt         # Byte codes, Report Descriptors (Combo 4-in-1, Report ID 1-4)
│       │       │   └── SettingsRepository.kt   # Quản lý SharedPreferences StateFlow
│       │       ├── power/              # Chính sách quản lý năng lượng và màn hình
│       │       │   └── ScreenWakePolicy.kt     # Quản lý FLAG_KEEP_SCREEN_ON & timeout ngủ tự nhiên
│       │       ├── service/            # Android Foreground Service duy trì kết nối nền
│       │       │   └── HidDeviceService.kt     # Foreground Service loại connectedDevice
│       │       ├── transport/          # Tầng truyền dẫn Bluetooth HID
│       │       │   ├── BtHidTransport.kt       # Triển khai BluetoothHidDevice profile API (SDP 0xC8)
│       │       │   └── InputTransport.kt       # Interface chuẩn hóa gửi phím, chuột, media, gamepad
│       │       └── ui/                 # Giao diện Jetpack Compose (Command Deck)
│       │           ├── components/     # UI Components tái sử dụng
│       │           │   ├── ConnectionBar.kt        # Thanh trạng thái kết nối
│       │           │   ├── DeckComponents.kt       # Phím bấm, layout grid, thumb modifier
│       │           │   ├── DedicatedNumberRow.kt   # Hàng phím số cố định + Shift + Repeat Backspace
│       │           │   ├── MultiTouchTrackpad.kt   # Bàn rê đa điểm 1-4 ngón + Fast Scroll Zone
│       │           │   ├── PocketImeInputView.kt   # Custom InputConnection đón nhận Android IME trực tiếp
│       │           │   ├── VirtualThumbstick.kt    # Cần analog ảo 2D với phản hồi vị trí thời gian thực
│       │           │   ├── VirtualFaceButtons.kt   # Cụm nút bấm A/B/X/Y công thái học
│       │           │   ├── VirtualDpad.kt          # D-Pad 8 hướng chuẩn Hat switch
│       │           │   └── VirtualShoulderCluster.kt # Cụm cò LB/RB và LT/RT analog
│       │           ├── screens/        # Các màn hình điều khiển chính
│       │           │   ├── MainScreen.kt           # Màn hình mẹ điều hướng 5 modes
│       │           │   ├── KeyboardScreen.kt       # Bàn phím đa tầng (Portrait / Type)
│       │           │   ├── MouseScreen.kt          # Chuột / Trackpad portrait
│       │           │   ├── GamepadScreen.kt        # Màn hình tay cầm chơi game chuyên nghiệp
│       │           │   ├── OneHandScreen.kt        # Touch remote 1 tay (Web & Video modes)
│       │           │   ├── PresenterScreen.kt      # Điều khiển trình chiếu & Laser mode
│       │           │   ├── LandscapeDeckScreen.kt  # Bàn phím cơ xoay ngang 3-zone thumb-first
│       │           │   ├── CommandPaletteSheet.kt  # Bảng phím tắt nhanh Quick Actions
│       │           │   ├── SettingsScreen.kt       # Cài đặt độ nhạy, haptics, deadzone
│       │           │   ├── SettingsSections.kt     # Phân rã modular các panel cài đặt (chống JVM 64KB)
│       │           │   ├── PairingSheet.kt         # Quản lý ghép đôi thiết bị Bluetooth
│       │           │   ├── DiagnosticsSheet.kt     # HUD chẩn đoán tabbed: System, Gamepad, Media, Checklist
│       │           │   ├── deck/                   # Sub-components của Landscape Deck
│       │           │   └── diagnostics/            # Sub-components của Diagnostics HUD
│       │           │       ├── EnumerationStatusCard.kt # Xác thực SDP 0xC8, Collections, Report ID 4
│       │           │       ├── GamepadTestPanel.kt      # Test 11 buttons, 2D sticks, raw hex, neutral, latch
│       │           │       ├── MediaConsumerTestPanel.kt # Test 7 phím Media với 75ms hold cycle
│       │           │       └── HostChecklistPanel.kt    # Quy trình kiểm thử Windows joy.cpl
│       │           └── theme/          # Bảng màu tối công nghệ, typography, shape tokens
│       │               ├── Color.kt
│       │               ├── Theme.kt
│       │               └── Type.kt
│       └── test/java/dev/aleian/pockethid/ # Bộ Kiểm Thử Tự Động Toàn Diện (Unit Tests)
│           ├── action/                 # Test ActionResolver (Windows/Mac/Linux) & ActionDispatcher
│           ├── gamepad/                # Test GamepadMath & GamepadReportDescriptorTest (13-byte spec)
│           ├── mapping/                # Test GestureInterpreter, KeyMapper, FastScrollController
│           └── power/                  # Test ScreenWakePolicy (10-phút timeout, không khóa máy)
├── gradle/                             # Gradle Wrapper
├── build.gradle.kts                    # Root build script
├── settings.gradle.kts                 # Cấu hình repository & modules
├── DECISIONS.md                        # Nhật ký quyết định kiến trúc dự án (ADR)
├── CONTEXT_MANIFEST.md                 # Quản lý ngữ cảnh nạp vào context LLM
└── todo.md                             # Board theo dõi tiến độ phát triển
```

---

## 2. Quy Ước Thiết Kế & Nguyên Tắc Độc Lập

1. **Tách biệt giữa Giao diện và Truyền dẫn:** Mọi màn hình UI chỉ giao tiếp với tầng kết nối qua interface `InputTransport`, không gọi trực tiếp vào lớp Android Bluetooth nội bộ.
2. **Quy tắc Cử chỉ Không xung đột (Gesture Priority Lock):** Bộ máy `GestureInterpreter` luôn ưu tiên từ 4 ngón -> 3 ngón -> 2 ngón -> 1 ngón, khóa sequence cảm ứng để cử chỉ đa ngón không kích hoạt nhầm thao tác di chuyển chuột hoặc click đơn ngón.
3. **Dedicated Number Row:** Bàn phím ảo luôn duy trì hàng phím số chuyên dụng phía trên hàng chữ QWERTY cả khi xoay ngang lẫn thẳng đứng, hỗ trợ ký tự Shifted và phím Backspace tự động lặp lại khi ấn giữ.
