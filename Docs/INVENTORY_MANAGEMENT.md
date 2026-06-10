# Tài liệu Kỹ thuật: Quản lý Kho phòng trống (Inventory Management)

Tài liệu này mô tả chi tiết cơ chế cập nhật số phòng trống, đồng bộ giao dịch đặt phòng, chiến lược khóa chống bán vượt (overselling) và cơ chế khôi phục kho phòng trong hệ thống **OmniBooking**.

---

## 1. Mô hình Dữ liệu Kho phòng trống (Room Availability Model)

Dữ liệu phòng trống được quản lý theo từng ngày và từng hạng phòng (`RoomType`) thông qua bảng `room_availabilities` (thực thể `RoomAvailability`):

- `available_count`: Số lượng phòng còn trống có thể bán trong ngày cụ thể đó.
- `price_override`: Giá bán đặc biệt áp dụng riêng cho ngày đó (nếu có, đè lên giá basePrice của RoomType).
- `is_closed`: Cờ đóng phòng thủ công của khách sạn.

---

## 2. Quy trình Giữ chỗ Kho phòng (Inventory Reservation Flow)

Khi một đặt phòng được tạo ra thông qua `createBooking`:

1. **Transaction Isolation:** Tiến trình bọc trong một transaction `@Transactional`.
2. **Khóa bản ghi (Locking):** Hệ thống thực hiện khóa dòng dữ liệu phòng trống cho từng ngày lưu trú (từ `checkInDate` đến trước ngày `checkOutDate`).
   - Sử dụng khóa bi quan write-lock (`SELECT ... FOR UPDATE`):
      ```java
      roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDateWithLock(roomTypeId, date)
      ```
3. **Kiểm tra tính khả dụng:** Nếu phòng bị đóng (`isClosed == true`) hoặc số lượng phòng trống còn lại nhỏ hơn số phòng khách yêu cầu đặt, hệ thống ném ngoại lệ `BOOKING_001` (hủy toàn bộ giao dịch, rollback).
4. **Trừ số phòng trống:** Cập nhật `availableCount = availableCount - numRooms`.
5. **Ghi nhật ký vận hành (Inventory Ledger):** Tạo một bản ghi nghiệp vụ `RESERVE` vào bảng `inventory_operations`.

---

## 3. Quy trình Giải phóng Kho phòng (Inventory Release Flow)

Để đảm bảo kho phòng trống không bị rò rỉ (leakage), bất kỳ khi nào một đặt phòng bị hủy (`CANCELLED`) hoặc hết hạn thanh toán (`EXPIRED`):

1. **Atomic State Guard:** Trạng thái booking chuyển sang `CANCELLED` hoặc `EXPIRED` nguyên tử thành công (rowsUpdated > 0).
2. **Cập nhật số phòng trống:** Tăng lại số lượng phòng trống tương ứng qua câu lệnh UPDATE trực tiếp:
   ```sql
   UPDATE room_availability SET available_count = available_count + :rooms
   WHERE room_type_id = :roomTypeId AND availability_date = :date
   ```
3. **Ghi nhật ký vận hành (Inventory Ledger):** Tạo một bản ghi nghiệp vụ `RELEASE` vào bảng `inventory_operations` để phục vụ việc đối soát và kiểm tra rò rỉ định kỳ.

---

## 4. Nhật ký Vận hành Kho phòng (Inventory Operations Ledger)

Mọi biến động kho phòng phải ghi nhận đầy đủ vào bảng `inventory_operations` (thực thể `InventoryOperation`) làm dữ liệu đối soát:

- `booking_id`: Liên kết tới đặt phòng.
- `room_type_id`: Hạng phòng bị ảnh hưởng.
- `availability_date`: Ngày bị ảnh hưởng.
- `operation_type`: Loại vận hành (`RESERVE` hoặc `RELEASE`).
- `num_rooms`: Số lượng phòng thay đổi.
- `created_at`: Thời điểm ghi nhận.

---

## 5. Chiến lược đối phó với Đặt phòng đồng thời (Concurrency Strategy)

Để giải quyết vấn đề đặt phòng đồng thời trên cùng một phòng trống cuối cùng:

- **Pessimistic Lock (Hiện tại):** Khóa cứng dòng dữ liệu để đảm bảo tính đúng đắn tuyệt đối. Nhược điểm: tăng lock wait time và có thể gây deadlock nếu cập nhật nhiều ngày không theo thứ tự.
- **Atomic Update (Thử nghiệm & Đánh giá):** Sử dụng câu truy vấn cập nhật nguyên tử có điều kiện (Optimistic-style):
   ```sql
   UPDATE room_availability SET available_count = available_count - :rooms
   WHERE room_type_id = :roomTypeId AND availability_date = :date
   AND available_count >= :rooms AND is_closed = false
   ```
   Phương pháp này giảm contention nhưng yêu cầu xử lý logic bù (rollback thủ công nếu đặt phòng nhiều ngày bị lỗi nửa chừng).
