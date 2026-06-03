<div align="center">

# OmniBooking

### The Enterprise Booking Ecosystem

**Hệ sinh thái quản lý đặt phòng hiệu năng cao — Thiết kế theo tiêu chuẩn doanh nghiệp**

---

<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
<img src="https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
<img src="https://img.shields.io/badge/Next.js-16-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" alt="Next.js" />
<img src="https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
<img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
<img src="https://img.shields.io/badge/Redis-Stack-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
<img src="https://img.shields.io/badge/Apache%20Kafka-KRaft-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" alt="Kafka" />
<img src="https://img.shields.io/badge/Elasticsearch-8.x-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
<img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />

<br />

<img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License MIT" />
<img src="https://img.shields.io/github/actions/workflow/status/NQHxDev/OmniBooking/ci.yml?branch=main&style=flat-square" alt="Build Status" />

---

[**Kiến trúc**](./ARCHITECTURE.md) · [**Đóng góp**](./CONTRIBUTING.md) · [**Quy tắc ứng xử**](./CODE_OF_CONDUCT.md) · [**Bảo mật**](./SECURITY.md) · [**Changelog**](./CHANGELOG.md) · [**Thiết kế UI/UX**](./Client/DESIGN_SYSTEM.md) · [**Observability**](./Docs/OBSERVABILITY_ARCHITECTURE.md) · [**ADR Auth**](./Docs/ADR_MONOREPO_AUTH_COOKIES.md)

</div>

---

## Mục lục

- [Tổng quan Hệ thống](#tổng-quan-hệ-thống)
- [Kiến trúc Micro-Monorepo](#kiến-trúc-micro-monorepo)
- [Công nghệ Cốt lõi](#công-nghệ-cốt-lõi)
- [Bảo mật Đa lớp (Defense in Depth)](#bảo-mật-đa-lớp-defense-in-depth)
- [Hiệu năng & Khả năng mở rộng](#hiệu-năng--khả-năng-mở-rộng)
- [Hệ thống Tìm kiếm & Khám phá](#hệ-thống-tìm-kiếm--khám-phá)
- [Messaging & Xử lý Bất đồng bộ](#messaging--xử-lý-bất-đồng-bộ)
- [Observability & Giám sát](#observability--giám-sát)
- [Tính năng Nghiệp vụ](#tính-năng-nghiệp-vụ)
- [Bắt đầu Nhanh](#bắt-đầu-nhanh)
- [Lệnh Phát triển](#lệnh-phát-triển)
- [Cấu trúc Dự án](#cấu-trúc-dự-án)
- [Tài liệu Kỹ thuật](#tài-liệu-kỹ-thuật)

---

## Tổng quan Hệ thống

**OmniBooking** là một nền tảng đặt phòng khách sạn toàn diện, được xây dựng từ đầu với triết lý **"Production-First"** — mọi quyết định kiến trúc đều hướng tới khả năng vận hành ở quy mô lớn, bảo mật theo tiêu chuẩn enterprise, và trải nghiệm người dùng premium.

Hệ thống kết hợp sức mạnh của **Spring Boot 3.4** (Backend) và **Next.js 16** (Frontend) trong một cấu trúc **Micro-Monorepo** hiện đại, phục vụ ba nhóm người dùng qua ba ứng dụng chuyên biệt:

| Ứng dụng           |  Cổng  | Mô tả                                                                  |
| :----------------- | :----: | :--------------------------------------------------------------------- |
| **Web Portal**     | `3000` | Cổng đặt phòng dành cho khách hàng — tìm kiếm, so sánh, đặt phòng      |
| **Partner Portal** | `3002` | Bảng điều khiển cho đối tác khách sạn — quản lý tài sản, phòng, giá cả |
| **Owner Portal**   | `3005` | Trang quản trị hệ thống — giám sát toàn bộ nền tảng                    |

> Tất cả ứng dụng chia sẻ một **Shared Package** chung chứa API client, schemas, types, stores, và utilities — đảm bảo tính nhất quán trên toàn hệ thống.

---

## Kiến trúc Micro-Monorepo

OmniBooking áp dụng mô hình **Micro-Monorepo (Modular Monolith)** — một backend monolith được tổ chức theo module, kết hợp với nhiều frontend apps trong cùng một repository:

```
                           ┌─────────────────────────────────────────────┐
                           │            INFRASTRUCTURE LAYER             │
                           │  PostgreSQL · Redis Stack · Kafka · ES 8    │
                           │  Prometheus · Grafana · Kibana · Kafdrop    │
                           └──────────────────┬──────────────────────────┘
                                              │
                           ┌──────────────────▼──────────────────────────┐
                           │          SPRING BOOT 3.4 (Java 21)          │
                           │                                             │
                           │  ┌─────────┐  ┌──────────┐  ┌───────────┐   │
                           │  │  Auth   │  │ Booking  │  │  Search   │   │
                           │  │ Module  │  │  Engine  │  │  Engine   │   │
                           │  └─────────┘  └──────────┘  └───────────┘   │
                           │  ┌─────────┐  ┌──────────┐  ┌───────────┐   │
                           │  │ Partner │  │  Media   │  │ Currency  │   │
                           │  │ Module  │  │ Service  │  │  System   │   │
                           │  └─────────┘  └──────────┘  └───────────┘   │
                           │  ┌─────────┐  ┌──────────┐  ┌───────────┐   │
                           │  │  User   │  │  Comms   │  │   Core    │   │
                           │  │ Module  │  │ (Email)  │  │  Services │   │
                           │  └─────────┘  └──────────┘  └───────────┘   │
                           └──────────────────┬──────────────────────────┘
                                              │
              ┌───────────────────────────────┼────────────────────────────┐
              │                               │                            │
    ┌─────────▼──────────┐      ┌─────────────▼──────────┐    ┌───────────▼────────┐
    │     Web Portal     │      │     Partner Portal     │    │     Owner Portal   │
    │     Next.js 16     │      │        Next.js 16      │    │     Next.js 16     │
    │     Port 3000      │      │        Port 3002       │    │     Port 3005      │
    └────────────────────┘      └────────────────────────┘    └────────────────────┘
              │                               │                            │
              └───────────────────────────────┼────────────────────────────┘
                                              │
                               ┌──────────────▼──────────────┐
                               │      Shared Package         │
                               │  API · Schemas · Types      │
                               │  Store · Utils · Styles     │
                               └─────────────────────────────┘
```

**Tại sao Modular Monolith?**

- Triển khai và debug đơn giản hơn microservices
- Chia sẻ transaction context (ACID guarantees)
- Module boundaries rõ ràng — sẵn sàng tách microservice khi cần
- Kafka được sử dụng cho async processing, không phải inter-service communication

---

## Công nghệ Cốt lõi

### Backend

| Công nghệ                   | Phiên bản | Vai trò                                              |
| :-------------------------- | :-------: | :--------------------------------------------------- |
| Java (Virtual Threads)      |    21     | Ngôn ngữ chính — hỗ trợ 100k+ concurrent connections |
| Spring Boot                 |   3.4.3   | Application framework                                |
| Spring Security             |     —     | Authentication & Authorization                       |
| Spring Data JPA / Hibernate |     —     | ORM & Database access                                |
| Flyway                      |     —     | Database migration & versioning                      |
| Spring Data Redis           |     —     | Caching, Sessions, Bloom Filter, Rate Limiting       |
| Spring Kafka                |     —     | Event-driven messaging                               |
| Elasticsearch Client        |    8.x    | Full-text & Geo-spatial search                       |
| Resilience4j                |   2.2.0   | Circuit Breaker & Retry patterns                     |
| JJWT                        |  0.12.6   | JWT token handling                                   |
| WebAuthn4j                  |  0.24.0   | Passkey / FIDO2 authentication                       |
| Argon2 (BouncyCastle)       |   1.80    | Password hashing (memory-hard)                       |
| MapStruct                   |   1.6.3   | Compile-time DTO mapping                             |
| Micrometer + Prometheus     |     —     | Application metrics & monitoring                     |
| Sentry                      |  8.42.0   | Error tracking & distributed tracing                 |
| Thymeleaf                   |     —     | Email template rendering                             |
| Cloudinary SDK              |   2.0.0   | Image CDN & optimization                             |
| Resend SDK                  |  4.13.0   | Transactional email delivery                         |
| GeoIP2 (MaxMind)            |   4.2.1   | IP-based geolocation                                 |
| springdoc-openapi           |   2.8.5   | API documentation (Swagger)                          |

### Frontend

| Công nghệ                | Vai trò                            |
| :----------------------- | :--------------------------------- |
| Next.js 16+ (App Router) | React framework với SSR/SSG        |
| TypeScript               | Type-safe JavaScript               |
| Tailwind CSS 4           | Utility-first styling              |
| Shadcn/UI                | Premium component library          |
| Zustand                  | Lightweight state management       |
| TanStack Query           | Server state & data fetching       |
| Framer Motion            | Micro-animations & transitions     |
| Zod + React Hook Form    | Schema-based form validation       |
| next-intl                | Internationalization (vi/en)       |
| Leaflet                  | Interactive map engine             |
| Axios                    | HTTP client với smart interceptors |

### Infrastructure

| Dịch vụ              | Vai trò                                |  Cổng  |
| :------------------- | :------------------------------------- | :----: |
| PostgreSQL 16        | Database chính (ACID)                  | `5432` |
| Redis Stack          | Cache, Sessions, Bloom Filter, Pub/Sub | `6379` |
| Apache Kafka (KRaft) | Event streaming & async processing     | `9092` |
| Elasticsearch 8.x    | Full-text search & geo-spatial queries | `9200` |
| Prometheus           | Metrics collection                     | `9090` |
| Grafana              | Metrics dashboards & alerting          | `3001` |
| Kibana               | Elasticsearch visualization            | `5601` |
| Kafdrop              | Kafka monitoring UI                    | `9000` |
| Sentry               | Error tracking & APM                   |   —    |
| Cloudinary           | Image CDN & on-the-fly optimization    |   —    |

---

## Bảo mật Đa lớp (Defense in Depth)

OmniBooking triển khai mô hình bảo mật **Defense in Depth** — nhiều lớp bảo vệ chồng lên nhau, mỗi lớp hoạt động độc lập để đảm bảo rằng việc xâm phạm một lớp không ảnh hưởng đến toàn bộ hệ thống.

### Authentication (Xác thực)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION FLOW                                 │
│                                                                         │
│  Client Request                                                         │
│       │                                                                 │
│       ▼                                                                 │
│  ┌─────────────────────┐     ┌──────────────────────────────┐           │
│  │ Extract JWT Cookie  │────▶│ Validate JWT Signature       │           │
│  │ (HttpOnly, Lax)     │     │ (JJWT + HMAC-SHA256)         │           │
│  └─────────────────────┘     └──────────────┬───────────────┘           │
│                                             │                           │
│                                             ▼                           │
│                              ┌──────────────────────────────┐           │
│                              │ Fingerprint Verification     │           │
│                              │ SHA256(x_fgp) == JWT claim   │           │
│                              └──────────────┬───────────────┘           │
│                                             │                           │
│                                             ▼                           │
│                              ┌──────────────────────────────┐           │
│                              │ Redis Session Validation     │           │
│                              │ (Fail-Closed: 503 on error)  │           │
│                              └──────────────┬───────────────┘           │
│                                             │                           │
│                                             ▼                           │
│                              ┌──────────────────────────────┐           │
│                              │ Token Version Check          │           │
│                              │ JWT.version == DB.version    │           │
│                              └──────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
```

| Tính năng                       | Mô tả                                                                                                               |
| :------------------------------ | :------------------------------------------------------------------------------------------------------------------ |
| **JWT + HttpOnly Cookies**      | Access token, Refresh token, Session ID, Fingerprint — tất cả lưu trong HttpOnly, SameSite=Lax cookies              |
| **Dual-Key Fingerprinting**     | JWT chứa hash của cookie `x_fgp`. Cả hai phải khớp mới xác thực — chống token theft & session hijacking             |
| **Redis Session (Fail-Closed)** | Mỗi request validate session trong Redis. Nếu Redis lỗi → **HTTP 503** (không bao giờ fallback insecure)            |
| **Token Version Revocation**    | Mỗi User có `token_version`. Đổi mật khẩu → tăng version → tất cả JWT cũ bị thu hồi ngay lập tức trên toàn hệ thống |
| **Atomic Refresh Rotation**     | Lưu & verify session mới trong Redis TRƯỚC KHI thu hồi session cũ. Nếu fail → rollback giữ session cũ hoạt động     |
| **Refresh DoS Prevention**      | Distributed lock (SET NX PX) + Lua script release — ngăn chặn tấn công DoS trên endpoint refresh                    |
| **Double-Submit CSRF**          | Cookie `csrf_token` + Header `X-CSRF-Token` + Origin validation = chống CSRF toàn diện                              |
| **Cookie Domain Safety**        | `CookieDomainInitializer` kiểm tra `COOKIE_DOMAIN` phải được cấu hình khi chạy production profile                   |

### Phương thức Xác thực

| Phương thức              | Chi tiết                                                         |
| :----------------------- | :--------------------------------------------------------------- |
| **Email / Password**     | Hash với Argon2 (memory-hard algorithm) — chống brute-force      |
| **OAuth2 Social Login**  | Google & Zalo với CSRF `state` parameter lưu Redis (15 phút TTL) |
| **Passkeys / WebAuthn**  | FIDO2 passwordless authentication — tiêu chuẩn bảo mật cao nhất  |
| **TOTP 2FA**             | Xác thực hai yếu tố qua ứng dụng authenticator                   |
| **Cloudflare Turnstile** | Bot protection cho các form công khai                            |

### Bảo vệ Dữ liệu

| Tính năng                         | Mô tả                                                                                             |
| :-------------------------------- | :------------------------------------------------------------------------------------------------ |
| **Searchable Phone Encryption**   | AES-256-GCM encryption + HMAC-SHA256 blind index — mã hóa mà vẫn tìm kiếm được                    |
| **Bloom Filter Anti-Enumeration** | Redis Bloom Filter kiểm tra email/username — chặn enumeration attack, giảm 99% DB lookups         |
| **Error Obfuscation**             | Login/reset luôn trả thành công bất kể email có tồn tại — không rò rỉ thông tin                   |
| **Sliding Window Rate Limit**     | Redis-based rate limiting (3 req/phút) cho password reset — chống abuse                           |
| **RBAC + Permissions**            | Roles (Admin, Partner, Driver, User) → Permissions (e.g., `property:write`) — phân quyền chi tiết |

---

## Hiệu năng & Khả năng mở rộng

### Java 21 Virtual Threads

Hệ thống bật **Virtual Threads** cho phép xử lý **100,000+ kết nối đồng thời** mà không cần thread pool phức tạp. Mỗi request được xử lý trên một virtual thread riêng, sử dụng tối ưu tài nguyên hệ thống.

### Đăng ký Đồng thời Cực cao (World Cup Scale)

Kiến trúc **Buffered Batch & Event-Driven** cho phép xử lý đỉnh 100k+ đăng ký/giây:

```
Request → Bloom Filter Pre-check → Redis Queue (LPUSH)
                                         │
                              ┌──────────▼───────────┐
                              │  Batch Worker        │
                              │  (100 records/batch) │
                              │  Bulk DB Insert      │
                              └──────────┬───────────┘
                                         │
                              ┌──────────▼───────────┐
                              │  Kafka CDC Event     │
                              │  (Welcome Email)     │
                              └──────────────────────┘
                                         │
                              ┌──────────▼───────────┐
                              │  SSE Notification    │
                              │  (Real-time result)  │
                              └──────────────────────┘
```

### Tối ưu Database

| Kỹ thuật                 | Lợi ích                                                                                            |
| :----------------------- | :------------------------------------------------------------------------------------------------- |
| **UUID v7 Primary Keys** | Time-ordered → tối ưu B-Tree index, truy vấn hàng triệu bản ghi không giảm tốc                     |
| **N+1 Query Detection**  | Auto-warn khi >30 queries/request — phát hiện sớm performance regression                           |
| **OSIV Disabled**        | Clean transaction boundaries — không lazy loading ngoài transaction                                |
| **JPA Specifications**   | Dynamic multi-criteria filtering (`?price_gt=100&city=Hanoi`) — không viết repository methods thừa |
| **Optimistic Locking**   | Column `version` chống overwrite trong concurrent environments                                     |
| **Soft Delete**          | `deleted_at` cho data recovery và auditing                                                         |
| **HikariCP Tuned**       | 50 max connections, 20 min idle — tối ưu connection pool                                           |

### Cache Đa tầng

```
Request → Redis Cache (L1) → PostgreSQL (L2) → External API (L3)
            4h TTL              Audit Trail         Source of Truth
```

Áp dụng cho Exchange Rates, Partner Properties, và các dữ liệu nóng khác.

---

## Hệ thống Tìm kiếm & Khám phá

### Elasticsearch Search Engine

| Tính năng                 | Chi tiết                                                                         |
| :------------------------ | :------------------------------------------------------------------------------- |
| **Full-text Search**      | Fuzzy matching, hỗ trợ có dấu/không dấu tiếng Việt                               |
| **Geo-spatial Search**    | GeoPoint coordinates + radius-based search — tìm kiếm theo bán kính trên bản đồ  |
| **Multi-criteria Filter** | Price, Accommodation Type, Amenities, Review Score — lọc phức tạp không giảm tốc |
| **Search Analytics**      | Trending locations từ search frequency 7 ngày gần nhất                           |
| **Manual Boosting**       | Admin có thể đẩy locations promotional lên đầu                                   |
| **i18n Search**           | Kết quả tìm kiếm phù hợp với locale người dùng (vi/en)                           |

### Interactive Map (Leaflet)

- **Price-tag Markers** hiển thị giá trực tiếp trên bản đồ — trải nghiệm như Booking.com & Airbnb
- **Dynamic Import** cho SSR optimization — không ảnh hưởng LCP
- **IP-based Geolocation** (MaxMind GeoIP2) — tự động detect quốc gia người dùng

### Real-time Sync (Kafka → Elasticsearch)

- **CDC-like Pattern**: Thay đổi dữ liệu PostgreSQL → Kafka event → Elasticsearch index tức thì
- **Deferred Sync**: Property mới chỉ được index sau khi main image upload hoàn tất — search results luôn có hình ảnh
- **Nightly Reconciliation**: Job chạy lúc 1:00 AM kiểm tra PostgreSQL ↔ Elasticsearch — auto-repair drift

---

## Messaging & Xử lý Bất đồng bộ

### Transactional Outbox Pattern

OmniBooking triển khai **Transactional Outbox** ở mức production-grade với các đặc điểm:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  TRANSACTIONAL OUTBOX PIPELINE                       │
│                                                                      │
│  Business Transaction                                                │
│       │                                                              │
│       ├──▶ Save Entity (DB)         ── Same Transaction ──┐          │
│       └──▶ Save OutboxEvent (DB)    ◀─────────────────────┘          │
│                    │                                                 │
│                    ▼                                                 │
│            afterCommit() → Wake-Up Signal (AtomicBoolean)            │
│                    │                                                 │
│                    ▼                                                 │
│         ┌────────────────────────┐                                   │
│         │   OutboxWorker         │                                   │
│         │   SELECT ... FOR       │                                   │
│         │   UPDATE SKIP LOCKED   │                                   │
│         └─────────┬──────────────┘                                   │
│                   │                                                  │
│                   ▼                                                  │
│         ┌─────────────────────┐     ┌──────────────────────┐         │
│         │   Publish to Kafka  │────▶│  Idempotent Consumer │         │
│         └─────────────────────┘     │  (Claim-then-Process)│         │
│                                     └──────────────────────┘         │
│                                                                      │
│  On Failure: Exponential Backoff (1m → 5m → 15m → 1h → Dead Letter)  │
└──────────────────────────────────────────────────────────────────────┘
```

| Tính năng                     | Chi tiết                                                          |
| :---------------------------- | :---------------------------------------------------------------- |
| **Atomic Persistence**        | Business data + Event lưu trong cùng 1 DB transaction             |
| **Guaranteed Delivery**       | At-Least-Once delivery — event luôn được gửi dù Kafka tạm offline |
| **Hybrid Wake-Up**            | afterCommit() signal → giảm latency xuống gần real-time           |
| **Cluster-Safe**              | `SELECT ... FOR UPDATE SKIP LOCKED` — an toàn multi-instance      |
| **Exponential Backoff**       | 1 phút → 5 phút → 15 phút → 1 giờ → Dead Letter Queue             |
| **Event Upcaster**            | Schema evolution layer — v1 event tự động transform sang v2       |
| **Token Bucket Rate Limiter** | Redis Lua script — chống retry storm trên external services       |
| **Outbox Auto-Cleanup**       | Prune PROCESSED events >30 ngày — chạy lúc 3:00 AM hàng ngày      |

### Idempotent Consumer (Claim-then-Process)

| Giai đoạn     | Mô tả                                                                       |
| :------------ | :-------------------------------------------------------------------------- |
| **Claim**     | `SELECT ... FOR UPDATE` + Insert `PROCESSING` state                         |
| **Process**   | Thực thi business logic                                                     |
| **Complete**  | Update → `COMPLETED`                                                        |
| **Fail**      | Update → `FAILED` (cho phép retry)                                          |
| **Recovery**  | `IdempotencyRecoveryWorker` quét mỗi phút — reset events stuck ở PROCESSING |
| **Heartbeat** | Lease renewal cho long-running operations — chống false positive timeout    |

### Background Workers (5)

| Worker                       | Schedule          | Vai trò                                      |
| :--------------------------- | :---------------- | :------------------------------------------- |
| `OutboxWorker`               | Polling + Wake-up | Publish outbox events → Kafka                |
| `RegistrationBatchWorker`    | Continuous        | Batch process registration queue (100/batch) |
| `CurrencyWorker`             | Mỗi 4 giờ         | Cập nhật tỷ giá từ ExchangeRate API          |
| `IdempotencyRecoveryWorker`  | Mỗi 1 phút        | Recovery events stuck ở PROCESSING           |
| `SearchReconciliationWorker` | 1:00 AM           | PostgreSQL ↔ Elasticsearch reconciliation    |

---

## Observability & Giám sát

### Distributed Tracing

Mỗi request được gắn `X-Request-ID` duy nhất, truyền xuyên suốt từ Client → Next.js → Spring Boot → Kafka → Database:

```
Client                 Next.js               Spring Boot            Kafka Consumer
  │                      │                      │                      │
  │ X-Request-ID: abc123 │                      │                      │
  ├─────────────────────▶├─────────────────────▶├─────────────────────▶│
  │                      │                      │ MDC.put("requestId") │
  │                      │                      │ SQL Comment: abc123  │
```

### Structured Logging (4 Streams)

| Log File              | Nội dung                                     | Format                       |
| :-------------------- | :------------------------------------------- | :--------------------------- |
| `system.log`          | Hoạt động hệ thống chung                     | Dev: Color text · Prod: JSON |
| `request_success.log` | Requests thành công (2xx) + execution time   | Dev: Color text · Prod: JSON |
| `request_error.log`   | Business errors (4xx) + validation failures  | Dev: Color text · Prod: JSON |
| `error.log`           | Critical errors (5xx) + unhandled exceptions | Dev: Color text · Prod: JSON |

- **Hierarchical Archiving**: `.gz` trong `logs/archive/YYYY-MM/YYYY-MM-DD/`
- **AOP Execution Time**: Tự động tracking latency mỗi request

### Sentry Integration

| Tính năng               | Chi tiết                                         |
| :---------------------- | :----------------------------------------------- |
| **PII Sanitization**    | Tự động mask JWT, email, phone, sensitive keys   |
| **Noise Reduction**     | Ignore AppException, validation errors, 403s     |
| **Anti-Double-Capture** | `sentry_captured` request attribute flag         |
| **Cron Monitoring**     | Giám sát OutboxWorker & CurrencyWorker schedules |

### Prometheus + Grafana Metrics

<details>
<summary><strong>Custom Metrics (Click để xem)</strong></summary>

| Metric                             | Loại    | Mô tả                                                              |
| :--------------------------------- | :------ | :----------------------------------------------------------------- |
| `auth_rejection_total`             | Counter | Số lần từ chối xác thực (by reason: expired, fingerprint, version) |
| `csrf_rejection_total`             | Counter | Số lần từ chối CSRF                                                |
| `redis_session_failure_total`      | Counter | Số lần Redis session lookup lỗi                                    |
| `refresh_lock_contention_total`    | Counter | Số lần refresh token bị lock contention                            |
| `omnibooking.outbox.pending.count` | Gauge   | Số events đang chờ xử lý                                           |
| `omnibooking.outbox.dead.count`    | Gauge   | Số events trong Dead Letter Queue                                  |
| `duplicate_event_count`            | Counter | Số lần phát hiện duplicate events                                  |
| `orphaned_media_cleanup_count`     | Counter | Số lần cleanup Cloudinary orphans                                  |
| `search_sync_failure_count`        | Counter | Số lần Elasticsearch sync thất bại                                 |

</details>

---

## Tính năng Nghiệp vụ

### Booking Engine

- **Full Lifecycle**: Pending → Confirmed → Stayed → Cancelled → Refunded
- **Status Audit Trail**: `BookingStatusLog` ghi lại mọi thay đổi trạng thái
- **Guest-centric Data**: Thông tin khách có thể khác với chủ tài khoản
- **Cancellation Policies**: Free cancellation windows + penalty tiers tự động
- **Transaction Tracking**: PAYMENT/REFUND events với JSONB metadata (Stripe-ready)
- **Coupon System**: Percentage/Fixed discounts với usage limits

### Property Management

- **Property Types**: Hotel, Apartment, Villa, Resort
- **Room Types**: Base pricing, capacity, bed configuration
- **Dynamic Pricing**: Daily inventory + seasonal price overrides
- **Amenities**: Property-level + Room-level (many-to-many)
- **Cloudinary CDN**: On-the-fly image optimization theo viewport

### Global Currency System

- **USD-based Core**: Tất cả giá lưu trữ bằng USD — đảm bảo consistency
- **Real-time Conversion**: 3-tier cache (Redis → DB → ExchangeRate API)
- **Organic Pricing**: Random markup cho VND (300-1000₫), 5% spread cho currencies khác
- **Exchange Rate Lock**: Tỷ giá cố định tại thời điểm booking

### Email System (Transactional Outbox)

- **Guaranteed Delivery**: Email events qua Outbox Pattern — không bao giờ mất mail
- **Thymeleaf Templates**: Premium HTML email templates
- **Exponential Backoff**: Retry tự động với backoff 1m → 5m → 15m → 1h
- **Dead Letter Queue**: Events fail 5 lần → DLQ để operators kiểm tra

### Internationalization (i18n)

- **Bilingual**: Vietnamese (vi) & English (en) toàn hệ thống
- **next-intl**: Locale-based routing (`/vi/...`, `/en/...`)
- **Server-side Messages**: Error codes backend → localized messages frontend
- **Locale-aware Formatting**: Date (date-fns), Currency (Intl.NumberFormat)

---

## Bắt đầu Nhanh

### Yêu cầu Hệ thống

| Phần mềm                | Phiên bản | Mục đích                |
| :---------------------- | :-------: | :---------------------- |
| Docker & Docker Compose |  Latest   | Container orchestration |
| Java (JDK)              |    21+    | Backend runtime         |
| Node.js                 |    23+    | Frontend runtime        |
| npm                     |    10+    | Package management      |

### Thiết lập

```bash
# 1. Clone repository
git clone https://github.com/NQHxDev/OmniBooking.git
cd OmniBooking

# 2. Cấu hình environment
cp .env.example .env
cp Server/.env.example Server/.env
cp Client/.env.example Client/.env
# → Chỉnh sửa các file .env theo môi trường của bạn

# 3. Cài đặt dependencies
make install

# 4. Khởi động hạ tầng (PostgreSQL, Redis, Kafka, Elasticsearch, Monitoring)
make docker-infra
# Chờ Elasticsearch healthy trước khi tiếp tục...

# 5. Khởi chạy toàn bộ ứng dụng
make dev
```

Sau khi khởi chạy thành công:

| Dịch vụ            | URL                                   |
| :----------------- | :------------------------------------ |
| **Web Portal**     | http://localhost:3000                 |
| **Partner Portal** | http://localhost:3002                 |
| **Owner Portal**   | http://localhost:3005                 |
| **API Server**     | http://localhost:8080                 |
| **Swagger UI**     | http://localhost:8080/swagger-ui.html |
| **Grafana**        | http://localhost:3001                 |
| **Prometheus**     | http://localhost:9090                 |
| **Kibana**         | http://localhost:5601                 |
| **Kafdrop**        | http://localhost:9000                 |

---

## Lệnh Phát triển

Toàn bộ workflow được quản lý qua **Makefile**:

### Development

```bash
make dev             # Chạy Server + tất cả 3 Client apps (parallel)
make dev-server      # Chỉ chạy Spring Boot Server
make dev-client      # Chạy tất cả 3 Client apps (parallel)
make dev-web         # Chỉ chạy Web Portal (port 3000)
make dev-partner     # Chỉ chạy Partner Portal (port 3002)
make dev-owner       # Chỉ chạy Owner Portal (port 3005)
```

### Infrastructure

```bash
make docker-infra    # Start: PostgreSQL, Redis, Kafka, Elasticsearch, Prometheus, Grafana
make docker-up       # Start toàn bộ stack trong Docker
make docker-down     # Dừng và xóa tất cả containers
make docker-stop     # Dừng containers (giữ data)
make monitoring      # Chỉ start Prometheus + Grafana
```

### Build & Test

```bash
make install         # Cài đặt toàn bộ dependencies (Root + Server + Client)
make build           # Build production: Server JAR + 3 Client builds
make test-server     # Chạy unit tests backend
make clean           # Xóa build artifacts
make clear-logs      # Xóa log files
```

---

## Cấu trúc Dự án

```
OmniBooking/
├── Client/                          # Frontend Monorepo (npm workspaces)
│   ├── apps/
│   │   ├── web/                     # Guest Booking Portal
│   │   ├── partner/                 # Hotel Partner Portal
│   │   └── owner/                   # System Admin Portal
│   ├── packages/
│   │   └── shared/                  # Shared Library
│   │       ├── src/api/             #    API client & interceptors
│   │       ├── src/constants/       #    Shared constants
│   │       ├── src/schemas/         #    Zod validation schemas
│   │       ├── src/services/        #    Service layer
│   │       ├── src/store/           #    Zustand stores
│   │       ├── src/types/           #    TypeScript interfaces
│   │       └── src/utils/           #    Utility functions
│   ├── Dockerfile                   # Multi-stage production build
│   └── DESIGN_SYSTEM.md             # UI/UX design standards
│
├── Server/                          # Backend (Spring Boot 3.4)
│   └── src/main/java/com/omnibooking/
│       ├── annotation/              # Custom annotations (@Idempotent)
│       ├── aspect/                  # AOP (LoggingAspect)
│       ├── config/                  # App, Redis, Kafka, Cloudinary config
│       ├── constant/                # SecurityConstants, ErrorCodes
│       ├── consumer/                # Kafka consumers
│       ├── context/                 # RequestContext (ThreadLocal)
│       ├── controller/              # 15 REST controllers
│       ├── document/                # Elasticsearch documents
│       ├── dto/                     # Data Transfer Objects
│       ├── exception/               # Global error handling
│       ├── mapper/                  # MapStruct mappers
│       ├── model/                   # 25 JPA entities + enums
│       ├── repository/              # Data access layer
│       ├── security/                # JWT, CSRF, CORS, SecurityConfig
│       ├── services/                # 9 service modules
│       ├── specification/           # JPA Specifications
│       ├── util/                    # CookieUtils, SecurityUtils
│       ├── validation/              # Custom validators
│       └── worker/                  # 5 background workers
│
├── Docs/                            # Technical Documentation
│   ├── ADR_MONOREPO_AUTH_COOKIES.md # Architecture Decision Record
│   ├── CLASS_DIAGRAM.md             # Entity relationship diagram
│   ├── HIGH_CONCURRENCY_REGISTRATION.md
│   ├── OBSERVABILITY_ARCHITECTURE.md
│   ├── PROFILE_ENCRYPTION.md        # Blind indexing strategy
│   └── sequence-diagrams/           # UML sequence diagrams
│
├── infra/                           # Prometheus & Grafana configs
├── docker-compose.yml               # 10 services orchestration
├── Makefile                         # Build & dev automation
├── ARCHITECTURE.md                  # Comprehensive architecture doc (35KB)
├── .github/workflows/ci.yml         # GitHub Actions CI pipeline
├── .husky/                          # Git hooks (lint-staged)
└── .prettierrc                      # Code formatting config
```

---

## Tài liệu Kỹ thuật

| Tài liệu                                                        | Mô tả                                                                 |
| :-------------------------------------------------------------- | :-------------------------------------------------------------------- |
| [**ARCHITECTURE.md**](./ARCHITECTURE.md)                        | Kiến trúc hệ thống toàn diện — 18 chương, 35KB chi tiết               |
| [**DESIGN_SYSTEM.md**](./Client/DESIGN_SYSTEM.md)               | Tiêu chuẩn thiết kế UI/UX — typography, colors, spacing, components   |
| [**ADR: Auth Cookies**](./Docs/ADR_MONOREPO_AUTH_COOKIES.md)    | Architecture Decision Record — chiến lược authentication cho monorepo |
| [**Observability**](./Docs/OBSERVABILITY_ARCHITECTURE.md)       | Kiến trúc observability — logging, tracing, metrics, alerting         |
| [**Class Diagram**](./Docs/CLASS_DIAGRAM.md)                    | Sơ đồ lớp — 25 entities và mối quan hệ                                |
| [**High Concurrency**](./Docs/HIGH_CONCURRENCY_REGISTRATION.md) | Kiến trúc đăng ký đồng thời cao — Redis queue + batch processing      |
| [**Profile Encryption**](./Docs/PROFILE_ENCRYPTION.md)          | Mã hóa dữ liệu nhạy cảm — AES-256-GCM + blind indexing                |
| [**Sequence Diagrams**](./Docs/sequence-diagrams/)              | UML sequence diagrams cho các luồng nghiệp vụ                         |

---

## Design Patterns

Hệ thống áp dụng các design patterns ở mức production-grade:

| Pattern                       | Áp dụng tại                                        |
| :---------------------------- | :------------------------------------------------- |
| **Transactional Outbox**      | Guaranteed message delivery (Email, Search Sync)   |
| **Idempotent Consumer**       | Claim-then-Process state machine với lease renewal |
| **Strategy + Factory**        | OAuth2 providers (Google, Zalo — extensible)       |
| **Specification**             | Dynamic query filtering (JPA Specifications)       |
| **Blind Indexing**            | Searchable encryption (Phone numbers)              |
| **Circuit Breaker + Retry**   | Resilience4j cho external services                 |
| **Token Bucket Rate Limiter** | Redis Lua script — chống retry storm               |
| **Compensation**              | Cloudinary rollback khi DB transaction fail        |
| **Event Upcasting**           | Schema evolution cho outbox events                 |
| **CDC Simulation**            | PostgreSQL → Kafka → Elasticsearch sync            |

---

<div align="center">

### Code Quality

**Husky** Git Hooks · **lint-staged** · **Prettier** · **ESLint** · **Checkstyle** · **EditorConfig**

---

**Built with Precision by OmniBooking Team © 2026**

_Java 21 · Spring Boot 3.4 · Next.js 16 · PostgreSQL 16 · Redis Stack · Apache Kafka · Elasticsearch 8_

</div>
