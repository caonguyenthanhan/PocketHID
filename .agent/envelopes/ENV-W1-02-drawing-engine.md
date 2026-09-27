<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Khảo sát kiến trúc Drawing Engine hoàn tất, độc lập hoàn toàn với Android View và Compose.
- Trả lời TỪNG 🟡 worker đã nộp: Không có.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: `WORKFLOW.md` v1.4, chuẩn envelope template v1.1.

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-worker-1. Model ghế (theo bảng model-mỗi-ghế): Claude 3.5 Sonnet / GPT-4o-mini (T8/T9).
Đọc trước khi làm: `WORKFLOW.md`, `PROJECT_STRUCTURE.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: `CONTEXT_MANIFEST.md`, `PROJECT_STRUCTURE.md`, `todo.md`, `.agent/feedback-src/focus-lock-and-drawing-board-spec.md` (Mục 2)
- Trạng thái kế thừa: Commit `1d2cee8` `[đã đo]`. Dự án chưa có module drawing `[đã đo]`.
- Interface/contract liên quan:
  ```kotlin
  data class DrawingPoint(val x: Float, val y: Float)
  enum class DrawingTool { PEN, ERASER }
  enum class DrawingColor(val argb: Long) {
      WHITE(0xFFFFFFFF), CYAN(0xFF00E5FF), YELLOW(0xFFFFEA00), RED(0xFFFF3D00), BLUE(0xFF2979FF)
  }
  data class DrawingStroke(
      val points: List<DrawingPoint>,
      val color: DrawingColor,
      val width: Float,
      val tool: DrawingTool,
      val opacity: Float = 1.0f
  )
  data class DrawingState(
      val strokes: List<DrawingStroke> = emptyList(),
      val redoStack: List<DrawingStroke> = emptyList(),
      val activeTool: DrawingTool = DrawingTool.PEN,
      val selectedColor: DrawingColor = DrawingColor.WHITE,
      val strokeWidth: Float = 4f,
      val isClearConfirmationPending: Boolean = false
  )
  ```
- ĐIỂM DỄ SAI NHẤT của task này:
  - Lưu trữ raw Android View / Canvas class trong model (Model PHẢI là pure Kotlin).
  - Không chặn giới hạn Undo/Redo stack dẫn đến nguy cơ OOM (phải chặn trần `MAX_HISTORY = 50`).
  - Eraser phá hủy stroke history không thể undo được.

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Xây dựng Domain Model và State Controller cho Electronic Drawing Board (`DrawingController`). Hỗ trợ quản lý nét vẽ (`startStroke`, `appendPoint`, `finishStroke`), Undo/Redo với bounded history, Clear có xác nhận (`requestClear`, `confirmClear`, `cancelClear`), chuyển đổi công cụ Pen/Eraser, bảng màu 5 màu (White, Cyan, Yellow, Red, Blue), cỡ nét (2, 4, 8, 12dp), thuật toán tẩy nét vẽ (`eraseAt`), và xử lý logic lọc đa chạm (1 ngón = vẽ, 2+ ngón = hoàn tất/bỏ qua).
Input: Các sự kiện điểm chạm cảm ứng `DrawingPoint(x, y)`, lệnh Undo/Redo/Clear, thay đổi màu sắc/kích thước.
Output: StateFlow `DrawingState` bất biến (immutable), bộ Unit Test `DrawingControllerTest.kt` kiểm chứng 11 tiêu chuẩn.
Vị trí trong pipeline: Task 2/3 của Wave 1 (Thi công song song độc lập với Task 1).

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `app/src/main/java/dev/aleian/pockethid/drawing/**` (mới)
  - `app/src/test/java/dev/aleian/pockethid/drawing/**` (mới)
- KHÔNG được chạm:
  - `app/src/main/java/dev/aleian/pockethid/ui/**` (tuyệt đối không sửa UI ở task này)
  - `app/src/main/java/dev/aleian/pockethid/transport/**`
  - `app/src/main/java/dev/aleian/pockethid/service/**`
  - `app/src/main/java/dev/aleian/pockethid/mapping/**`
- RÀO tài nguyên chia sẻ: Nhánh `feat/drawing-engine`, không dùng file chia sẻ ngoài `drawing/**`.
- Nhánh gốc: `main` @ `1d2cee8` · đã `ls-tree` các path sửa-file-có-sẵn: 0/0 (toàn bộ file mới) (lint 18).
- Hạn mức diff: Lõi ≤ 350 dòng (`drawing/**`) / Vỏ = 0 / Test không hạn mức. Vượt $\rightarrow$ dừng và giải trình (W7).

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass (11 test cases trong `DrawingControllerTest.kt`):
  1. `testStartStrokeAndAppendPoints`
  2. `testFinishStrokeAppendsToState`
  3. `testUndoRemovesLastStrokeAndPushesRedo`
  4. `testRedoRestoresStroke`
  5. `testNewStrokeClearsRedoStack`
  6. `testBoundedHistoryEnforcesMaxLimit`
  7. `testRequestClearSetsPendingConfirmation`
  8. `testConfirmClearRemovesAllStrokes`
  9. `testCancelClearPreservesStrokes`
  10. `testToolAndColorAndWidthSelection`
  11. `testEraseStrokeAtPoint`
- Negative control (W8): Tiêm lỗi giả định `undo()` không lưu vào `redoStack` $\rightarrow$ ca `testRedoRestoresStroke` PHẢI ĐỎ.
- Định dạng bàn giao: Pure Kotlin classes, test pass 100%, nộp theo FORMAT BÁO CÁO WORKER.

## 6. ĐÓNG VÒNG LẶP
Sau khi xong: Cập nhật `todo.md` mục Task W1-02, nộp báo cáo 4 mục.
lint 17/17 PASS · template-version 1.1 · lint-version 1.3 · 2026-09-27 · mục 17 n/a (M21 PROPOSED)
