<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Đạt [đã đo — build assembleDebug thành công tại commit 689a5f3]
- Trả lời TỪNG 🟡 worker đã nộp: khối 0 trống — task đầu tiên của Wave 1.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: Tuân thủ quy chuẩn multi-agent v1.4, M12 đối chiếu board, M1/M3 nhãn [đã đo]/[chưa đo].

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-Worker-1 (T8). Model ghế: Claude 3.5 Sonnet / GPT-4o.
Đọc trước khi làm: `D:\An-tool-ecosystem\.claude\skills\project-structure\SKILL.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: CONTEXT_MANIFEST.md, .agent/reports/wave0-codemap.md, .agent/reports/wave0-review.md, todo.md
- Trạng thái kế thừa (re-verify trên main NGAY TRƯỚC khi phát):
  - Nhánh main sạch, HEAD tại 689a5f3 [đã đo]
  - Thư mục app/src/test hiện chưa có test case nào [đã đo]
  - KeyMapper ánh xạ ký tự và Consumer codes độc lập, không phụ thuộc Android framework [đã đo]
  - GestureInterpreter tính toán cử chỉ thuần túy toán học [đã đo]
- Interface/contract liên quan: `KeyMapper.kt`, `GestureInterpreter.kt`, `HidConstants.kt`
- ĐIỂM DỄ SAI NHẤT của task này: Nhầm lẫn giữa bitmask modifier HID và phím thường; giả lập touch event sai biên độ khiến GestureInterpreter không nhận diện được scroll / deadzone.

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Tạo file `PROJECT_STRUCTURE.md` chuẩn hóa cây thư mục dự án theo checklist scaffold. Xây dựng bộ Unit Test tự động cho `KeyMapper` (kiểm thử phím ASCII, modifier Shift/Ctrl/Alt/GUI, F1-F12, Consumer report ID 3) và `GestureInterpreter` (deadzone, acceleration curves, 2-finger scroll).
Input: Mã nguồn hiện tại của `KeyMapper.kt`, `GestureInterpreter.kt`.
Output: `PROJECT_STRUCTURE.md` tại root, `app/src/test/java/dev/aleian/pockethid/mapping/KeyMapperTest.kt`, `app/src/test/java/dev/aleian/pockethid/mapping/GestureInterpreterTest.kt`.
Task 1/3 của Wave 1.

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `PROJECT_STRUCTURE.md`
  - `app/src/test/java/dev/aleian/pockethid/mapping/KeyMapperTest.kt`
  - `app/src/test/java/dev/aleian/pockethid/mapping/GestureInterpreterTest.kt`
- KHÔNG được chạm: Tất cả các file trong `app/src/main/` và cấu hình Gradle.
- RÀO tài nguyên chia sẻ: nhánh `feat/w1-01-core-unit-tests`
- Nhánh gốc: `main` @ `689a5f3` · đã `ls-tree` các path sửa-file-có-sẵn: 0/0 (tạo mới 3 file)
- Hạn mức diff: lõi ≤ 0 dòng / vỏ ≤ 400 dòng / test+tài liệu KHÔNG hạn mức.

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass: `./gradlew.bat testDebugUnitTest` pass 100% không có lỗi.
- Negative control (W8): Cố tình đổi kỳ vọng scancode phím 'A' từ 0x04 thành 0x05 -> test PHẢI đỏ và báo fail chính xác.
- Định dạng bàn giao: Git diff sạch + kết quả chạy `./gradlew.bat testDebugUnitTest`.

## 6. ĐÓNG VÒNG LẶP
Sau khi xong: cập nhật `todo.md`.
Báo cáo theo FORMAT BÁO CÁO WORKER bên dưới.

---
### FORMAT BÁO CÁO WORKER
① BẢNG SỐ 4 TRƯỜNG (M3): pass/fail/skip · hash mốc đo · ai/máy/lượt · negative-control
② PHẢN BIỆN (W1/W2): mâu thuẫn trong đặc tả → LIỆT KÊ; tiền đề sai → từ chối kèm số đo
③ 🟡/🛑 + TỰ KHAI (W4): sự cố tự khai kèm số đo; 🟡 cũ chưa được trả lời → nêu lại tại đây
④ CẦN XÁC MINH / DUYỆT (M10): mọi điều đã tự quyết trong khoảng mờ
