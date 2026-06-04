# OmniBooking System Keys & Event Constants Documentation

This document lists all Redis cache regions, custom Redis keys, Kafka topics, and Outbox event constants used across the OmniBooking system to assist in future maintenance, scaling, and upgrading.

---

## 1. Redis Cache & Data Mappings

The application uses Redis both as a standard cache (via Spring Cache `@Cacheable`) and as a key-value store/distributed synchronization mechanism (via `StringRedisTemplate`).

### 1.1 Spring Cache Regions

These cache regions are managed by the Spring `RedisCacheManager` configured in `RedisConfig`.

| Cache Region / Value    | Key Format                               | Default TTL | Description / Cache Eviction                                                               |
| :---------------------- | :--------------------------------------- | :---------- | :----------------------------------------------------------------------------------------- |
| `featured_properties`   | `'featured:' + limit`                    | 6 Hours     | Featured properties list on homepage. Evicted on new property creation.                    |
| `trending_destinations` | Dynamic                                  | 24 Hours    | Trending destinations cache region.                                                        |
| `partner_bookings`      | `#partnerId`                             | 1 Hour      | List of bookings for a partner dashboard. Evicted when a new booking is created/confirmed. |
| `partner_properties`    | `#ownerId`                               | 1 Hour      | List of properties managed by a partner. Evicted on property creation/modification.        |
| `properties`            | `'featured:' + limit` / `'new:' + limit` | 1 Hour      | Public property lists (featured/new). Evicted on property creation/modification.           |

### 1.2 Custom Redis Key-Value & Lock Mappings

These keys are managed directly via `StringRedisTemplate`.

| Redis Key Pattern                    | Value Type                 | TTL        | Description                                                                                           |
| :----------------------------------- | :------------------------- | :--------- | :---------------------------------------------------------------------------------------------------- |
| `role:<name>`                        | `Role` (JSON)              | 24 Hours   | Cached Role entity (e.g., `role:ROLE_USER`) to avoid querying the DB. Managed by `CachedRoleService`. |
| `refresh:<sessionId>`                | `RedisSessionInfo` (JSON)  | Sliding    | Session data mapped to the session UUID, rotated on token refresh.                                    |
| `user_sessions:<userId>`             | `ZSet` (Session ID, Expry) | 30 Days    | Sorted set containing all active session IDs for a specific user.                                     |
| `lock:refresh:<sessionId>`           | `String` (UUID)            | 5 Seconds  | Concurrency lock during refresh token rotation to prevent race conditions.                            |
| `rate_limit:forgot_password:<email>` | `String` (Integer)         | 1 Minute   | Rate limit bucket for forgot password requests (max 3 req/min).                                       |
| `reset_token:<token>`                | `String` (Email)           | 15 Minutes | Verification token used during password reset flow.                                                   |
| `auth:resend-limit:<userId>`         | `String` ("true")          | 30 Seconds | Rate limit buffer for resending email verification code.                                              |
| `property:images:<propertyId>`       | `List<String>` (JSON)      | 7 Days     | Cache for all image URLs belonging to a property.                                                     |
| `lock:property:images:<propertyId>`  | `String` ("locked")        | 10 Seconds | Concurrency lock for loading property images cache from DB.                                           |
| `rate_limit:tokens:<key>`            | `String` (Double)          | 1 Hour     | Current tokens count for Token Bucket rate limiting.                                                  |
| `rate_limit:last_refill:<key>`       | `String` (Long)            | 1 Hour     | Timestamp of the last refill action for Token Bucket.                                                 |
| `bf:user_emails`                     | `Bloom Filter` (Rebloom)   | Persistent | Bloom Filter containing all registered user emails to optimize signup checks.                         |
| `security_otp:<userId>`              | `String` (OTP)             | 5 Minutes  | Sensitive action security OTP code.                                                                   |
| `trusted_session:<userId>`           | `String` ("true")          | 30 Minutes | Session trust verification flag after verifying security OTP.                                         |
| `challenge:<userId>`                 | `String` (Base64)          | 5 Minutes  | Active WebAuthn (Passkey) login challenge.                                                            |
| `oauth2_state:<state>`               | `String` ("valid")         | 15 Minutes | OAuth2 state parameter validation token.                                                              |

---

## 2. Event Messaging & Constants

OmniBooking uses Kafka as its core event broker, backed by the Transactional Outbox pattern to guarantee event delivery.

### 2.1 Kafka Topics

| Kafka Topic Name            | Producer Component    | Consumer Component     | Partition Key                     | Description                                                                               |
| :-------------------------- | :-------------------- | :--------------------- | :-------------------------------- | :---------------------------------------------------------------------------------------- |
| `omnibooking-mail-topic`    | `OutboxWorker`        | `EmailConsumer`        | `aggregateId` (User/Booking UUID) | Outbox-published communication events (verifications, OTPs, booking confirmations).       |
| `omnibooking-media-topic`   | `MediaProducer`       | `MediaConsumer`        | `correlationId` / `aggregateId`   | Asset upload events to Cloudinary.                                                        |
| `omnibooking-property-sync` | `OutboxWorker`        | `PropertySyncConsumer` | `propertyId`                      | Elasticsearch index update events for properties.                                         |
| `omnibooking-user-cdc`      | `RegistrationService` | `UserCDCConsumer`      | `userId`                          | User registration CDC events triggered asynchronously for post-registration side-effects. |

### 2.2 Outbox Event Types & Registrations

These event types are defined in [EventConstants.java](file:///Users/anhjkr/Documents/My%20Projects/OmniBooking/Server/src/main/java/com/omnibooking/constant/EventConstants.java), registered in [OutboxEventRegistry.java](file:///Users/anhjkr/Documents/My%20Projects/OmniBooking/Server/src/main/java/com/omnibooking/services/core/OutboxEventRegistry.java), and map to specific Java payload DTOs.

| Event Constant (EventConstants.java) | Database String Key             | Payload DTO Class         | Target Kafka Topic          | Trigger Description                                                             |
| :----------------------------------- | :------------------------------ | :------------------------ | :-------------------------- | :------------------------------------------------------------------------------ |
| `USER_REGISTERED_MAIL`               | `USER_REGISTERED_MAIL`          | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent via CDC listener to verify a newly registered email address (Upcasted V2). |
| `USER_REGISTERED`                    | `USER_REGISTERED`               | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent via auth registration flow to verify email address.                        |
| `USER_RESEND_VERIFICATION_MAIL`      | `USER_RESEND_VERIFICATION_MAIL` | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent when requesting a new verification email (alternative).                    |
| `RESEND_VERIFICATION`                | `RESEND_VERIFICATION`           | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent when requesting a new verification email.                                  |
| `USER_FORGOT_PASSWORD_MAIL`          | `USER_FORGOT_PASSWORD_MAIL`     | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent when forgot password reset link is requested (alternative).                |
| `FORGOT_PASSWORD`                    | `FORGOT_PASSWORD`               | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent when forgot password reset link is requested.                              |
| `SECURITY_OTP_SEND`                  | `SECURITY_OTP_SEND`             | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent when sensitive account configurations require security OTP.                |
| `TWO_FACTOR_OTP_SEND`                | `2FA_OTP_SEND`                  | `EmailEvent.class`        | `omnibooking-mail-topic`    | Reserved for Two-Factor Authentication codes during login.                      |
| `PARTNER_OTP_SEND`                   | `PARTNER_OTP_SEND`              | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent for partner activation verification OTP.                                   |
| `TWO_FACTOR_ENABLED`                 | `TWO_FACTOR_ENABLED`            | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent to users when 2FA is successfully enabled.                                 |
| `PROPERTY_SYNC`                      | `PROPERTY_SYNC`                 | `PropertySyncEvent.class` | `omnibooking-property-sync` | Sent to update property search indexes in Elasticsearch.                        |
| `BOOKING_CONFIRMED_MAIL`             | `BOOKING_CONFIRMED_MAIL`        | `EmailEvent.class`        | `omnibooking-mail-topic`    | Sent to guests upon successful booking confirmation.                            |
