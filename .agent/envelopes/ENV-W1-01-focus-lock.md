<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Wave 0 Baseline Review hoàn tất, 100% test hiện có pass (`testDebugUnitTest` PASS 22/22 up-to-date) `[đã đo]`.
- Trả lời TỪNG 🟡 worker đã nộp: Không có 🟡 tồn từ phiên trước.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: `WORKFLOW.md` v1.4, kỷ luật session-handoff (trần 40 dòng master-state).

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-worker-1. Model ghế (theo bảng model-mỗi-ghế): Claude 3.5 Sonnet / GPT-4o-mini (T8/T9).
Đọc trước khi làm: `WORKFLOW.md`, `PROJECT_STRUCTURE.md`, `rules/context-hygiene.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: `CONTEXT_MANIFEST.md`, `PROJECT_STRUCTURE.md`, `todo.md`, `.agent/feedback-src/focus-lock-and-drawing-board-spec.md` (Mục 1)
- Trạng thái kế thừa: Commit `1d2cee8` `[đã đo]`. Hệ thống có 5 chế độ Portrait (NavigationBar 0-4) và Landscape (`DeckModeSwitcher` 0-4). Chưa có cơ chế khóa mode chống chạm nhầm `[đã đo]`.
- Interface/contract liên quan:
  - `FocusLockController`:
    ```kotlin
    object FocusLockController {
        val isLocked: StateFlow<Boolean>
        fun toggle()
        fun setLocked(locked: Boolean)
        fun reset()
    }
    ```
- ĐIỂM DỄ SAI NHẤT của task này:
  - Tự ý disable phím điều khiển bên trong mode (chỉ được khóa việc CHUYỂN mode, các phím gõ, rê chuột, gamepad bên trong mode PHẢI tiếp tục hoạt động 100%).
  - Quên reset Focus Lock khi ngắt kết nối Bluetooth (`ConnectionState.Disconnected`).

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Triển khai tính năng Focus Mode / Mode Lock. Bổ sung nút bấm điều khiển Focus nhỏ gọn trên `TopCommandBar` (Portrait & Landscape): hiển thị `○ FOCUS` khi tắt, `● FOCUS` kèm viền/accent Cyan (`0xFF00E5FF`) khi bật. Khi bật, chặn toàn bộ thao tác chuyển tab trên `NavigationBar` (Portrait) và `DeckModeSwitcher` (Landscape). Chạm lại nút Focus sẽ lập tức mở khóa.
Input: User chạm nút Focus; User chạm các tab chuyển mode khi đang bật/tắt Focus; Sự kiện Bluetooth Disconnect.
Output: Trạng thái Focus Lock duy nhất `isFocusModeLocked`, UI phản hồi tức thì, bộ unit test `FocusLockTest.kt` kiểm chứng trọn vẹn.
Vị trí trong pipeline: Task 1/3 của Wave 1 (Độc lập với Task 2).

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `app/src/main/java/dev/aleian/pockethid/model/FocusLockController.kt` (mới)
  - `app/src/main/java/dev/aleian/pockethid/ui/components/DeckComponents.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/MainScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/LandscapeDeckScreen.kt`
  - `app/src/test/java/dev/aleian/pockethid/model/FocusLockTest.kt` (mới)
- KHÔNG được chạm:
  - `app/src/main/java/dev/aleian/pockethid/drawing/**`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/KeyboardScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/MouseScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/GamepadScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/PresenterScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/OneHandScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/transport/**`
- RÀO tài nguyên chia sẻ: Nhánh `feat/focus-lock`, không đổi port, không push remote.
- Nhánh gốc: `main` @ `1d2cee8` · đã `ls-tree` các path sửa-file-có-sẵn: 3/3 (lint 18).
- Hạn mức diff: Lõi ≤ 100 dòng (`FocusLockController.kt`) / Vỏ ≤ 120 dòng (`DeckComponents.kt`, `MainScreen.kt`, `LandscapeDeckScreen.kt`) / Test không hạn mức. Vượt $\rightarrow$ dừng và giải trình số học (W7).

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass:
  - Focus OFF $\rightarrow$ cho phép chuyển mode
  - Focus ON $\rightarrow$ từ chối chuyển mode, giữ nguyên mode hiện tại
  - Focus ON $\rightarrow$ các hành động điều khiển con không bị chặn
  - Focus OFF lại $\rightarrow$ chuyển mode hoạt động lại bình thường
  - Disconnect Bluetooth $\rightarrow$ tự động mở khóa
- Negative control (W8): Tiêm lỗi giả định `isLocked` luôn trả về `false` $\rightarrow$ ca kiểm thử `testFocusLockBlocksModeSwitch` PHẢI ĐỎ.
- Định dạng bàn giao: Diff code sạch, bộ test xanh 100%, báo cáo theo FORMAT BÁO CÁO WORKER.

## 6. ĐÓNG VÒNG LẶP
Sau khi xong: Cập nhật `todo.md` mục Task W1-01, nộp báo cáo theo format 4 mục.
lint 17/17 PASS · template-version 1.1 · lint-version 1.3 · 2026-09-27 · mục 17 n/a (M21 PROPOSED)
