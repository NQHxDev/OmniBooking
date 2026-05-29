# OmniBooking Class Diagram

This document contains a simplified Class Diagram for the domain models (entities) of the **OmniBooking** project, built using **Mermaid**. Classes have been trimmed to show only 2-3 core attributes along with a `...` symbol to represent omitted fields, ensuring a clean and readable layout within Mermaid's rendering boundaries.

## 1. Mermaid Class Diagram

```mermaid
classDiagram
    direction TB

    %% Abstract Base Class
    class BaseEntity {
        <<abstract>>
        +UUID id
        ...
    }

    %% User Management Module
    class User {
        +String username
        +String email
        ...
    }

    class UserProfile {
        +String displayName
        +Integer points
        ...
    }

    class Role {
        +String name
        ...
    }

    class Permission {
        +String name
        ...
    }

    class Rank {
        +String name
        +Integer minPoints
        ...
    }

    %% Property Management Module
    class Property {
        +String name
        +PropertyType propertyType
        +String city
        ...
    }

    class RoomType {
        +String name
        +BigDecimal basePrice
        +Integer totalRooms
        ...
    }

    class Amenity {
        +String name
        +AmenityCategory category
        ...
    }

    class RoomAvailability {
        +LocalDate availabilityDate
        +Integer availableCount
        ...
    }

    %% Booking & Transaction Module
    class Booking {
        +LocalDate checkInDate
        +LocalDate checkOutDate
        +BigDecimal finalPrice
        ...
    }

    class BookingStatusLog {
        +BookingStatus oldStatus
        +BookingStatus newStatus
        ...
    }

    class Transaction {
        +BigDecimal amount
        +TransactionType transactionType
        +TransactionStatus status
        ...
    }

    class CancellationPolicy {
        +String name
        +BigDecimal penaltyPercentage
        ...
    }

    class Coupon {
        +String code
        +BigDecimal discountValue
        ...
    }

    %% Media Polymorphic Entity
    class Media {
        +String url
        +UUID entityId
        ...
    }

    %% Financial Core System
    class Currency {
        +String code
        +boolean isBase
        ...
    }

    class ExchangeRate {
        +String toCurrency
        +BigDecimal rate
        ...
    }

    %% Enums
    class PropertyType {
        <<enumeration>>
        HOTEL
        APARTMENT
        VILLA
        RESORT
    }

    class AmenityCategory {
        <<enumeration>>
        GENERAL
        ROOM
        BATHROOM
        KITCHEN
    }

    class BookingStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        STAYED
        CANCELLED
        REFUNDED
    }

    class TransactionType {
        <<enumeration>>
        PAYMENT
        REFUND
    }

    class TransactionStatus {
        <<enumeration>>
        PENDING
        SUCCESS
        FAILED
        VOIDED
    }

    class DiscountType {
        <<enumeration>>
        PERCENT
        FIXED_AMOUNT
    }

    %% Inheritance Relations
    User --|> BaseEntity
    Role --|> BaseEntity
    Permission --|> BaseEntity
    Rank --|> BaseEntity
    Property --|> BaseEntity
    RoomType --|> BaseEntity
    Amenity --|> BaseEntity
    RoomAvailability --|> BaseEntity
    Booking --|> BaseEntity
    BookingStatusLog --|> BaseEntity
    Transaction --|> BaseEntity
    CancellationPolicy --|> BaseEntity
    Coupon --|> BaseEntity
    Media --|> BaseEntity
    Currency --|> BaseEntity
    ExchangeRate --|> BaseEntity

    %% Entity Association & Aggregation
    User "1" *-- "0..1" UserProfile : "profile"
    User "*" o-- "*" Role : "roles"
    Role "*" o-- "*" Permission : "permissions"
    UserProfile "*" --> "0..1" Rank : "rank"

    Property "*" --> "1" User : "owner"
    Property "*" --> "0..1" CancellationPolicy : "cancellationPolicy"
    Property "*" o-- "*" Amenity : "amenities"
    Property "1" *-- "*" RoomType : "roomTypes"

    RoomType "*" o-- "*" Amenity : "amenities"
    RoomAvailability "*" --> "1" RoomType : "roomType"

    Booking "*" --> "1" User : "user"
    Booking "*" --> "1" RoomType : "roomType"
    Booking "*" --> "0..1" Coupon : "coupon"

    BookingStatusLog "*" --> "1" Booking : "booking"
    BookingStatusLog "*" --> "0..1" User : "changedBy"

    Transaction "*" --> "1" Booking : "booking"

    %% Enum Bindings
    Property --> PropertyType : "type"
    Amenity --> AmenityCategory : "category"
    Booking --> BookingStatus : "status"
    BookingStatusLog --> BookingStatus : "oldStatus/newStatus"
    Transaction --> TransactionType : "transactionType"
    Transaction --> TransactionStatus : "status"
    Coupon --> DiscountType : "discountType"
```

## 2. Modules & Relationships Breakdown

### 2.1. User Management Module

- **BaseEntity**: An abstract class defining shared fields such as the UUID v7 primary key (`id`), optimistic locking field (`version`), and auditing/soft-delete timestamps.
- **User & UserProfile**: A 1-to-1 relationship implemented via `@MapsId`. `UserProfile` holds extended profile data including user points (`points`) and rank (`rank`).
- **Role & Permission**: A many-to-many (`@ManyToMany`) relationship. Each user is assigned a set of roles, and each role defines a list of specific permissions.

### 2.2. Property Management Module

- **Property**: Represents an accommodation establishment (Hotel, Villa, Apartment, etc.) managed by an owner (`User` with a Partner or Admin role).
- **RoomType**: Represents a specific type of room offered by a property. It holds base pricing (`basePrice`), capacity, size, and bed configuration.
- **Amenity**: Standard convenience features that can be linked via many-to-many associations to both properties and room types.
- **RoomAvailability**: Manages daily inventory counts and handles seasonal or event-based price overrides.

### 2.3. Booking & Transaction Module

- **Booking**: A reservation record linking a customer (`User`), a selected room type (`RoomType`), check-in/check-out dates, and discount coupon applications (`Coupon`).
- **BookingStatusLog**: A status transition log (e.g., Pending -> Confirmed -> Stayed -> Cancelled -> Refunded) ensuring auditability for every booking status change.
- **Transaction**: Tracks payment and refund transactions associated with bookings, including a JSONB `metadata` column for payment gateway integration (Stripe, etc.).
- **CancellationPolicy**: Defines properties' cancellation policies, such as the free cancellation window and penalty percentages.

### 2.4. Auxiliary Systems

- **Media**: Polymorphic media storage hosting uploaded assets on Cloudinary, dynamically mapped via `entityId` and `entityType` to various entities (properties, room types, user profiles, etc.).
- **Currency & ExchangeRate**: Multi-currency and real-time exchange rate caching structures used for on-the-fly price conversion.
