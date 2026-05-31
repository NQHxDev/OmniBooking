# Kế Hoạch Cải Tiến Hệ Thống Outbox, Idempotency & Consistency (OmniBooking)

## I. Phân Tích & Kế Hoạch Cải Tiến Outbox Pattern (Issue #8)

*(Lưu ý: Các đầu việc của Issue #8 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 13:07 ngày 30/05/2026).*

---

## II. Phân Tích & Kế Hoạch Triển Khai Idempotency cho Kafka Consumers (Issue #9)

*(Lưu ý: Các đầu việc của Issue #9 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 13:16 ngày 30/05/2026).*

---

## III. Phân Tích & Kế Hoạch Triển Khai Xử Lý Thứ Tự Sự Kiện & Tính Nhất Quán (Issue #10)

*(Lưu ý: Các đầu việc của Issue #10 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 13:24 ngày 30/05/2026).*

---

## IV. Phân Tích & Kế Hoạch Khắc Phục Lỗi Race Condition bằng Mô Hình Claim-then-Process (Issue #11)

*(Lưu ý: Các đầu việc của Issue #11 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 13:50 ngày 30/05/2026).*

---

### 1. Đánh giá tính chính xác của ý kiến từ Sếp
Sếp của bạn **hoàn toàn chính xác (100% đúng)**. 
*   **Vấn đề:** Cơ chế hiện tại ở Consumers sử dụng mô hình "Check-then-Act" (`isProcessed()` -> `process()` -> `markProcessed()`). Đây không phải là một thao tác nguyên tử (non-atomic).
*   **Lỗi tranh chấp (Race Condition):** Nếu hai luồng hoặc hai pod xử lý cùng một tin nhắn Kafka trùng lặp gần như đồng thời (ví dụ khi partition rebalance hoặc song song nhiều luồng), cả hai đều kiểm tra `isProcessed()` và nhận kết quả `false`, dẫn tới việc cả hai đều chạy side-effects (ví dụ gửi trùng lặp OTP hoặc email) rồi mới ghi nhận trạng thái đã xử lý.
*   **Giải pháp:** Mô hình **Claim-then-Process** (Nhận quyền -> Xử lý). Consumer sẽ cố gắng chèn bản ghi vào bảng `processed_events` bằng một giao dịch mới (`REQUIRES_NEW`). Nếu chèn thành công (lấy quyền claim thành công), consumer sẽ xử lý. Nếu chèn lỗi (do xung đột khóa chính duy nhất), consumer biết đã có pod khác claim và bỏ qua một cách an sau. Nếu quá trình xử lý nghiệp vụ thất bại, consumer sẽ thực hiện xóa (release claim) để cho phép thử lại trong tương lai.

### 2. Kế hoạch triển khai kỹ thuật (Todo Checklist cho Issue #11)

#### Pha 1: Cập Nhật `IdempotencyService`
- [x] **Sửa đổi interface `IdempotencyService.java`**:
  - [x] Đổi `isProcessed` và `markProcessed` thành `claimEvent` và `releaseClaim`.
- [x] **Sửa đổi triển khai `IdempotencyServiceImpl.java`**:
  - [x] Implement `claimEvent(UUID eventId, String consumerGroup)` chạy dưới `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Thực hiện chèn `ProcessedEvent` vào database và bắt `DataIntegrityViolationException` để trả về `false` (nếu đã bị claim).
  - [x] Implement `releaseClaim(UUID eventId, String consumerGroup)` chạy dưới `@Transactional(propagation = Propagation.REQUIRES_NEW)` để xóa dòng sự kiện khi gặp lỗi nghiệp vụ.

#### Pha 2: Cập Nhật Tất Cả Consumers
- [x] **Cập nhật `EmailConsumer.java`**:
  - [x] Đổi logic kiểm tra sang `claimEvent`. Bọc logic gửi email trong khối `try-catch`. Nếu xảy ra ngoại lệ lỗi gửi email, thực hiện gọi `releaseClaim` và ném lại lỗi để Kafka thực hiện retry.
- [x] **Cập nhật `MediaConsumer.java`**:
  - [x] Đổi logic kiểm tra sang `claimEvent`. Nếu xử lý ảnh/lưu DB lỗi, thực hiện gọi `releaseClaim` và ném lại lỗi để Kafka thực hiện retry.
- [x] **Cập nhật `UserCDCConsumer.java`**:
  - [x] Đổi logic kiểm tra sang `claimEvent`. Nếu lỗi cdc, thực hiện gọi `releaseClaim` và ném lại lỗi để Kafka thực hiện retry.
- [x] **Cập nhật `PropertySyncConsumer.java`**:
  - [x] Đổi logic kiểm tra sang `claimEvent`. Nếu lỗi đồng bộ, thực hiện gọi `releaseClaim` và ném lại lỗi để Kafka thực hiện retry.

---

## V. Phân Tích & Kế Hoạch Triển Khai Máy Trạng Thái Idempotency & Phục Hồi (Issue #12)

*(Lưu ý: Các đầu việc của Issue #12 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 13:58 ngày 30/05/2026).*

### 1. Đánh giá tính chính xác của ý kiến từ Sếp
Sếp của bạn **hoàn toàn chính xác (100% đúng)**.
*   **Vấn đề:** Nếu một consumer bị crash (ví dụ: mất điện, restart node, OutOfMemory) khi đang xử lý một sự kiện sau khi đã gọi `claimEvent()`, sự kiện đó sẽ bị kẹt vĩnh viễn ở trạng thái "đã claim" mà không bao giờ hoàn tất hoặc được giải phóng.
*   **Giải pháp:** Chuyển sang máy trạng thái 3 trạng thái (`PROCESSING`, `COMPLETED`, `FAILED`).
    *   `PROCESSING`: Đang xử lý (được đánh dấu khi claim thành công).
    *   `COMPLETED`: Đã xử lý thành công (được gọi ở cuối khối `try` của consumer).
    *   `FAILED`: Xử lý lỗi (được gọi trong khối `catch` để cho phép retry).
    *   **Worker Phục Hồi (IdempotencyRecoveryWorker)**: Tự động chạy mỗi phút để quét các sự kiện bị kẹt ở `PROCESSING` lâu hơn 5 phút (do crash node) và chuyển chúng về `FAILED` để cho phép retry.
    *   **Worker Dọn Dẹp (Purge)**: Dọn dẹp các sự kiện cũ hơn 30 ngày để giữ kích thước bảng tối ưu.

### 2. Kế hoạch triển khai kỹ thuật (Todo Checklist cho Issue #12)

#### Pha 1: Tạo Database Migration & Entity/Repository
- [x] **Tạo file migration `V9__Add_Idempotency_State_Machine.sql`** để thêm cột `status` và `updated_at`, đồng thời cập nhật các bản ghi cũ sang `COMPLETED`.
- [x] **Cập nhật Entity `ProcessedEvent.java`** để tương thích với các cột mới.
- [x] **Cập nhật `ProcessedEventRepository.java`** thêm query `findByIdForWrite` khóa bi quan, và các phương thức tìm kiếm stale / dọn dẹp.

#### Pha 2: Cập Nhật Idempotency Service
- [x] **Khai báo `completeEvent`** trong `IdempotencyService`.
- [x] **Triển khai máy trạng thái** trong `IdempotencyServiceImpl` (`claimEvent` khóa bi quan, cập nhật trạng thái `COMPLETED` và `FAILED`).

#### Pha 3: Cập Nhật Các Kafka Consumers
- [x] **Cập nhật `EmailConsumer.java`** để gọi `completeEvent` khi thành công.
- [x] **Cập nhật `MediaConsumer.java`** để gọi `completeEvent` khi thành công.
- [x] **Cập nhật `UserCDCConsumer.java`** để gọi `completeEvent` khi thành công.
- [x] **Cập nhật `PropertySyncConsumer.java`** để gọi `completeEvent` khi thành công.

#### Pha 4: Triển Khai Background Worker & Cấu Hình Schedulers
- [x] **Cập nhật `ServerApplication.java`** thêm `@EnableScheduling`.
- [x] **Tạo mới `IdempotencyRecoveryWorker.java`** chạy scheduler phục hồi stale claims quá 5 phút và dọn dẹp log quá 30 ngày.

---

## VI. Phân Tích & Kế Hoạch Triển Khai Epic Concurrency, Reliability & Refactoring (Issue #14)

*(Lưu ý: Các đầu việc của Issue #14 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 14:23 ngày 30/05/2026).*

### 1. Đánh giá tính chính xác của ý kiến từ Sếp
Sếp của bạn **hoàn toàn chính xác (100% đúng)**.
*   **Vấn đề 1 (Race Condition Rate Limiter):** Thao tác đọc-tính toán-ghi tuần tự trên Redis có nguy cơ ghi đè dữ liệu cũ khi có nhiều Pods xử lý song song. Giải pháp: Chuyển sang thực thi script Lua nguyên tử.
*   **Vấn đề 2 (Recovery Timeout cho Heavy Jobs):** Một số tác vụ nặng (ví dụ upload Cloudinary) chạy quá 5 phút sẽ bị recovery worker coi là bị treo và đánh dấu `FAILED` trước khi chạy xong, dẫn tới việc xử lý trùng lặp. Giải pháp: Sử dụng cơ chế gia hạn Lease (Heartbeat) tự động thông qua `LeaseRenewer` (AutoCloseable).
*   **Vấn đề 3 (Vi phạm Open-Closed ở Upcaster):** Sử dụng các nhánh if-else cứng để route version upcasting của event làm phình to code. Giải pháp: Áp dụng Strategy Pattern và tự động đăng ký Bean thông qua Dependency Injection.

### 2. Kế hoạch triển khai kỹ thuật (Todo Checklist cho Issue #14)

#### Pha 1: Module 1 - Rate Limiter Nguyên Tử (Atomic Rate Limiter)
- [x] **Cập nhật `DistributedRateLimiter.java`** để thực thi thuật toán Token Bucket bằng Lua Script trên Redis.

#### Pha 2: Module 2 - Thiết lập cơ chế gia hạn khoá xử lý (Event Lease & Heartbeat)
- [x] **Tạo file migration `V10__Add_Processed_Events_Lease.sql`** để thêm cột `lease_until`.
- [x] **Cập nhật Entity `ProcessedEvent.java`** để ánh xạ cột `leaseUntil`.
- [x] **Khai báo và triển khai `renewLease`** trong `IdempotencyService` & `IdempotencyServiceImpl`.
- [x] **Tạo mới lớp trợ giúp `LeaseRenewer.java`** tự động gia hạn định kỳ kiểu `AutoCloseable`.
- [x] **Cập nhật 4 Consumers** (`EmailConsumer`, `MediaConsumer`, `UserCDCConsumer`, `PropertySyncConsumer`) sử dụng `LeaseRenewer` qua try-with-resources.
- [x] **Điều chỉnh `IdempotencyRecoveryWorker.java`** tìm kiếm stale claims theo `lease_until < now`.

#### Pha 3: Module 3 - Tái cấu trúc Event Upcaster (Strategy Pattern)
- [x] **Tạo mới interface `EventUpcasterStrategy.java`**.
- [x] **Tạo mới lớp chiến lược cụ thể `UserRegisteredMailV1ToV2Strategy.java`**.
- [x] **Refactor `EventUpcaster.java`** tự động tiêm danh sách chiến lược (Strategy Collection).

---

## VII. Phân Tích & Kế Hoạch Triển Khai Outbox Pattern Enhancements (Issue #15)

*(Lưu ý: Các đầu việc của Issue #15 đã được hoàn thành và đẩy lên nhánh `dev` thành công vào lúc 14:45 ngày 30/05/2026).*

### 1. Đánh giá tính chính xác của ý kiến từ Sếp
Sếp của bạn **hoàn toàn chính xác (100% đúng)**.
*   **Vấn đề 1 (Metadata Abstraction):** Logic lưu sự kiện (`saveEvent`) chứa các khối `instanceof` rẽ nhánh cứng cho từng loại DTO event. Khi hệ thống mở rộng lên hàng chục hoặc hàng trăm loại sự kiện, file này sẽ bị phình to và vi phạm nguyên tắc Open-Closed (OCP). Giải pháp: Triển khai Strategy Pattern bằng cách định nghĩa interface `EventMetadataProvider` và bọc dữ liệu trong `EventEnvelope` giúp tự động hoá việc trích xuất và gán ID.
*   **Vấn đề 2 (afterCommit Wake-up Storms):** Sự kiện xử lý Outbox được kích hoạt đồng bộ trên mọi luồng commit giao dịch (`afterCommit`). Dưới tải cao (2000 tx/sec), điều này tạo ra 2000 tín hiệu đánh thức luồng xử lý Outbox đồng thời, dẫn tới log noise cực lớn và lãng phí tài nguyên CPU của JVM. Giải pháp: Sử dụng cờ tín hiệu không khóa `wakeUpPending` kết hợp với xử lý bất đồng bộ (`@Async`) và vòng lặp drain queue (drain loop) để gom luồng xử lý và triệt tiêu log noise.

### 2. Kế hoạch triển khai kỹ thuật (Todo Checklist cho Issue #15)

#### Pha 1: Khai Báo & Định Nghĩa Metadata Provider
- [x] **Tạo mới interface `EventMetadataProvider.java`** định nghĩa supports, setEventId và extractMetadata.
- [x] **Tạo mới lớp bọc sự kiện `EventEnvelope.java`** để tự động liên kết metadata.
- [x] **Tạo mới `EmailEventMetadataProvider.java`** xử lý cho email events.
- [x] **Tạo mới `MediaUploadEventMetadataProvider.java`** xử lý cho media events.
- [x] **Tạo mới `PropertySyncEventMetadataProvider.java`** xử lý cho property sync events.

#### Pha 2: Cập Nhật & Tái Cấu Trúc Outbox Service
- [x] **Khai báo `processOutboxAsync`** trong `OutboxService.java`.
- [x] **Cập nhật `OutboxServiceImpl.java`**:
  - [x] Tiêm danh sách `EventMetadataProvider` thông qua dependency injection.
  - [x] Tái cấu trúc `saveEvent` để sử dụng `EventEnvelope` giải quyết metadata động.
  - [x] Triển khai `processOutboxAsync()` có đánh dấu `@Async`.
  - [x] Tối ưu hóa `processOutbox()` để drain sạch hàng đợi trong vòng lặp batching (drain loop) và tự động đặt lại cờ `wakeUpPending = false` trước khi fetch batch tiếp theo.
  - [x] Sửa đổi `afterCommit` chỉ đánh thức bất đồng bộ khi `wakeUpPending.compareAndSet(false, true)` thành công.

---

## VIII. Phân Tích & Kế Hoạch Triển Khai Kích Hoạt Bảo Vệ CSRF (Issue #17)

### 1. Đánh giá tính chính xác của ý kiến từ Sếp
Sếp của bạn **hoàn toàn chính xác (100% đúng)**.
*   **Vấn đề:** Hệ thống lưu trữ Access & Refresh tokens trong HttpOnly Cookies để tăng tính bảo mật nhưng lại vô hiệu hóa bảo vệ CSRF (`.csrf(AbstractHttpConfigurer::disable)`). Điều này dẫn đến lỗ hổng bảo mật nghiêm trọng (CSRF) cho phép kẻ tấn công thực hiện các thao tác thay đổi trạng thái trái phép thay người dùng (như gửi OTP, đổi mật khẩu, hoặc đặt phòng/huỷ phòng).
*   **Giải pháp:** Kích hoạt `CustomCsrfFilter` trong Security Filter Chain để kiểm tra trùng khớp giữa Cookie `csrf_token` và HTTP Header `X-CSRF-Token` cho toàn bộ các request thay đổi trạng thái (POST, PUT, DELETE, PATCH) ngoại trừ các endpoint công khai (được bypass qua annotation `@Anonymous`).

---

### 2. Kế hoạch triển khai kỹ thuật (Todo Checklist cho Issue #17)

#### Pha 1: Cấu hình Spring Security
- [x] **Đăng ký `CustomCsrfFilter`**:
  - [x] Inject `ObjectMapper` và `RequestMappingHandlerMapping` vào `SecurityConfig`.
  - [x] Đăng ký `CustomCsrfFilter` chạy ngay sau `JwtAuthenticationFilter` trong Security Filter Chain.

#### Pha 2: Định nghĩa các Endpoint Bypassed & Public
- [x] **Xác định các endpoint công khai**: Các endpoint công khai thay đổi trạng thái (như `/auth/login`, `/auth/register`) đã được gắn annotation `@Anonymous` và sẽ được `CustomCsrfFilter` tự động bỏ qua.

#### Pha 3: Viết Integration Tests
- [x] **Viết bộ kiểm thử tích hợp `CsrfIntegrationTest.java`**:
  - [x] Test case: Cho phép các request công khai (như `/auth/login`) bỏ qua kiểm tra CSRF.
  - [x] Test case: Từ chối các request thay đổi trạng thái được bảo vệ khi thiếu CSRF token (trả về lỗi `SEC_001` - 403 Forbidden).
  - [x] Test case: Từ chối các request thay đổi trạng thái được bảo vệ khi CSRF token không khớp hoặc sai.
  - [x] Test case: Xử lý bình thường khi CSRF token hợp lệ (vượt qua bộ lọc CSRF).

#### Pha 4: Cập nhật tài liệu kỹ thuật
- [x] **Cập nhật `ARCHITECTURE.md`**: Cập nhật phần Security Architecture xác nhận CSRF đã được kích hoạt thông qua `CustomCsrfFilter`.



