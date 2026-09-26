# Wave 0 Baseline Review — PocketHID (Chế độ CHỈ ĐỌC)

Báo cáo kiểm toán chất lượng mã nguồn ban đầu của dự án PocketHID tại commit `689a5f3`.
Tuân thủ nguyên tắc Wave 0: Không sửa mã nguồn, chỉ phân tích và phân loại rủi ro.

---

## 1. Kết Quả Kiểm Tra Rò Rỉ Bí Mật (Secret Leak Scan)

- **Trạng thái:** ÂM TÍNH (PASS) `[đã xác minh]`
- **Phương pháp đo:**
  - Regex pattern: `(?i)(api[_-]?key|secret|password|bearer|auth[_-]?token|private[_-]?key)` trên toàn bộ repo qua `git grep`.
  - Kết quả: 0 phát hiện trong mã nguồn.
  - **Phép kiểm đối chứng dương (Positive Control):** Đã kiểm tra `git grep "BluetoothHidDevice"` trả về chính xác 10 dòng kết quả tại `app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt` -> Xác nhận công cụ tìm kiếm hoạt động chuẩn xác, không bị bỏ sót file.

---

## 2. Đối Chiếu Cấu Trúc Dự Án Với Skill `project-structure`

| Tiêu chí | Hiện trạng PocketHID | Đánh giá | Trạng thái |
| :--- | :--- | :--- | :--- |
| **`PROJECT_STRUCTURE.md`** | Chưa có file tại project root | Thiếu file định nghĩa kiến trúc theo checklist scaffold | `[đã xác minh]` (Cần bổ sung ở Wave 1) |
| **`CONTEXT_MANIFEST.md`** | Đã tồn tại tại root | Đạt chuẩn lưu trữ context | `[đã xác minh]` |
| **Cấu trúc Package Android** | Tách bạch tốt: `model`, `transport`, `service`, `mapping`, `ui` | Tuân thủ convention kiến trúc Android Clean/Modular | `[đã xác minh]` |
| **Thư mục Tests (`app/src/test`)** | Trống, không có test file nào | Thiếu hoàn toàn tầng Unit Test & Automation Test | `[đã xác minh]` |
| **Đặt tên file / class** | PascalCase cho Composable/Class, camelCase cho biến | Đúng chuẩn Kotlin / Jetpack Compose | `[đã xác minh]` |

---

## 3. Bảng Phân Loại Rủi Ro Theo Mức Độ (Prioritized Risks)

### 3.1 Mức Độ Cao (HIGH)

#### [R-HIGH-01] Thiếu 100% Unit Test cho các tầng chuyển đổi dữ liệu trọng yếu
- **Vị trí:** `app/src/test/java/` (hiện không có file) `[đã xác minh]`
- **Bằng chứng:** Thư mục test trống hoàn toàn. Các lớp tính toán nghiệp vụ phức tạp gồm `KeyMapper.kt` (chuyển đổi ASCII, Modifier bitmask, Scancode HID), `GestureInterpreter.kt` (tính quán tính, Deadzone, 3 đường cong gia tốc Precision/Dynamic/Linear), và `SettingsRepository.kt` đều chưa được bảo vệ bằng test case tự động.
- **Tác động:** Dễ hồi quy (regression) khi tối ưu hiệu năng hoặc thêm phím tắt/cử chỉ mới.
- **Phân loại:** `[đã xác minh]`

#### [R-HIGH-02] UI Composable "God Functions" kích thước quá lớn gây nghẽn recomposition
- **Vị trí:**
  - `LandscapeDeckScreen.kt:83-997` (Hàm `LandscapeDeckScreen` dài 915 dòng, 68 điểm gọi `sendRawKey`) `[đã xác minh]`
  - `SettingsScreen.kt:85-806` (Hàm `SettingsScreen` dài 722 dòng) `[đã xác minh]`
  - `KeyboardScreen.kt:89-761` (Hàm `KeyboardScreen` dài 673 dòng) `[đã xác minh]`
  - `CommandPaletteSheet.kt:85-702` (Hàm `CommandPaletteSheet` dài 618 dòng) `[đã xác minh]`
- **Bằng chứng:** Số liệu trích xuất trực tiếp từ MCP `code-review-graph::find_large_functions_tool`.
- **Tác động:** Rất khó bảo trì, nguy cơ recomposition diện rộng làm sụt giảm framerate (rớt dưới 60fps) khi người dùng gõ phím nhanh hoặc rê chuột liên tục.
- **Phân loại:** `[đã xác minh]`

---

### 3.2 Mức Độ Trung Bình (MEDIUM)

#### [R-MED-01] Khớp nối trực tiếp (Tight Coupling) giữa UI Screens và Transport
- **Vị trí:** `dev.aleian.pockethid.ui.screens.*` trực tiếp gọi `InputTransport` methods `[đã xác minh]`
- **Bằng chứng:** Phân tích MCP `code-review-graph::get_architecture_overview_tool` chỉ ra cảnh báo ghép nối: giữa `screens-send` (Community 7) và `transport-send` (Community 5) có tới 70 cạnh phụ thuộc (edges). Các Composable trực tiếp kích hoạt gửi gói tin Bluetooth thay vì thông qua một lớp trung gian ViewModel / Action Intent.
- **Tác động:** Không thể mock transport để chạy snapshot UI test hoặc kiểm thử UI độc lập với phần cứng Bluetooth thật.
- **Phân loại:** `[đã xác minh]`

#### [R-MED-02] Giao thức Keyboard Report giới hạn 1 Keycode (Chưa hỗ trợ đa phím đồng thời 6KRO)
- **Vị trí:** `app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt:373-378` `[đã xác minh]`
- **Bằng chứng:**
  ```kotlin
  val keyboardReport = byteArrayOf(
      modifiers,
      0x00, // reserved
      keyCode,
      0x00, 0x00, 0x00, 0x00, 0x00
  )
  ```
  HID Keyboard Report hỗ trợ 6 phím đồng thời (bytes 2 đến 7). Triển khai hiện tại luôn cố định 5 byte sau là `0x00`, khiến việc giữ phím khi chơi game hoặc bấm tổ hợp phím không qua modifier bị giới hạn.
- **Phân loại:** `[đã xác minh]`

---

### 3.3 Mức Độ Thấp / Cần Giám Sát (LOW / SUSPECTED)

#### [R-LOW-01] Nguy cơ rò rỉ bộ nhớ hoặc xung đột khi unbind `HidDeviceService`
- **Vị trí:** `MainActivity.kt:50-90` và `BtHidTransport.kt:255-270` `[nghi ngờ]`
- **Bằng chứng:** Khi xoay màn hình liên tục hoặc app chuyển trạng thái background/foreground nhanh, `ServiceConnection` và `BluetoothProfile.ServiceListener` cần đảm bảo không bị đăng ký chồng chéo hoặc giải phóng proxy khi chưa hoàn tất luồng ngắt kết nối.
- **Phân loại:** `[nghi ngờ]` (cần viết test kiểm chứng vòng đời).

#### [R-LOW-02] Quản lý cấu hình Settings chưa có schema migration
- **Vị trí:** `app/src/main/java/dev/aleian/pockethid/model/SettingsRepository.kt` `[nghi ngờ]`
- **Bằng chứng:** Dùng `SharedPreferences` dạng raw keys, chưa có phiên bản cấu hình (`version`). Nếu cập nhật hoặc thêm trường cấu hình mới (ví dụ Macro profile), người dùng cũ có thể nhận giá trị default ngoài ý muốn.
- **Phân loại:** `[nghi ngờ]`.
