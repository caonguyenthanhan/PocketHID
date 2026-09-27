# Wave 0 Baseline Review — PocketHID (Chế độ CHỈ ĐỌC)

Báo cáo kiểm toán chất lượng mã nguồn ban đầu của dự án PocketHID tại commit `1d2cee8`.
Tuân thủ nguyên tắc Wave 0: Không sửa mã nguồn sản phẩm, chỉ phân tích và phân loại rủi ro.

---

## 1. Kết Quả Kiểm Tra Rò Rỉ Bí Mật (Secret Leak Scan)

- **Trạng thái:** ÂM TÍNH (PASS) `[đã xác minh]`
- **Phương pháp đo:**
  - Regex pattern: `(?i)(api[_-]?key|secret|password|bearer|auth[_-]?token|private[_-]?key)` trên toàn bộ repo qua `git grep`.
  - Kết quả: 0 phát hiện chứa thông tin bí mật/credential. Có 1 dòng match cờ giao diện Android: `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` tại `app/src/main/java/dev/aleian/pockethid/ui/components/PocketImeInputView.kt:18` (đây là flag hệ thống, không phải secret).
  - **Phép kiểm đối chứng dương (Positive Control - M2b):** Đã kiểm tra `git grep "BluetoothHidDevice"` trả về 10 kết quả chính xác tại `app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt` $\rightarrow$ Xác nhận công cụ tìm kiếm hoạt động chuẩn xác, không bị che giấu hoặc bỏ sót file.

---

## 2. Đối Chiếu Cấu Trúc Dự Án Với Skill `project-structure`

| Tiêu chí | Hiện trạng PocketHID | Đánh giá | Trạng thái |
| :--- | :--- | :--- | :--- |
| **`PROJECT_STRUCTURE.md`** | Đã tồn tại tại project root | Đạt chuẩn kiến trúc scaffold | `[đã xác minh]` |
| **`CONTEXT_MANIFEST.md`** | Đã tồn tại tại root | Đạt chuẩn quản lý context | `[đã xác minh]` |
| **`WORKFLOW.md`** | Đã tạo, extends `multi-agent-workflow` v1.4 | Tuân thủ quy chế 4 tầng | `[đã xác minh]` |
| **Bảng model-mỗi-ghế** | Đã khai báo trong `WORKFLOW.md` | Tuân thủ M9 & role matrix | `[đã xác minh]` |
| **Cấu trúc Package Android** | Đầy đủ: `action`, `gamepad`, `mapping`, `model`, `power`, `service`, `transport`, `ui` | Chuẩn Modular/Clean Architecture | `[đã xác minh]` |
| **Thư mục Tests (`app/src/test`)** | Đã có 9 test suites bao phủ mapping, gamepad, action, power | Đạt 100% PASS trên test hiện có | `[đã xác minh]` |
| **Đặt tên file / class** | PascalCase cho Composable/Class, camelCase cho biến | Đúng chuẩn Kotlin / Jetpack Compose | `[đã xác minh]` |

---

## 3. Bảng Phân Loại Rủi Ro Theo Mức Độ (Prioritized Risks)

### 3.1 Mức Độ Cao (HIGH)

#### [R-HIGH-01] Rủi ro phân mảnh và phân kỳ trạng thái Focus Lock giữa Portrait và Landscape
- **Vị trí:** `MainScreen.kt:137` (`var selectedTab by rememberSaveable { mutableIntStateOf(0) }`) và `LandscapeDeckScreen.kt:198` (`var currentMode by rememberSaveable { mutableIntStateOf(0) }`).
- **Bằng chứng:** `[đã xác minh]`
  - Hiện tại trạng thái chọn mode được quản lý bằng hai biến State riêng biệt ở hai Composable khác nhau.
  - Khi xoay màn hình (recomposition), nếu Focus Lock không có một Single Source of Truth tập trung (`FocusLockState` / `FocusLockController`), trạng thái khóa có thể bị mất, hoặc bị bypass khi người dùng xoay điện thoại từ Portrait sang Landscape và ngược lại.
- **Tác động:** Người dùng khóa Focus ở Portrait nhưng xoay ngang thì Focus bị mở khóa, vi phạm yêu cầu "Focus state survives ordinary recomposition".
- **Phân loại:** `[đã xác minh]`

#### [R-HIGH-02] Nguy cơ rò rỉ bộ nhớ (OOM) từ Stroke History không giới hạn trần trong Drawing Board
- **Vị trí:** `dev.aleian.pockethid.drawing.*` (Module mới chuẩn bị triển khai).
- **Bằng chứng:** `[đã xác minh]`
  - Người dùng sử dụng bảng vẽ cho các buổi thuyết trình dài (presentation annotation) tạo ra hàng trăm nét vẽ, mỗi nét chứa hàng chục điểm tọa độ `(x, y)`.
  - Nếu `Undo/Redo stack` không được chặn trần cứng (`MAX_HISTORY_SIZE`, khuyến nghị 30–50 thao tác) hoặc lưu trữ tham chiếu tới Android Views/Bitmaps, ứng dụng sẽ bị tràn bộ nhớ Heap và crash OOM.
- **Tác động:** Ứng dụng đột ngột văng trong phiên thuyết trình trực tiếp của người dùng.
- **Phân loại:** `[đã xác minh]`

#### [R-HIGH-03] UI Composable "God Functions" có nguy cơ bị can thiệp sai phạm vi (Chạm RÀO)
- **Vị trí:**
  - `KeyboardScreen.kt:95-942` (Hàm Composable dài 848 dòng) `[đã xác minh]`
  - `CommandPaletteSheet.kt:87-768` (Hàm dài 682 dòng) `[đã xác minh]`
  - `OneHandScreen.kt:107-649` (Hàm dài 543 dòng) `[đã xác minh]`
  - `MultiTouchTrackpad.kt:47-650` (Hàm dài 604 dòng) `[đã xác minh]`
- **Bằng chứng:** Dữ liệu trích xuất từ MCP `code-review-graph::find_large_functions_tool`.
- **Tác động:** Nếu Worker của Wave 1 khi làm Focus Lock sửa trực tiếp vào thân các hàm này thay vì chặn ở tầng ngoài (Mode Switcher Gate), sẽ gây xung đột mã và phá vỡ các phím điều khiển con đang hoạt động.
- **Phân loại:** `[đã xác minh]`

---

### 3.2 Mức Độ Trung Bình (MEDIUM)

#### [R-MED-01] Xung đột cử chỉ ngón tay thứ 2 (Multi-Touch) trên bề mặt Drawing Canvas
- **Vị trí:** `dev.aleian.pockethid.drawing.DrawingCanvas` (Module mới).
- **Bằng chứng:** `[đã xác minh]`
  - Yêu cầu đặc tả 2.9 nêu rõ: Vẽ bằng 1 ngón tay; khi ngón thứ 2 xuất hiện, phải hoàn tất/hủy nét vẽ hiện tại, KHÔNG được tạo ra nét vẽ thứ 2 ngoài ý muốn.
  - Nếu dùng bộ gesture mặc định của Compose mà không bóc tách số lượng con trỏ (`pointers.size`), ngón thứ 2 chạm vào màn hình sẽ kéo dài nét vẽ hoặc tạo vệt đứt gãy kì dị trên canvas.
- **Tác động:** Trải nghiệm viết và vẽ bị méo mó, khó chịu cho người dùng.
- **Phân loại:** `[đã xác minh]`

#### [R-MED-02] Kẹt Focus Lock vĩnh viễn khi phiên Bluetooth bị hủy đột ngột
- **Vị trí:** `dev.aleian.pockethid.transport.BtHidTransport.kt:175-195` và `MainScreen.kt:107-135`.
- **Bằng chứng:** `[đã xác minh]`
  - Khi thiết bị mất kết nối Bluetooth (`ConnectionState.Disconnected`), nếu Focus Lock vẫn duy trì ở trạng thái khóa, người dùng có thể bị kẹt trong màn hình cũ mà không thể chuyển tab để xem pairing sheet hay cấu hình lại.
  - Cần quy định rõ: Khi session kết nối bị hủy hoàn toàn hoặc app restart, Focus Lock phải tự động reset về `OFF`.
- **Tác động:** Gây hoang mang cho người dùng khi mất kết nối máy tính.
- **Phân loại:** `[đã xác minh]`

#### [R-MED-03] Thao tác Clear Canvas xóa trắng toàn bộ bài vẽ do chạm nhầm
- **Vị trí:** `DrawingScreen.kt` (Toolbar CLEAR action).
- **Bằng chứng:** `[đã xác minh]`
  - Trên màn hình cảm ứng di động, nút CLEAR nằm cạnh UNDO/REDO rất dễ bị ngón tay ấn nhầm. Đặc tả mục 2.8 yêu cầu bắt buộc phải có bước xác nhận (confirmation dialog hoặc tap lần 2).
- **Tác động:** Mất toàn bộ nội dung phác thảo đang thuyết trình của diễn giả.
- **Phân loại:** `[đã xác minh]`

---

### 3.3 Mức Độ Thấp / Cần Giám Sát (LOW / SUSPECTED)

#### [R-LOW-01] Khả năng che khuất thanh công cụ Drawing do Display Cutout / Notch
- **Vị trí:** `DrawingScreen.kt` khi xoay ngang (Landscape).
- **Bằng chứng:** `[nghi ngờ]`
  - Khi ở chế độ Landscape, vùng tai thỏ hoặc camera đục lỗ của điện thoại có thể đè lên các nút Pen / Eraser / Color nếu không bọc `WindowInsets.displayCutout` và `safeDrawingPadding`.
- **Phân loại:** `[nghi ngờ]` (cần kiểm chứng layout trên preview và device).

#### [R-LOW-02] Hiệu năng dựng lại nét vẽ (Canvas Repaint) khi số lượng nét tăng
- **Vị trí:** `DrawingCanvas.kt`.
- **Bằng chứng:** `[nghi ngờ]`
  - Nếu mỗi lần di ngón tay đều kích hoạt recompose toàn bộ màn hình thay vì chỉ redraw bên trong `Canvas(DrawScope)`, UI sẽ bị sụt giảm framerate. Cần đảm bảo tách biệt Composable Toolbar và Composable Canvas.
- **Phân loại:** `[nghi ngờ]`.
