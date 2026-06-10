# Tài liệu Kỹ thuật: Chiến lược Bảo vệ Tính Khả trùng (Idempotency Strategy)

Tài liệu này trình bày chi tiết cơ chế bảo vệ chống lặp yêu cầu gửi lên từ Client (Double click, retry do lỗi mạng) và chống lặp sự kiện trong xử lý bất đồng bộ trong hệ thống **OmniBooking**.

---

## 1. Idempotency cấp độ Request-API (Request-level Idempotency)

Đối với các API thay đổi dữ liệu nhạy cảm như tạo đặt phòng (`/api/v1/bookings`), hệ thống yêu cầu Client truyền kèm header `X-Idempotency-Key` (một chuỗi UUID được sinh ngẫu nhiên ở client cho mỗi phiên làm việc).

### Luồng xử lý chi tiết (IdempotencyAspect):

1. **Kiểm tra Header:** Aspect interceptor bắt các request có chứa header `X-Idempotency-Key`.
2. **Tạo Hash Body:** Tính toán mã băm SHA-256 của Request Body.
3. **Tra cứu trong Redis Cache:**
   - Hệ thống tìm kiếm key tương ứng trong Redis với format: `idempotency:<key>`.
   - **Trường hợp 1 (Key đã tồn tại và mã băm trùng khớp):** Trả về ngay lập tức phản hồi (response) đã được lưu từ Redis mà không gọi vào luồng xử lý service hoặc DB.
   - **Trường hợp 2 (Key đã tồn tại nhưng mã băm khác biệt):** Hệ thống từ chối yêu cầu và ném ra mã lỗi `IDEMPOTENCY_CONFLICT` (HTTP 409 Conflict) vì cùng một key nhưng nội dung request bị thay đổi.
   - **Trường hợp 3 (Key chưa tồn tại):** Tạo một khóa tạm thời (Lock) trong Redis với thời gian hết hạn (TTL) mặc định (ví dụ: 120 giây) để biểu diễn request đang được xử lý.
4. **Thực thi và Lưu phản hồi:**
   - Gọi tiếp tục luồng xử lý nghiệp vụ chính.
   - Sau khi hoàn thành và có phản hồi thành công, cập nhật thông tin phản hồi (Response status & body) và mã băm SHA-256 vào Redis key.

---

## 2. Idempotency cấp độ Xử lý Sự kiện (Event-level Idempotency)

Đối với các consumer lắng nghe sự kiện bất đồng bộ từ hàng đợi tin nhắn (Kafka):

- **Vấn đề:** Kafka đảm bảo phân phối tin nhắn ít nhất một lần (at-least-once). Điều này có nghĩa tin nhắn có thể bị gửi lặp lại trong trường hợp lỗi mạng khi commit offset.
- **Giải pháp:** Sử dụng bảng `processed_events` trong PostgreSQL để lưu lịch sử các event ID đã xử lý.
- **Quy trình:**
   1. Khi nhận được tin nhắn sự kiện, lấy `event_id` từ header của tin nhắn.
   2. Thực hiện chèn `event_id` vào bảng `processed_events` trong cùng transaction xử lý nghiệp vụ.
   3. Nếu chèn bị lỗi trùng lặp khóa chính (duplicate key), transaction tự động rollback và consumer chỉ cần bỏ qua tin nhắn lặp này một cách an toàn.

---

## 3. Chính sách Hết hạn (Expiration Policy)

- **Redis Idempotency Keys:** Các kết quả phản hồi của API được lưu giữ với thời gian TTL thích hợp (mặc định là 24 giờ) để đảm bảo client có thể lấy lại phản hồi trong trường hợp mất kết nối mạng ngay khi gửi request thành công.
- **Processed Events Ledger:** Bảng `processed_events` lưu lịch sử lâu dài để làm đối soát chéo và có tiến trình định kỳ dọn dẹp các sự kiện cũ hơn 30 ngày.
