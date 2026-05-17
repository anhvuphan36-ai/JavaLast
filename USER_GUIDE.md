# Hướng dẫn sử dụng — AI-Powered CP Judge System

## 1. Màn hình Dashboard

Khi khởi động, bạn sẽ thấy màn hình chính với các nút chức năng:

- ** Nhập đề thi** — Tạo bài toán mới
- **🤖 AI Phân tích** — AI sinh testcase + code mẫu tự động
- **💻 Nộp code** — Nhập code và chấm bài
- ** Kết quả chấm** — Xem trạng thái chấm bài
- **📖 Hướng dẫn** — Xem tài liệu sử dụng (popup)
- **🚪 Thoát** — Đóng ứng dụng

Dashboard hiển thị thống kê: số đề thi, số code mẫu trong hệ thống.

---

## 2. Nhập đề thi mới (Problem Entry)

### 2.1. Nhập thông tin cơ bản
- **Tiêu đề**: Tên bài toán (ví dụ: "Tổng 2 số")
- **Nội dung đề**: Mô tả chi tiết đề bài, input/output format, ràng buộc
- **Loại kỳ thi**: ICPC / IOI / Codeforces / AtCoder / Other
- **Time limit**: Mặc định 2000ms (1-30000ms)
- **Memory limit**: Mặc định 256MB (1-2048MB)

### 2.2. Upload ảnh đề bài (tùy chọn)
- Bấm **Chọn ảnh...** → chọn file `.jpg`, `.png`, `.gif`, `.bmp`, `.webp`
- Ảnh sẽ được lưu vào `JudgeSystemData/Problems/problem_X/`
- AI có thể đọc ảnh này nếu bạn không nhập text

### 2.3. Lưu đề thi
- Bấm **💾 Lưu đề mới** → Tạo đề mới, ID hiển thị trong log
- Bấm **🔄 Cập nhật** → Sửa đề đã chọn trong danh sách bên trái
- Bấm **📄 Mới** → Xóa trắng form để nhập đề khác
- **🗑 Xóa đề** → Xóa đề + tất cả testcase, code mẫu, submission liên quan

> **Lưu ý:** Sau khi lưu đề, hệ thống tự động gọi AI phân tích và sinh testcase ngầm.

---

## 3. AI Phân tích & Sinh Testcase (AI Panel)

### 3.1. Chọn đề thi
- Từ dropdown, chọn đề vừa nhập
- Bấm **🔄 Tải lại** nếu đề chưa xuất hiện

### 3.2. Tùy chọn
- ☑ **Sinh checker script**: AI sẽ viết script Python checker nếu đề cần chấm tùy biến

### 3.3. Phân tích đề
- Bấm **🚀 Phân tích đề & Sinh testcase**
- Hệ thống gọi Gemini API, log hiển thị tiến trình
- **Nhấn lại nút** trong khi đang chạy để **hủy** tác vụ AI

**Kết quả mong đợi:**
- Mô tả ngắn gọn bài toán
- 5+ testcase đa dạng (small, large, edge, normal)
- Code AC (nếu AI sinh được) lưu vào hệ thống
- Output của testcase được tính bằng cách chạy code AC → **chính xác 100%**

> **Lưu ý**: Cần kết nối Internet và API Key hợp lệ trong `config.properties`.

### 3.4. Quản lý testcase
- **📋 Xem testcase hiện có**: Hiển thị tất cả testcase của đề
- **🗑 Xóa testcase AI**: Xóa chỉ testcase do AI sinh (giữ testcase thủ công)
- **💥 Xóa tất cả TC**: Xóa toàn bộ testcase của đề

---

## 4. Nộp code mẫu / Chấm thử (Code Submit)

### 4.1. Nhập code
- Chọn đề thi, ngôn ngữ (`java`, `cpp`, `python`), và loại code (`AC`, `WA`, `TLE`)
- Dán code vào textarea HOẶC bấm **📁 Chọn file** để load từ file

### 4.2. AI Sinh Code Mẫu
- Bấm **🤖 AI Sinh Code Mẫu** → chọn sinh 1 ngôn ngữ hoặc cả 3
- AI sẽ sinh code theo loại: AC (đúng), WA (sai), TLE (chậm)

### 4.3. Lưu code mẫu
- Bấm **💾 Lưu code mẫu** để lưu vào CSDL mà không chấm

### 4.4. Chấm thử
- Bấm **⚖️ Chấm thử** để hệ thống:
  1. Compile code
  2. Chạy với tất cả testcase của đề
  3. So sánh output với expected output
  4. Ghi kết quả vào bảng

**Giải thích kết quả:**
| Loại code | Kết quả mong đợi | Ý nghĩa |
|-----------|------------------|---------|
| AC | Tất cả AC ✅ | Testcase ĐÚNG, code chạy đúng |
| WA | Có WA ✅ | Testcase ĐỦ MẠNH, bắt được code sai |
| TLE | Có TLE ✅ | Testcase ĐỦ LỚN, bắt được code chậm |

### 4.5. Tạo Expected Output từ code
- Bấm **📐 Tạo Expected Output từ code này**
- Chạy code hiện tại với từng testcase input, lấy kết quả làm expected output mới
- **CHỈ DÙNG KHI CODE LÀ CODE AC CHÍNH XÁC**

---

## 5. Xem kết quả chấm (Result Panel)

Bảng hiển thị tất cả submissions với màu sắc:

| Màu | Ý nghĩa |
|---|---|
| 🟢 **Xanh lá** | **AC** — Accepted (đúng hoàn toàn) |
| 🔴 **Đỏ** | **WA** — Wrong Answer (output sai) |
| 🟡 **Vàng** | **TLE** — Time Limit Exceeded (quá thời gian) |
| ⚪ **Xám** | **RE/CE/MLE** — Runtime/Compile/Memory Error |

- **Lọc theo đề**: Dropdown chọn đề cụ thể hoặc "Tất cả đề thi"
- **Tải kết quả**: Refresh bảng
- **Xóa hiển thị**: Xóa bảng (không xóa dữ liệu trong DB)

---

## 6. Luồng thao tác đề xuất

```
Bước 1: Nhập đề thi (Problem Entry)
    ↓
Bước 2: Dùng AI sinh testcase + solution (AI Panel)
    ↓
Bước 3: Nhập code WA/TLE để kiểm tra độ mạnh testcase (Code Submit)
    ↓
Bước 4: Xem kết quả, đánh giá testcase có đủ mạnh không (Result Panel)
    ↓
Bước 5: Nếu testcase yếu → quay lại AI Panel yêu cầu sinh thêm edge case
```

---

## 7. Ví dụ cụ thể: Đề "Alpha Country"

**Bước 1 — Đề đã có sẵn trong hệ thống:**
Sau khi khởi động Docker, bài "Alpha Country" đã tự động load vào CSDL.
Vào Dashboard → chọn "Alpha Country" trong danh sách đề thi.

**Bước 2 — Xem testcase có sẵn:**
Vào AI Panel → chọn đề "Alpha Country" → bấm "📋 Xem testcase hiện có"
→ Hệ thống hiển thị 14 testcase đã được load sẵn (small, normal, edge, anti-wa).

**Bước 3 — Chấm code AC có sẵn:**
Vào Code Submit → chọn đề "Alpha Country" → chọn code mẫu AC → bấm "⚖️ Chấm thử"
→ Kết quả: **14/14 AC** ✅ — Testcase đúng, code chạy đúng.

**Bước 4 — Chấm code WA có sẵn:**
Chọn code mẫu WA (bug index) → bấm "️ Chấm thử"
→ Kết quả: **Có WA** ✅ — Testcase đủ mạnh, bắt được lỗi sai index.

**Bước 5 — Chấm code TLE có sẵn:**
Chọn code mẫu TLE (O(n³)) → bấm "⚖️ Chấm thử"
→ Kết quả: **Có TLE** ✅ — Testcase n=500 đủ lớn, bắt được code chậm.

---

## 8. Mẹo sử dụng

- **Testcase đủ mạnh?** Nhập code cố tình sai (thiếu modulo, overflow int) để xem AI testcase có bắt được không.
- **Không có mạng?** Bạn vẫn có thể nhập đề + testcase + code thủ công, chỉ không dùng được AI.
- **Backup dữ liệu**: Thư mục `./JudgeSystemData/` chứa toàn bộ file vật lý. Database có thể backup bằng `mysqldump`.
- **Dọn dẹp**: Khi đóng ứng dụng, thư mục tạm (`%TEMP%\JudgeSystem`) tự động được xóa.
- **Chạy tests**: `mvn clean test` — 37 tests cover JudgeEngine, FileManager, CodeFormatter, DAO, ProblemService.
