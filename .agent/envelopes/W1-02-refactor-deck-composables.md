<!-- envelope theo template-version 1.1 -->

## 0. PHẢN HỒI ĐI XUỐNG (M7)
- Kết quả L3 của task trước: Đạt [đã đo — build assembleDebug thành công tại commit 689a5f3]
- Trả lời TỪNG 🟡 worker đã nộp: khối 0 trống — task thứ hai của Wave 1.
- Ghi công: Không có.
- Luật mới có hiệu lực từ wave này: Tuân thủ quy chuẩn multi-agent v1.4, M12 đối chiếu board, M1/M3 nhãn [đã đo]/[chưa đo].

## 1. ĐỊNH VỊ & SKILL
Dự án: PocketHID. Ghế nhận việc: AI-Worker-2 (T8). Model ghế: Claude 3.5 Sonnet / GPT-4o.
Đọc trước khi làm: `D:\An-tool-ecosystem\.claude\skills\multi-agent-workflow\SKILL.md`.

## 2. NGỮ CẢNH TỰ CHỨA (chống rời rạc hoá)
- File ngữ cảnh phải đọc: .agent/reports/wave0-codemap.md, .agent/reports/wave0-review.md, todo.md
- Trạng thái kế thừa (re-verify trên main NGAY TRƯỚC khi phát):
  - Nhánh main sạch, HEAD tại 689a5f3 [đã đo]
  - Hàm `LandscapeDeckScreen` dài 915 dòng trong file `LandscapeDeckScreen.kt` 998 dòng [đã đo]
  - Đang quản lý cả 3 Zone (Zone 1 Macros/D-Pad, Zone 2 QWERTY, Zone 3 Numpad/Trackpad) trong 1 file monolithic [đã đo]
- Interface/contract liên quan: Giữ nguyên toàn bộ public composable signature `LandscapeDeckScreen(transport: InputTransport, onExitLandscape: () -> Unit, onOpenSettings: () -> Unit, onOpenPalette: () -> Unit, modifier: Modifier = Modifier)`.
- ĐIỂM DỄ SAI NHẤT của task này: Làm mất các state nội bộ (`activeSubMode`, `modifierState`, `hapticFeedbacks`) hoặc gây recomposition lag do truyền lambda không ổn định (`rememberUpdatedState`).

## 3. NHIỆM VỤ & HỢP ĐỒNG I/O
Việc cần làm: Tách nhỏ file `LandscapeDeckScreen.kt` (998 dòng) thành các Composable con chuyên trách đặt trong package `dev.aleian.pockethid.ui.screens.deck.*`:
  - `DeckMacroZone.kt`: Left Deck Macros, D-Pad, Layer Selector
  - `DeckKeyboardZone.kt`: Main 75% Virtual Keyboard Grid & Scancode buffer
  - `DeckUtilityZone.kt`: Right Deck Numpad & Mini-Trackpad Switcher
Giữ file `LandscapeDeckScreen.kt` làm orchestrator ngắn gọn (< 250 dòng).
Input: `app/src/main/java/dev/aleian/pockethid/ui/screens/LandscapeDeckScreen.kt`.
Output: File `LandscapeDeckScreen.kt` đã refactor + 3 files sub-components mới trong `app/src/main/java/dev/aleian/pockethid/ui/screens/deck/`.
Task 2/3 của Wave 1.

## 4. RÀO PHẠM VI
- CHỈ được sửa/tạo:
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/LandscapeDeckScreen.kt`
  - `app/src/main/java/dev/aleian/pockethid/ui/screens/deck/*`
- KHÔNG được chạm: Tất cả các package khác (`transport/`, `service/`, `mapping/`, `model/`, và các screen khác).
- RÀO tài nguyên chia sẻ: nhánh `feat/w1-02-refactor-deck`
- Nhánh gốc: `main` @ `689a5f3` · đã `ls-tree` các path sửa-file-có-sẵn: 1/1 (`LandscapeDeckScreen.kt` có sẵn)
- Hạn mức diff: lõi ≤ 300 dòng / vỏ ≤ 900 dòng / test+tài liệu KHÔNG hạn mức.

## 5. TIÊU CHÍ HOÀN THÀNH
- Test phải pass: `./gradlew.bat assembleDebug` pass 100%, không phát sinh warning/error biên dịch Compose.
- Negative control (W8): Cố tình đổi sai kiểu tham số trong `DeckMacroZone` -> build PHẢI đỏ báo lỗi type mismatch.
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
