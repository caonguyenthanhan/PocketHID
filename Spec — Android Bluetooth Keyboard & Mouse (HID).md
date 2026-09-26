# Spec — Android Bluetooth Keyboard & Mouse (HID)

Sep 26, 2026 · @Aleian

## 1. Problem Statement

Người dùng cần điều khiển PC/Mac/TV box ở xa (trình chiếu, media center, server không màn hình, máy để ở góc phòng) nhưng không có bàn phím/chuột rời trong tay — điện thoại thì luôn có. Các app hiện có trên Play Store hoặc đòi cài server trên PC (Unified Remote, KDE Connect), hoặc chạy Wi-Fi nên phụ thuộc mạng LAN chung, hoặc đầy quảng cáo.

**Giải pháp:** app Android giả lập **thiết bị Bluetooth HID chuẩn** (Keyboard + Mouse combo). Host nhận nó như một bàn phím/chuột Bluetooth bình thường — **không cần cài phần mềm phía host**, hoạt động với mọi OS hỗ trợ BT HID (Windows, macOS, Linux, Android TV, smart TV, kể cả BIOS/UEFI trên một số máy).

**Ràng buộc định hình toàn bộ spec:** API `android.bluetooth.BluetoothHidDevice` chỉ có từ **Android 9 (API 28)** và yêu cầu chip BT của điện thoại hỗ trợ HID Device role (đa số máy từ 2019 trở đi đều có; một số hãng cắt profile này ở firmware). Không có đường tương đương trên iOS — đó là lý do scope chốt Android-only, Kotlin native.

## 2. Goals (v1)

1. **Zero-install phía host:** ghép đôi và dùng được với Windows 10/11, macOS 12+, Ubuntu 22.04+ mà không cài gì trên host.
2. **Độ trễ chuột cảm nhận được là "tức thì":** end-to-end (chạm → con trỏ di chuyển) ≤ 40 ms p95 trên kết nối BT Classic.
3. **Gõ không rớt ký tự:** gõ 60 WPM liên tục 5 phút, 0 ký tự mất hoặc lặp trên cả 3 OS.
4. **Kết nối lại tự động:** mở app khi host đã ghép đôi trước → dùng được trong ≤ 3 giây, không thao tác thêm.
5. **Pin:** dùng liên tục 1 giờ tiêu ≤ 5% pin điện thoại (màn hình sáng đã tính).

Mục tiêu 1 và 3 là điều kiện ship; 2, 4, 5 là ngưỡng chất lượng đo ở giai đoạn beta.

## 3. Non-Goals (v1)

| Ngoài scope | Lý do |
| --- | --- |
| iOS | Không có API HID peripheral công khai; cần kiến trúc khác hoàn toàn (Wi-Fi + agent trên host) |
| Wi-Fi / mạng LAN làm kênh truyền | Mất lợi thế zero-install; nếu cần thì là v2 dưới dạng transport thứ hai |
| Truyền màn hình host về điện thoại (screen mirroring) | Không thể qua HID; là sản phẩm khác |
| Gamepad / joystick HID | Report descriptor khác, UI khác; tách thành module riêng nếu có nhu cầu |
| Bàn phím ngôn ngữ ngoài US-QWERTY / gõ tiếng Việt Telex | HID gửi *scancode*, không gửi ký tự — layout do host quyết định. Tiếng Việt cần IME phía host hoặc chiến lược Unicode input riêng theo OS — đã chốt: bộ gõ phía host lo (Unikey / EVKey / ibus-unikey) |
| Tính năng macro, clipboard sync, file transfer | Không phải HID; giữ app đúng một việc |
| Hỗ trợ Android < 9 | Không có `BluetoothHidDevice`; đường cũ (root + hciconfig) không bền |

## 4. User Stories

**Persona chính — người trình chiếu / dùng media PC (theo thứ tự ưu tiên):**

1. Là người trình chiếu, tôi muốn ghép đôi điện thoại với laptop một lần rồi lần sau mở app là dùng được ngay, để không phải loay hoay trước khán giả.
2. Là người dùng media PC, tôi muốn vuốt trên màn hình để di con trỏ và chạm để click, để điều khiển từ ghế sofa mà không cần chuột rời.
3. Là người dùng, tôi muốn gõ văn bản (URL, mật khẩu, lệnh terminal) bằng bàn phím điện thoại, để không phải lết ra máy.
4. Là người dùng, tôi muốn có phím Ctrl/Alt/Shift/Win giữ được (sticky) và phím mũi tên, Esc, Tab, để thao tác shortcut và di chuyển trong terminal.
5. Là người dùng, tôi muốn cuộn trang bằng 2 ngón, để đọc tài liệu từ xa.
6. Là người dùng, tôi muốn thấy rõ trạng thái *đã kết nối với máy nào*, để không gõ nhầm sang máy khác.

**Persona phụ — IT/admin (Aleian):**

7. Là IT, tôi muốn dùng điện thoại làm bàn phím tạm cho server/workstation không màn hình hoặc máy vừa cài lại, để không phải mang bàn phím đi.
8. Là IT, tôi muốn app hoạt động cả trong màn hình đăng nhập Windows và BIOS (nếu chip BT máy hỗ trợ), để xử lý được lúc OS chưa lên.

**Edge cases phải có story:**

- Host tắt/ngủ giữa phiên → app báo mất kết nối trong ≤ 2 s, không crash, tự reconnect khi host quay lại.
- Điện thoại không hỗ trợ HID Device role → app báo rõ ngay ở màn đầu, không để user thử ghép đôi vô ích.
- User đổi app / khóa màn hình → kết nối giữ được ≥ 5 phút ở nền (foreground service), sau đó có thể ngắt để tiết kiệm pin (tùy setting).

## 5. Requirements

### P0 — Must-have (không có thì không ship)

| ID | Yêu cầu | Acceptance criteria |
| --- | --- | --- |
| P0-1 | Đăng ký app làm BT HID Device (combo keyboard + mouse, một report descriptor, 2 report ID) | Host liệt kê thiết bị là "Keyboard" và "Mouse" (Windows Device Manager / macOS System Info) sau khi ghép đôi |
| P0-2 | Kiểm tra khả năng HID Device role khi khởi động | Máy không hỗ trợ → màn chặn có giải thích, không vào tab chính |
| P0-3 | Luồng ghép đôi: app bật discoverable, hướng dẫn user chọn thiết bị từ phía host | Ghép đôi thành công trên Win11, macOS 14, Ubuntu 24.04 theo hướng dẫn trong app, không cần đọc tài liệu ngoài |
| P0-4 | Tab Mouse: trackpad relative, tap = left click, 2-finger tap = right click, 2-finger drag = scroll dọc | Mỗi gesture ra đúng hành động trên 3 OS; con trỏ không nhảy khi nhấc ngón |
| P0-5 | Tab Keyboard: nhận ký tự từ IME hệ thống, ánh xạ ra HID usage code (US layout), có hàng modifier + phím đặc biệt (Esc, Tab, Enter, Backspace, Del, mũi tên, F1–F12) | Chuỗi kiểm thử 200 ký tự ASCII in ra khớp 100%; Ctrl+C / Alt+Tab / Cmd+Space hoạt động |
| P0-6 | Trạng thái kết nối luôn hiển thị (tên host, trạng thái Connected/Connecting/Disconnected) | Ngắt host → UI đổi trạng thái ≤ 2 s |
| P0-7 | Auto-reconnect với host đã ghép đôi khi mở app | ≤ 3 s từ mở app đến gửi được report |
| P0-8 | Foreground service giữ kết nối khi app ở nền / màn hình tắt | Kết nối sống ≥ 5 phút với màn hình tắt |
| P0-9 | Quyền runtime: BLUETOOTH\_CONNECT, BLUETOOTH\_ADVERTISE (API 31+), có giải thích trước khi xin | Từ chối quyền → thông báo rõ, có nút mở Settings |

### P1 — Nice-to-have (fast follow sau launch)

| ID | Yêu cầu |
| --- | --- |
| P1-1 | Danh sách nhiều host đã ghép đôi, chuyển nhanh (nút Switch) |
| P1-2 | Chỉnh tốc độ con trỏ, gia tốc, đảo chiều scroll, kích cỡ phím |
| P1-3 | Haptic feedback khi click / gõ (tắt được) |
| P1-4 | Chế độ "Presenter": nút to Next/Prev/Blank/Start slideshow (map sang Page Down/Up, B, F5/Shift+F5) |
| P1-5 | Volume / Media keys (Consumer Control usage page, thêm report ID thứ 3) |
| P1-6 | Chế độ tối, giữ màn hình sáng khi đang kết nối |
| P1-7 | Nhập chuỗi văn bản dài (paste từ clipboard điện thoại) → gửi tuần tự với rate limit an toàn |

### P2 — Future considerations (thiết kế để không chặn đường)

- **Transport thứ hai qua Wi-Fi + agent trên host** cho máy không có BT — tách lớp `InputTransport` ngay từ v1 để cắm thêm sau.
- **Gõ Unicode / tiếng Việt** qua chiến lược theo OS (Alt+numpad trên Windows, Unicode Hex Input trên macOS, Ctrl+Shift+U trên GTK) — cần biết host OS, nên v1 lưu "OS hint" do user chọn khi ghép đôi.
- **Gamepad profile** — report descriptor tách riêng, không nhét vào combo hiện tại.
- **BLE HID (HOGP)** thay cho BT Classic để tiết kiệm pin — Android `BluetoothHidDevice` hiện chỉ đi Classic; theo dõi API.

## 6. UI Spec — 2 tab

**Khung chung (mọi màn):** thanh trạng thái trên cùng gồm chấm màu (xanh = Connected, vàng = Connecting, xám = Disconnected) + tên host + nút Pair/Switch. Bottom navigation 2 tab: **Keyboard** | **Mouse**. Chuyển tab không làm mất kết nối, không reset trạng thái modifier.

### Tab Mouse

| Vùng | Hành vi |
| --- | --- |
| Trackpad (≥ 70% chiều cao màn) | 1 ngón kéo → di chuyển relative (dx, dy). Tap 1 ngón → left click. Tap 2 ngón → right click. Kéo 2 ngón → scroll dọc (và ngang nếu bật). Double-tap-and-hold → drag (giữ nút trái) |
| Hàng nút dưới | Left · Middle · Right (giữ = hold). Có thể ẩn trong setting |
| Gia tốc | Curve mặc định: `v_out = v_in × (1 + 0.5 × min(v_in/8, 1))`; user điều chỉnh hệ số 0.5–3.0 (P1) |
| Dead zone | Bỏ chuyển động < 1 px trong 30 ms đầu sau touch-down để tap không kéo con trỏ |

### Tab Keyboard

| Vùng | Hành vi |
| --- | --- |
| Ô nhập ẩn (`EditText` transparent, luôn focus) | Nhận ký tự từ IME hệ thống của điện thoại → chuyển từng ký tự sang keycode + modifier → gửi press/release. Không lưu nội dung |
| Hàng modifier (sticky toggle) | Ctrl · Shift · Alt · Win/Cmd · Fn. Trạng thái bật hiển thị rõ; tự tắt sau khi gõ 1 phím thường (trừ khi giữ 2 lần = lock) |
| Hàng phím đặc biệt (cuộn ngang) | Esc · Tab · ↑↓←→ · Home · End · PgUp · PgDn · Ins · Del · F1–F12 · PrtSc |
| Nút gửi IME | Mở/đóng bàn phím hệ thống |

**Không làm:** không vẽ bàn phím QWERTY riêng trong v1 — dùng IME hệ thống để có autocorrect, swipe, và giảm 80% khối lượng UI.

## 7. Kiến trúc kỹ thuật

**Stack:** Kotlin, minSdk 28, targetSdk 36 (máy test chạy Android 16), Jetpack Compose, Coroutines/Flow, Hilt. Không thư viện BT bên thứ ba — `BluetoothHidDevice` trong AOSP là đủ.

### Phân lớp

| Lớp | Trách nhiệm | Thành phần chính |
| --- | --- | --- |
| UI | 2 tab, thanh trạng thái, thu gesture/keystroke | `MouseScreen`, `KeyboardScreen`, `ConnectionBar` (Compose) |
| Input mapping | Touch → (dx, dy, buttons, wheel); ký tự → (modifier, usage code) | `GestureInterpreter`, `KeyMapper` (bảng US-QWERTY → HID Usage Page 0x07) |
| Report builder | Đóng gói thành byte array đúng report ID, throttle chuột \~125 Hz, gộp dx/dy giữa 2 frame | `HidReportEncoder` |
| Transport (interface `InputTransport`) | Gửi report, phát trạng thái kết nối | `BtHidTransport` (v1); `WifiTransport` (P2) |
| HID service | Giữ `BluetoothHidDevice` proxy, register/unregister app, callback connect state, quản lý SDP record | `HidDeviceService` (foreground service, `START_STICKY`) |
| Persistence | Host đã ghép đôi, OS hint, setting | DataStore |

### Luồng khởi tạo HID

1. `BluetoothAdapter.getProfileProxy(ctx, listener, BluetoothProfile.HID_DEVICE)` → nhận `BluetoothHidDevice`.
2. `registerApp(sdpSettings, null, qosOut, executor, callback)` với `BluetoothHidDeviceAppSdpSettings(name, description, provider, subclass = COMBO (0xC0), descriptors)`.
3. `onAppStatusChanged(registered=true)` → bật discoverable (`ACTION_REQUEST_DISCOVERABLE`) nếu chưa có host đã ghép đôi; ngược lại `connect(device)`.
4. `onConnectionStateChanged(STATE_CONNECTED)` → mở cửa gửi `sendReport(device, reportId, bytes)`.
5. Thoát app → `disconnect` → `unregisterApp` → `closeProfileProxy` (không unregister sẽ để lại SDP record ma, host lần sau ghép đôi lỗi).

### HID Report Descriptor (combo, 2 report ID)

```
// Report ID 1 — Keyboard (8 byte: modifiers, reserved, 6 keycodes)
0x05,0x01, 0x09,0x06, 0xA1,0x01, 0x85,0x01,
  0x05,0x07, 0x19,0xE0, 0x29,0xE7, 0x15,0x00, 0x25,0x01,
  0x75,0x01, 0x95,0x08, 0x81,0x02,            // 8 modifier bits
  0x95,0x01, 0x75,0x08, 0x81,0x01,            // reserved byte
  0x95,0x06, 0x75,0x08, 0x15,0x00, 0x25,0x65,
  0x05,0x07, 0x19,0x00, 0x29,0x65, 0x81,0x00, // 6 key array
0xC0,
// Report ID 2 — Mouse (4 byte: buttons, dx, dy, wheel)
0x05,0x01, 0x09,0x02, 0xA1,0x01, 0x85,0x02, 0x09,0x01, 0xA1,0x00,
  0x05,0x09, 0x19,0x01, 0x29,0x03, 0x15,0x00, 0x25,0x01,
  0x95,0x03, 0x75,0x01, 0x81,0x02,            // 3 buttons
  0x95,0x01, 0x75,0x05, 0x81,0x01,            // padding
  0x05,0x01, 0x09,0x30, 0x09,0x31, 0x09,0x38,
  0x15,0x81, 0x25,0x7F, 0x75,0x08, 0x95,0x03, 0x81,0x06, // X, Y, wheel relative
0xC0, 0xC0
```

**Quy ước gửi:** mỗi phím = 2 report (press rồi release, cách ≥ 8 ms); chuột gộp dx/dy tích lũy rồi flush theo tick 8 ms, kẹp về \[−127, 127\]; scroll gửi ±1 mỗi \~40 px ngón di chuyển. Output report từ host (LED Caps/Num) nhận qua `onSetReport` — v1 chỉ log.

### Threading

- Gesture/keystroke thu trên main thread → đẩy vào `Channel<InputEvent>(CONFLATED cho mouse, UNLIMITED cho key)`.
- Một coroutine trên `Dispatchers.Default` tiêu thụ channel, encode, `sendReport` (API này blocking nhẹ, không gọi trên main).
- Trạng thái kết nối phát qua `StateFlow<ConnectionState>` cho UI.

## 8. Tương thích host và điểm cần lưu ý

| Host | Mức hỗ trợ v1 | Lưu ý đã biết (cần xác minh khi test) |
| --- | --- | --- |
| Windows 10/11 | Đầy đủ | Ghép đôi phải khởi phát từ phía Windows (Add device). Windows có thể cache report descriptor theo địa chỉ BT — đổi descriptor giữa các build thì phải Remove device rồi ghép lại. Hoạt động ở màn login nếu driver BT của máy load sớm |
| macOS 12+ | Đầy đủ | macOS đôi khi hiện hộp "Keyboard Setup Assistant" lần đầu — cho nhấn Shift ngay bên trái Z rồi bỏ qua. Modifier Win map thành Cmd. Scroll direction mặc định ngược Windows |
| Ubuntu / Fedora (BlueZ 5.6x) | Đầy đủ | BlueZ cần `bluetoothd` với plugin `input` (mặc định có). Một số bản Wayland có gia tốc chuột riêng làm con trỏ nhanh hơn kỳ vọng |
| Android TV / Google TV | Cơ bản | Nhận keyboard tốt; con trỏ chuột hiện nhưng nhiều app TV không tối ưu cho chuột |
| Smart TV (Tizen, webOS) | Không cam kết | Chỉ chấp nhận thiết bị HID trong whitelist; test thử, không hứa |
| BIOS/UEFI | Tùy máy | Chỉ khi firmware máy có BT stack và host đã lưu link key — thường là laptop cao cấp; đưa vào kiểm thử khám phá, không đưa vào goal |

**Máy test chính — Redmi Note 17 Pro Max 5G:** [Snapdragon 6 Gen 5, Android 16 / HyperOS 3, Bluetooth 6.0](https://www.91mobiles.com/xiaomi-redmi-note-17-pro-max-5g-price-in-india) — API HID Device có sẵn ở tầng Android. **Rủi ro lớn nhất còn lại:** Xiaomi/HyperOS từng cắt HID Device profile trên một số model (`getProfileProxy` trả null hoặc `registerApp` thất bại im lặng); chưa có dữ liệu cho HyperOS 3 nên cổng giai đoạn 0 là bắt buộc. Nếu qua được, mở rộng ma trận test thêm 1 Pixel và 1 Samsung khi có máy mượn.

## 9. Success Metrics

| Chỉ số | Loại | Ngưỡng ship | Mục tiêu stretch | Cách đo |
| --- | --- | --- | --- | --- |
| Độ trễ chuột end-to-end p95 | Leading | ≤ 40 ms | ≤ 25 ms | Camera 240 fps quay đồng thời ngón tay và màn hình host, 50 mẫu |
| Tỷ lệ mất/lặp ký tự | Leading | 0 / 3.000 ký tự | — | Script gõ chuỗi biết trước qua app, diff với nội dung nhận trên host |
| Thời gian reconnect | Leading | ≤ 3 s p95 | ≤ 1,5 s | Log timestamp trong app từ `onCreate` đến `STATE_CONNECTED` |
| Tỷ lệ ghép đôi thành công lần đầu | Leading | ≥ 90% trên 3 OS × 4 máy test | 100% | Bảng test thủ công |
| Pin / giờ dùng liên tục | Leading | ≤ 5% | ≤ 3% | Battery Historian, màn hình 50% sáng |
| Crash-free sessions (nếu phát hành) | Lagging | ≥ 99% | ≥ 99,5% | Firebase Crashlytics sau 30 ngày |
| Retention D7 (nếu phát hành) | Lagging | ≥ 20% | ≥ 35% | Play Console |

Đã chốt dùng nội bộ → hai chỉ số lagging bỏ. Thay bằng một chỉ số thực dụng: **số lần mở app trong tuần beta ≥ 5** (log cục bộ) — dưới ngưỡng đó nghĩa là app chưa đủ tiện để thay bàn phím rời.

## 10. Open Questions

**Blocking — cần trả lời trước khi bắt đầu code:**

- [x] **(Owner)** Mục đích cuối: **dùng nội bộ / cá nhân** → bỏ Crashlytics, privacy policy, store listing; metrics lagging bỏ; giai đoạn 4 bỏ.
- [x] **(Owner)** Điện thoại phát triển/test: **Redmi Note 17 Pro Max 5G** (Snapdragon 6 Gen 5, Android 16 / HyperOS 3, Bluetooth 6.0) → API `BluetoothHidDevice` chắc chắn có; rủi ro còn lại là HyperOS cắt HID Device profile — vẫn phải qua cổng giai đoạn 0.
- [x] **(Owner)** Gõ tiếng Việt v1: **không** — bộ gõ phía host lo (Unikey trên Windows; EVKey/OpenKey trên macOS; ibus-unikey trên Linux). App chỉ gửi scancode US, gõ Telex trên IME điện thoại phải tắt.

**Non-blocking — giải quyết trong lúc làm:**

- [x] **(Eng)** Combo descriptor 1 SDP record trên Windows: **giữ combo** (đơn giản, 1 lần ghép đôi). Kiểm chứng ở giai đoạn 0; chỉ tách 2 record nếu Windows từ chối trên máy test.
- [x] **(Eng)** Tần suất flush chuột: **125 Hz mặc định**, hằng số cấu hình được; đo trên máy test ở giai đoạn 0, hạ 62,5 Hz nếu thấy giật.
- [x] **(Eng)** Foreground service type: **`connectedDevice`** (targetSdk 36), khai báo `FOREGROUND_SERVICE_CONNECTED_DEVICE`; xác nhận không bị kill sau 6 giờ ở beta.
- [x] **(Design)** Trackpad trong tab Keyboard: **không làm v1**; xem lại sau 1 tuần beta.
- [x] **(Owner)** Tên app: **PocketHID** (gọi đúng bản chất — thiết bị HID trong túi; dễ nhận ra giữa các app khác trên host). Package `dev.aleian.pockethid`. Phương án dự phòng: *TapDesk*, *BlueKM*. Icon: **có, nhưng tự sinh** — adaptive icon từ Android Studio Image Asset, nền một màu, glyph vector đơn giản (phím vuông + mũi tên con trỏ chồng góc phải dưới); không thuê designer.

## 11. Phasing

| Giai đoạn | Nội dung | Điều kiện qua cổng | Ước tính |
| --- | --- | --- | --- |
| 0 — Spike kỹ thuật | Skeleton app + `HidDeviceService` + descriptor, gửi được 1 report chuột và 1 phím "a" tới Windows và macOS | Con trỏ nhích, ký tự "a" hiện trên 2 OS từ máy test chính | 2–3 ngày |
| 1 — Core (P0-1 → P0-9) | 2 tab đầy đủ, ghép đôi, reconnect, foreground service, quyền | Toàn bộ P0 acceptance criteria pass trên Win11 + macOS + Ubuntu, trên ≥ 2 điện thoại khác hãng | 2 tuần |
| 2 — Beta nội bộ | Dùng thật 1 tuần trong công việc (trình chiếu, server không màn hình), đo metrics mục 9 | Metrics leading đạt ngưỡng ship; danh sách bug ≤ 5 lỗi P1 | 1–2 tuần |
| 3 — P1 fast follow | P1-1 → P1-7 theo ưu tiên từ feedback beta | Từng mục có acceptance riêng | 1–2 tuần |
| 4 — (tùy chọn) Play Store | Privacy policy, store listing, Crashlytics | Bỏ — đã chốt dùng nội bộ | 1 tuần |

Giai đoạn 0 là cổng quan trọng nhất: nếu điện thoại phát triển không hỗ trợ HID Device role, dừng và đổi máy test trước khi đầu tư thêm.
