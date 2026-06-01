# Kế Hoạch Triển Khai Luồng Đặt Phòng Kết Hợp & Đăng Ký Ngầm (Hybrid Booking & Lazy Sign-up)

### 1. Ý tưởng giải pháp (Product Design)
- **Guest Checkout**: Cho phép đặt phòng không cần tài khoản từ trước, chỉ yêu cầu Email + Số điện thoại liên hệ để tối đa hóa tỷ lệ chuyển đổi (Conversion Rate).
- **Email Validation**: Sử dụng **Redis Bloom Filter** để kiểm tra email người dùng ở trang điền thông tin:
  - Nếu email đã có tài khoản, hiển thị gợi ý đăng nhập để nhận ưu đãi Genius và điền thông tin tự động.
  - Nếu email chưa có tài khoản, cho phép thanh toán vãng lai bình thường.
- **Lazy Sign-up**: Sau khi đặt phòng thành công, tại trang Thank-you page và Email xác nhận, cung cấp tuỳ chọn đặt mật khẩu nhanh để kích hoạt tài khoản thành viên liên kết với email đặt phòng đó, nâng cấp tài khoản lên thành viên chính thức và được nhận ưu đãi Genius cho lần đặt sau.

### 2. Danh sách công việc (Checklist)
- [ ] **Frontend**: Thiết kế giao diện nhập thông tin liên hệ không yêu cầu đăng nhập.
- [ ] **Frontend**: Tích hợp check email với Bloom Filter của Backend.
- [ ] **Backend**: Cập nhật API tạo đơn đặt phòng (`/bookings`) cho phép tạo đơn vãng lai (không bắt buộc `userId`).
- [ ] **Backend**: Viết logic tự động tạo tài khoản tạm thời (`User` có trạng thái chưa kích hoạt) liên kết với email khách vãng lai.
- [ ] **Backend**: Gửi email kích hoạt tài khoản kèm mã xác nhận đặt phòng qua Kafka + Resend SDK.
- [ ] **Frontend**: Triển khai trang kích hoạt tài khoản nhanh từ link email.




