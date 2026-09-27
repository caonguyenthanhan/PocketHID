---
name: pockethid-workflow
version: "1.4"
project: PocketHID
extends: "D:\\An-tool-ecosystem\\.claude\\skills\\multi-agent-workflow\\SKILL.md"
last_updated: "2026-09-27"
status: active
---

# PocketHID — Quy Trình Multi-Agent Workflow (Extends v1.4)

Tài liệu này định nghĩa cấu hình và phân vai riêng của dự án PocketHID, kế thừa toàn bộ luật chơi chuẩn nền tại `multi-agent-workflow` v1.4 (`D:\An-tool-ecosystem\.claude\skills\multi-agent-workflow\SKILL.md`).

---

## 1. Đội Hình 4 Tầng & Bảng Model-Mỗi-Ghế

```
OWNER (Human / Aleian.C — phê duyệt 🛑, accept cuối)
  → MANAGER (Claude chat — luật chơi, audit digest)
    → AI-MASTER (Ghế T7 — Gemini 3.8 Flash: phân rã wave, soạn envelope, nghiệm thu)
      → AI-WORKER-1 (Ghế T8/T9: thi công code trong RÀO)
      → AI-REVIEWER (Ghế AI-r: khác hệ model với worker — audit A/B/C)
```

### Bảng Model-Mỗi-Ghế (Hiệu lực từ 2026-09-27)

| Ghế | Vai trò | Model (hệ) | Tier | Từ ngày | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AI-master** | Điều phối, chẻ wave, nghiệm thu | Gemini 3.8 Flash | T7 | 2026-09-27 | Ghế T7, tay trên git (spike/ điều phối) |
| **AI-worker-1** | Thi công tính năng trong RÀO | Claude 3.5 Sonnet / GPT-4o-mini | T8/T9 | 2026-09-27 | Thi công độc lập, tuân thủ W1–W8 |
| **AI-r (reviewer)** | Phản biện, mutant test, audit diff | Claude 3.7 Sonnet / DeepSeek | T-R | 2026-09-27 | **Bắt buộc khác hệ** với ghế worker |

---

## 2. Quy Tắc Riêng Dự Án PocketHID

1. **Thứ tự nghiệm thu:** `accepted (owner) → merged` là chuẩn bắt buộc.
2. **Kỷ luật phiên & wave:**
   - 1 phiên Master = 1 wave.
   - Master đóng phiên ngay sau khi xuất digest wave và rewrite `.agent/sessions/master-state.md`.
   - `master-state.md` tuân thủ trần 40 dòng, con-trỏ-không-chép (skill `session-handoff`).
3. **Envelope:**
   - Soạn từ `envelope-template.md` v1.1.
   - Chạy đủ `lint-envelope.md` v1.3 (17 mục bắt buộc: 1–16 + 18, mục 17 n/a do M21 PROPOSED) trước khi trình hoặc phát.
4. **Auto-🛑 cơ học (7 mục):**
   - Chạm 1 trong 7 danh mục (chi phí, dung lượng lớn, pháp lý, dữ liệu thật/khách, phân phối ngoài đội, secret/keys, nới lỏng cả code lẫn test trong cùng lần nộp) $\rightarrow$ Tự động 🛑, dừng chờ Owner quyết qua mục **CẦN XÁC MINH / DUYỆT**.
   - Mọi plan chẻ wave mới $\rightarrow$ 🛑 trình Owner phê duyệt trước khi phát envelope cho Worker.
5. **Vệ sinh ngữ cảnh (Context Hygiene):**
   - Tuân thủ `D:\An-tool-ecosystem\rules\context-hygiene.md`.
   - Quét và loại trừ cache build, file nén trùng lặp (`stitch_new_build_project.zip`) trước khi nạp context lớn.
