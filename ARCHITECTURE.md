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
- **N+1 Query Optimization & Cache Consistency (Partner Properties)**: Optimized retrieval of property listing endpoints (`getPropertiesByOwner`, `getFeaturedProperties`, `getNewProperties`) to fetch properties and their main images via a batch select `findMainImagesByEntityIds` (reducing database hits from $N+1$ to exactly $2$ queries). A dedicated Redis cache region `partner_properties` caches the partner properties list. To bypass Spring AOP self-invocation caching proxy limitations, public eviction methods are exposed in `PropertyService` and triggered externally by `MediaConsumer` when a main image is processed via Kafka, ensuring both partner and homepage caches are evicted instantly. Defensive stream grouping in mapping avoids structure crashes on database image duplicates.
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

- **Security Foundation**: Robust `SecurityConfig` set up with CORS enabled, stateless session policy, and CSRF protection enabled for cookie-based authentication via `CustomCsrfFilter`. State-changing requests verify that the `csrf_token` cookie and `X-CSRF-Token` header match, using HMAC-SHA256 bound to the backend `session_id` to prevent token spoofing. It also performs normalized Origin comparison using parsed URIs.
- **Public/Private Split**: Clear separation between public metadata/swagger endpoints and secured API routes.
- **RBAC (Role-Based Access Control)**: Comprehensive system with `roles` (Admin, Partner, Driver, User) and granular `permissions` (e.g., `property:write`, `ride:manage`). Managed via `SecurityConstants` for type-safety.
- **Security Standard**: All API security checks must use constants from `SecurityConstants` via SpEL (e.g., `T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN`) to ensure consistency and avoid hardcoded strings.
- **Layout Guards**: Role-based access control implemented at the Next.js layout level using derived state from Zustand to prevent unauthorized access and flickering.

## 7. Authentication & Session Management

The system implements a robust, secure authentication mechanism designed for production environments:

- **SameSite Cookie Isolation (Strict/Lax)**: Access tokens, refresh tokens, session IDs, and fingerprints are stored as secure HttpOnly cookies. Crucially, refresh tokens (`refresh_token`) and fingerprint cookies (`x_fgp`) are configured with `SameSite=Strict` to prevent CSRF. The session ID (`session_id`) cookie uses `SameSite=Lax` to allow cross-site navigation while preserving session binding.
- **CSRF Cookie Integration & Dynamic Nonces**: The `csrf_token` cookie is managed with `SameSite=Lax` and `httpOnly(false)`, enabling Next.js clients to attach the token as the `X-CSRF-Token` header. Validation utilizes a secure, dynamic `csrfNonce` stored in session metadata to generate dynamic HMAC-SHA256 CSRF signatures. A bootstrap mechanism automatically generates and attaches a CSRF cookie on `GET` requests even for anonymous (unauthenticated) users, securing the client before authentication. A bootstrap endpoint `GET /auth/csrf` is provided to fetch the CSRF token on startup.
- **Session Version Validation & Claim Binding**: The JWT contains a `sv` (session version) claim. The `JwtAuthenticationFilter` checks this claim against the current `sessionVersion` stored in Redis on every request. Any mismatch results in immediate session invalidation, neutralizing older tokens after rotation.
- **Key-Versioned AES-GCM Credentials Encryption**: To protect temporary child credentials stored in the parent session during the rotation grace period, the child's credentials (`childAccessToken` + `childRefreshToken`) are serialized and encrypted using AES-GCM with a secret key derived from `AppProperties.credentialEncryptionKey`. A key version suffix (e.g., `aes-v1`) is embedded to support seamless key rotation.
- **Fail-Closed Redis Outage Protection**: Database connectivity failures (e.g., `DataAccessException`) are propagated to the filter layer. `JwtAuthenticationFilter` blocks requests and returns a `503 Service Unavailable` status rather than silently bypassing verification, maintaining a fail-closed security posture during infrastructure outages.
- **HMAC-Signed Anti-Tampering Audit Logs**: Serious security anomalies, such as replay attacks, generate structured logs signed cryptographically with HMAC-SHA256 using `AppProperties.auditSecret`. This prevents log tempering and ensures forensic audit trail integrity during compromises.
- **Session Fixation Protection**: During login or registration, the backend automatically revokes any existing sessions for the user before establishing a new one, preventing session fixation vulnerabilities.
- **Enhanced Fingerprinting (32-byte Base64URL)**: The fingerprint cookie (`x_fgp`) is generated using `SecureRandom` to produce 32 cryptographically strong random bytes, which are then Base64URL-encoded (padding-free) and rotated on key lifecycle events to prevent JWT hijacking.
- **Device Binding & Compatibility**: The system parses the user's browser context into a normalized `DeviceSignature` structure (`version`, `platform`, `browserFamily`). During token refresh, this signature is matched against the saved session signature to detect stolen refresh tokens. A legacy bypass mechanism is implemented to allow older sessions (which lack this structure) to transition smoothly without user disruption.
- **Distributed Session Index Cleanup & Lock**: Active sessions for each user are indexed in a Redis Sorted Set (`user_sessions:{userId}`) with score equal to their expiry timestamp. Inline cleanup removes expired sessions during save/delete operations. A background cron job executes every 6 hours to clean up orphan expired session indexes, protected by a Redis distributed lock (`lock:session-cleanup`) acquired using `SET NX PX` and released via an owner-token-verified Lua script to avoid multi-instance execution conflicts.
- **Idempotent Session Revocation**: The password change flow increments the `tokenVersion` and updates the Redis cache. All active sessions are then completely revoked in Redis. To guarantee stability and prevent race conditions if logout, password change, and admin revocation are triggered concurrently, `revokeAllUserSessions` executes atomically via a Redis Lua script.
- **JWT Verification Optimization**: The authentication filter (`JwtAuthenticationFilter`) parses the JWT and extracts claims exactly once, improving authentication performance by ~5.8x compared to multiple parses.
- **Atomic Failure Counters**: Redis tracking of failed login attempts and 2FA failures utilizes atomic Lua scripts to prevent concurrency issues and race conditions.
- **Harden Refresh Token Rotation (Atomic & Resilient)**: Automated token rotation is implemented with atomic execution. The backend saves and verifies the new session in Redis _before_ revoking the old session. A transactional rollback strategy is deployed so that if any step of saving, verifying, or revoking fails, the newly generated session is cleaned up and the old session is kept active, preventing unintended user lockouts.
- **Refresh Session DoS Prevention**: The distributed lock TTL on refresh operations is set to a short duration of 2 seconds (originally 5 seconds) to prevent prolonged session lockups and protect against Denial of Service (DoS) attacks on session refresh.
- **JWT Revocation & Revocation Gap Mitigation**: The backend mitigates the JWT revocation gap by maintaining a `token_version` on the `User` entity (synced in the database and embedded as a claim in the JWT). During authentication in `JwtAuthenticationFilter`, the `token_version` extracted from the JWT is compared against the database-loaded `UserPrincipal`. Any mismatch immediately invalidates the authentication context, allowing instant global token revocation upon critical events (e.g., password change, explicit token revocation).
- **OAuth2 CSRF Protection**: Both Google and Zalo OAuth2 providers implement secure `state` parameter validation. A unique UUID state is dynamically generated and stored in Redis with a 15-minute expiration before initiating the authorization flow. Upon callback execution, the state parameter returned by the provider is matched against Redis, mitigating OAuth CSRF and session hijacking attempts.
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

To guarantee absolute consistency between the relational Database and the Message Broker (Kafka), the system implements a highly resilient, distributed-safe Transactional Outbox Pattern:

- **Atomic Operations**: Critical events (such as User Registration or Password Reset) are not published directly to Kafka. Instead, they are persisted to the `outbox_events` table in the exact same database transaction as the business state change.
- **Guaranteed Delivery**: An `OutboxWorker` (Scheduled Task) periodically polls unprocessed events and publishes them to Kafka. This ensures that once a user is successfully created, the welcome/verification email is guaranteed to be sent, even if Kafka is temporarily offline.
- **Hybrid Wake-Up Mechanism**: To optimize latency, the system utilizes a real-time wake-up mechanism. As soon as the business transaction commits successfully, an internal signal immediately wakes up the Outbox worker to process the event, bypassing the standard polling delay.
- **Concurrency & Cluster-Level Safety**:
   - **Distributed Lock & Release**: To safely support multiple instances (pods) scaling horizontally, the scheduler fetches events using `SELECT ... FOR UPDATE SKIP LOCKED` where `status IN ('PENDING', 'PROCESSING') AND next_retry_at <= :now`.
   - **Short-Lived Transactions**: In a separate transaction (`REQUIRES_NEW`), the worker immediately marks the fetched batch as `PROCESSING` and sets `next_retry_at = now + 5 minutes` (serving as a lock lease time) before committing. This releases the database locks immediately, preventing other instances from processing the same events while avoiding self-deadlocks and keeping database transaction times extremely short.
- **Resilience & Exponential Backoff**:
   - **Automatic Retries**: If sending to Kafka fails, the event is rolled back to `PENDING` and scheduled for retry using an exponential backoff strategy (1 min, 5 min, 15 min, 1 hour).
   - **Dead Letter Handling**: If an event fails 5 times, it is marked as `DEAD` (Dead Letter Queue) to prevent infinite retry loops and head-of-line blocking, allowing operators to inspect the failure reason stored in `last_error`.
- **Data Integrity & Schema Evolution**: The event payload is stored as JSON. The system utilizes an `OutboxEventRegistry` to explicitly map the `event_type` string to its corresponding Java class type (e.g. `EmailEvent.class`), avoiding fragile reflection practices (like `Class.forName()`) which can break during package refactoring or class renames.
- **End-to-End Idempotency (Idempotent Consumer)**:
   - **Unique Event Identity**: Every event transmitted via Kafka is assigned a unique, time-ordered UUID (`eventId`). When events are generated via the Outbox, the `eventId` matches the primary key `id` of the `outbox_events` table, ensuring absolute correlation.
   - **Deduplication Store (`processed_events`)**: A centralized database table `processed_events` tracks processed event IDs per consumer group. It uses a composite primary key `(event_id, consumer_group)` to allow different consumer groups to consume the same event while preventing duplicate executions within the same group.
   - **Atomic Claim-then-Process State Machine**: To prevent race conditions in highly concurrent environments (e.g., when partition rebalancing or parallel execution triggers duplicate consumption simultaneously), the check-then-act pattern has been upgraded to a transactional "Claim-then-Process" state machine with recovery:
      - **State Machine Definition**: Tracks event processing statuses: `PROCESSING` (currently handled), `COMPLETED` (processing succeeded), and `FAILED` (processing failed) using `status` and `updated_at` columns in the database.
      - **Pessimistic Locking Claim**: Before processing, the consumer calls `claimEvent` which queries the event record with a pessimistic write lock (`SELECT ... FOR UPDATE`). If it's a new event, it inserts it with status `PROCESSING`. If it already exists in the `FAILED` state, it updates status back to `PROCESSING` to allow retries. If it is already `COMPLETED` or actively `PROCESSING`, the duplicate claim is rejected and skipped safely.
      - **Completion & Failure Transition**: Upon successful completion of business logic, the consumer updates the status to `COMPLETED`. If processing fails, it catches the exception and updates the status to `FAILED` instead of deleting the record, capturing the state accurately.
      - **Stale Claim Recovery Worker**: A background worker (`IdempotencyRecoveryWorker`) runs every minute, scanning for events stuck in the `PROCESSING` state where the `lease_until` timestamp has expired (e.g., due to sudden consumer crash or JVM OOM) and resets them to `FAILED` to allow subsequent retries.
      - **Heartbeat & Lease Renewal**: To prevent heavy operations (such as media processing or slow database queries) from being prematurely marked as `FAILED` while still running, a dynamic lease renewal is used. When claiming, a `lease_until` timestamp is set (default +5 minutes). The consumer runs a background daemon thread (`LeaseRenewer`) to periodically extend this lease as long as it is actively processing, preventing double execution.
      - **History Purging Job**: To keep database sizes optimal, a scheduled job automatically deletes completed/failed logs older than 30 days every day at 2:00 AM.
   - **Idempotency Metrics**: The system registers Prometheus counter metrics `duplicate_event_count` and `skipped_event_count` to monitor and alert on duplicate message delivery in real-time.
- **Kafka Event Ordering (Partition Key)**:
   - **Aggregate Isolation**: To preserve message ordering for events related to the same resource, all domain events published from the Outbox are sent using the `aggregateId` or `entityId` as the Kafka partition key.
   - **Ordering Guarantee**: In the `RegistrationService`, the `userId` is used as the key for `UserCreatedEvent`. This ensures that all updates/deletions for a specific entity are routed to the same partition, avoiding race conditions and processing events in strict chronological order.
- **Cloudinary Compensation (Orphan Prevention)**:
   - **Rollback Compensation Pattern**: In the `MediaConsumer`, files are uploaded to Cloudinary before being saved to PostgreSQL. If the database transaction fails (e.g., connection timed out or database constraint error), the consumer catches the exception and immediately invokes a rollback compensation query calling `cloudinaryService.delete(publicId)`.
   - **Orphan Cleanup Metric**: The system monitors rollback events via the counter `orphaned_media_cleanup_count` to detect database failures during media upload processing.
- **Elasticsearch Sync Reliability (Outbox Sync)**:
   - **Outbox-based Synchronization**: To prevent Elasticsearch search indices from diverging from PostgreSQL (due to direct Kafka publish failures during network partitions), the `PropertySyncEvent` is routed through the Transactional Outbox.
   - **Transactional & Resilient Sync**: Synchronization requests are persisted in the same transaction as the media save. The outbox worker publishes the event to Kafka with retry capability and DLQ protection, and `search_sync_failure_count` tracks synchronization failures.
- **Production-Grade Enhancements (Epic Reliability)**:
   - **Outbox Queue Monitoring**: Dynamic Micrometer Gauges (exposed via `/actuator/prometheus`) track pending (`omnibooking.outbox.pending.count`), processing (`omnibooking.outbox.processing.count`), dead letter (`omnibooking.outbox.dead.count`), and total retry attempts (`omnibooking.outbox.retry.count`) for real-time queue health alerts.
   - **Outbox Auto-Cleanup**: A scheduled pruning task runs daily at 3:00 AM, purging processed events older than 30 days (`PROCESSED` status) to prevent index bloat and ensure optimal database performance.
   - **Event Schema Upcaster**: A schema evolution layer checks payload versioning. If the publisher pushes a v1 event but the consumer requires v2, the `EventUpcaster` dynamically transforms the JSON structure before broker dispatch (e.g. `USER_REGISTERED_MAIL` fields transformation), ensuring seamless backward compatibility. The upcasting routing logic is decoupled using the **Strategy Pattern** for scalable event schema evolution.
   - **Nightly Search Reconciliation**: A background reconciliation job audits data consistency daily at 1:00 AM. It compares PostgreSQL hotels against the Elasticsearch index, auto-detecting any missing/drifted items, and republishes correction events back to the outbox for automatic repair.
   - **Distributed Token Bucket Rate Limiter**: To prevent failure cascading or "retry storms" on external services (e.g. Cloudinary, Resend) during network recovery, retry dispatches are throttled using a Redis-backed Distributed Rate Limiter. The Token Bucket algorithm is executed atomically via a Redis Lua script to eliminate concurrency race conditions. Exceeded retries are rescheduled 30 seconds later, safely yielding database lock leases.
   - **Event Ingestion (Metadata Abstraction)**: To prevent Open-Closed Principle violations in the outbox event saving path (`saveEvent`), the system decouples metadata extraction and entity mapping using the **Strategy Pattern** via `EventMetadataProvider` and wraps events in a generic `EventEnvelope`. This avoids hardcoded `instanceof` blocks, allowing new event types to be introduced dynamically by registering provider beans.
   - **Throttled Transactional Commit Wake-Up**: To support extreme write throughput without thread pool exhaustion or CPU spikes, the transactional `afterCommit` hook utilizes a lock-free signaling flag (`AtomicBoolean wakeUpPending`). Wake-up notifications are throttled so that at most one asynchronous outbox processing worker (`processOutboxAsync` with `@Async`) is active at a time. The worker executes a batch drain loop to consume all pending outbox records cleanly, minimizing JVM context switches and eliminating redundant logger noise.
   - **At-Least-Once Delivery & Idempotency Tradeoff**: The Outbox pattern guarantees **At-Least-Once** delivery of events. If Kafka successfully publishes the message but database updates to mark the outbox status as `PROCESSED` fail, the event is re-delivered. Since all consumers implement idempotency checks via `processed_events` deduplication store, duplicate messages are safely skipped, maintaining strict system consistency without exactly-once overhead.

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

- **Client IP Extraction (Trusted & Secure)**: The backend utilizes Spring Boot's native forward headers strategy (`server.forward-headers-strategy=native`) to securely process forwarded headers (such as `X-Forwarded-For` and `X-Forwarded-Proto`) sent by trusted reverse proxies. The client IP is extracted directly using `HttpServletRequest.getRemoteAddr()`, which is automatically populated by the servlet container with the validated client IP, preventing IP spoofing attacks.
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

## 19. Reviews & Ratings Architecture

To provide a robust, production-ready reviews and ratings system, OmniBooking implements a comprehensive reviews architecture with strict transactional guarantees, concurrency safety, and anti-abuse safeguards.

### 19.1. Concurrency-Safe Rating Aggregation & Lock Contention

- **Rationale**: Simultaneous review submissions for the same property can lead to race conditions (dirty reads/writes) if aggregations are computed in memory.
- **Data Model Changes**:
   - Add a `rating_sum` column (**BIGINT/Long** to prevent integer overflow for high-volume hotels) and `review_count` column (INTEGER) to the `properties` table.
   - Add `average_rating` (**NUMERIC(4,2)** to allow precise fractional scoring and future-proofing) column to the `properties` table.
- **Transactional Guarantees & Lock Timeout**:
   - The system utilizes **Pessimistic Write Locking** (`SELECT ... FOR UPDATE`) at the database layer. When a review is created, modified, or deleted, the parent `Property` entity is locked immediately.
   - **Lock Timeout Strategy**: To prevent system-wide gridlocks, the query uses a strict **3-second lock timeout** (`javax.persistence.lock.timeout = 3000`). If lock acquisition fails, the transaction is rolled back, the counter `review_property_lock_contention_total` is incremented, and the request fails with a `409 Conflict` (`REVIEW_LOCK_TIMEOUT`).
   - Rating recalculation is performed inside this write lock transaction:
     `rating_sum = rating_sum + new_rating`
     `review_count = review_count + 1`
     `average_rating = rating_sum / review_count`
   - **Trade-offs**: Pessimistic write locking blocks concurrent writes for the same property, preventing dirty writes but introducing latency spikes (hotspots) for ultra-popular hotels receiving dozens of reviews simultaneously.
   - **Future Scaling Strategy (Roadmap)**:
      1. **Atomic SQL Updates**: Transition to lock-free statements (e.g., `UPDATE properties SET rating_sum = rating_sum + :rating, review_count = review_count + 1 WHERE id = :id`), which lock only during the execution statement rather than the whole transaction.
      2. **Event-Driven Batch Aggregation**: Offload review aggregation to an asynchronous worker queue that aggregates review ratings in memory (or using Redis atomic operations) and writes to PostgreSQL in batches every 5 minutes.
- **Hot Property Observability & Troubleshooting**:
   - Under heavy traffic, lock contention hotspots can develop on popular hotels (e.g. during promotional campaigns).
   - High contention will trigger Prometheus alerts when the rate of `review_property_lock_contention_total` exceeds 5/minute.
   - **Operational Troubleshooting Procedures**:
     If lock starvation is reported, operators query active database backend locks:
      ```sql
      SELECT pid, query, state, age(clock_timestamp(), query_start)
      FROM pg_stat_activity
      WHERE query LIKE '%FOR UPDATE%' AND state = 'active'
      ORDER BY age DESC;
      ```
      If stuck backends or connection leaks are identified, operators terminate the blocking PID using `SELECT pg_terminate_backend(pid);`.

### 19.2. Soft Delete & Auditability

- **Rationale**: Deleting a review permanently destroys valuable platform audit trails and historical records.
- **Data Model Changes**:
   - `reviews` table includes audit columns: `deleted_at` (TIMESTAMP), `deleted_by` (UUID), and `deletion_reason` (VARCHAR).
- **Operational Behavior**:
   - The default deletion behavior is converted to a **Soft Delete**.
   - Soft-deleted reviews are automatically filtered out from all public queries and excluded from rating calculations (average rating and review count are recalculated and decremented upon soft deletion).
   - Deletion audits record who performed the action (User, Partner, or Admin) and the justification.

### 19.3. Review Moderation & Immutable Audit Trail

- **Rationale**: Prevent spam and offensive reviews from displaying publicly while maintaining an immutable history of moderation actions for auditing.
- **Data Model Changes**:
   - Add a `status` column (VARCHAR) mapped to the `ReviewStatus` enum: `PENDING`, `PUBLISHED`, `HIDDEN`, `REMOVED`.
   - Add audit columns: `moderated_by` (UUID), `moderated_at` (TIMESTAMP), and `moderation_reason` (VARCHAR) to the `reviews` table.
- **Moderation Query Performance**:
   - Moderation-heavy workflows scan for reviews in states other than `PUBLISHED`. To prevent full-table scans, the system implements a composite index:
     `CREATE INDEX idx_reviews_moderation ON reviews(status, created_at) WHERE deleted_at IS NULL;`
   - This optimizes loading the administrator moderation queues.
- **Roadmap for Immutable Moderation History**:
   - Initially, moderation actions overwrite the audit columns directly on the `Review` entity.
   - **Roadmap**: As moderation frequency grows, we will introduce a `review_moderation_history` table to log every moderation state transition sequentially, preserving previous statuses and reasons:
      - Columns: `id` (UUID PRIMARY KEY), `review_id` (UUID), `old_status` (VARCHAR), `new_status` (VARCHAR), `action_by` (UUID), `reason` (VARCHAR), `action_at` (TIMESTAMP).

### 19.4. Stronger Business Validation & Content Policy

- **Rationale**: Enforce data integrity and trust by restricting reviews to actual customers, while maintaining a minimum quality threshold for comments.
- **Validation Rules**:
   - **Ownership**: The `booking.getUser().getId()` must match the authenticated `currentUserId`.
   - **Booking Status**: The booking must be in the `STAYED` state.
   - **Chronological Check**: Stays can only be reviewed after the checkout date (`booking.getCheckOutDate()`) has passed.
   - **Invalid Bookings**: Cancelled (`CANCELLED`), refunded (`REFUNDED`), or active bookings are blocked from writing reviews.
   - **Uniqueness**: A booking can have at most one associated review.
- **Content Policy & Comment Length**:
   - Submissions containing text must meet a **minimum length of 10 characters** (e.g. to filter out low-effort or automated words like "ok", "good").
   - Rating-only reviews (no text) are allowed if the comment is completely null or empty.
- **Database Defense-in-Depth**:
   - Enforce comment length limits directly at the database level by specifying the column type as `VARCHAR(1000)` (or adding a check constraint `CHECK (char_length(comment) <= 1000)`). This prevents oversized payloads from persisting even if application-layer validation fails.

### 19.5. Partner Reply Governance

- **Rationale**: Standardize communication between guests and partners while preserving reply history.
- **Data Model Changes**:
   - `reviews` table includes reply fields: `reply` (TEXT) and `reply_updated_at` (TIMESTAMP).
- **Operational Behavior**:
   - Property partners with the `review:reply` authority can reply to reviews for properties they own.
   - **Edit Behavior**: Replies are editable. When a partner overwrites their previous reply, the system updates `reply_updated_at` and registers an audit log.
   - **Roadmap for Reply History**: Introduce a `review_replies_history` table to store all versions of a partner's reply, preventing malicious partners from changing replies to gaslight guests.

### 19.6. Anti-Abuse Protection & Redis Failure Policy

- **Rationale**: Prevent malicious users or bots from spamming reviews or running brute-force rating campaigns.
- **Operational Behavior**:
   - **Rate Limiting**: Integrated Redis-based sliding window rate limiter (max 5 review creations per hour per user).
   - **Spam Filtering**: Basic blocklist filtering on comments prior to publishing (flagging matches to `PENDING` status).
- **Redis Failure Policy (Fail Open)**:
   - If the Redis cluster is down, the system defaults to **Fail Open**. Rate limit checks are bypassed, and review creation is allowed to proceed to avoid breaking user experience.
   - **Monitoring & Alerting**: A Prometheus counter `redis_rate_limit_failure_total` tracks rate-limiting lookup failures, triggering immediate operational alerts when failure rates exceed thresholds.

### 19.7. Elasticsearch Failure Handling & Eventual Consistency (PROPERTY_SYNC Idempotency)

- **Rationale**: Network partitions or Elasticsearch downtime must not lead to data loss, while duplicate sync messages must resolve safely.
- **Eventual Consistency SLA**:
   - The database remains the source of truth; Elasticsearch acts as a read-only index.
   - Target sync synchronization lag is **<= 2 seconds** under normal load.
   - **Monitoring**: Alerts trigger if outbox backlog (`omnibooking.outbox.pending.count`) > 500 for > 5 minutes, or if Kafka consumer lag exceeds 1000 messages.
- **PROPERTY_SYNC Idempotency**:
   - The `consumePropertySync` Kafka listener is designed to be **fully idempotent**. It queries the latest property statistics directly from PostgreSQL and overrides the Elasticsearch document. Duplicate message delivery cannot result in inconsistent states.
- **Elasticsearch Disaster Recovery & Reindex Hardening**:
   - An administrative background job `/admin/search/reindex` is introduced to perform a batch rebuild of Elasticsearch documents directly from PostgreSQL.
   - **Batching & Rate Limiting**: The job processes properties in batches of 100 with a configurable sleep delay between batches to control throughput and prevent overloading Elasticsearch or Kafka.
   - **Checkpoint & Resume**: The job logs and caches the last successfully indexed property ID (cursor) in Redis or a DB metadata table, allowing the job to resume from the checkpoint if interrupted.

### 19.8. Outbox Retention & Maintenance

- **Rationale**: Avoid transactional outbox table bloat, which degrades database performance over time.
- **Retention Policy**:
   - `PROCESSED` (Success) events are retained for **30 days** for troubleshooting.
   - `DEAD` (Failed) events are retained for **90 days** to allow manual auditing and re-processing before pruning.
- **Operational Procedures**:
   - The `purgeOldOutboxEvents` scheduled job runs daily at 3:00 AM, executing bounded deletes to prune expired events without causing transaction log overflow.

### 19.9. Query, Pagination Standards & Cursor-based Roadmap

- **Operational Behavior**:
   - **Default Sort**: Reviews are fetched ordered by `created_at DESC`.
   - **Sort Options**: Future pagination queries will support sorting by `rating DESC` (highest first) and `rating ASC` (lowest first).
   - **Page Size Limits**: Page sizes are strictly capped between 10 (default) and 50 records to protect server memory.
- **Pagination Scalability Roadmap**:
   - Currently, offset-based pagination (`LIMIT/OFFSET`) is used.
   - **Roadmap**: For popular properties with massive review datasets, offset pagination scales poorly due to $O(N)$ row-scan overhead. The system defines a migration path to **Cursor-based pagination** using the review's time-ordered UUID v7 as a cursor (`WHERE (created_at, id) < (:cursor_created_at, :cursor_id)`), guaranteeing $O(1)$ query times at any depth.

### 19.10. Database Performance

- **Rationale**: High-traffic properties will query reviews frequently, requiring optimal index layouts.
- **Index Optimization**:
   - Index on `(property_id, created_at)`: Optimizes fetching paginated reviews for a property sorted by time.
   - Index on `(user_id)`: Speed up retrieving a user's review history.
   - Index on `(booking_id)`: Accelerate checking duplicate review constraints.

### 19.11. Aggregate Repair Job Hardening & Distributed Execution Safety

- **Rationale**: Data anomalies, schema updates, or manual DB changes can cause rating statistics to drift. Long-running calculations must not block web request threads.
- **Operational Job**:
   - An administrative task `/admin/properties/rebuild-ratings` is introduced to rebuild `review_count`, `rating_sum`, and `average_rating` for a specific property or all properties by running an aggregate scan over the `reviews` table.
   - **Asynchronous Execution**: The job is triggered asynchronously as a background task to prevent HTTP request timeout issues.
   - **Distributed Execution Safety**: To prevent lock thrashing and multiple instances from executing the rebuild concurrently, the job requires acquiring a **PostgreSQL Advisory Lock** (`pg_try_advisory_xact_lock(1911)`) or a Redis distributed lock (`lock:rebuild-ratings`) with a lease time of 1 hour.
   - **Verification Procedure**: The job prints pre-update and post-update statistics to logs and publishes a bulk `PROPERTY_SYNC` event to sync corrected ratings to Elasticsearch.
   - **Roadmap for Durable Jobs**: Replace Spring in-memory `@Async` processing with **Quartz Scheduler** or database-backed job runner tables to ensure that long-running operations can recover from server crashes/restarts.

### 19.12. Operational Metrics & Observability

- **Prometheus Custom Metrics**:
   - `review_property_lock_wait_seconds` (Summary/Timer): Tracks the latency of locking property rows.
   - `review_property_lock_contention_total` (Counter): Measures lock timeouts and acquisition failures.
   - `review_creation_total` (Counter): Counts total review submissions (labelled by status).
   - `review_moderation_total` (Counter): Counts moderation transitions (labelled by transition type).
   - `review_repair_job_duration_seconds` (Summary/Timer): Measures the duration of the rebuild ratings job.
- **Alerting Recommendations**:
   - **Lock Contention Alert**: Trigger P1 alert if `review_property_lock_contention_total` rate > 5 per minute, indicating potential database connection starvation or deadlocks.
   - **Rate Limit Failures Alert**: Trigger alert if `redis_rate_limit_failure_total` rate > 10 per minute, suggesting Redis cluster unavailability.

### 19.13. Expanded Test Coverage

- **Testing Requirements**:
   - **Concurrency Tests**: Simulated multi-threaded review submissions targeting a single property to verify that `review_count`, `rating_sum`, and `average_rating` update correctly without deadlocks.
   - **Soft Deletion & Restoration Tests**: Confirm rating metrics decrement/increment correctly upon soft deletion/restoration.
   - **Authorization Boundary Tests**: Verify that users cannot edit partner replies, and partners cannot delete reviews.
   - **Outbox Retry Verification**: Validate that simulated Kafka broker failures trigger correct backoff and DLQ routing.

### 19.14. Review Table Growth & Partitioning Roadmap

- **Rationale**: When tables exceed millions of records, read/write latencies degrade due to index tree depth.
- **Growth Thresholds**: Table partitioning will be triggered once the `reviews` table exceeds **10 million records**.
- **Partitioning Strategy**:
   - **HASH Partitioning by `property_id`**: Optimal for routing read and write queries, co-locating all reviews of a hotel in a single partition.
   - **RANGE Partitioning by `created_at` (Quarterly)**: Secondary option to align with monthly retention workflows.
   - **Migration strategy**: Execute online partitioning utilizing PgBouncer and `pg_partman` to partition active tables with zero application downtime.

### 19.15. Data Lifecycle & Archival Roadmap

- **Rationale**: Storing historical reviews in primary databases indefinitely increases backup times and costs.
- **Archival Policy**:
   - Reviews are retained active for **7 years** to support analytics.
   - Older reviews are exported to compressed cold storage (AWS S3 Glacier / CSV format) and purged from PostgreSQL.
- **GDPR / Compliance Anonymization**:
   - Under privacy regulations (Right to be Forgotten), when a user profile is deleted, their reviews are **anonymized** (setting `user_id` to a generic system-null user and clearing name/email from the guest metadata) instead of deleted, preserving the historical ratings of properties while ensuring full compliance.

---

_Last Updated: 2026-06-07_
