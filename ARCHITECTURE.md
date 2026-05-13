# OmniBooking Architecture Document

This document outlines the technical architecture and design patterns implemented in the OmniBooking Monorepo.

## 1. System Overview

- **Monorepo Structure**: Centralized management of Client (Next.js) and Server (Spring Boot).
- **Core Technologies**:
   - **Backend**: Java 21, Spring Boot 3.4.x, Spring Data JPA, Hibernate.
   - **Frontend**: Next.js 15+, TypeScript, Tailwind CSS 4, Shadcn/ui.
   - **Infrastructure**: Docker Compose, PostgreSQL 16, Redis Stack (with Bloom Filter).

## 2. Infrastructure Architecture

### Docker Orchestration

Everything is orchestrated via `docker-compose.yml` with secrets managed through a root-level `.env` file (not committed to Git).

### Redis Stack & Bloom Filter

Implemented `redis/redis-stack-server` to support high-performance operations and security layers:

- **Session/Token Management**: Offloaded to Redis for scalability and distributed session handling.
- **Bloom Filter (Security & Performance)**:
   - **Initialization**: Configured with a 1% false-positive rate and 10,000 initial capacity using `BF.RESERVE`.
   - **Startup Warmup**: A dedicated `CommandLineRunner` populates the filter with all existing user emails from PostgreSQL on application startup.
   - **Enumeration Protection**: Rejects login attempts for non-existent emails instantly at the Redis layer, protecting against brute-force/enumeration.
   - **Database Shield**: In registration flow, it acts as a fast pre-check, reducing 99% of unnecessary "Exists" queries to the database.

## 3. Database Design Patterns

### Advanced Schema Logic

- **UUID v7 Primary Keys**: Time-ordered UUIDs for optimal B-Tree index performance.
- **Soft Delete**: All major tables include `deleted_at` for data recovery and auditing.
- **Optimistic Locking**: Implementation of `version` columns to prevent data overwriting in concurrent environments.

- **JPA Specification & Search Criteria**: Implementation of `GenericSpecification` for dynamic, multi-criteria filtering (e.g., `?price_gt=100&city=Hanoi`) without writing redundant repository methods.
- **UUID v7 Primary Keys**: Time-ordered UUIDs for optimal B-Tree index performance.

### Business Modules

...

### API Standardization

...

### Performance & Scalability

- **Distributed Caching**: Integrated Spring Cache with Redis to reduce DB load and improve response times.
- **MapStruct for DTO Mapping**: Automated DTO-Entity conversion using MapStruct to reduce boilerplate, improve type safety, and maintain a clean Service layer.
- **Idempotency Framework (X-Idempotency-Key)**: Custom `@Idempotent` annotation and AOP-based interceptor using Redis to prevent duplicate request processing (Double-submit) for critical operations.
- **Resilience Layer (Fault Tolerance)**: Integration of **Resilience4j** implementing Circuit Breaker and Retry patterns for third-party service calls (Cloudinary, Resend) to prevent cascading failures.
- **Stateless Architecture**: Secured for future JWT integration with stateless session management.

### Observability & Traceability

- **Request Tracing**: `RequestIdFilter` generates a unique `X-Request-ID` for every request.
   - **4-Layer Split Logging**: Hệ thống phân tách log thành 4 luồng riêng biệt để tối ưu giám sát:
      - `system.log`: Hoạt động chung của hệ thống.
      - `request_success.log`: Toàn bộ request thành công (2xx) kèm theo thời gian xử lý (`execution time`).
      - `request_error.log`: Các lỗi nghiệp vụ và client (4xx, Validation).
      - `error.log`: Các lỗi nghiêm trọng (5xx) và Exceptions.
   - **Hierarchical Archiving**: Log cũ được nén `.gz` và lưu trữ theo cấu trúc thư mục `logs/archive/YYYY-MM/YYYY-MM-DD/` để quản lý gọn gàng và dễ dàng truy xuất theo thời gian.
   - **Execution Time Tracking**: Sử dụng AOP (`LoggingAspect`) để đo lường và ghi nhận độ trễ (latency) của từng request ngay trong log.
   - **MDC Integration**: Trace ID (`requestId`) được tự động tiêm vào mọi dòng log để liên kết dữ liệu giữa 4 file log khác nhau.

- **Health Monitoring**: Integrated Spring Boot Actuator for real-time system monitoring.
- **Request Context**: `RequestContextHolder` (ThreadLocal) provides global access to request-scoped data (Trace ID, User ID), similar to NestJS ExecutionContext.

## 5. Frontend Core Patterns

### API Architecture

- **Axios Client**: Centralized `apiClient` with interceptors for:
   - **Cross-system Traceability**: Tự động tạo và đính kèm `X-Request-ID`.
   - **Synchronized Refresh Queue**: Cơ chế hàng đợi thông minh giúp chặn đứng việc gọi `refresh` token trùng lặp. Khi có nhiều request cùng bị lỗi 401, chỉ có duy nhất 1 request `refresh` được gửi đi, các request còn lại sẽ nằm trong hàng đợi và tự động thực hiện lại sau khi có session mới.
   - **Global Error Normalization**: Chuyển đổi mã lỗi backend thành thông báo thân thiện cho người dùng.

### State Management

- **Zustand Store**: Lightweight and persistent global state management (e.g., `authStore`) for user sessions and application settings.

### Form & Validation

- **Zod & React Hook Form**: Standardized schema-based validation for robust client-side data integrity.

### Component Architecture

- **Standardized Directory Structure**:
   - `services/`: API communication layer.
   - `store/`: Global state definitions.
   - `types/`: Shared TypeScript interfaces.
   - `components/ui/`: Reusable atomic components (Shadcn/ui).

### Monitoring

- **Health Monitoring**: Integrated Spring Boot Actuator for real-time system monitoring.

## 6. Security Architecture

- **Security Foundation**: Robust `SecurityConfig` set up with CSRF protection disabled (for tokens), CORS enabled, and stateless session policy.
- **Public/Private Split**: Clear separation between public metadata/swagger endpoints and secured API routes.
- **RBAC (Role-Based Access Control)**: Comprehensive system with `roles` (Admin, Partner, Driver, User) and granular `permissions` (e.g., `property:write`, `ride:manage`). Managed via `SecurityConstants` for type-safety.
- **Security Standard**: All API security checks must use constants from `SecurityConstants` via SpEL (e.g., `T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN`) to ensure consistency and avoid hardcoded strings.
- **Layout Guards**: Role-based access control implemented at the Next.js layout level using derived state from Zustand to prevent unauthorized access and flickering.

## 7. Authentication & Session Management

The system implements a robust, secure authentication mechanism designed for production environments:

- **JWT with HttpOnly Cookies**: Access and Refresh tokens are stored in secure, HttpOnly, SameSite=Lax cookies to mitigate XSS attacks.
- **Fingerprinting & Security Header**: To prevent JWT theft/hijacking, the system uses a dual-key verification. The backend compares a hash in the JWT with a raw `x_fgp` value. For Server-side fetches in Next.js (Server Components), this header must be manually extracted from cookies and propagated to the backend fetch call.
- **Refresh Token Logic**: Automated token rotation. When a request fails with `AUTH_006` (Token Expired), the client transparently attempts to refresh the session before retrying the original request.
- **Stateless Verification**: The backend validates JWTs without DB lookups for primary access, using Redis for session invalidation (Logout).

## 8. Partner Onboarding Lifecycle

To maintain platform quality, the partner registration is a multi-stage verified process:

1. **OTP Verification**: Quy trình xác thực email/OTP được thực hiện tin cậy thông qua **Transactional Outbox Pattern**, đảm bảo mã xác thực luôn được ghi nhận và gửi đến người dùng kể cả khi có sự cố mạng.
2. **Role Upgrade**: Upon verification, the user's role is upgraded to `ROLE_PARTNER` via a specialized backend endpoint.
3. **Session Re-authentication**: After upgrade, a new JWT containing the updated roles is issued to ensure immediate access to partner-specific features.

## 9. Messaging & Asynchronous Processing

To ensure high performance and non-blocking operations, the system implements an asynchronous messaging pipeline:

- **Kafka Integration**: Used as the primary message broker for cross-service communication and background tasks.
- **Reliable Email System**:
   - **Persistence**: Các sự kiện email được lưu vào bảng `outbox_events` ngay trong cùng transaction nghiệp vụ.
   - **Worker**: `OutboxWorker` quét bảng và đẩy dữ liệu sang Kafka topic `omnibooking-mail-topic`.
   - **Delivery**: Consumer nghe topic và gọi **Resend SDK** để gửi email thực tế.
- **Template Engine**: Uses **Thymeleaf** to render premium HTML email templates with dynamic data injection.

## 10. Configuration Management (NestJS Style)

The system uses a centralized, type-safe configuration pattern similar to NestJS ConfigModule:

- **Centralized POJO**: `AppProperties` class serves as the single source of truth for all custom configurations.
- **Grouped Settings**: Properties are logically grouped (e.g., `app.security`, `app.mail`).
- **Validation & Fail-Fast**: Uses Bean Validation (`@NotBlank`, `@Validated`) to ensure all critical environment variables are present at startup.
- **Environment Mapping**: Mapped from the root `.env` file into `application.properties` via `${VAR_NAME}` syntax.

## 11. Database V2 Architecture (Booking & Property)

The V2 schema expands the system to support a complete hospitality business lifecycle:

### Property Management Module

- **Granular Inventory**: Separation of `properties` (the establishment) and `room_types` (specific offerings).
- **Dynamic Pricing & Availability**: `room_availability` table manages daily inventory counts and `price_override` for seasonal or event-based pricing.
- **Flexible Amenities**: A polymorphic-style approach using junction tables for both property-level and room-level amenities.

### Booking Engine

- **State Machine Logging**: `bookings` status changes are tracked in `booking_status_logs` for full traceability (Pending -> Confirmed -> Stayed -> Cancelled).
- **Guest-centric Data**: Stores specific guest information for each booking, allowing it to differ from the account holder's profile.

### Financials & Cancellations

- **Automated Refund Calculation**: `cancellation_policies` define free cancellation windows and penalty tiers.
- **Transaction History**: `transactions` table tracks all `PAYMENT` and `REFUND` events, with a `JSONB` metadata column for deep integration with payment providers (Stripe, etc.).
- **Promotion Ready**: `coupons` table supports various discount types (Percentage, Fixed) with usage limits and validity periods.

## 12. Media Management & Image Optimization

OmniBooking uses a hybrid approach for media to ensure high performance:

### Cloudinary CDN Integration

- **Image Hosting**: All property and user images are stored on **Cloudinary** for global delivery.
- **On-the-fly Optimization**: Images are resized and compressed via URL parameters to reduce frontend bundle size and improve LCP.
- **Typed Responses**: The system uses a dedicated `CloudinaryResponse` DTO to ensure type-safety across the backend.

### Asynchronous Processing with Kafka

To prevent bottlenecks during heavy media operations:

- **Async Workflow**: For non-critical updates (e.g., bulk property image sync), the backend publishes a `MediaProcessedEvent` to Kafka.
- **Decoupled Processing**: Worker services consume these events to perform secondary tasks like generating search indices or updating social metadata without blocking the user request.

## 13. Security & Password Recovery Flow

The system implements a multi-layered security approach for sensitive operations like password resets:

### Password Recovery Lifecycle

1. **Request Phase**:
   - **Enumeration Protection**: The system always returns a success message regardless of whether the email exists in the database.
   - **Rate Limiting**: Implemented a sliding window limit (3 requests per 1 minute) via Redis `INCR` to prevent mail server abuse.
2. **Verification Phase**:
   - **Short-lived Tokens**: Reset tokens are stored in Redis with a strict 15-minute expiration.
   - **One-time Use**: Tokens are immediately invalidated after a successful reset.
3. **Session Invalidation**:
   - **Global Logout**: Users have the option to "Sign out from all other devices" during reset. This triggers `SessionService.revokeAllUserSessions()`, which clears all related session keys in Redis.

### Security Best Practices

- **Vietnamese Typography**: Shared `AuthBranding` component uses optimized `leading-tight` and `font-weight` to prevent character clipping for accented Vietnamese text.
- **Error Obfuscation**: Client-side mappings convert technical `errorCodes` into user-friendly localized messages without leaking backend implementation details.

## 14. OAuth2 & Social Authentication Architecture

Hệ thống triển khai một kiến trúc linh hoạt cho phép tích hợp không giới hạn các nhà cung cấp định danh (Social Providers):

### OAuth2 Strategy & Factory Pattern

- **OAuth2ProviderService (Strategy)**: Interface định nghĩa các phương thức chung cho mọi nhà cung cấp (generate URL, exchange code).
- **OAuth2ServiceFactory (Factory)**: Tự động phát hiện và cung cấp Service tương ứng dựa trên provider name (`google`, `apple`...).
- **Unified Controller**: Mọi provider đều dùng chung các endpoint động `/auth/{provider}/url` và `/auth/{provider}/callback`.

### Smart Data Synchronization

- **Smart Name Splitting**: Triển khai thuật toán tách tên dựa trên logic văn hóa (Last word = Tên, Rest = Họ và Đệm) để đảm bảo dữ liệu trong `UserProfile` luôn chuẩn xác và không bị trùng lặp khi lấy từ các API xã hội.
- **Avatar Protection Policy**: Hệ thống chỉ tự động cập nhật ảnh đại diện từ mạng xã hội nếu người dùng chưa có ảnh hoặc đang sử dụng ảnh cũ của mạng xã hội đó. Nếu người dùng đã tự upload ảnh cá nhân, hệ thống sẽ tôn trọng và bảo vệ lựa chọn đó.

## 15. Smart Search & Real-time Indexing Architecture

Kiến trúc tìm kiếm của OmniBooking được thiết kế để xử lý hàng triệu bản ghi với tốc độ mili giây, kết hợp sức mạnh của Elasticsearch và luồng dữ liệu thời gian thực của Kafka:

### Elasticsearch Search Engine

- **Full-text Search**: Sử dụng Elasticsearch 8.x để tìm kiếm không dấu/có dấu, tìm kiếm gần đúng (Fuzzy Search) cho các địa danh.
- **Criteria-based Filtering**: Triển khai `CriteriaQuery` để xử lý các bộ lọc phức tạp (Giá, Loại hình chỗ nghỉ, Tiện ích, Điểm đánh giá) một cách linh hoạt mà không làm giảm hiệu năng.
- **Geo-spatial Search**: Lưu trữ tọa độ (`GeoPoint`) để hỗ trợ tìm kiếm theo bán kính và hiển thị bản đồ.

### Real-time Data Synchronization (Kafka Pipeline)

- **CDC-like Pattern**: Mọi thay đổi về thông tin khách sạn hoặc giá phòng ở PostgreSQL đều kích hoạt một sự kiện (Event) đẩy vào Kafka.
- **Search Indexer**: Một Service chuyên biệt lắng nghe Kafka và cập nhật ngay lập tức vào Elasticsearch Index, đảm bảo dữ liệu tìm kiếm luôn đồng bộ với Database chính.

### Interactive Map Engine (Leaflet)

- **Client-side Rendering**: Sử dụng Leaflet với cơ chế **Dynamic Import** để tối ưu hóa SEO và tốc độ tải trang (LCP).
- **Price-tag Markers**: Custom Markers hiển thị trực tiếp giá tiền trên bản đồ, giúp người dùng có trải nghiệm tương tác trực quan như các nền tảng OTA hàng đầu thế giới.

## 16. Reliable Messaging (Transactional Outbox Pattern)

Để đảm bảo tính nhất quán tuyệt đối giữa Database và Message Broker (Kafka), hệ thống triển khai Transactional Outbox Pattern:

- **Atomic Operations**: Các sự kiện (như Register, Forgot Password) không bắn trực tiếp vào Kafka. Thay vào đó, chúng được lưu vào bảng `outbox_events` trong cùng một Transaction với dữ liệu nghiệp vụ.
- **Guaranteed Delivery**: Một `OutboxWorker` (Scheduled Task) định kỳ quét các sự kiện chưa xử lý và đẩy chúng vào Kafka. Điều này đảm bảo hễ User được tạo thành công thì chắc chắn Email sẽ được gửi, kể cả khi Kafka tạm thời bị sập.
- **Hybrid Wake-Up Mechanism**: Để tối ưu độ trễ, hệ thống sử dụng cơ chế "đánh thức" tức thì. Ngay sau khi Transaction nghiệp vụ commit thành công, một tín hiệu sẽ kích hoạt Worker xử lý ngay mà không đợi đến chu kỳ quét tiếp theo.
- **Concurrency & Safety**:
   - **Instance Level**: Sử dụng `AtomicBoolean` để đảm bảo trong cùng một máy chủ, chỉ có duy nhất một luồng xử lý Outbox tại một thời điểm, tránh lãng phí tài nguyên.
   - **Cluster Level**: Sử dụng câu lệnh SQL `FOR UPDATE SKIP LOCKED` giúp nhiều instance có thể chạy song song mà không tranh chấp hoặc xử lý trùng lặp dữ liệu.
- **Data Integrity**: Payload của sự kiện được lưu trữ dưới dạng JSON kèm theo thông tin `payload_class` để đảm bảo việc giải mã chính xác tại worker trước khi gửi đi.

## 17. Global Search & Discovery Engine

Hệ thống tìm kiếm của OmniBooking được thiết kế để mang lại trải nghiệm nhanh chóng, chính xác và mang tính khám phá cao, tương đương với các tiêu chuẩn của Airbnb hay Booking.com.

### 17.1. Hybrid Search Architecture (Elasticsearch + Postgres)

- **Search Index (Elasticsearch)**: Toàn bộ dữ liệu về địa danh (Cities, Regions, Hotels, Landmarks) được đánh chỉ mục vào Elasticsearch.
- **Full-text & Fuzzy Matching**: Sử dụng cơ chế `bool query` kết hợp `match_phrase_prefix` và `fuzzy` để xử lý các tìm kiếm có dấu, không dấu, hoặc gõ sai chính tả của người dùng.
- **Unified DTO Mapping**: Backend trả về một cấu trúc `DestinationSuggestionResponse` nhất quán, hỗ trợ i18n đa ngôn ngữ cho các loại địa danh (ví dụ: City -> Thành phố, Hotel -> Khách sạn).

### 17.2. Search Analytics & Discovery (Trending Logic)

- **Search Logging**: Mọi hành vi tìm kiếm của người dùng đều được ghi lại vào bảng `search_logs` thông qua `SearchLogService`. Dữ liệu bao gồm: từ khóa, mã quốc gia (dựa trên IP), và tọa độ địa lý.
- **Trending Algorithm**: Danh sách "Trending Now" được tính toán dựa trên tần suất xuất hiện của từ khóa trong 7 ngày gần nhất.
- **Manual Boosting**: Hệ thống hỗ trợ cờ `is_boosted` trong Database để quản trị viên có thể đẩy các địa danh chiến dịch (Promotion) lên vị trí đầu tiên của danh sách gợi ý.

### 17.3. Internationalization (i18n) Strategy

- **Dynamic Labels**: Các loại địa danh (Type Labels) được dịch động tại Frontend dựa trên namespace `Common.Search` trong các tệp `vi.json` và `en.json`.
- **Locale-aware Search**: Elasticsearch được cấu hình để ưu tiên các kết quả phù hợp với ngôn ngữ hiện tại của người dùng, đảm bảo trải nghiệm bản địa hóa hoàn toàn.

## 18. Global Currency & Real-time Pricing System

Hệ thống OmniBooking được thiết kế để hoạt động trên quy mô toàn cầu với khả năng xử lý đa tiền tệ linh hoạt và chính xác:

### 18.1. USD-Based Financial Core

- **Single Base Currency**: Toàn bộ dữ liệu giá tiền (Property rates, Booking totals, Transactions) trong Database được lưu trữ duy nhất dưới đơn vị **USD** để đảm bảo tính nhất quán và dễ dàng hạch toán.
- **Dynamic Conversion**: Giá tiền chỉ được quy đổi sang đơn vị tiền tệ của người dùng (VND, EUR...) tại thời điểm hiển thị hoặc thanh toán dựa trên tỉ giá thực tế.

### 18.2. Multi-layer Exchange Rate Strategy

Hệ thống sử dụng cơ chế lấy tỉ giá 3 lớp để tối ưu hóa hiệu năng và độ tin cậy:

1. **Layer 1: Redis Cache**: Truy xuất tức thì với TTL 4 giờ.
2. **Layer 2: Database**: Lưu trữ lịch sử tỉ giá (Audit Trail) và làm nguồn dự phòng nếu Redis bị trống.
3. **Layer 3: External API (ExchangeRate-API)**: Nguồn dữ liệu gốc, chỉ gọi khi cả Redis và DB đều không có dữ liệu mới.

### 18.3. Organic Pricing & Profit Margin (Markup)

Để đảm bảo an toàn tài chính và tạo trải nghiệm "thật" cho người dùng, hệ thống áp dụng logic Markup thông minh:

- **Automatic Markup**: Mọi tỉ giá lấy từ API đều được cộng thêm một khoảng chênh lệch trước khi lưu vào hệ thống:
   - **VND**: Cộng ngẫu nhiên từ **300đ - 1,000đ** (Random Markup) để giá tiền thay đổi sinh động mỗi ngày.
   - **Other Currencies**: Cộng thêm **5%** phí bảo hiểm tỉ giá.
- **Update Cycle**: Một `CurrencyWorker` chạy định kỳ mỗi **4 tiếng** (0h, 4h, 8h, 12h, 16h, 20h) để cập nhật tỉ giá và thay đổi mức Random Markup, giúp website luôn có cảm giác "sống" và cập nhật liên tục.

---

_Last Updated: 2026-05-13_
