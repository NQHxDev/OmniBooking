# Tài liệu Kỹ thuật: Xử lý Thanh toán & Nhất quán Giao dịch (Payment Processing)

Tài liệu này mô tả chi tiết quy trình xử lý thanh toán, webhook callback từ nhà cung cấp bên thứ ba (như MoMo/Visa), cơ chế ngăn chặn giao dịch trùng lặp và đảm bảo nhất quán dữ liệu trong hệ thống **OmniBooking**.

---

## 1. Vòng đời Giao dịch Thanh toán (Payment Transaction Lifecycle)

Khi khách hàng chọn phương thức thanh toán online (Momo/Visa), đặt phòng được tạo ở trạng thái `PENDING_PAYMENT` và phát sinh một yêu cầu đặt cọc:

1. **Khởi tạo thanh toán:** Hệ thống tạo đường link thanh toán và trả về cho client. Trạng thái đặt phòng là `PENDING_PAYMENT`.
2. **Khách hàng thanh toán:** Khách thực hiện giao dịch trên cổng thanh toán của đối tác.
3. **Webhook Callback:** Cổng thanh toán gửi thông báo (IPN/Callback Webhook) về máy chủ OmniBooking để xác nhận kết quả thanh toán.
4. **Xử lý và Hoàn thành:** Trạng thái đặt phòng chuyển sang `CONFIRMED` và ghi nhận một bản ghi giao dịch thành công.

---

## 2. Xử lý Trùng lặp Callback (Duplicate Callback Protection)

Cổng thanh toán bên thứ ba có thể gửi lại webhook nhiều lần (do mạng trễ, cấu hình retry, hoặc người dùng reload trang). Hệ thống bảo vệ ở 2 cấp độ:

### Cấp độ 1: Cập nhật Trạng thái Nguyên tử (Atomic Confirmation)

Hệ thống sử dụng câu lệnh SQL nguyên tử có điều kiện để thực hiện chuyển đổi trạng thái:

```sql
UPDATE booking SET status = 'CONFIRMED', expires_at = NULL, updated_at = :now
WHERE id = :bookingId AND status = 'PENDING_PAYMENT'
```

- **rowsUpdated == 1:** Đây là lần callback hợp lệ đầu tiên xử lý đặt phòng. Tiến hành ghi nhận giao dịch và kích hoạt email.
- **rowsUpdated == 0:** Đặt phòng đã được confirm trước đó bởi một tiến trình khác (hoặc đã bị hủy/hết hạn). Hệ thống lập tức dừng lại và bỏ qua toàn bộ các side-effects phía sau.

### Cấp độ 2: Ràng buộc Duy nhất của ID giao dịch đối tác (Unique Constraint)

Bảng `transactions` cấu hình ràng buộc `UNIQUE` trên cột `provider_transaction_id` (mã giao dịch do Momo/Visa cấp):

- Khi callback đầu tiên ghi nhận giao dịch thành công vào DB, nó sẽ ghi lại `provider_transaction_id`.
- Nếu callback thứ hai cố gắng ghi cùng một giao dịch, database sẽ ném ra ngoại lệ `DataIntegrityViolationException` (xung đột khóa duy nhất).
- Hệ thống bắt ngoại lệ này, log thông tin và bỏ qua một cách an toàn mà không làm rollback tiến trình đặt phòng hay tạo giao dịch kép.

---

## 3. Nhất quán qua Transactional Outbox (Outbox Pattern Integration)

Việc gửi email xác nhận đặt phòng cho khách hàng (thông qua SMTP/Mail service) chứa đựng nguy cơ lỗi mạng. Nếu gửi email đồng bộ trực tiếp trong transaction xử lý payment:

- Một lỗi kết nối mạng với máy chủ email sẽ gây lỗi ngoại lệ và làm rollback toàn bộ transaction thanh toán (hủy kết quả cập nhật trạng thái `CONFIRMED` và giao dịch trong DB). Việc này dẫn đến việc khách đã bị trừ tiền cọc nhưng hệ thống vẫn báo chưa thanh toán.

**Giải pháp:**

1. Trong cùng một transaction xử lý payment, hệ thống lưu một bản ghi sự kiện `EmailEvent` vào bảng `outbox_events`.
2. Giao dịch DB commit thành công, xác nhận kết quả thanh toán chắc chắn đã được lưu.
3. Một worker bất đồng bộ (`OutboxWorker`) sẽ quét bảng `outbox_events` để gửi email. Nếu gửi lỗi, worker sẽ retry sau mà không ảnh hưởng tới kết quả thanh toán của đặt phòng.
