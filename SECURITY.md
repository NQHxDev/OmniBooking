# Chính Sách Bảo Mật (Security Policy)

Chào mừng bạn đến với chính sách bảo mật của **OmniBooking**. Chúng tôi rất coi trọng việc bảo mật thông tin và sự an toàn của hệ sinh thái đặt phòng này.

---

## Phiên Bản Được Hỗ Trợ (Supported Versions)

Chúng tôi chỉ tích cực vá các lỗ hổng bảo mật cho phiên bản mới nhất trên nhánh `main` và các phiên bản release chính thức gần đây:

| Phiên Bản | Được Hỗ Trợ |
| :-------- | :---------: |
| >= 1.0.0  |     Có      |
| < 1.0.0   |    Không    |

---

## Báo Cáo Lỗ Hổng Bảo Mật (Reporting a Vulnerability)

Nếu bạn phát hiện ra một vấn đề liên quan đến bảo mật (lỗ hổng SQL Injection, CSRF bypass, rò rỉ PII, JWT/Session validation bypass...), **vui lòng KHÔNG mở một Issue công khai trên GitHub**. Việc công khai lỗ hổng có thể đặt dự án và thông tin của người dùng vào vòng nguy hiểm trước khi bản vá kịp hoàn thiện.

Thay vào đó, vui lòng thực hiện các bước sau để báo cáo bảo mật một cách an toàn:

1. Gửi email trực tiếp đến địa chỉ bảo mật của chúng tôi: `security@omnibooking.com` (hoặc tạo một **Draft Security Advisory** trực tiếp trên repository GitHub nếu bạn có quyền).
2. Trong email, vui lòng mô tả chi tiết:
   - Loại lỗ hổng bảo mật.
   - Vị trí của lỗ hổng trong mã nguồn (file, class, API endpoint).
   - Các bước chi tiết để tái hiện lỗ hổng (PoC - Proof of Concept).
   - Tác động tiềm ẩn nếu lỗ hổng bị khai thác.
3. Chúng tôi sẽ phản hồi lại email của bạn trong vòng **48 giờ** để xác nhận và thảo luận về kế hoạch vá lỗi.
4. Sau khi bản vá được phát triển và kiểm tra kỹ lưỡng, chúng tôi sẽ phát hành phiên bản mới cùng với tài liệu ghi nhận sự đóng góp của bạn (trừ khi bạn muốn ẩn danh).

---

## Cam Kết Bảo Mật Thông Tin

Ban quản trị cam kết sẽ giữ kín mọi thông tin liên quan đến người báo cáo và nội dung lỗ hổng cho đến khi lỗ hổng đó được vá hoàn toàn và công bố chính thức.

Cảm ơn bạn đã đồng hành và giúp OmniBooking trở nên an toàn hơn cho mọi người!
