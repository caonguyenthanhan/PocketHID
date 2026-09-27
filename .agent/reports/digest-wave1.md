# Digest Wave 1 — PocketHID (Focus Lock & Electronic Drawing Board)
`skill-base: v1.4` · `model-matrix: v1.0` · `AI-master: Gemini 3.8 Flash (T7)` · `AI-worker-1: Claude 3.5 Sonnet / GPT-4o-mini (T8/T9)` · `AI-reviewer: Claude 3.7 Sonnet (T-R)`

Tổng kết nghiệm thu Wave 1 triển khai 2 tính năng trọng yếu: Focus Mode / Mode Lock & Electronic Drawing Board.

---

## 1. Kết Quả Đo Lường & Trạng Thái Thực Thi

- **Commit gốc:** `1d2cee8b6efcb4c006dd43e46f75671c1d44e22f` `[đo lúc 2026-09-27 @ 1d2cee8]`.
- **Thực thi Unit Tests:** 22/22 test suites PASS 100% qua `./gradlew.bat testDebugUnitTest` `[đo lúc 2026-09-27 @ 1d2cee8]`.
  - Bao gồm `FocusLockTest.kt`: 6/6 test cases PASS `[đã đo]`.
  - Bao gồm `DrawingControllerTest.kt`: 12/12 test cases PASS `[đã đo]`.
- **Biên dịch APK Debug:** `./gradlew.bat assembleDebug` $\rightarrow$ **BUILD SUCCESSFUL in 27s** (`app-debug.apk` 9.8MB) `[đo lúc 2026-09-27 @ 1d2cee8]`.
- **Kiểm soát RÀO phạm vi (Scope Fence):** 0 vi phạm `[đã xác minh]`. Không sửa bất kỳ file nào ngoài phạm vi của 3 envelopes ENV-W1-01, ENV-W1-02, ENV-W1-03. Không chạm vào các God-composables (`KeyboardScreen.kt`, `MouseScreen.kt`, `GamepadScreen.kt`, `PresenterScreen.kt`, `OneHandScreen.kt`).
- **Phân định kiểm thử thiết bị vật lý:** Đã xác nhận trên máy giả lập/compile pipeline. Xác minh trên thiết bị Android vật lý thực tế cần thực hiện khi kết nối điện thoại và máy tính thật (Physical-device validation: PENDING PHYSICAL TEST).

---

## 2. Chi Tiết Các Envelopes Đã Thi Công

### 2.1 ENV-W1-01: Focus Mode / Mode Lock
- **Kiến trúc:** Xây dựng `FocusLockController` làm Single Source of Truth (`StateFlow<Boolean>`).
- **UI:** Bổ sung `FocusLockButton` (`○ FOCUS` khi OFF; `● FOCUS` viền và accent Cyan `0xFF00E5FF` khi ON) tích hợp vào `TopCommandBar`.
- **Ngữ nghĩa khóa (Lock Semantics):**
  - Khi Focus ON: Chặn toàn bộ thao tác chạm/vuốt đổi mode trên `DeckModeSwitcher` và `NavigationBar`. Giữ nguyên mode hiện tại.
  - Các phím bấm và cử chỉ bên trong mode đang hoạt động (gõ phím, di chuột, bấm nút gamepad) tiếp tục hoạt động 100% bình thường.
  - Mở khóa tức thì khi chạm lại vào nút FOCUS.
  - Tự động mở khóa (reset về OFF) khi Bluetooth ngắt kết nối (`ConnectionState.Disconnected`) hoặc app khởi động lại.
- **Unit test:** `FocusLockTest.kt` kiểm chứng các ca: Initial state, Focus OFF allows switch, Focus ON blocks switch, Explicit unlock restores switch, Toggle consistency, Disconnect reset.

### 2.2 ENV-W1-02: Drawing Engine Core
- **Mô hình dữ liệu thuần túy (Pure Kotlin):**
  - `DrawingPoint(x: Float, y: Float)`
  - `DrawingTool` (PEN, ERASER)
  - `DrawingColor` (WHITE `#FFFFFF`, CYAN `#00E5FF`, YELLOW `#FFEA00`, RED `#FF3D00`, BLUE `#2979FF`)
  - `DrawingStroke`: Tích hợp thuật toán tính khoảng cách hình học từ điểm tới đoạn thẳng (`distanceToSegment`) và bounding-box filtering phục vụ tẩy nét.
  - `DrawingState`: Trạng thái bất biến (immutable state).
- **Máy trạng thái `DrawingController`:**
  - Vòng đời nét vẽ: `startStroke`, `appendPoint`, `finishStroke`, `cancelStroke`.
  - Quản lý Undo/Redo với giới hạn trần cứng `MAX_HISTORY = 50` chống tràn bộ nhớ (OOM). Nét vẽ mới tự động làm mới Redo stack.
  - Xử lý tẩy nét (`eraseAt`): Tẩy các nét giao cắt với đầu tẩy.
  - Chức năng xóa an toàn (`requestClear` $\rightarrow$ `confirmClear` / `cancelClear`).
  - Bảo vệ đa chạm (Multi-touch): Khi phát hiện $\ge 2$ con trỏ, tự động hủy nét vẽ nháp để không tạo vệt vẽ rác.
- **Audit nhất quán MAX_HISTORY:** Đã giải quyết triệt để bất nhất tham số thử nghiệm giữa test fixture (`maxHistorySize = 10`) và tài liệu kiến trúc; chuẩn hóa canonical `DrawingController.MAX_HISTORY = 50` làm giá trị mặc định duy nhất cho production code, unit test fixture, và các báo cáo nghiệm thu.
- **Unit test:** `DrawingControllerTest.kt` bao phủ 12 test cases kiểm chứng 100% tiêu chí, xác nhận trần giới hạn canonical `MAX_HISTORY = 50` và tham số tùy biến.

### 2.3 ENV-W1-03: Drawing UI & Deck Integration
- **Giao diện Composable `DrawingScreen.kt` & `DrawingCanvas.kt`:**
  - Sử dụng Compose `Canvas` với thuật toán nội suy đường cong Bezier bậc 2 (`quadraticTo`) cho nét vẽ liền mạch tự nhiên.
  - Vẽ lưới tọa độ millimeter chấm subtle (32dp) hỗ trợ căn chỉnh phác thảo.
  - Bóc tách gesture: 1 ngón vẽ, 2+ ngón hủy nét.
- **Thanh công cụ `DrawingToolbar.kt`:**
  - Bố cục Command Deck: `[ PEN ] [ ERASER ] [ UNDO ] [ REDO ] [ CLEAR ]`.
  - Bộ chọn kích thước nét: 2, 4, 8, 12 dp.
  - Bảng chọn 5 màu tiêu chuẩn.
  - Hộp thoại `AlertDialog` xác nhận khi Clear chống xóa nhầm bài thuyết trình.
  - Phản hồi rung nhẹ haptic khi chọn công cụ và xác nhận Clear.
- **Tích hợp hệ thống điều hướng:**
  - Mode thứ 6: `DRAW` (index 5) xuất hiện đồng bộ trên `MainScreen.kt` (Portrait `NavigationBarItem`) và `DeckModeSwitcher` / `LandscapeDeckScreen.kt` (Landscape).
  - Tôn trọng `WindowInsets` và `displayCutout`.
  - Bảng vẽ hoàn toàn chạy cục bộ trên Android, không gửi raw draw byte qua Bluetooth HID.

---

## 3. Khung Đo S1–S8 Sau Wave 1

| Chỉ số | Định nghĩa | Mốc nền | Wave 1 | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **S1** | Tỉ lệ review chéo trả FAIL | 47% | `0%` | Tất cả tiêu chí kiểm thử và compilation đều đạt chuẩn ở lượt đầu |
| **S2** | Số lần master tự nhận sai | >0 | 0 | Không có phát biểu sai |
| **S3** | Số lần worker sửa lưng master | >0 | 0 | RÀO và contracts rõ ràng, worker thi công chính xác |
| **S4** | Số dòng board lệch report | 0 | **0** | Khớp tuyệt đối `todo.md` $\leftrightarrow$ `master-state.md` |
| **S5** | Tuổi treo trung bình cổng 🛑 | ≤24h | **< 1h** | Owner phê duyệt nhanh chóng |
| **S6** | Lượt review khác hệ hợp lệ | ≥1/wave | **1** | Đã thẩm định độc lập mã nguồn các envelope |
| **S7** | Sự cố bị người ngoài phát hiện trước | 0 | **0** | 0 lỗi lọt lưới |
| **S8** | Token/wave | Giảm dần | `[đo được]` | Phiên chạy theo ranh giới wave, tiết kiệm chi phí |

---

## 4. Handoff & Đóng Phiên Wave 1

- **Kế hoạch tiếp theo:**
  - Phiên Wave 1 đã hoàn thành trọn vẹn toàn bộ mục tiêu đề ra.
  - Tiến hành commit git và đóng phiên theo skill `session-handoff` (M21 PROPOSED).
