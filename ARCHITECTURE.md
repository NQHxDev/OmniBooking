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
- **No Open-In-View (OSIV Disabled)**: Explicitly disabled OSIV (`spring.jpa.open-in-view=false`) to optimize database connection pool utilization and enforce clean boundaries. Mapping to DTOs is fully materialized inside transaction boundaries (`@Transactional` Service layer) using MapStruct to eliminate the risk of late `LazyInitializationException` and avoid N+1 query patterns during serialization.

### Observability & Traceability

- **Request Tracing**: `RequestIdFilter` generates a unique `X-Request-ID` for every request.
   - **4-Layer Split Logging**: The system segregates logs into 4 distinct streams to optimize monitoring:
      - `system.log`: General system operations and activities.
      - `request_success.log`: All successful requests (2xx) along with their processing duration (`execution time`).
      - `request_error.log`: Business logic and client-side errors (4xx, Validation failures).
      - `error.log`: Critical system errors (5xx) and unhandled exceptions.
   - **Hierarchical Archiving**: Historical logs are compressed as `.gz` and organized in a structured `logs/archive/YYYY-MM/YYYY-MM-DD/` directory path for tidy management and easy time-based retrieval.
   - **Execution Time Tracking**: Utilizes AOP (`LoggingAspect`) to measure and record the latency of each request directly in the logs.
   - **MDC Integration**: Trace ID (`requestId`) is automatically injected into all log entries to correlate log events across the 4 distinct log files.

- **Health Monitoring**: Integrated Spring Boot Actuator for real-time system monitoring.
- **Request Context**: `RequestContextHolder` (ThreadLocal) provides global access to request-scoped data (Trace ID, User ID), similar to NestJS ExecutionContext.

## 5. Frontend Core Patterns

### API Architecture

- **Axios Client**: Centralized `apiClient` with interceptors for:
   - **Cross-system Traceability**: Automatically generates and attaches the `X-Request-ID` header.
   - **Synchronized Refresh Queue**: A smart queuing mechanism that prevents duplicate refresh token requests. When multiple concurrent requests fail with a 401 error, only a single refresh request is sent; other pending requests are queued and automatically retried once a new session is established.
   - **Global Error Normalization**: Standardizes backend error codes into user-friendly localized notification messages.

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

1. **OTP Verification**: The email/OTP verification workflow is reliably executed via the **Transactional Outbox Pattern**, ensuring that verification codes are always persisted and successfully delivered to the user even in the event of network disruptions.
2. **Role Upgrade**: Upon verification, the user's role is upgraded to `ROLE_PARTNER` via a specialized backend endpoint.
3. **Session Re-authentication**: After upgrade, a new JWT containing the updated roles is issued to ensure immediate access to partner-specific features.

## 9. Messaging & Asynchronous Processing

To ensure high performance and non-blocking operations, the system implements an asynchronous messaging pipeline:

- **Kafka Integration**: Used as the primary message broker for cross-service communication and background tasks.
- **Reliable Email System**:
   - **Persistence**: Email events are stored in the `outbox_events` table within the same business database transaction.
   - **Worker**: The `OutboxWorker` scans this table periodically and publishes payload details to the Kafka topic `omnibooking-mail-topic`.
   - **Delivery**: A consumer listens to the topic and invokes the **Resend SDK** to perform the actual email delivery.
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

The system implements a flexible, highly extensible architecture allowing seamless integration of unlimited identity providers (Social Providers):

### OAuth2 Strategy & Factory Pattern

- **OAuth2ProviderService (Strategy)**: Interface defining common methods for all identity providers (e.g., generate authorization URL, exchange authorization code).
- **OAuth2ServiceFactory (Factory)**: Automatically detects and provides the appropriate Service based on the provider's name (`google`, `zalo`, etc.).
- **Unified Controller**: All social providers share the dynamic endpoints `/auth/{provider}/url` and `/auth/{provider}/callback`.

### Smart Data Synchronization

- **Smart Name Splitting**: Implements a culturally aware name splitting algorithm (e.g., Last word = First Name, Rest = Last & Middle Name) to ensure that the `UserProfile` data is accurate and not duplicated when retrieved from social APIs.
- **Avatar Protection Policy**: The system only automatically updates the user's avatar from their social profile if they don't have an avatar set or are using an older social-provided avatar. If the user has uploaded a custom personal avatar, the system respects and preserves their choice.

## 15. Smart Search & Real-time Indexing Architecture

OmniBooking's search architecture is engineered to query millions of records in milliseconds by leveraging Elasticsearch combined with real-time data streaming via Kafka:

### Elasticsearch Search Engine

- **Full-text Search**: Utilizes Elasticsearch 8.x for accented/unaccented searching, including Fuzzy Search for locations.
- **Criteria-based Filtering**: Implements complex criteria queries to handle advanced filtering (Price, Accommodation Type, Amenities, Review Score) dynamically without sacrificing performance.
- **Geo-spatial Search**: Stores coordinate points (`GeoPoint`) to support radius-based searching and map integration.

### Real-time Data Synchronization (Kafka Pipeline)

- **CDC-like Pattern**: Any updates to hotel information or room rates in PostgreSQL trigger an event published to Kafka.
- **Search Indexer**: A dedicated service consumes events from Kafka and immediately updates the Elasticsearch index, ensuring search data is perfectly synchronized with the primary database.
- **Deferred Property Creation Sync**: Upon creation of a new property, immediate synchronization to Elasticsearch is bypassed. Instead, the property is indexed in Elasticsearch only after its main image upload successfully finishes and is processed by `MediaConsumer`. This ensures search results do not present newly created properties without valid main images.

### Interactive Map Engine (Leaflet)

- **Client-side Rendering**: Uses Leaflet with **Dynamic Import** components to optimize SEO and page load performance (LCP).
- **Price-tag Markers**: Custom Markers display rates directly on the map, providing users with an intuitive, interactive experience similar to top-tier global Online Travel Agencies (OTAs).

## 16. Reliable Messaging (Transactional Outbox Pattern)

To guarantee absolute consistency between the relational Database and the Message Broker (Kafka), the system implements the Transactional Outbox Pattern:

- **Atomic Operations**: Critical events (such as User Registration or Password Reset) are not published directly to Kafka. Instead, they are persisted to the `outbox_events` table in the exact same database transaction as the business state change.
- **Guaranteed Delivery**: An `OutboxWorker` (Scheduled Task) periodically polls unprocessed events and publishes them to Kafka. This ensures that once a user is successfully created, the welcome/verification email is guaranteed to be sent, even if Kafka is temporarily offline.
- **Hybrid Wake-Up Mechanism**: To optimize latency, the system utilizes a real-time wake-up mechanism. As soon as the business transaction commits successfully, an internal signal immediately wakes up the Outbox worker to process the event, bypassing the standard polling delay.
- **Concurrency & Safety**:
   - **Instance Level**: Uses an `AtomicBoolean` lock flag to ensure that only one processing thread runs per server instance at a time, avoiding wasted system resources.
   - **Cluster Level**: Uses the `SELECT ... FOR UPDATE SKIP LOCKED` SQL query to enable multiple distributed instances to run concurrently without lock contention or duplicate event processing.
- **Data Integrity**: The event payload is stored as JSON alongside a `payload_class` metadata column to guarantee highly accurate deserialization at the worker level prior to dispatch.

## 17. Global Search & Discovery Engine

OmniBooking's search system is designed to provide a fast, accurate, and discovery-driven experience matching global standards established by platforms like Airbnb and Booking.com.

### 17.1. Hybrid Search Architecture (Elasticsearch + Postgres)

- **Search Index (Elasticsearch)**: All location-relevant data (Cities, Regions, Hotels, Landmarks) is indexed in Elasticsearch.
- **Full-text & Fuzzy Matching**: Leverages a `bool query` combined with `match_phrase_prefix` and `fuzzy` matching to handle accented, unaccented, or typo-ridden user searches.
- **Unified DTO Mapping**: The backend returns a standardized `DestinationSuggestionResponse` structure, fully supporting multi-language i18n translations for location types (e.g., City -> "City" / "Thành phố", Hotel -> "Hotel" / "Khách sạn").

### 17.2. Search Analytics & Discovery (Trending Logic)

- **Search Logging**: Every user search behavior is logged to the `search_logs` table via `SearchLogService`, capturing the keyword, country code (resolved via client IP), and geographic coordinates.
- **Trending Algorithm**: The "Trending Now" list is dynamically computed based on search term frequency over the past 7 days.
- **Manual Boosting**: Administrators can manually push specific promotional/campaign locations to the top of the search suggestions using the `is_boosted` flag in the database.

### 17.3. Internationalization (i18n) Strategy

- **Dynamic Labels**: Location type labels are translated dynamically on the frontend using the `Common.Search` namespace within `vi.json` and `en.json`.
- **Locale-aware Search**: Elasticsearch is configured to prioritize search results that match the user's active locale, ensuring a fully localized search experience.

### 17.4. IP-Based Geolocation & Country Detection

- **Client IP Extraction**: The backend extracts the client's public IP address via the `X-FORWARDED-FOR` request header (which is populated by reverse proxies or forwarded by the Next.js server). If not present, it falls back to `HttpServletRequest.getRemoteAddr()`.
- **MaxMind GeoIP2 Resolution**: The `GeoLocationService` reads the client IP and queries a local MaxMind GeoIP2 city database (`GeoLite2-City.mmdb`) to resolve the user's ISO 3166-1 alpha-2 country code (e.g. `VN`, `FR`, `US`).
- **Configurable Fallback**: If the IP is a local loopback address (`127.0.0.1`, `0:0:0:0:0:0:0:1`) or if the database is missing, it falls back to a default country configured via `app.geo.default-country` (defaults to `VN`).
- **Next.js SSR Forwarding**: For Server-side Rendering (SSR) fetches where the request originates from the Next.js server itself, the client's original IP address is read from the incoming request's `x-forwarded-for` header and forwarded to the backend API inside the `X-Forwarded-For` HTTP header. This ensures correct location targeting.

## 18. Global Currency & Real-time Pricing System

The OmniBooking system is designed to operate globally with flexible and highly precise multi-currency processing:

### 18.1. USD-Based Financial Core

- **Single Base Currency**: All financial pricing data (Property rates, Booking totals, Transactions) is stored exclusively in **USD** within the database to maintain absolute consistency and simplify accounting auditing.
- **Dynamic Conversion**: Rates are converted to the user's preferred local currency (VND, EUR, etc.) on-the-fly during rendering or payment processing based on live exchange rates.

### 18.2. Multi-layer Exchange Rate Strategy

To optimize performance and fault-tolerance, the system retrieves exchange rates via a 3-tier strategy:

1. **Layer 1: Redis Cache**: Immediate retrieval with a 4-hour Time-to-Live (TTL).
2. **Layer 2: Database**: Persisted exchange rate history (Audit Trail) serving as a fallback if the cache is empty.
3. **Layer 3: External API (ExchangeRate-API)**: The source of truth, fetched only when both Redis and the database lack current exchange rate data.

### 18.3. Organic Pricing & Profit Margin (Markup)

To ensure financial safety and foster a highly interactive user experience, the system applies a smart markup logic:

- **Automatic Markup**: Exchange rates fetched from the API are marked up before system storage:
   - **VND**: Applies a random markup ranging from **300 VND to 1,000 VND** (Random Markup) to make prices feel dynamic and alive daily.
   - **Other Currencies**: Incorporates a standard **5%** spread fee to cushion against intra-day currency fluctuations.
- **Update Cycle**: A periodic `CurrencyWorker` runs every **4 hours** (0:00, 4:00, 8:00, 12:00, 16:00, 20:00) to fetch the latest rates and recalculate the Random Markup, ensuring the website consistently feels active and freshly updated.

---

_Last Updated: 2026-05-29_
