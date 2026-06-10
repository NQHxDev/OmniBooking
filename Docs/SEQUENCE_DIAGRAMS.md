# Các Sơ Đồ Tuần Tự Hệ Thống (System Sequence Diagrams)

Tài liệu này chứa các sơ đồ tuần tự biểu diễn bằng Mermaid để trực quan hóa luồng đi của dữ liệu, phân tách trách nhiệm giữa các component và các ràng buộc nghiệp vụ trong luồng đặt phòng.

---

## 1. Sơ đồ luồng Tạo Đặt phòng (Booking Creation Flow)

Mô tả luồng từ khi Client gửi yêu cầu tạo đặt phòng cho đến khi lưu outbox sự kiện gửi email xác nhận.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client/Guest
    participant Ctrl as BookingController
    participant Service as BookingServiceImpl
    participant PriceSvc as PriceCalculationServiceImpl
    participant InvSvc as InventoryServiceImpl
    participant CouponSvc as CouponReservationServiceImpl
    participant DB as PostgreSQL Database

    Client->>Ctrl: POST /api/v1/bookings (payload + Idempotency-Key)
    Note over Ctrl: IdempotencyAspect kiểm tra Redis key
    Ctrl->>Service: createBooking(request, principal)

    Service->>PriceSvc: calculateStayPriceWithCoupon(...)
    PriceSvc->>DB: Query RoomAvailability theo dải ngày (Batch)
    DB-->>PriceSvc: List<RoomAvailability> (Price Override nếu có)
    Note over PriceSvc: Tính toán tổng giá và giá cuối cùng
    PriceSvc-->>Service: StayPriceResult

    Note over Service: Trừ coupon nếu có token/ID
    Service->>CouponSvc: consumeReservation(token)
    CouponSvc->>DB: Cập nhật trạng thái CouponReservation sang CONSUMED

    Service->>DB: Lưu Booking mới (status = PENDING_PAYMENT hoặc CONFIRMED)
    DB-->>Service: Booking Entity

    Service->>InvSvc: reserveInventory(booking, roomType, dates)
    loop Mỗi ngày trong thời gian lưu trú
        Note over InvSvc: Gọi deductAvailabilityAtomically() hoặc Lock
        InvSvc->>DB: Trừ available_count của ngày tương ứng
        InvSvc->>DB: Lưu InventoryOperation (RESERVE)
    end

    Service->>DB: Lưu các thực thể Price Breakdown & Rule Versions (Batch)
    Service->>DB: Ghi OutboxEvent (EMAIL_CONFIRMATION) & BookingStatusLog

    Service-->>Ctrl: BookingResponse
    Ctrl-->>Client: HTTP 200 OK (BookingResponse)
```

---

## 2. Sơ đồ luồng Xác nhận Thanh toán (Payment Confirmation Webhook Flow)

Mô tả cách thức hệ thống xử lý callback webhook từ Momo/Visa, ngăn chặn callback trùng lặp và bảo vệ tính nhất quán giao dịch.

```mermaid
sequenceDiagram
    autonumber
    actor Gateway as Payment Gateway (MoMo/Visa)
    participant Ctrl as PaymentController
    participant Service as BookingServiceImpl
    participant DB as PostgreSQL Database
    participant Outbox as OutboxWorker

    Gateway->>Ctrl: POST /api/v1/payments/callback (payment payload)
    Ctrl->>Service: confirmBooking(bookingId, method, providerTxId, metadata)

    Note over Service: STEP 1: Cập nhật trạng thái nguyên tử
    Service->>DB: atomicConfirmBooking(bookingId, PENDING_PAYMENT -> CONFIRMED)
    DB-->>Service: rowsUpdated

    alt rowsUpdated == 0 (Callback trùng lặp hoặc đã xử lý trước đó)
        Note over Service: Bỏ qua toàn bộ tiến trình phía sau để bảo vệ nhất quán
        Service-->>Ctrl: Return (Skip)
        Ctrl-->>Gateway: HTTP 200 OK (Đã ghi nhận)
    else rowsUpdated == 1 (Yêu cầu callback hợp lệ đầu tiên)
        Service->>DB: Lưu Transaction mới (UNIQUE providerTransactionId)
        alt Trùng lặp UNIQUE providerTransactionId (Hành vi ghi đè lỗi từ DB)
            DB-->>Service: DataIntegrityViolationException
            Note over Service: Catch exception, log warning và dừng lại an toàn
            Service-->>Ctrl: Return (Skip)
            Ctrl-->>Gateway: HTTP 200 OK
        else Ghi Transaction thành công
            Service->>DB: Ghi OutboxEvent (BOOKING_CONFIRMED_MAIL) & BookingStatusLog
            Service-->>Ctrl: Success
            Ctrl-->>Gateway: HTTP 200 OK (Success)

            Note over Outbox: Tác vụ bất đồng bộ quét bảng outbox
            Outbox->>DB: Quét OutboxEvent
            Outbox->>Gateway: Gửi Email thành công, đánh dấu PROCESSED
        end
    end
```

---

## 3. Sơ đồ luồng Hết hạn đặt phòng (Reservation Expiration Flow)

Mô tả luồng quét tự động của `BookingExpirationWorker` để hủy đặt phòng chưa thanh toán và giải phóng tài nguyên.

```mermaid
sequenceDiagram
    autonumber
    participant Worker as BookingExpirationWorker
    participant DB as PostgreSQL Database
    participant InvSvc as InventoryServiceImpl
    participant CouponSvc as CouponReservationServiceImpl

    Note over Worker: Chạy định kỳ mỗi phút (ShedLock guard)
    Worker->>DB: Quét Page các Booking hết hạn (PENDING_PAYMENT & expiresAt < Now)
    DB-->>Worker: List<Booking>

    loop Cho từng Booking hết hạn trong lô
        Note over Worker: Chạy trong transaction REQUIRES_NEW riêng biệt
        Worker->>DB: atomicExpireBooking(bookingId, PENDING_PAYMENT -> EXPIRED)
        DB-->>Worker: rowsUpdated

        alt rowsUpdated == 1 (Thực hiện thu hồi tài nguyên)
            Worker->>InvSvc: releaseInventory(booking)
            loop Cho mỗi ngày trong khoảng check-in/out
                InvSvc->>DB: UPDATE room_availability (available_count = available_count + rooms)
                InvSvc->>DB: Lưu InventoryOperation (RELEASE)
            end

            Note over Worker: Primary Recovery: Retry giải phóng coupon
            loop Thử lại tối đa 3 lần (REQUIRES_NEW + Backoff)
                Worker->>CouponSvc: tryReleaseCoupon(couponId, userId)
                CouponSvc->>DB: Cập nhật CouponReservation thành EXPIRED & hoàn count
            end

            Worker->>DB: Ghi BookingStatusLog (EXPIRED)
        end
    end
```

---

## 4. Sơ đồ luồng Hủy Đặt phòng (Booking Cancellation Flow)

Mô tả luồng khách hàng chủ động hủy đặt phòng đang hoạt động và quy trình giải phóng tự động.

```mermaid
sequenceDiagram
    autonumber
    actor User as User/Partner/Admin
    participant Ctrl as BookingController
    participant Service as BookingServiceImpl
    participant SM as BookingStateMachine
    participant InvSvc as InventoryServiceImpl
    participant CouponSvc as CouponReservationServiceImpl
    participant DB as PostgreSQL Database

    User->>Ctrl: POST /api/v1/bookings/{id}/cancel (cancellation reason)
    Ctrl->>Service: cancelBooking(bookingId, reason, changedBy)

    Service->>SM: transition(booking, CANCELLED)
    Note over SM: Kiểm tra quy tắc chuyển trạng thái hợp lệ
    SM->>DB: Ghi BookingStatusLog (CANCELLED)

    Service->>DB: Lưu Booking cập nhật trạng thái CANCELLED

    Service->>InvSvc: releaseInventory(booking)
    loop Mỗi ngày đặt phòng
        InvSvc->>DB: Cộng lại available_count trong room_availability
        InvSvc->>DB: Lưu InventoryOperation (RELEASE)
    end

    Service->>CouponSvc: refundReservation(couponId, customerId)
    CouponSvc->>DB: Cập nhật CouponReservation thành EXPIRED và hoàn trả lượt dùng

    Service-->>Ctrl: Success Response
    Ctrl-->>User: HTTP 200 OK
```

---

## 5. Sơ đồ luồng Giữ phòng và Giải phóng Kho phòng (Inventory Reservation and Release Flow)

Trực quan hóa chi tiết các thao tác ghi nhật ký ledger và cập nhật số lượng phòng khả dụng.

```mermaid
sequenceDiagram
    autonumber
    participant App as BookingService
    participant InvSvc as InventoryServiceImpl
    participant Repo as RoomAvailabilityRepository
    participant DB as Database (room_availabilities)

    alt Luồng RESERVATION (Giữ phòng khi tạo booking)
        App->>InvSvc: reserveInventory(booking, roomType, checkIn, checkOut, rooms)
        loop Cho mỗi ngày từ check-in đến check-out
            InvSvc->>Repo: findByRoomTypeIdAndAvailabilityDateWithLock(...)
            Repo->>DB: SELECT ... FOR UPDATE (Khóa dòng dữ liệu)
            DB-->>Repo: RoomAvailability Entity
            Note over InvSvc: Kiểm tra: isClosed == false và availableCount >= rooms
            InvSvc->>Repo: save(RoomAvailability)
            Repo->>DB: UPDATE available_count = available_count - rooms
            InvSvc->>DB: INSERT INTO inventory_operations (RESERVE)
        end
    end

    alt Luồng RELEASE (Giải phóng phòng khi hủy/hết hạn)
        App->>InvSvc: releaseInventory(booking)
        loop Cho mỗi ngày từ check-in đến check-out
            InvSvc->>Repo: incrementAvailability(roomTypeId, date, rooms)
            Repo->>DB: UPDATE available_count = available_count + rooms
            InvSvc->>DB: INSERT INTO inventory_operations (RELEASE)
        end
    end
```
