<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Task W1-01 (Focus Lock) và Task W1-02 (Drawing Engine) đã hoàn thành và đạt 100% test pass.
- Trả lời TỪNG 🟡 worker đã nộp: Không có.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: `WORKFLOW.md` v1.4, chuẩn envelope template v1.1.

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-worker-1. Model ghế (theo bảng model-mỗi-ghế): Claude 3.5 Sonnet / GPT-4o-mini (T8/T9).
Đọc trước khi làm: `WORKFLOW.md`, `PROJECT_STRUCTURE.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: `CONTEXT_MANIFEST.md`, `PROJECT_STRUCTURE.md`, `todo.md`, `.agent/feedback-src/focus-lock-and-drawing-board-spec.md` (Mục 2-5)
- Trạng thái kế thừa: `DrawingController` và `FocusLockController` đã sẵn sàng.
- Interface/contract liên quan:
  - `DrawingScreen(controller: DrawingController, onBack: () -> Unit)`
  - `DrawingCanvas(controller: DrawingController, modifier: Modifier)`
  - `DrawingToolbar(state: DrawingState, onSelectTool, onSelectColor, onSelectWidth, onUndo, onRedo, onClear)`
- ĐIỂM DỄ SAI NHẤT của task này:
  - Gửi drawing points qua Bluetooth HID (Tuyệt đối KHÔNG làm: Drawing canvas chỉ là tương tác cục bộ trên Android).
  - Không tôn trọng WindowInsets/display cutouts khiến toolbar bị tai thỏ che khuất khi xoay ngang.
  - Sửa vào thân các god composables (`KeyboardScreen.kt`, `MouseScreen.kt`).

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Xây dựng giao diện Electronic Drawing Board Composable (`DrawingScreen.kt`, `DrawingCanvas.kt`, `DrawingToolbar.kt`) và tích hợp `DRAW` mode vào hệ thống điều hướng `MainScreen.kt` (Portrait NavigationBar tab thứ 6) và `DeckModeSwitcher.kt` (Landscape mode 5). Giao diện tuân thủ bảng màu Command Deck (Dark navy `0xFF0A0E17`, Dark blue-gray `0xFF161F30`, Cyan accent `0xFF00E5FF`). Hỗ trợ xoay ngang mượt mà, tối đa hóa diện tích canvas, toolbar tinh gọn.
Input: Chạm vẽ ngón tay, thao tác chọn công cụ / màu / kích thước nét / Clear.
Output: Màn hình Composable `DrawingScreen` hoàn chỉnh, tích hợp với `DRAW` mode, compile APK debug thành công.
Vị trí trong pipeline: Task 3/3 của Wave 1 (Phụ thuộc vào W1-01 và W1-02).

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/DrawingScreen.kt` (mới)
  - `app/src/main/java/dev/aleian/pockethid/ui/components/DrawingToolbar.kt` (mới)
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/MainScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/LandscapeDeckScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/components/DeckComponents.kt`
- KHÔNG được chạm:
  - `app/src/main/java/dev/aleian/pockethid/drawing/model/**`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/KeyboardScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/MouseScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/GamepadScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/PresenterScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/OneHandScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/transport/**`
- RÀO tài nguyên chia sẻ: Nhánh `feat/drawing-ui`.
- Nhánh gốc: `main` @ `1d2cee8` · đã `ls-tree` các path sửa-file-có-sẵn: 3/3 (lint 18).
- Hạn mức diff: Lõi ≤ 300 dòng (`DrawingScreen.kt`, `DrawingToolbar.kt`) / Vỏ ≤ 100 dòng (`MainScreen.kt`, `LandscapeDeckScreen.kt`, `DeckComponents.kt`) / Test không hạn mức. Vượt $\rightarrow$ dừng và giải trình (W7).

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass:
  - `./gradlew.bat testDebugUnitTest` pass 100%
  - `./gradlew.bat assembleDebug` BUILD SUCCESSFUL
  - DRAW mode xuất hiện trên NavigationBar và DeckModeSwitcher
  - Canvas chiếm phần lớn diện tích màn hình, không bị tai thỏ cắt xén
  - Hộp thoại Clear Confirmation hiển thị khi bấm Clear
- Negative control (W8): Cố tình xóa icon DRAW khỏi `DeckModeSwitcher` $\rightarrow$ kiểm tra visual regression hoặc test navigation PHẢI ĐỎ.
- Định dạng bàn giao: Composable screens, APK build thành công, nộp theo FORMAT BÁO CÁO WORKER.

## 6. ĐÓNG VÒNG LẶP
Sau khi xong: Cập nhật `todo.md` mục Task W1-03, nộp báo cáo theo format 4 mục.
lint 17/17 PASS · template-version 1.1 · lint-version 1.3 · 2026-09-27 · mục 17 n/a (M21 PROPOSED)
