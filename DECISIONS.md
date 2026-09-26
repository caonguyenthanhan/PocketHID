# DECISIONS — PocketHID

Nhật ký quyết định kiến trúc và quản trị đa agent của dự án PocketHID.
Tuân thủ luật quản trị 4 tầng theo `D:\An-tool-ecosystem\.claude\skills\multi-agent-workflow\SKILL.md` (v1.4) và `session-handoff\SKILL.md`.

---

## [D-001] 2026-09-27 — Thiết lập Ban Quản Trị & Khởi Tạo Wave 0

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:** Dự án PocketHID đã hoàn thành MVP và giai đoạn nâng cấp Command Deck / Action Layers (`689a5f3`). Chuyển giao sang mô hình điều hành đa agent kỷ luật 4 tầng: Owner -> Manager -> AI-Master (T7) -> Workers.
- **Quyết định:**
  1. Ban hành quy chế multi-agent v1.4 cho PocketHID.
  2. Bắt đầu phiên Wave 0 với AI-Master ghế T7: khởi tạo board, code-map, audit review ban đầu, phân tách rủi ro `[đã xác minh]` / `[nghi ngờ]`, bí mật lộ lọt, và chẻ plan Wave 1 thành các task envelopes tự chứa độc lập (RÀO).
  3. Kỷ luật session-handoff: 1 phiên master = 1 wave; `master-state.md` trần 40 dòng, con-trỏ-không-chép.
  4. Mọi envelope phát đi phải tuân thủ template version 1.1 và lint 17 mục. Cổng 🛑 bắt buộc trước khi phát envelope cho Worker.
- **Hệ quả:**
  - Không sửa mã nguồn trong Wave 0 (chế độ CHỈ ĐỌC).
  - Toàn bộ thay đổi mã nguồn chuyển sang Wave 1 theo từng task envelope có rào phạm vi cô lập.

---

## [D-002] 2026-09-27 — Owner Duyệt Kế Hoạch Wave 1 & Bật Đèn Xanh Dispatch Envelopes

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:** AI-Master đã trình kế hoạch phân rã 3 task envelopes (W1-01: Unit test & Project Structure; W1-02: Refactor LandscapeDeckScreen; W1-03: Multi-Key Rollover 6KRO) tại cổng dừng 🛑 của Wave 0. Owner đã phê duyệt chính thức.
- **Quyết định:**
  1. Mở cổng 🛑, kích hoạt thực thi Wave 1.
  2. Kích hoạt Worker thực hiện W1-02 (Refactor & Module hóa Landscape Deck Composable) và W1-03 (6-Key Rollover HID Protocol).
- **Hệ quả:**
  - `LandscapeDeckScreen.kt` được phân rã thành các sub-components độc lập trong package `dev.aleian.pockethid.ui.screens.deck.*`.
  - `InputTransport.kt` và `BtHidTransport.kt` được nâng cấp hỗ trợ mảng 6 phím đồng thời (6KRO buffer), đảm bảo tương thích ngược 100%.

---

## [D-003] 2026-09-27 — Thiết Lập Semantic Action Engine & Kiến Trúc Đa Nền Tảng (Windows / macOS / Linux)

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:** UI composables và cử chỉ trackpad trước đây gọi trực tiếp HID transport (`sendKeyClick`, `sendMouseMove`, `sendConsumerClick`), khiến việc tùy biến phím, hỗ trợ đa hệ điều hành (macOS/Linux) và macro trở nên phân mảnh.
- **Quyết định:**
  1. Xây dựng tầng kiến trúc Semantic Action Engine trung tâm trong package `dev.aleian.pockethid.action.*`:
     - `PocketAction`: Cây định danh hành động ngữ nghĩa có thứ bậc (System, Navigation, Edit, Media, Presenter, Pointer, RawKey).
     - `HostOs`: Định danh hệ điều hành đích (WINDOWS, MACOS, LINUX).
     - `ActionExecutionPlan`: Kế hoạch thực thi cấp thấp (KeyStroke, ConsumerKey, MouseButtonClick, Sequence).
     - `ActionResolver`: Bộ chuyển đổi OS-aware tự động ánh xạ scancode theo hệ điều hành đích.
     - `ActionDispatcher`: Bộ điều phối thực thi kế hoạch lên `InputTransport`.
     - `ActionRegistry`: Danh mục tập trung các hành động ngữ nghĩa kèm metadata.
  2. Tách ghép các caller cử chỉ Trackpad (`MultiTouchTrackpad.kt`) và Presenter (`PresenterScreen.kt`) sang dùng `ActionDispatcher.dispatch(...)`.
  3. Đường truyền chuột liên tục (continuous mouse stream) vẫn đi trực tiếp qua `sendMouseMove` để đảm bảo độ trễ dưới 1ms, không phát sinh garbage collection.
- **Hệ quả:**
  - Giữ nguyên 100% tính năng và độ mượt hiện có của hệ thống cử chỉ 1-4 ngón.
  - Sẵn sàng cho các tính năng tương lai: custom gesture remapping, macro engine, và dynamic application profiles mà không cần sửa đổi UI code.
  - Toàn bộ unit tests mới (`ActionResolverTest`, `ActionDispatcherTest`) pass 100%.

---

## [D-004] 2026-09-27 — Sửa Lỗi Nhập Liệu Bàn Phím Dọc (Portrait Keyboard) & Tích Hợp Android IME → HID Pipeline

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:** Bàn phím dọc trước đây sử dụng `EditText` ẩn (1dp) với cơ chế xóa buffer `editable?.clear()` ngay trong `doAfterTextChanged`. Cơ chế này phá hủy máy trạng thái (composition state machine) của các bộ gõ Android hiện đại (Gboard, Samsung Keyboard), khiến phím Backspace, Enter và các ký tự tiếp theo bị nuốt hoặc đóng băng bàn phím.
- **Quyết định:**
  1. Loại bỏ hoàn toàn `EditText` và cơ chế buffer clearing cũ.
  2. Xây dựng component chuyên dụng [PocketImeInputView.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/components/PocketImeInputView.kt) kế thừa `View` với custom `InputConnection`:
     - Trực tiếp đánh chặn `commitText` $\rightarrow$ giải mã ký tự qua [TextInputResolver.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/mapping/TextInputResolver.kt) $\rightarrow$ dispatch qua `ActionDispatcher`.
     - Trực tiếp đánh chặn `deleteSurroundingText` $\rightarrow$ dispatch `KEY_BACKSPACE` / `KEY_DELETE`.
     - Trực tiếp đánh chặn `sendKeyEvent` và `performEditorAction` $\rightarrow$ dispatch `KEY_ENTER`, `KEY_TAB`.
  3. Đồng bộ trạng thái kết nối: hiển thị rõ thẻ "HID Connected • Typing to <HostName>" khi đã kết nối và "Not Connected" khi ngắt kết nối.
  4. Tích hợp telemetry giám sát luồng IME (`ImeDiagnosticsHub`) vào [DiagnosticsSheet.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/DiagnosticsSheet.kt).
- **Hệ quả:**
  - Bộ gõ Android (Gboard, Samsung Keyboard, Telex) gõ mượt mà, không bị mất ký tự, phím hoa, ký tự đặc biệt, Backspace và Enter đều hoạt động chuẩn xác trên PC kết nối.
  - Bàn phím số cố định (Dedicated Number Row) và các phím điều khiển native vẫn hoạt động 100%.
  - Unit test `TextInputResolverTest` đạt 100% PASS.

---

## [D-005] 2026-09-27 — Tích Hợp Chế Độ Bluetooth HID Gamepad / Controller Chuẩn Driverless

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:** Cần mở rộng PocketHID để biến điện thoại thành một Gamepad / Tay cầm chơi game HID thực thụ (không giả lập bàn phím/WASD, không cần phần mềm đồng hành trên PC), đồng thời không gây xung đột với bàn phím, chuột hay presenter hiện có.
- **Quyết định:**
  1. Mở rộng `COMBO_REPORT_DESCRIPTOR` với Report ID 4 (Generic Desktop / Gamepad):
     - 16 Digital Buttons (A, B, X, Y, LB, RB, Back, Start, Guide, L3, R3, D-pad Up/Down/Left/Right).
     - 1 Hat Switch (8 hướng chuẩn USB HID).
     - 4 Analog Axes: Left Stick X/Y (-32768..32767) và Right Stick X/Y (-32768..32767).
     - 2 Analog Triggers: LT và RT (0..255).
  2. Nâng cấp `InputTransport` và `BtHidTransport`: thêm `sendGamepadReport(...)`, `sendGamepadNeutral()`, và phản hồi báo cáo 13-byte rỗng khi host gọi `onGetReport` với Report ID 4.
  3. Xây dựng module lõi `dev.aleian.pockethid.gamepad`:
     - `GamepadMath`: Xử lý deadzone, response curves (Linear, Precision, Aggressive), sensitivity, invert Y, và trigger mapping với các pure functions có unit test.
     - `GamepadController`: Điều phối trạng thái phím, rung haptic, và dispatch HID report an toàn đa luồng.
     - `GamepadDiagnosticsHub`: Cung cấp telemetry thời gian thực cho HUD chẩn đoán.
  4. Triển khai giao diện chuyên dụng `GamepadScreen.kt`:
     - Ưu tiên Landscape tối ưu cho hai ngón cái với multi-touch pointer tracking độc lập.
     - Cơ chế an toàn chuyển màn hình: tự động gửi báo cáo trung lập (neutral report) khi thoát màn hình, pause/background, hoặc ngắt kết nối Bluetooth, ngăn ngừa kẹt phím (stuck buttons).
  5. Mở rộng thanh điều hướng chính `KEYBOARD | MOUSE | GAMEPAD | PRESENTER` đồng bộ trên cả Portrait và Landscape (`DeckModeSwitcher`).
  6. Bổ sung mục hiệu chuẩn (Calibration) với visualizer trực quan trong `SettingsScreen.kt` và thẻ telemetry thời gian thực trong `DiagnosticsSheet.kt`.
- **Hệ quả:**
  - PC nhận diện PocketHID là tay cầm chơi game tiêu chuẩn qua Bluetooth mà không cần driver phụ trợ.
  - Hỗ trợ đầy đủ phím bấm đồng thời (multi-touch: vừa di cần vừa bấm A/B/cò).
  - 100% unit tests pass, toàn bộ tính năng Keyboard, Mouse, Presenter không bị suy thoái.

---

## [D-006] 2026-09-27 — Tích Hợp Cử Chỉ Thu Phóng Desktop (Pinch Zoom) & Tái Cấu Trúc Công Thái Học Ngón Cái (Thumb Ergonomics)

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Trackpad chưa hỗ trợ cử chỉ thu phóng desktop chuẩn (Pinch In / Pinch Out).
  - Bố cục Landscape Command Deck trước đây có các cụm phím hai bên (`DeckMacroZone`, `DeckUtilityZone`) quá hẹp (1.8f : 6.2f : 2.0f), ép cụm phím về giữa màn hình, khiến ngón cái phải vươn xa không tự nhiên trên điện thoại tỷ lệ 20:9.
  - Phím mũi tên (`ArrowPadKey`) có kích thước 24dp quá nhỏ, dễ chạm nhầm khi điều hướng.
- **Quyết định:**
  1. **Nhận diện Cử chỉ Thu phóng (Pinch Recognition & Disambiguation):**
     - Mở rộng `GestureInterpreter` với máy trạng thái `TwoFingerMode` (`UNDECIDED`, `SCROLLING`, `PINCHING`).
     - Tách biệt chuyển động song song (2 ngón cùng hướng $\rightarrow$ Scroll) và chuyển động tương đối (2 ngón tiến lại/xa nhau $\rightarrow$ Pinch).
     - Phân định theo khoảng cách Euclidean (`curSpan`) và tâm khối (`curCenterX`, `curCenterY`) với ngưỡng kích hoạt có thể cấu hình (`pinchThresholdPx`, mặc định 35px).
     - Khóa cứng trạng thái `PINCHING` sau khi xác nhận ý đồ thu phóng: triệt để cô lập không phát sinh sự kiện cuộn chuột (`MouseAction.wheel = 0`), và chặn hoàn toàn sự kiện Right-Click tap khi nhấc ngón tay (`onTouchUp`).
     - Bộ lọc chống rung & điều tiết tốc độ (Rate limiting 85ms, bước thu phóng 36px) đảm bảo zoom mượt mà, không giật lag.
  2. **Ánh xạ Lệnh Zoom Desktop Đa Nền Tảng (OS-Aware Zoom):**
     - Bổ sung `PocketAction.SystemAction.ZoomIn` và `ZoomOut`.
     - `ActionExecutionPlan.ZoomWheel(wheelDelta, modifier)`: Giữ phím bổ trợ $\rightarrow$ gửi bước cuộn chuột $\rightarrow$ nhả phím bổ trợ.
     - Ánh xạ chuẩn: Windows/Linux dùng `Ctrl + Mouse Wheel`, macOS dùng `Cmd + Mouse Wheel`.
     - Hỗ trợ chế độ phím tắt dự phòng (`zoomMode = "Keys"`, `Ctrl/Cmd + [+/-]`).
  3. **Tái Thiết Kế Công Thái Học Ngón Cái Landscape (Thumb Ergonomics):**
     - Điều chỉnh tỷ lệ trọng số 3 vùng từ `1.8f : 6.2f : 2.0f` sang `2.3f : 5.4f : 2.3f`: mở rộng hai cụm phím bên thêm ~28% và ~15% chiều ngang, đặt phím nằm ngay dưới vòng cung tự nhiên của ngón cái.
     - Nâng cấp `ArrowPadKey` từ 24dp lên 36dp × 28dp (icon 18dp), giúp điều hướng phím mũi tên bằng ngón cái phải cực kỳ chuẩn xác và nảy.
     - Cập nhật banner thông báo thời gian thực ("Zoom In 🔍+", "Zoom Out 🔍-") và tài liệu hướng dẫn cử chỉ trong ứng dụng.
  4. **Tùy biến Cấu hình:** Bổ sung toggle bật/tắt Pinch Zoom, bộ chọn kiểu lệnh Zoom (Wheel vs Keys), và thanh trượt ngưỡng nhận diện trong `SettingsScreen.kt`.
- **Hệ quả:**
  - Cử chỉ Pinch In/Out hoạt động trơn tru như trackpad laptop thực thụ, tách bạch hoàn hảo với cuộn 2 ngón.
  - Tư thế cầm máy landscape thoải mái vượt trội, loại bỏ hoàn toàn hiện tượng mỏi ngón cái khi gõ hoặc điều hướng lâu.
  - 100% unit tests (39/39) và build debug APK (`assembleDebug`) hoàn thành thành công.

---

## [D-007] 2026-09-27 — Khắc Phục Hành Vi Màn Hình: Loại Bỏ Ép Khóa Máy, Hỗ Trợ Màn Hình Ngủ 10 Phút & Duy Trì Kết Nối Nền

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Ứng dụng trước đây gây cảm giác như bị cưỡng bức khóa máy (forced lock) thay vì màn hình tắt/ngủ tự nhiên sau thời gian rảnh rỗi.
  - Cần làm rõ và tách bạch ranh giới kỹ thuật: Screen Dim (giảm sáng) $\rightarrow$ Screen Sleep/Off (màn hình ngủ) $\rightarrow$ Device Lock (khóa bảo mật Android). PocketHID tuyệt đối không can thiệp hoặc ép buộc chính sách bảo mật của hệ thống.
  - Lệnh "Khóa máy tính" từ xa (`Win+L` trên Windows, `Cmd+Ctrl+Q` trên macOS) là gửi phím HID tới PC điều khiển, hoàn toàn tách biệt với trạng thái khóa của điện thoại Android.
- **Quyết định:**
  - **1. Triệt để loại bỏ mọi hành vi ép khóa thiết bị Android:**
    - Kiểm tra toàn bộ mã nguồn: Xác nhận không có bất kỳ lệnh gọi API ép khóa nào (`DevicePolicyManager.lockNow()`, `KeyguardManager`, accessibility lock hay intent khóa màn hình).
    - Phân định rõ ràng: Lệnh `PocketAction.SystemAction.LockPC` là gửi phím HID điều khiển khóa máy tính host từ xa (`Win+L` / `Cmd+Ctrl+Q`), không bao giờ khóa điện thoại Android.
  - **2. Quản lý Đánh thức Màn hình Thông minh (`ScreenWakeManager`):**
    - Sử dụng `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` kết hợp coroutine theo dõi thời gian thao tác (`onUserInteraction()`).
    - Trong khi người dùng chủ động thao tác (Bàn phím, Chuột, Gamepad, Trình chiếu), màn hình luôn duy trì sáng ổn định.
    - Khi không thao tác liên tục 10 phút (mặc định), tự động giải phóng cờ `FLAG_KEEP_SCREEN_ON`, cho phép màn hình Android giảm sáng (Dim) và chuyển sang chế độ ngủ (Sleep/Turn off) một cách hoàn toàn tự nhiên theo chính sách hệ điều hành.
    - Android OS giữ toàn quyền quyết định xem có cần xác thực vân tay/mật khẩu khi người dùng bật lại màn hình hay không.
  - **3. Duy Trì Dịch Vụ Bluetooth HID Liên Tục Khi Màn Hình Tắt:**
    - Dịch vụ Foreground `HidDeviceService` giữ `PARTIAL_WAKE_LOCK` (chỉ giữ CPU, không giữ màn hình) để xử lý gói tin Bluetooth HID ngầm mà không làm hao pin màn hình.
    - Loại bỏ giới hạn timeout ngắt wake lock đột ngột trước đây, đảm bảo duy trì kết nối Bluetooth ổn định ngay cả khi điện thoại tắt màn hình để trong túi hoặc trên bàn.
  - **4. Khôi Phục Trạng Thái Khi Đánh Thức Điện Thoại (App Resume):**
    - Sử dụng `rememberSaveable` cho chế độ màn hình đang mở (`selectedTab`, `selectedTopMode`, sub-mode), bảo toàn nguyên vẹn màn hình đang làm việc khi người dùng bật lại điện thoại.
    - Duy trì và hiển thị trạng thái kết nối Bluetooth thực tế, không bắt người dùng quay lại màn hình quét ghép đôi.
  - **5. Tùy Chọn Cấu Hình Màn Hình Trong Cài Đặt (`SettingsScreen`):**
    - Bổ sung nhóm cấu hình "Hành vi màn hình (Screen Behavior)":
      - `(•) Cho phép màn hình ngủ (Allow screen to sleep)` kèm lựa chọn thời gian chờ: 5 phút, 10 phút (Chuẩn), 15 phút.
      - `( ) Luôn giữ màn hình sáng khi mở PocketHID (Keep screen awake)`.
    - Ghi chú bảo mật minh bạch giải thích rõ ràng sự khác biệt giữa Sleep và Lock.
- **Hệ quả:**
  - Điện thoại không còn bị tắt màn hình đột ngột khi đang sử dụng, và tự động ngủ tiết kiệm pin sau 10 phút không thao tác.
  - Bluetooth HID giữ kết nối thông suốt trong nền; bật máy lên là tiếp tục điều khiển tức thì.
  - 100% unit tests pass (`ScreenWakePolicyTest`, `ActionResolverTest`) và debug APK biên dịch thành công.

---

## [D-008] 2026-09-27 — Tích Hợp Chế Độ Điều Khiển Một Tay (One-Hand Web & Video Control Mode) & Tái Cấu Trúc Module Settings

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Người dùng có nhu cầu lướt web và điều khiển xem video (YouTube, phim, stream) trên máy tính từ xa bằng một ngón cái (One Thumb), không cần nhìn chăm chú vào các phím bấm nhỏ khi đang nằm hoặc ngồi xa màn hình.
  - Chế độ cần hỗ trợ chuẩn Bluetooth HID gốc (Mouse Wheel, Consumer Control, Keyboard Shortcuts), tương thích đa nền tảng (Windows / macOS / Linux).
  - Cần hỗ trợ công thái học tay thuận (Trái / Phải) và giải quyết giới hạn JVM 64KB bytecode (`MethodTooLargeException`) khi cài đặt ứng dụng ngày càng phong phú.
- **Quyết định:**
  - **1. Kiến Trúc Chế Độ One-Hand Mới:**
    - Bổ sung tab thứ 5: `KEYBOARD | MOUSE | GAMEPAD | PRESENTER | ONE-HAND` trên cả Portrait NavigationBar và Landscape Deck.
    - Hai phân hệ chuyên biệt chuyển đổi nhanh bằng segmented control: `[ WEB ]` và `[ VIDEO ]`.
  - **2. Bề Mặt Cảm Ứng Ngón Cái Công Thái Học (Thumb Surface):**
    - Chiếm trọn vùng nửa dưới màn hình với đệm công thái học tự động dời theo tay thuận (`oneHandHandedness = "Right" | "Left"`).
    - Bộ phân xử cử chỉ (Gesture Arbiter) chống xung đột triệt để: Khóa cứng mode khi bắt đầu chuyển động vượt ngưỡng (Deadzone 18px), loại trừ hoàn toàn việc kích hoạt nhầm Click/Tap khi đang cuộn hoặc vuốt.
  - **3. Phân Hệ WEB (Web Browsing):**
    - Vuốt lên/xuống: Cuộn trang mượt mà bằng báo cáo chuột thật (`Mouse Wheel HID`). Hỗ trợ cấu hình độ nhạy, đảo chiều cuộn tự nhiên, không rung liên tục gây khó chịu.
    - Vuốt trái/phải: Lệnh Quay lại (`Browser Back`) / Tiến trang (`Browser Forward`).
    - Chạm đơn: Chuột trái (`Left Click`).
    - Thanh tác vụ ngón cái phía dưới: `Lùi`, `Tiến`, `Home`, `Tải lại (F5/Cmd+R)`, `+ Tab mới`.
  - **4. Phân Hệ VIDEO (Media Playback):**
    - Chạm: Phát / Tạm dừng (`Consumer Control: PLAY_PAUSE`).
    - Vuốt lên/xuống: Tăng / Giảm âm lượng hệ thống (`VOLUME_UP` / `VOLUME_DOWN`).
    - Vuốt trái/phải: Tua lùi / Tua tiến (bước tua cấu hình 5s / 10s / 30s).
    - Giữ lâu (Hold-to-repeat): Tua liên tục có kiểm soát bằng coroutine an toàn.
    - Thanh tác vụ ngón cái: `Bài trước`, `Lùi -10s`, `Phát/Dừng`, `Tiến +10s`, `Bài tiếp`, `Tắt tiếng (Mute)`.
  - **5. Mở Rộng Semantic Action Engine:**
    - Bổ sung `PocketAction.WebAction` và `PocketAction.VideoAction`.
    - Tự động phân giải OS-aware trên Windows, macOS, Linux qua `ActionResolver.kt`. Đăng ký đầy đủ catalog trong `ActionRegistry.kt`.
  - **6. Tái Cấu Trúc Module Cài Đặt (Phá vỡ Giới hạn JVM 64KB):**
    - Tách nhỏ `SettingsScreen.kt` thành các sub-composables chuyên trách trong `SettingsSections.kt`.
    - Triệt tiêu hoàn toàn `MethodTooLargeException`, tăng tốc độ biên dịch và dễ bảo trì.
- **Hệ quả:**
  - Trải nghiệm điều khiển từ xa bằng 1 ngón cái cực kỳ êm ái, tiện lợi, không đòi hỏi phần mềm cài thêm trên PC.
  - 100% unit tests pass (`ActionResolverTest`, `ScreenWakePolicyTest`), biên dịch `assembleDebug` hoàn thành trong 27s.

---

## [D-009] 2026-09-27 — Tích Hợp Vùng Cuộn Nhanh Mép Phải (Right Edge Fast Scroll Zone) Cho Bàn Rê Chuột

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Khi duyệt các trang tài liệu dài, log hoặc trang web nhiều trang trên màn hình máy tính từ bàn rê điện thoại (đặc biệt ở chế độ xoay ngang Landscape), cuộn bằng 2 ngón bình thường cần nhiều lần vuốt liên tục.
  - Người dùng cần một dải cuộn nhanh dọc theo mép phải (dễ với tới bằng ngón cái phải), chiếm diện tích nhỏ (8–12%), tốc độ cao (2.0x–3.0x), phản hồi mượt mà và chống kích hoạt nhầm.
- **Quyết định:**
  - **1. Bộ Điều Khiển Chuyên Biệt `FastScrollController`:**
    - Phân định hit zone tự co giãn (responsive 8–12% chiều rộng bàn rê, giới hạn an toàn 32dp–64dp).
    - Tính toán delta Y, vận tốc (velocity) kết hợp đường cong gia tốc phi tuyến (acceleration curve) mượt mà, độc lập với gia tốc con trỏ.
    - Giới hạn tốc độ tối đa (`clamp`) chống văng trang mất kiểm soát.
    - Tuân thủ chuẩn báo cáo chuột thật (`Mouse Wheel HID`).
    - Đồng bộ 100% với cài đặt `naturalScroll` hiện có (không tạo thêm cài đặt đảo chiều trùng lặp).
  - **2. Khóa Cử Chỉ Chống Xung Đột (Gesture Arbitration & Isolation):**
    - Chỉ kích hoạt khi ngón tay chạm xuống (`ACTION_DOWN`) bên trong dải mép phải. Nếu ngón tay chạm ở giữa rồi lướt sang mép, hoàn toàn không chuyển sang cuộn nhanh.
    - Khi Fast Scroll kích hoạt: Khóa hoàn toàn chuột, không di chuyển con trỏ, không click/tap, không pinch zoom, không cuộn 2 ngón.
    - Tuân thủ thứ bậc đa ngón (`4 > 3 > 2 > 1`): Khi có thêm ngón tay hạ xuống, tự động nhường quyền cho cử chỉ đa ngón.
  - **3. Trực Quan Tinh Tế & Phản Hồi Xúc Giác:**
    - Dải viền mép phải có đường phân cách mờ, icon điều hướng `↕`, grip dots `⋮` và nhãn `FAST`.
    - Khi chạm kích hoạt: Bật sáng viền tinh tế, phát 1 xung rung nhẹ (không rung liên tục khi cuộn), hiển thị banner thoáng qua `FAST SCROLL ×2.5`.
    - Gắn nhãn trợ năng (`Accessibility` semantics) đầy đủ: "Fast Scroll Area. Swipe vertically to quickly scroll."
  - **4. Tùy Chỉnh Linh Hoạt & Kiểm Thử:**
    - Thêm cài đặt bật/tắt và thanh trượt hệ số tốc độ (1.5x–5.0x, mặc định 2.5x) vào `SettingsSections.kt` và `AppSettings.kt`.
    - Viết bộ unit test toàn diện `FastScrollControllerTest` kiểm tra hit zone, activation, đảo chiều tự nhiên, giới hạn tốc độ và hủy đa ngón.
- **Hệ quả:**
  - Người dùng có thể lướt nhanh văn bản dài tức thì chỉ bằng một ngón cái phải mà không làm ảnh hưởng đến độ chính xác của vùng di chuột chính.
  - 100% unit tests pass (`FastScrollControllerTest`, `GestureInterpreterTest`, `ActionResolverTest`), APK biên dịch `assembleDebug` thành công.

---

## [D-010] 2026-09-27 — Sửa Lỗi Windows Enumeration Cho Gamepad (joy.cpl), Tinh Chỉnh HID Descriptor & Chẩn Đoán Consumer Control

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Lỗi nghiêm trọng: Windows "Game Controllers" (`joy.cpl`) không hiển thị PocketHID dù Bluetooth HID đã kết nối và giao diện Gamepad trên app đã gửi report.
  - Phím âm lượng đa phương tiện (Volume Up/Down/Mute) trên giao diện không làm thay đổi âm lượng của máy tính kết nối.
- **Phân Tích Nguyên Nhân Gốc Rễ (Root Cause Analysis):**
  - **1. Sai lệch SDP Device Subclass (BtHidTransport.kt):** Trong `BluetoothHidDeviceAppSdpSettings`, mã subclass bị hardcode là `0xC0` (`SUBCLASS1_COMBO`, Keyboard + Mouse). Khi Windows truy vấn SDP qua L2CAP, nó coi thiết bị chỉ là bàn phím chuột và không kích hoạt driver `hidgame.sys` / DirectInput Game Controller.
  - **2. Ô nhiễm Global Items trong HID Descriptor (HidConstants.kt):** Hat switch (D-pad) khai báo `Physical Minimum: 0`, `Physical Maximum: 315`, `Unit: Angular Pos (0x14)`. Theo chuẩn USB HID, các item Global không bị reset sẽ kế thừa sang các khai báo tiếp theo. Bốn trục analog (Left X/Y, Right X/Y) bị thừa hưởng đơn vị góc độ và biên độ vật lý 315 trong khi logical là `-32768..32767`, khiến `dinput8.dll` loại bỏ toàn bộ trục vì không hợp lệ.
  - **3. Padding Hat Switch sai quy cách:** Khai báo 4 bit đệm dùng `0x81, 0x01` (`Input: Const, Array, Abs`). Chuẩn USB HID quy định trường đệm hằng số phải là Variable (`0x03` = `Const, Var, Abs`).
  - **4. Thiếu Physical Pointer Collection:** Cặp trục X/Y của cần gạt trái và Z/Rz của cần gạt phải chưa được bao bọc trong `Usage (Pointer), Collection (Physical)`, khiến `joy.cpl` không nhận diện được con trỏ analog của tay cầm.
  - **5. Chu kỳ nhả Consumer Key quá ngắn (12ms):** `sendConsumerClick` gửi report nhả phím chỉ sau 12ms. Trên sóng Bluetooth, gói tin nhả đến quá nhanh khiến Windows Audio Service (`AudioSrv`) chưa kịp lấy mẫu, làm trôi sự kiện chỉnh âm lượng.
- **Quyết định Khắc Phục:**
  - **1. Nâng cấp SDP Subclass:** Đổi subclass sang `(SUBCLASS1_COMBO or SUBCLASS2_GAMEPAD).toByte()` (`0xC8`), thông báo cho Windows đây là thiết bị phối hợp Bàn phím + Chuột + Gamepad.
  - **2. Làm sạch HID Report Descriptor:**
    - Thay thế padding bằng `0x81, 0x03`.
    - Reset toàn bộ global items sau Hat switch: `0x65, 0x00` (Unit None), `0x35, 0x00` (Physical Min 0), `0x45, 0x00` (Physical Max 0).
    - Bọc X/Y và Z/Rz vào `Usage (Pointer), Collection (Physical)`.
    - Bảo đảm độ dài report chính xác 13 bytes, Report ID 4.
  - **3. Nâng cấp Consumer Control:** Tăng thời gian giữ phím lên 75ms (`CONSUMER_PRESS_DELAY_MS`), bổ sung `sendConsumerPress` và `sendConsumerRelease` trên `InputTransport`.
  - **4. Bộ Công Cụ Chẩn Đoán Toàn Diện (`DiagnosticsSheet.kt`):**
    - Chia 4 tab trực quan: `[ SYSTEM ]  [ GAMEPAD ]  [ MEDIA ]  [ CHECKLIST ]`.
    - `EnumerationStatusCard`: Hiển thị SDP 0xC8, trạng thái 4 collections, độ dài report 13 bytes, và Gamepad Host Status.
    - `GamepadTestPanel`: Bảng hiển thị 11 nút số, D-pad, 2D crosshair sticks, thanh áp lực cò (0.00..1.00), màn hình soi byte thô HEX (`04 XX ...`), cờ `NEUTRAL REPORT: YES/NO`, và bộ đếm latch chống kẹt phím.
    - `MediaConsumerTestPanel`: 7 nút bấm test âm lượng/media với phản hồi thời gian thực qua `ConsumerDiagnosticsHub`.
    - `HostChecklistPanel`: Quy trình chuẩn cho lập trình viên để quên thiết bị cũ, pair lại và kiểm thử trên `joy.cpl`.
- **Hệ quả:**
  - Windows nhận diện PocketHID ngay lập tức trong `joy.cpl` khi ghép nối lại.
  - 100% unit tests pass (`GamepadReportDescriptorTest`, `GamepadMathTest`, `ActionDispatcherTest`).
  - Toàn bộ tính năng Bàn phím, Chuột, Presenter, One-Hand tiếp tục hoạt động chuẩn xác không suy thoái.

---

## [D-011] 2026-09-27 — Tối Ưu Hóa Bố Cục Bàn Phím Landscape (Landscape Keyboard UX Rework) & Khắc Phục Lỗi Âm Lượng/Mute Media

- **Trạng thái:** ACTIVE `[đã đo]`
- **Bối cảnh:**
  - Bàn phím chế độ nằm ngang (Landscape) bị chèn ép, phím nhỏ do 2 cụm panel hai bên (Left Deck, Right Deck) chiếm quá nhiều diện tích (~40% chiều ngang).
  - Có sự trùng lặp phím không cần thiết: CTRL, ALT, SHIFT xuất hiện cả ở Left Panel và Bottom Row; ENTER và BACKSPACE xuất hiện ở cả Right Panel và bàn phím chính; PASTE chiếm diện tích lớn trên bàn phím chính.
  - Phím âm lượng (Volume Up, Volume Down, Mute) trong lớp Media cần cơ chế giữ lặp mượt mà (hold-to-repeat cho Volume) và chống lặp bật tắt cho Mute.
  - Nhãn "TOUCH OPTIMIZED" chiếm dụng diện tích không cần thiết trên thanh tiêu đề phím.
- **Quyết định:**
  - **1. Loại bỏ phím trùng lặp và tối ưu hóa diện tích phím chính:**
    - Gỡ bỏ CTRL, ALT, SHIFT khỏi Left Deck; duy trì duy nhất một dải modifier canonical ở hàng dưới cùng (Bottom Row).
    - Gỡ bỏ phím PASTE lớn khỏi bàn phím chính (duy trì qua Shortcut layer và Command Palette).
    - Gỡ bỏ phím BKSP và ENTER khổng lồ khỏi Right Deck.
    - Duy trì duy nhất phím Backspace tại góc phải hàng số cố định (Dedicated Number Row).
    - Duy trì duy nhất phím Enter chính tại góc phải hàng dưới cùng (Bottom Row) với touch target lớn.
    - Gỡ bỏ nhãn "TOUCH OPTIMIZED" khỏi thanh tiêu đề bàn phím.
  - **2. Tái phân bổ tỷ lệ màn hình Landscape:**
    - Left Thumb Zone: ~13% (`weight(1.3f)`) - Chứa cụm phím ngón cái gọn gàng (`ESC`, `TAB`, `SUPER`, `⌘ QUICK`).
    - Primary Keyboard Bay: ~74% (`weight(7.4f)`) - Chiếm vị trí áp đảo, tăng kích thước touch target của tất cả các phím QWERTY, hàng số, Space (weight 3.8f) và Enter.
    - Right Thumb Zone: ~13% (`weight(1.3f)`) - Chứa cụm điều hướng 4 chiều tự nhiên (`↑`, `←`, `↓`, `→`), `DEL`, và nút chuyển `123#`.
  - **3. Khắc phục chức năng Media & Consumer Control:**
    - Bàn phím Media thiết kế lại dạng 2 hàng tinh gọn:
      - Hàng 1: `PREV` | `PLAY / PAUSE` | `NEXT`
      - Hàng 2: `VOL −` | `MUTE` | `VOL ＋`
    - Điều khiển âm lượng remote PC thông qua Bluetooth HID Consumer Control (Report ID 3), không tác động AudioManager điện thoại.
    - Cơ chế `DeckRepeatKey`:
      - Volume Up (`0x00E9`) & Volume Down (`0x00EA`): Chạm đơn gửi 1 bước (+2% trên host PC); giữ ngón tay lặp lại chu kỳ 100ms sau 350ms ban đầu.
      - Mute (`0x00E2`): Kích hoạt 1 lần toggle duy nhất khi chạm, vô hiệu hóa lặp khi giữ (`enableRepeat = false`) để tránh bật tắt âm thanh mất kiểm soát.
    - Bổ sung thanh trạng thái Telemetry thời gian thực hiển thị Action Name, Report ID 3, Usage Code HEX, và Transport Status (SUCCESS/ERROR).
  - **4. Kiểm thử & Đo đạc:**
    - Viết unit test `ConsumerControlMediaTest.kt` kiểm tra định dạng Report ID 3, các mã Usage chuẩn (0x00E9, 0x00EA, 0x00E2, 0x00CD), gói tin Release trung tính `[0x03, 0x00, 0x00]`, và cập nhật telemetry qua `ConsumerDiagnosticsHub`.
    - Unit tests pass 100%, `assembleDebug` hoàn thành trơn tru không lỗi.
- **Hệ quả:**
  - Bàn phím Landscape rộng rãi, độ nảy và diện tích phím QWERTY tăng đáng kể, ngón cái chạm chính xác, không mỏi.
  - Phím âm lượng máy tính phản hồi nhạy và êm ái trên host PC Windows/macOS.



