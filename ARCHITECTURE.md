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

Implemented `redis/redis-stack-server` to support high-performance operations:

- **Session/Token Management**: Offloaded to Redis for scalability.
- **Bloom Filter**: Used for ultra-fast existence checks (e.g., username/email availability) to prevent unnecessary DB hits.

## 3. Database Design Patterns

### Advanced Schema Logic

- **UUID v7 Primary Keys**: Time-ordered UUIDs for optimal B-Tree index performance.
- **Soft Delete**: All major tables include `deleted_at` for data recovery and auditing.
- **Optimistic Locking**: Implementation of `version` columns to prevent data overwriting in concurrent environments.

### Business Modules

- **Account/Profile Separation**: Decoupling authentication data (`users`) from metadata (`user_profiles`) for security and flexibility.
- **RBAC (Role-Based Access Control)**: Comprehensive system with `roles`, `permissions`, and junction tables.
- **Loyalty Rank System**: Multi-tier ranking (`ranks`) based on points to drive customer engagement.

## 4. Backend Core Patterns

### API Standardization

- **API Versioning**: Global prefix `/api/v1/` implemented for future-proof service evolution.
- **Standardized Response**: All APIs return a consistent `ApiResponse<T>` structure.
- **Global Error Handling**: Centralized `GlobalExceptionHandler` ensures professional error messages without leaking system internals.
- **Pagination**: Standardized `PageResponse` supporting "Lazy Loading" style data delivery.

### Performance & Scalability

- **Distributed Caching**: Integrated Spring Cache with Redis to reduce DB load and improve response times.
- **Stateless Architecture**: Secured for future JWT integration with stateless session management.

### Observability & Traceability

- **Request Tracing**: `RequestIdFilter` generates a unique `X-Request-ID` for every request.
- **Professional Logging**:
   - **AOP Logging**: Automatic logging of method entry/exit for all controllers.
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

## 7. Messaging & Asynchronous Processing

To ensure high performance and non-blocking operations, the system implements an asynchronous messaging pipeline:

- **Kafka Integration**: Used as the primary message broker for cross-service communication and background tasks.
- **Background Email System**:
    - **Producer**: `EmailProducer` pushes events to `omnibooking-mail-topic`.
    - **Consumer**: `EmailConsumer` listens to the topic and processes emails.
    - **Delivery**: Integrated with **Resend SDK** for professional email delivery.
- **Template Engine**: Uses **Thymeleaf** to render premium HTML email templates with dynamic data injection.

## 8. Configuration Management (NestJS Style)

The system uses a centralized, type-safe configuration pattern similar to NestJS ConfigModule:

- **Centralized POJO**: `AppProperties` class serves as the single source of truth for all custom configurations.
- **Grouped Settings**: Properties are logically grouped (e.g., `app.security`, `app.mail`).
- **Validation & Fail-Fast**: Uses Bean Validation (`@NotBlank`, `@Validated`) to ensure all critical environment variables are present at startup.
- **Environment Mapping**: Mapped from the root `.env` file into `application.properties` via `${VAR_NAME}` syntax.

---

_Last Updated: 2026-05-08_
