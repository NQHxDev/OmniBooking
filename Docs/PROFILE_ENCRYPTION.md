# Searchable Phone Encryption (Blind Indexing) - OmniBooking

Tài liệu này mô tả cơ chế bảo mật số điện thoại (PII - Personally Identifiable Information) của người dùng nhưng vẫn đảm bảo khả năng tìm kiếm chính xác hiệu năng cao.

## 1. Vấn đề (Problem)

- **Bảo mật**: Số điện thoại là dữ liệu nhạy cảm, không nên lưu dưới dạng plain-text trong database để tránh lộ thông tin khi bị dump data.
- **Tìm kiếm**: Nếu chỉ mã hóa thông thường (như AES), dữ liệu mã hóa của cùng một số điện thoại sẽ khác nhau mỗi lần (do IV/Nonce), dẫn đến không thể dùng SQL `WHERE phone = '...'` để tìm kiếm.

## 2. Giải pháp (Solution): Blind Indexing

Chúng ta sử dụng kết hợp hai kỹ thuật:

1. **Mã hóa (Encryption)**: Dùng **AES-256-GCM** để lưu trữ. Dữ liệu này có thể giải mã để hiển thị cho người dùng.
2. **Blind Index (Hashing)**: Dùng **HMAC-SHA256** để tạo ra một "chỉ mục mù" dùng riêng cho việc tìm kiếm.

### Cơ chế hoạt động:

| Cột trong DB        | Thuật toán                        | Mục đích                        |
| :------------------ | :-------------------------------- | :------------------------------ |
| `phone_encrypted`   | AES-256-GCM + `ENCRYPTION_SECRET` | Giải mã để hiển thị trên UI.    |
| `phone_search_hash` | HMAC-SHA256 + `HASH_PEPPER`       | Dùng để tìm kiếm (Exact Match). |

## 3. Quy trình thực hiện (Workflow)

### A. Lưu trữ dữ liệu (Save/Update)

1. Nhận số điện thoại từ Client (VD: `0966111111`).
2. **Encrypt**: Tạo `phone_encrypted` bằng AES-256-GCM.
3. **Hash**: Tạo `phone_search_hash` bằng HMAC-SHA256 với một chuỗi `Pepper` bí mật.
4. Lưu cả hai vào database.

### B. Truy vấn dữ liệu (Search)

1. Nhận số điện thoại cần tìm từ Client.
2. Tạo Hash từ số điện thoại đó bằng cùng thuật toán HMAC và `Pepper` đã dùng lúc lưu.
3. Thực hiện SQL: `SELECT * FROM user_profiles WHERE phone_search_hash = :generated_hash`.
4. Nhờ có **Database Index** trên cột `phone_search_hash`, tốc độ tìm kiếm là **O(1)**.

## 4. Cấu hình hệ thống

Các bí mật được lưu trong file `.env`:

- `ENCRYPTION_SECRET`: Khóa mã hóa AES (32 bytes Hex/Text).
- `HASH_PEPPER`: Chuỗi bí mật dùng để hash (ngăn chặn tấn công Rainbow Table).

## 5. Lưu ý bảo mật

- **Pepper** phải được giữ bí mật tuyệt đối. Nếu mất Pepper, bạn không thể tạo lại Hash để tìm kiếm dữ liệu cũ.
- Sử dụng **AES-GCM** đảm bảo tính toàn vẹn của dữ liệu (Authentic Encryption), chống lại việc sửa đổi cipher-text.

---

_Tài liệu được khởi tạo ngày: 13/05/2026_
