# Hướng dẫn đóng góp phát triển (Contributing Guide)

Chào mừng bạn và cảm ơn bạn đã quan tâm đến việc đóng góp cho dự án **OmniBooking**! Sự tham gia của bạn giúp hệ sinh thái này ngày càng hoàn thiện và mạnh mẽ hơn.

Dưới đây là các hướng dẫn và tiêu chuẩn để bạn có thể bắt đầu đóng góp cho dự án một cách thuận lợi nhất.

---

## Mục lục

1. [Quy tắc ứng xử](#quy-tắc-ứng-xử)
2. [Cách báo cáo lỗi (Bug Reports)](#cách-báo-cáo-lỗi-bug-reports)
3. [Đề xuất tính năng mới (Feature Requests)](#đề-xuất-tính-năng-mới-feature-requests)
4. [Quy trình phát triển (Development Workflow)](#quy-trình-phát-triển-development-workflow)
5. [Tiêu chuẩn viết mã (Coding Standards)](#tiêu-chuẩn-viết-mã-coding-standards)
6. [Quy chuẩn Commit Message](#quy-chuẩn-commit-message)
7. [Quy trình gửi Pull Request (PR)](#quy-trình-gửi-pull-request-pr)

---

## Quy tắc ứng xử

Bằng cách tham gia đóng góp cho dự án này, bạn đồng ý tôn trọng tất cả các thành viên trong cộng đồng, giao tiếp một cách văn minh, xây dựng và cởi mở.

---

## Cách báo cáo lỗi (Bug Reports)

Nếu bạn phát hiện lỗi (bug) trong quá trình sử dụng hoặc phát triển:

1. **Kiểm tra trước**: Xem danh sách các [Issues](https://github.com/NQHxDev/OmniBooking/issues) hiện tại xem lỗi đó đã được báo cáo hoặc đang được xử lý chưa.
2. **Tạo Issue mới**: Nếu chưa có, hãy tạo một Issue mới với mô tả chi tiết:
   - **Tiêu đề**: Tóm tắt ngắn gọn lỗi.
   - **Các bước tái hiện (Steps to Reproduce)**: Cung cấp các bước rõ ràng để người khác có thể chạy thử và gặp lỗi tương tự.
   - **Kết quả thực tế (Actual Behavior)** và **Kết quả mong đợi (Expected Behavior)**.
   - **Thông tin môi trường**: Phiên bản OS, JDK, Node.js, Docker...
   - **Ảnh chụp màn hình hoặc Log lỗi** (nếu có).

---

## Đề xuất tính năng mới (Feature Requests)

Chúng tôi luôn hoan nghênh các ý tưởng mới! Để đề xuất tính năng:

1. Mở một Issue mới và chọn nhãn (label) là `enhancement` hoặc `feature`.
2. Mô tả rõ tính năng đó giải quyết vấn đề gì, mang lại giá trị gì cho người dùng.
3. Đề xuất sơ bộ giải pháp kỹ thuật (nếu bạn có ý định tự tay phát triển tính năng đó).

---

## Quy trình phát triển (Development Workflow)

Dự án áp dụng mô hình **Git Flow** cơ bản:

- Nhánh chính: `main` (mã nguồn production ổn định).
- Nhánh phát triển chính: `dev` (mọi nhánh tính năng đều tách ra từ `dev` và gộp lại vào `dev`).

### Các bước bắt đầu:

1. **Fork** repository này về tài khoản cá nhân của bạn.
2. **Clone** repository đã fork về máy cục bộ:
   ```bash
   git clone https://github.com/YOUR_USERNAME/OmniBooking.git
   cd OmniBooking
   ```
3. Tạo nhánh phát triển mới từ nhánh `dev`:
   ```bash
   git checkout dev
   git checkout -b feature/ten-tinh-nang
   # Hoặc: git checkout -b bugfix/ten-loi
   ```
4. Cài đặt các dependencies và chạy hạ tầng:
   ```bash
   make install
   make docker-infra
   ```
5. Thực hiện sửa đổi, chạy thử và kiểm tra kỹ lưỡng.

---

## Tiêu chuẩn viết mã (Coding Standards)

Để giữ cho mã nguồn đồng nhất và sạch đẹp, vui lòng tuân thủ:

### Backend (Java / Spring Boot)

- Sử dụng Java 21 (Virtual Threads) đúng cách.
- Tuân thủ quy chuẩn định dạng code của dự án (kiểm tra bằng Checkstyle).
- **Bảo mật kiểu (Type Safety)**: Hạn chế tối đa việc sử dụng ép kiểu thô (raw types) và `@SuppressWarnings`. Luôn sử dụng Generics an toàn.
- **Xử lý ngoại lệ**: Luôn ném `AppException` đi kèm `ErrorCode` tương ứng thay vì các Exception chung chung.

### Frontend (Next.js / TypeScript)

- Đảm bảo kiểu dữ liệu TypeScript được định nghĩa chặt chẽ (không lạm dụng kiểu `any`).
- Sử dụng Tailwind CSS 4 cho styling.
- Định dạng code bằng Prettier và ESLint trước khi commit.

---

## Quy chuẩn Commit Message

Dự án sử dụng chuẩn **Conventional Commits** để tự động tạo changelog và dễ theo dõi lịch sử. Định dạng commit chuẩn:

```
<type>(<scope>): <description>

[optional body]
```

Trong đó:

- **type** bắt buộc phải là một trong các giá trị sau:
   - `feat`: Thêm tính năng mới (feature).
   - `fix`: Sửa lỗi (bug fix).
   - `docs`: Thay đổi tài liệu hướng dẫn (documentation).
   - `style`: Thay đổi format code (không ảnh hưởng logic: space, format, checkstyle...).
   - `refactor`: Tái cấu trúc code (không sửa lỗi cũng không thêm tính năng).
   - `test`: Viết thêm unit test hoặc tích hợp.
   - `chore`: Các công việc vặt khác (update dependency, config build...).
- **scope**: Module hoặc thành phần bị tác động (ví dụ: `auth`, `booking`, `search`, `client-web`...).
- **description**: Viết ngắn gọn bằng tiếng Anh hoặc tiếng Việt, bắt đầu bằng chữ thường, không kết thúc bằng dấu chấm.

_Ví dụ_: `feat(auth): add passkey login support` hoặc `fix(booking): resolve csrf token validation crash`

---

## Quy trình gửi Pull Request (PR)

Khi bạn đã hoàn thành phần code của mình và sẵn sàng gộp vào nhánh chính:

1. Chạy toàn bộ test suite cục bộ để đảm bảo không làm hỏng tính năng cũ:
   ```bash
   make test-server
   # Hoặc dùng lệnh maven trực tiếp: ./mvnw clean test
   ```
2. Đẩy nhánh của bạn lên fork:
   ```bash
   git push origin feature/ten-tinh-nang
   ```
3. Truy cập repository gốc của **OmniBooking** và nhấn **Compare & pull request**.
4. Viết mô tả PR rõ ràng:
   - PR này giải quyết vấn đề gì? (Liên kết tới Issue bằng cú pháp `Closes #123`).
   - Những thay đổi chính là gì?
   - Bạn đã kiểm thử nó như thế nào?
5. Gửi PR và chờ đợi người quản trị (Maintainers) review. Vui lòng phản hồi các góp ý của reviewer để PR sớm được duyệt và merge!

Cảm ơn sự đóng góp của bạn!
