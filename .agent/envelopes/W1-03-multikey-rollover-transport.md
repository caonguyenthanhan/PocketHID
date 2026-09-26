<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Đạt [đã đo — build assembleDebug thành công tại commit 689a5f3]
- Trả lời TỪNG 🟡 worker đã nộp: khối 0 trống — task thứ ba của Wave 1.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: Tuân thủ quy chuẩn multi-agent v1.4, M12 đối chiếu board, M1/M3 nhãn [đã đo]/[chưa đo].

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-Worker-3 (T8). Model ghế: Claude 3.5 Sonnet / GPT-4o.
Đọc trước khi làm: `D:\An-tool-ecosystem\.claude\skills\multi-agent-workflow\SKILL.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: .agent/reports/wave0-codemap.md, .agent/reports/wave0-review.md, todo.md
- Trạng thái kế thừa (re-verify trên main NGAY TRƯỚC khi phát):
  - Nhánh main sạch, HEAD tại 689a5f3 [đã đo]
  - `sendKeyPress` hiện tại chỉ nhận 1 `keyCode: Byte` và điền vào byte thứ 2 của mảng 8-byte, 5 byte còn lại luôn là 0x00 [đã đo]
  - Giao thức HID Keyboard Report Descriptor chuẩn (Report ID 1) hỗ trợ tối đa 6 phím đồng thời (6KRO) ở các byte từ 2 đến 7 [đã đo]
- Interface/contract liên quan: `InputTransport.kt`, `BtHidTransport.kt`, `HidConstants.kt`
- ĐIỂM DỄ SAI NHẤT của task này: Phá vỡ tương thích ngược với các hàm gọi cũ `sendKeyPress(keyCode, modifiers)`; truyền quá 6 phím gây tràn mảng byte.

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Mở rộng `InputTransport` và `BtHidTransport` để bổ sung phương thức `sendKeyReport(keyCodes: ByteArray, modifiers: Byte)` hỗ trợ gửi tối đa 6 phím đồng thời (6-key rollover). Giữ nguyên phương thức `sendKeyPress(keyCode: Byte, modifiers: Byte)` dưới dạng overload gọi vào `sendKeyReport` để đảm bảo 100% tương thích ngược với toàn bộ UI Screens hiện có.
Input: `app/src/main/java/dev/aleian/pockethid/transport/InputTransport.kt`, `BtHidTransport.kt`.
Output: Bản cập nhật `InputTransport.kt` và `BtHidTransport.kt` hỗ trợ multi-key report.
Task 3/3 của Wave 1.

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `app/src/main/java/dev/aleian/pockethid/transport/InputTransport.kt`
  - `app/src/main/java/dev/aleian/pockethid/transport/BtHidTransport.kt`
- KHÔNG được chạm: Tất cả các file trong `ui/`, `mapping/`, `model/`, `service/`.
- RÀO tài nguyên chia sẻ: nhánh `feat/w1-03-multikey-rollover`
- Nhánh gốc: `main` @ `689a5f3` · đã `ls-tree` các path sửa-file-có-sẵn: 2/2 (`InputTransport.kt`, `BtHidTransport.kt`)
- Hạn mức diff: lõi ≤ 80 dòng / vỏ ≤ 40 dòng / test+tài liệu KHÔNG hạn mức.

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass: `./gradlew.bat assembleDebug` pass 100%. Các màn hình UI cũ gọi `sendKeyPress` không bị ảnh hưởng.
- Negative control (W8): Truyền mảng keyCodes rỗng hoặc mảng vượt quá 6 phần tử -> hàm xử lý an toàn (cắt gọn tối đa 6 phím), không văng `ArrayIndexOutOfBoundsException`.
- Định dạng bàn giao: Git diff sạch + kết quả build APK `./gradlew.bat assembleDebug`.

## 6. ĐÓNG VÒNG LẶP
Sau khi xong: cập nhật `todo.md`.
Báo cáo theo FORMAT BÁO CÁO WORKER bên dưới.

---
### FORMAT BÁO CÁO WORKER
① BẢNG SỐ 4 TRƯỜNG (M3): pass/fail/skip · hash mốc đo · ai/máy/lượt · negative-control
② PHẢN BIỆN (W1/W2): mâu thuẫn trong đặc tả → LIỆT KÊ; tiền đề sai → từ chối kèm số đo
③ 🟡/🛑 + TỰ KHAI (W4): sự cố tự khai kèm số đo; 🟡 cũ chưa được trả lời → nêu lại tại đây
④ CẦN XÁC MINH / DUYỆT (M10): mọi điều đã tự quyết trong khoảng mờ
