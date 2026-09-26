# Digest Wave 1 — PocketHID

**Thời gian hoàn tất:** 2026-09-27 04:30  
**Vai trò:** AI-Master (T7)  
**Trạng thái Wave 1:** HOÀN TẤT THÀNH CÔNG (100% Tests Pass, Build APK Thành công)

---

## 1. Kết Quả Thực Thi 3 Task Envelopes

### W1-01: Core Unit Tests & Regression Prevention
- Bổ sung `testImplementation("junit:junit:4.13.2")` trong [build.gradle.kts](file:///d:/desktop/PocketHID/app/build.gradle.kts).
- Tạo [GestureInterpreterTest.kt](file:///d:/desktop/PocketHID/app/src/test/java/dev/aleian/pockethid/mapping/GestureInterpreterTest.kt): kiểm thử toàn diện acceleration curve, deadzone, 2-finger scroll, 3/4-finger swipe, priority lock.
- Tạo [KeyMapperTest.kt](file:///d:/desktop/PocketHID/app/src/test/java/dev/aleian/pockethid/mapping/KeyMapperTest.kt): kiểm thử mapping phím thường, ký tự hoa, ký tự đặc biệt hàng số, và navigation keys.
- Kết quả: `testDebugUnitTest` PASS 100%.

### W1-02: Modularization của LandscapeDeckScreen (God Composable)
- Tách `LandscapeDeckScreen.kt` (998 dòng) thành 3 Zone riêng biệt trong package `dev.aleian.pockethid.ui.screens.deck`:
  1. [DeckMacroZone.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/deck/DeckMacroZone.kt) (Zone 1: Quick Actions & Macro Keys)
  2. [DeckKeyboardZone.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/deck/DeckKeyboardZone.kt) (Zone 2: 6 tab layers Type/Fn/Nav/Numpad/Media/Symbols)
  3. [DeckUtilityZone.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/deck/DeckUtilityZone.kt) (Zone 3: Connection status, host info, layer selector, settings, trackpad launcher)
- File chính [LandscapeDeckScreen.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/LandscapeDeckScreen.kt) giảm xuống còn ~250 dòng đóng vai trò Container/Orchestrator sạch và dễ bảo trì.

### W1-03: Multi-Key Rollover (6KRO) Transport & Click API
- Cập nhật [InputTransport.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/transport/InputTransport.kt) & [BtHidTransport.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt):
  - Bổ sung `sendKeyReport(keyCodes: ByteArray, modifiers: Byte)` hỗ trợ lên tới 6 phím đồng thời theo chuẩn USB HID Boot Keyboard.
  - Bổ sung `sendMouseClick(buttons: Byte)` dạng `suspend fun` cho phép dispatch click mượt mà với coroutine delay.
  - Hoàn toàn tương thích ngược với API `sendKeyPress` và `sendMouseMove` hiện hành.

---

## 2. Các Tính Năng Đã Tích Hợp Đầy Đủ
1. **Multi-Touch Trackpad (1–4 Ngón):**
   - Đã xử lý bắt cảm ứng đa điểm trực tiếp qua `pointerInteropFilter` và `GestureInterpreter`.
   - 1 ngón: Di chuyển chuột, click trái, double tap, tap+hold drag.
   - 2 ngón: Cuộn mượt mà V/H, tap (chuột phải), tap+hold.
   - 3 ngón: Tap (chuột giữa), swipe trái/phải (chuyển Virtual Desktop), swipe lên/xuống (Task View / Show Desktop).
   - 4 ngón: Tap (mở Command Deck / Palette), swipe trái/phải (Alt+Tab), swipe lên/xuống.
   - Gesture Priority Lock (4 > 3 > 2 > 1) chống kích hoạt nhầm.
   - Nút bấm chuột chuyên dụng (LEFT, MID, RIGHT, LOCK) hỗ trợ kéo thả trực quan.
   - Banner phản hồi cử chỉ thời gian thực & Dialog Hướng dẫn cử chỉ.
2. **Dedicated Number Row:**
   - Hàng phím số cố định (`1 2 3 4 5 6 7 8 9 0 - = BKSP`) kèm ký tự Shift.
   - Hỗ trợ nhấn giữ Backspace tự động lặp (repeat) thông qua Coroutine.
3. **Telemetry & Diagnostics:**
   - Tích hợp HUD giám sát cử chỉ theo thời gian thực trong [DiagnosticsSheet.kt](file:///d:/desktop/PocketHID/app/src/main/java/dev/aleian/pockethid/ui/screens/DiagnosticsSheet.kt).
4. **Tài Liệu Hóa Dự Án:**
   - Cập nhật toàn diện [README.md](file:///d:/desktop/PocketHID/README.md) với bảng tra cứu cử chỉ 1-4 ngón, kiến trúc hệ thống, hướng dẫn build và vận hành.
