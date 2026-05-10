# OmniBooking: The Enterprise Booking Ecosystem

<div align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4+-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Next.js-15+-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" alt="Next.js" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Redis-Stack-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Apache%20Kafka-Latest-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" alt="Kafka" />
</div>

---

**OmniBooking** là một hệ sinh thái quản lý đặt phòng (booking) hiệu năng cao, được thiết kế theo tiêu chuẩn doanh nghiệp. Dự án kết hợp sức mạnh của **Spring Boot 3.4** (Backend) và **Next.js 15** (Frontend) trong một cấu trúc **Micro-Monorepo** hiện đại, sẵn sàng cho việc mở rộng quy mô lớn và xử lý đồng thời cực cao.

[**Khám phá Kiến trúc System**](./ARCHITECTURE.md) • [**Tài liệu Thiết kế (UI/UX)**](./Client/DESIGN_SYSTEM.md)

---

## Những Đặc Điểm Nổi Bật (Key Pillars)

### Hiệu Năng Cao (High Performance)

- **Time-Ordered UUID v7**: Tối ưu hóa hiệu suất chỉ mục B-Tree trong database, giúp truy vấn hàng triệu bản ghi mà không giảm tốc độ.
- **Redis Stack Integration**: Tích hợp **Bloom Filter** để kiểm tra tồn tại (email/username) siêu tốc, giảm 90% tải cho database chính.
- **CDN Optimization**: Tích hợp Cloudinary giúp nén và biến đổi hình ảnh theo viewport, giảm tới 80% dung lượng tải trang mà vẫn giữ nguyên chất lượng.
- **Distributed Caching**: Hệ thống cache đa tầng giúp phản hồi API trong thời gian mili giây.

### Bảo Mật Đa Lớp (Advanced Security)

- **Fingerprinting & JWT**: Cơ chế xác thực Dual-key kết hợp HttpOnly Cookie và Fingerprinting chống lại các cuộc tấn công XSS và Session Hijacking.
- **RBAC (Role-Based Access Control)**: Phân quyền tinh vi dựa trên quyền hạn (Permissions) thay vì chỉ dừng lại ở vai trò (Roles).
- **Idempotency Ready**: Đảm bảo an toàn cho các giao dịch đặt phòng và thanh toán, ngăn chặn dữ liệu trùng lặp.

### Trải Nghiệm Người Dùng (Premium UI/UX)

- **Modern Tech Stack**: Xây dựng trên **Tailwind CSS 4** và **Shadcn/UI** với triết lý thiết kế tối giản nhưng sang trọng.
- **Micro-animations**: Hiệu ứng chuyển động mượt mà với Framer Motion, mang lại cảm giác sống động và phản hồi tức thì.
- **Cloudinary CDN**: Hình ảnh được tối ưu hóa tự động theo thiết bị người dùng, đảm bảo tốc độ tải trang (LCP) cực nhanh.

### Khả Năng Quan Sát & Vận Hành (Observability)

- **MDC Request Tracing**: Mỗi yêu cầu được gắn một `X-Request-ID` duy nhất xuyên suốt từ Client đến Server, giúp việc debug trở nên dễ dàng.
- **AOP Logging**: Tự động ghi lại nhật ký hệ thống một cách chuyên nghiệp mà không làm bẩn logic nghiệp vụ.
- **Health Monitoring**: Theo dõi trạng thái sức khỏe hệ thống thời gian thực qua Spring Boot Actuator.

---

## Công Nghệ Cốt Lõi

| Layer              | Technologies                                                             |
| :----------------- | :----------------------------------------------------------------------- |
| **Backend**        | Java 21, Spring Boot 3.4, Spring Security, JPA/Hibernate, Flyway         |
| **Frontend**       | Next.js 15 (App Router), TypeScript, Tailwind 4, Zustand, TanStack Query |
| **Data & Media**   | PostgreSQL 16, Redis Stack, Cloudinary CDN, Apache Kafka, Resend SDK     |
| **Infrastructure** | Docker, Makefile, GitHub Actions CI/CD                                   |

---

## Bắt Đầu Nhanh

Dự án được tối ưu hóa với **Makefile** để bạn có thể khởi chạy toàn bộ hệ thống chỉ với vài câu lệnh.

### 1. Chuẩn bị

- Đảm bảo máy tính đã cài đặt **Docker** và **Docker Compose**.
- Java 21+ và Node.js 23+ **(Tùy chọn: Chạy local không qua Docker)**
- Thiết lập file `.env` từ file `.env.example` của cả `Server` và `Client`.

### 2. Cài đặt dependencies

```bash
make install
```

### 3. Triển khai hạ tầng (Database, Redis, Kafka)

```bash
make docker-infra
```

### 4. Khởi chạy ứng dụng

```bash
# Chạy cả Client và Server ở chế độ Development
make dev
```

---

## Sơ đồ Cấu trúc Dự án

```text
OmniBooking/
├── Client/             # Next.js 15 Frontend (App Router)
├── Server/             # Spring Boot 3.4 Backend
├── docker-compose.yml  # Cấu hình hạ tầng container
├── Makefile            # Công cụ quản lý dự án tập trung
└── ARCHITECTURE.md     # Tài liệu kỹ thuật chi tiết
```

---

<div align="center">
  <sub>Built with Precision by OmniBooking Team © 2026</sub>
</div>
