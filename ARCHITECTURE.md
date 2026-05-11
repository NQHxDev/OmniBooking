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
- **Professional & Structured Logging**:
   - **AOP Logging**: Automatic logging of method entry/exit for all controllers.
   - **JSON Logging (Production)**: Uses `Logstash Logback Encoder` to output logs in structured JSON format for easy ingestion into ELK/CloudWatch (enabled via `prod` profile).
   - **MDC Integration**: Trace ID is automatically injected into every log line.
   - **Log Rotation**: Automated daily log compression and 30-day retention.

- **Health Monitoring**: Integrated Spring Boot Actuator for real-time system monitoring.
- **Request Context**: `RequestContextHolder` (ThreadLocal) provides global access to request-scoped data (Trace ID, User ID), similar to NestJS ExecutionContext.

## 5. Frontend Core Patterns

### API Architecture

- **Axios Client**: Centralized `apiClient` with interceptors for:
   - Automatic `X-Request-ID` generation for cross-system traceability.
   - Global error handling and response normalization.
   - Environment-based `BASE_URL` configuration.

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

1. **OTP Verification**: Async delivery of one-time passwords via Kafka to ensure email ownership.
2. **Role Upgrade**: Upon verification, the user's role is upgraded to `ROLE_PARTNER` via a specialized backend endpoint.
3. **Session Re-authentication**: After upgrade, a new JWT containing the updated roles is issued to ensure immediate access to partner-specific features.

## 9. Messaging & Asynchronous Processing

To ensure high performance and non-blocking operations, the system implements an asynchronous messaging pipeline:

- **Kafka Integration**: Used as the primary message broker for cross-service communication and background tasks.
- **Background Email System**:
   - **Producer**: `EmailProducer` pushes events to `omnibooking-mail-topic`.
   - **Consumer**: `EmailConsumer` listens to the topic and processes emails.
   - **Delivery**: Integrated with **Resend SDK** for professional email delivery.
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

---

_Last Updated: 2026-05-11_
