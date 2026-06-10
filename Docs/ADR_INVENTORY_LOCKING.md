# Quyết Định Kiến Trúc (ADR): Chiến lược Khóa Kho Phòng trống (Inventory Locking Strategy)

## 1. Trạng Thái (Status)

- **Đề xuất (Proposed):** 10/06/2026.
- **Quyết định (Decided):** Di chuyển sang **Atomic Update (Cập nhật nguyên tử)** dựa trên số liệu thực nghiệm.

---

## 2. Bối Cảnh (Context)

Hệ thống đặt phòng OmniBooking ban đầu sử dụng cơ chế Khóa Bi Quan (Pessimistic Locking - write-lock `SELECT FOR UPDATE`) tại tầng DB để khóa và trừ kho phòng trống (`RoomAvailability`) nhằm chống overselling (bán vượt tải).

Mặc dù giải pháp này đảm bảo tính toàn vẹn dữ liệu cực tốt, nó lại giữ khóa hàng trong DB suốt toàn bộ vòng đời của Transaction đặt phòng. Dưới tải cao, điều này gây tích tụ hàng đợi khóa (lock wait time), tăng nguy cơ deadlock khi đặt phòng nhiều ngày và giới hạn thông lượng phục vụ của API.

Yêu cầu đặt ra là phải đánh giá và so sánh thực tế với giải pháp **Cập nhật nguyên tử (Atomic Updates)** tại câu lệnh SQL để xem xét tính khả thi của việc tối ưu hóa.

---

## 3. Số Liệu Kiểm Định (Evidence)

Thực nghiệm chạy benchmark tại [INVENTORY_LOCKING_BENCHMARK.md](INVENTORY_LOCKING_BENCHMARK.md) chỉ ra:

- **Latency:** Atomic Update đạt **14.30 ms** so với **51.57 ms** của Pessimistic Lock (nhanh hơn **3.6 lần**).
- **Throughput:** Atomic Update đạt **2,187.52 RPS** so với **519.08 RPS** của Pessimistic Lock (chịu tải tốt hơn **4.2 lần**).
- **Độ chính xác:** Cả 2 giải pháp đều ngăn chặn overselling thành công 100% (0 ca bán vượt).

---

## 4. Quyết Định Kiến Trúc (Decision)

Chúng tôi quyết định di chuyển từ Pessimistic Locking sang **Atomic Update** cho luồng trừ kho phòng.

Cụ thể, thay vì gọi `findByRoomTypeIdAndAvailabilityDateWithLock` và cập nhật thực thể qua JPA, chúng ta sẽ gọi trực tiếp câu lệnh cập nhật nguyên tử có điều kiện:

```sql
UPDATE room_availability SET available_count = available_count - :rooms
WHERE room_type_id = :roomTypeId AND availability_date = :date
AND available_count >= :rooms AND is_closed = false
```

### Cách thức hoạt động:

- Truy vấn UPDATE sẽ trả về số lượng hàng bị ảnh hưởng (`rowsUpdated`).
- Nếu `rowsUpdated == 1`, thao tác trừ phòng ngày đó thành công.
- Nếu `rowsUpdated == 0`, kho phòng đã hết hoặc bị đóng. Hệ thống sẽ ngay lập tức ném lỗi `BOOKING_001` để rollback toàn bộ transaction đặt phòng và khôi phục các ngày đã trừ trước đó (nếu là đặt phòng nhiều ngày).

---

## 5. Hệ Quả (Consequences)

- **Tích cực (Positive):**
   - Tốc độ API tạo đặt phòng nhanh hơn đáng kể, trải nghiệm người dùng mượt mà hơn.
   - Giảm thiểu áp lực lock tài nguyên trên Database PostgreSQL.
   - Loại bỏ hoàn toàn nguy cơ deadlock liên quan tới kho phòng khi chạy nhiều tiến trình đặt phòng đồng thời.
- **Thách thức (Challenges):**
   - Đối với đặt phòng nhiều ngày (ví dụ: ở 5 ngày), nếu ngày thứ 4 bị hết phòng trong khi 3 ngày đầu trừ thành công, lệnh UPDATE của ngày thứ 4 trả về 0. Spring transaction manager sẽ tự động rollback toàn bộ giao dịch, đảm bảo 3 ngày đầu được trả lại kho phòng tự động. Chúng ta cần đảm bảo logic bọc trong một `@Transactional` duy nhất và kiểm tra số lượng `rowsUpdated` nghiêm ngặt cho từng ngày trong vòng lặp.

---

## 6. Lập Luận Kiến Trúc: Khởi Tạo Kho Phòng và Loại Bỏ REQUIRES_NEW (Architecture Justification)

Bản ghi `RoomAvailability` đại diện cho trạng thái kho phòng của một khách sạn/ngày phòng cụ thể, đóng vai trò là một **Infrastructure Aggregate (Hạ tầng tổng hợp)**, độc lập hoàn toàn với vòng đời của `Booking`. Sự tồn tại của nó trước hoặc sau khi có đặt phòng là trạng thái hợp lệ và không bị coi là "dữ liệu mồ côi" (orphaned data).

### Lý do loại bỏ REQUIRES_NEW:

Ban đầu, hệ thống thiết kế tách biệt logic khởi tạo availability bằng `@Transactional(propagation = Propagation.REQUIRES_NEW)` nhằm tránh việc lỗi vi phạm unique key khi chèn đồng thời làm hỏng transaction chính. Tuy nhiên, qua kiểm định thực nghiệm và chạy stress test:

1. **Deadlock Connection Pool:** Dưới tải cao đồng thời, việc sử dụng `REQUIRES_NEW` yêu cầu một kết nối DB thứ hai từ pool (HikariCP) trong khi vẫn giữ kết nối của transaction chính. Nếu tất cả các luồng trong pool đều bị treo chờ kết nối mới để phục vụ transaction con, hệ thống sẽ rơi vào tình trạng **cạn kiệt pool và deadlock hoàn toàn** (Connection Pool Deadlock).
2. **Lỗi treo transaction trong môi trường Test (JPA/H2 Suspension Anomaly):** Việc tạm dừng transaction cha trong môi trường kiểm thử (không thực hiện flush Booking xuống DB trước khi tạm dừng) dẫn tới việc Hibernate/H2 không nhìn thấy dữ liệu khóa ngoại khi chèn bản ghi liên kết, gây ra các lỗi vi phạm toàn vẹn dữ liệu (`Referential integrity constraint violation`).

### Hướng giải quyết trên Production:

- **Pre-population (Khởi tạo trước - Khuyên dùng):** Trong môi trường Production thực tế, hệ thống sẽ thực hiện chạy batch job định kỳ (ví dụ: hàng ngày/hàng tuần) để khởi tạo trước toàn bộ các bản ghi `RoomAvailability` cho các ngày phòng trong tương lai (365 ngày tiếp theo). Đây là cách tiếp cận chuẩn để tối ưu hiệu năng.
- **Fallback Dynamic Creation:** Logic tự động chèn trong `getOrCreateAvailability` chỉ đóng vai trò là cơ chế phòng vệ cuối cùng (fallback) khi một ngày phòng chưa kịp khởi tạo trước. Thao tác chèn này được chạy **trong cùng một transaction chính** (loại bỏ `REQUIRES_NEW` và proxy call) để bảo vệ connection pool và duy trì tính nhất quán giao dịch tối đa.
