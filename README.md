# 📱 PocketHID — Android Universal Bluetooth HID Controller & Command Deck

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_9.0%2B_(API_28%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Version" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Bluetooth-HID_Device_Composite_(SDP_0xC8)-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white" alt="Bluetooth HID" />
  <img src="https://img.shields.io/badge/Testing-JUnit4_100%25_Pass-brightgreen?style=for-the-badge&logo=junit5&logoColor=white" alt="Tests" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-orange?style=for-the-badge" alt="License" />
</p>

---

## 💡 Giới thiệu (Overview)

**PocketHID** biến điện thoại thông minh Android của bạn thành một bộ điều khiển **Universal Bluetooth HID** đa năng 5-trong-1 không dây độ trễ cực thấp:
1. **⌨️ Smart Typing & Landscape Command Deck** (Bàn phím cơ ảo đa tầng kèm hàng phím số cố định & tích hợp Android IME).
2. **🖱️ Precision Multi-Touch Trackpad** (Bàn rê cảm ứng 1–4 ngón, cử chỉ Desktop Zoom và vùng cuộn nhanh mép phải).
3. **🎮 Driverless Bluetooth HID Gamepad** (Tay cầm chơi game chuẩn USB HID nhận diện trực tiếp trong Windows `joy.cpl`).
4. **📽️ Presenter & Laser Remote** (Điều khiển thuyết trình từ xa chuyên nghiệp).
5. **📱 One-Hand Web & Video Touch Remote** (Điều khiển duyệt web và xem video chỉ với một ngón cái).

### 🌟 Điểm nổi bật: Chuẩn phần cứng Bluetooth HID — Không cần Server / Client!
PocketHID kích hoạt trực tiếp **Bluetooth HID Device Profile** tiêu chuẩn của hệ điều hành Android (Composite Device: Keyboard + Mouse + Consumer Media + Gamepad):
- ❌ **Không cần cài bất kỳ phần mềm, server hay driver nào** trên máy tính chủ.
- ❌ **Không phụ thuộc vào mạng Wi-Fi hay cổng kết nối mạng nội bộ**.
- ✅ **Hoạt động trơn tru ngay tại màn hình đăng nhập Windows/macOS, màn hình khóa và trong môi trường BIOS/UEFI.**
- ✅ **Hỗ trợ đa nền tảng:** Windows, macOS, Linux, ChromeOS, Android TV, Raspberry Pi.

---

## 🕹️ 5 Chế Độ Điều Khiển Cốt Lõi (Core Modes)

```text
┌────────────────────────────────────────────────────────────────────────┐
│               KEYBOARD  │  MOUSE  │  GAMEPAD  │  PRESENTER  │  ONE-HAND│
└────────────────────────────────────────────────────────────────────────┘
```

---

### 1. 🎮 Driverless Bluetooth HID Gamepad (Chuẩn Windows `joy.cpl`)
PocketHID xuất ra bộ báo cáo tay cầm tiêu chuẩn quốc tế (**Report ID 4, 13 bytes**) qua Bluetooth SDP Subclass `0xC8` (Combo Keyboard/Mouse + Gamepad):
- **Windows Enumeration:** Máy tính Windows nhận diện PocketHID ngay lập tức trong bảng điều khiển **Game Controllers (`joy.cpl`)** và **Device Manager** dưới dạng thiết bị DirectInput tiêu chuẩn mà không cần phần mềm hay driver ảo.
- **11 Nút bấm kỹ thuật số (Digital Buttons):** `A`, `B`, `X`, `Y`, `LB`, `RB`, `Back/Select`, `Start`, `Guide/Home`, `L3` (nhấn cần trái), `R3` (nhấn cần phải).
- **8-Way D-Pad (Hat Switch):** Điều hướng 8 hướng chuẩn USB HID Generic Desktop Hat Switch (`1..8`, neutral `0`).
- **4 Trục Analog (Dual Thumbsticks):**
  - **Left Stick (X, Y):** Đóng gói trong Pointer Physical Collection, dải giá trị chuẩn `-32768 .. +32767`.
  - **Right Stick (Z, Rz):** Đóng gói trong Pointer Physical Collection, dải giá trị chuẩn `-32768 .. +32767`.
  - Hỗ trợ tùy chỉnh Deadzone, thuật toán phản hồi (Linear, Precision, Aggressive), Sensitivity và Invert Y.
- **2 Cò Analog (Analog Triggers):** `LT` (Rx) và `RT` (Ry) với dải giá trị tuyến tính `0 .. 255`.
- **Cơ chế chống kẹt phím (Anti-Stuck Protection):** Tự động phát báo cáo trung lập (`Neutral Report`) khi rời màn hình, pause app, hoặc ngắt kết nối Bluetooth.

---

### 2. 🖱️ Precision Multi-Touch Trackpad & Right Edge Fast Scroll Zone
Mặt cảm ứng đa điểm diện tích lớn, phản hồi tức thì với thuật toán chống xung đột cử chỉ (**Gesture Priority Lock**):

| Cử chỉ | Thao tác | Tác vụ điều khiển |
| :--- | :--- | :--- |
| **1 ngón** | **Di chuyển (Move)** | Rê chuột mượt mà (Đường cong gia tốc phi tuyến + Deadzone chống rung) |
| | **Chạm (Tap)** | Nhấp chuột trái (Left Click) |
| | **Chạm đúp (Double Tap)** | Nhấp đúp chuột trái (Double Left Click) |
| | **Chạm + Giữ (Tap + Hold)** | Khóa chuột trái để kéo thả (Drag Mode) |
| **2 ngón** | **Rê dọc / Rê ngang** | Cuộn trang dọc & cuộn ngang (hỗ trợ Natural Scroll trong Cài đặt) |
| | **Chạm (Tap)** | Nhấp chuột phải (Right Click) |
| | **Pinch Out (Bung 2 ngón)** | **Zoom In** (Phóng to màn hình qua `Ctrl + Wheel Up` hoặc `Cmd + Plus`) |
| | **Pinch In (Chụm 2 ngón)** | **Zoom Out** (Thu nhỏ màn hình qua `Ctrl + Wheel Down` hoặc `Cmd + Minus`) |
| **3 ngón** | **Chạm (Tap)** | Nhấp chuột giữa (Middle Click) |
| | **Vuốt Trái / Phải** | Chuyển đổi Desktop ảo: `Ctrl + Win + ◀` / `Ctrl + Win + ▶` |
| | **Vuốt Lên (Swipe Up)** | Mở Task View / Quản lý tác vụ: `Win + Tab` |
| | **Vuốt Xuống (Swipe Down)** | Thu nhỏ toàn bộ về màn hình chính: `Win + D` |
| **4 ngón** | **Chạm (Tap)** | Mở nhanh Command Deck / Quick Actions Palette |
| | **Vuốt Trái / Phải** | Chuyển đổi ứng dụng đang chạy: `Alt + Tab` / `Alt + Shift + Tab` |
| | **Vuốt Lên / Xuống** | Task Overview / Show Desktop |

- **⚡ Right Edge Fast Scroll Zone:** Dải cuộn nhanh dọc mép phải (chiếm 8–12% diện tích, min 32dp, max 64dp). Vuốt dọc mép phải cho tốc độ cuộn siêu tốc (1.5x–5.0x, mặc định 2.5x) mà không ảnh hưởng vùng di chuột chính. Khóa cử chỉ cô lập: chỉ kích hoạt khi chạm bắt đầu tại mép phải, tự động nhường quyền khi có thêm ngón tay hạ xuống.
- **Cụm phím chuột vật lý độc lập (LEFT / MID / RIGHT / LOCK):** Hỗ trợ đè nút chuột trái bằng một tay và dùng ngón tay khác rê trên Trackpad để kéo thả văn bản, di chuyển cửa sổ chính xác.

---

### 3. 📱 One-Hand Web & Video Touch Remote
Chế độ điều khiển từ xa tối ưu hóa cho **một ngón cái**, hỗ trợ tùy chọn tay thuận Trái/Phải:
- **🌐 Web Sub-mode:**
  - Vuốt ngón cái lên/xuống: Cuộn trang mượt mà bằng báo cáo chuột thật (`Mouse Wheel HID`).
  - Vuốt trái/phải: Quay lại trang (`Browser Back`) / Tiến trang (`Browser Forward`).
  - Chạm đơn: Nhấp chuột trái.
  - Thanh công cụ ngón cái: `Lùi`, `Tiến`, `Home`, `Tải lại (F5)`, `+ Tab mới`.
- **🎬 Video Sub-mode:**
  - Chạm đơn: Phát / Dừng video (`Consumer Control: PLAY_PAUSE`).
  - Vuốt dọc: Tăng / Giảm âm lượng hệ thống với chu kỳ giữ phím chuẩn 75ms (`VOLUME_UP` / `VOLUME_DOWN`).
  - Vuốt ngang: Tua lùi 10s / Tua tiến 10s (chạm giữ để tua liên tục an toàn).
  - Thanh công cụ ngón cái: `Bài trước`, `Lùi 10s`, `Phát/Dừng`, `Tiến 10s`, `Bài tiếp`, `Tắt tiếng (Mute)`.

---

### 4. ⌨️ Smart Typing Deck & Portrait Android IME Integration
- **Tích hợp bộ gõ Android IME trực tiếp (`PocketImeInputView`):** Gõ tiếng Việt (Telex/VNI), tiếng Anh trên các bàn phím Gboard, Samsung Keyboard mượt mà, trực tiếp phân giải `commitText`, `deleteSurroundingText`, `sendKeyEvent` sang chuỗi HID scancodes không độ trễ.
- **Hàng phím số cố định (Dedicated Number Row):** Nằm trực tiếp trên hàng chữ QWERTY:
  ```text
  `  1  2  3  4  5  6  7  8  9  0  -  =  ⌫ (Backspace)
  ```
- **Ký tự đặc biệt qua SHIFT:** Khi kích hoạt `SHIFT`, hàng phím tự động chuyển sang ký tự đặc biệt:
  ```text
  ~  !  @  #  $  %  ^  &  *  (  )  _  +  ⌫
  ```
- **Backspace Tap + Hold to Repeat:** Giữ phím > 350ms sẽ tự động kích hoạt chế độ lặp lại xóa liên tục kèm rung xúc giác (haptic tick).
- **Safe Clipboard Paste:** Dán văn bản từ clipboard điện thoại sang PC với tốc độ điều tiết an toàn (`pasteDelayMs`), chống tràn bộ đệm L2CAP HID buffer.

---

### 5. 🕹️ Landscape Command Deck & Multi-Layer Engine
Khi xoay ngang điện thoại, PocketHID chuyển sang giao diện điều khiển công thái học 3 vùng:
- **Zone 1 — Left Thumb Cluster:** Cụm phím bổ trợ Sticky/Locked Modifiers (`CTRL`, `ALT`, `SUPER`, `SHIFT`), phím `ESC`, `TAB`, Safe Paste và Micro D-Pad.
- **Zone 2 — Central Command Bay:**
  - **TYPE Layer:** Bàn phím QWERTY đầy đủ có hàng phím số cố định.
  - **SHORTCUTS Layer:** Bảng macro một chạm cho Clipboard, Window Management, Terminal (`^C`, `^Z`, `^L`).
  - **MEDIA Layer:** Điều khiển đa phương tiện độc lập qua Consumer Report ID 3 (Volume, Play/Pause, Seek ±5s, Mute).
  - **SYSTEM Layer:** Quản lý cửa sổ Snap (Left/Right/Maximize), Virtual Desktops, Snip Tool, Explorer.
  - **F-KEYS Layer:** Dãy phím chức năng từ `F1` đến `F12` cùng `PrtSc`, `Home`, `End`, `PgUp`, `PgDn`.
  - **NUMPAD Layer:** Bàn phím số kế toán tiêu chuẩn.
- **Zone 3 — Right Thumb Cluster:** Phím `ENTER`, `BKSP` nổi bật và cụm phím điều hướng 4 chiều công thái học.
- **Presenter & Laser Pointer Mode:** Chế độ thuyết trình chuyên dụng (Next/Prev slide, Black screen, Laser pointer).
- **Command Palette Sheet (`Ctrl+K`):** Tìm kiếm và kích hoạt nhanh mọi lệnh điều khiển trong tích tắc.

---

### 6. 🔍 Bảng Chẩn Đoán & Kiểm Thử Toàn Diện (Diagnostics HUD)
Mở nhanh qua icon chẩn đoán hoặc cử chỉ 4 ngón, chia thành 4 tab trực quan:
- **`[ SYSTEM ]`:** Xác thực Bluetooth HID, độ trễ truyền tin, tốc độ lấy mẫu Polling rate, và telemetry giải mã bộ gõ IME.
- **`[ GAMEPAD ]`:** Bảng kiểm thử tay cầm trực tiếp:
  - Ma trận 11 nút số + D-pad sáng đèn khi chạm.
  - 2D Stick Visual Crosshair (lưới canvas 2D hiển thị chấm vị trí cần gạt thời gian thực).
  - Thanh áp lực cò analog LT/RT (chuẩn hóa `0.00 .. 1.00`).
  - Màn hình soi byte thô: `Bytes: [ 04 XX XX XX XX XX XX XX XX XX XX XX XX ]`.
  - Cờ trạng thái: `NEUTRAL REPORT: YES/NO`.
  - Bộ đếm Latch Multi-touch (Down transitions, Up transitions, phát hiện nút bấm bị kẹt).
- **`[ MEDIA ]`:** 7 nút test âm lượng/media với chu kỳ giữ phím 75ms và hiển thị phản hồi trạng thái từ `ConsumerDiagnosticsHub`.
- **`[ CHECKLIST ]`:** Quy trình chuẩn xác thực thiết bị trên Windows `joy.cpl` và Device Manager.

---

### 7. ⚡ Semantic Action Engine & Kiến Trúc Đa Nền Tảng
Hệ thống giải mã hành động trung tâm (`PocketAction`, `ActionResolver`, `ActionDispatcher`, `ActionRegistry`):
- Tự động nhận biết và điều chỉnh phím tắt theo hệ điều hành đích: **Windows**, **macOS**, và **Linux** (ví dụ: Copy trên Windows là `Ctrl+C`, trên macOS tự động chuyển sang `Cmd+C`).
- Chuẩn hóa toàn bộ cử chỉ trackpad, phím bấm trình chiếu và macro mà không gắn chặt vào scancode phần cứng.

---

### 8. 🔋 Quản Lý Năng Lượng & Ngủ Tự Nhiên (Power Policy)
- **Không bao giờ ép khóa máy:** PocketHID tuyệt đối không gọi các hàm API khóa máy cưỡng bức.
- **Timeout ngủ tự nhiên:** Tự động giữ sáng màn hình khi đang thao tác; sau **10 phút** không có tương tác, màn hình tự động tắt/ngủ tự nhiên theo cài đặt hệ thống.
- **Foreground Service bền bỉ:** Kết nối Bluetooth HID tiếp tục duy trì thông suốt ở chế độ nền khi màn hình tắt.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

| Thành phần | Công nghệ |
| :--- | :--- |
| **Ngôn ngữ** | Kotlin 100% (Kotlin 2.0.21) |
| **UI Framework** | Jetpack Compose BOM 2024.12.01, Material Design 3 |
| **Kiến trúc** | Clean Modular Architecture, MVI StateFlow, Semantic Action Engine |
| **Bluetooth HID** | Android Bluetooth HID Device API (`BluetoothHidDevice`, Composite SDP `0xC8`) |
| **Kiểm thử tự động** | JUnit 4 (100% Pass: `GamepadReportDescriptorTest`, `GamepadMathTest`, `ActionResolverTest`, `ScreenWakePolicyTest`, `FastScrollControllerTest`, `GestureInterpreterTest`, `KeyMapperTest`) |
| **Yêu cầu hệ thống** | Android 9.0 (API 28) trở lên |

---

## 📦 Hướng dẫn cài đặt & Biên dịch (Installation & Build)

### 1. Yêu cầu môi trường
- Máy tính đã cài đặt **JDK 17** hoặc **JDK 21**.
- Android SDK (API 34/35).

### 2. Biên dịch từ mã nguồn
```bash
# 1. Clone repository
git clone https://github.com/caonguyenthanhan/PocketHID.git
cd PocketHID

# 2. Chạy kiểm thử tự động (Unit Tests)
./gradlew testDebugUnitTest
# Trên Windows:
.\gradlew.bat testDebugUnitTest

# 3. Biên dịch bản cài đặt Debug APK
./gradlew assembleDebug
# Trên Windows:
.\gradlew.bat assembleDebug
```
File APK cài đặt sau khi build thành công nằm tại:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 Hướng dẫn kết nối với máy tính (How to Connect)

1. **Bật Bluetooth** trên cả điện thoại và máy tính.
2. Mở ứng dụng **PocketHID** trên điện thoại và cấp quyền Bluetooth (trên Android 12+, cấp quyền `Nearby Devices`).
3. **Ghép đôi lần đầu:**
   - Trong PocketHID, mở thẻ ghép đôi và nhấn **"Bật chế độ tìm kiếm (Make Discoverable)"**.
   - Trên máy tính: Vào **Settings** ➔ **Bluetooth & Devices** ➔ **Add Device** ➔ Chọn điện thoại của bạn và xác nhận mã PIN.
4. **Kiểm tra Gamepad trên Windows:**
   - Nhấn `Win + R` ➔ Gõ `joy.cpl` ➔ Nhấn Enter.
   - PocketHID sẽ xuất hiện trong danh sách Game Controllers đã cài đặt.
   - Nhấn **Properties** ➔ **Test** để trải nghiệm đầy đủ các nút bấm, D-pad và cần xoay analog!

---

## 📄 Bản quyền (License)

Dự án được phát hành theo giấy phép **Apache License 2.0**.

<p align="center">
  Phát triển với ❤️ bởi <b>Cao Nguyễn Thành An</b>
</p>
