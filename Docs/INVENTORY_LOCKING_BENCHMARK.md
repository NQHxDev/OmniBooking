# Báo Cáo Thử Nghiệm Khóa Kho Phòng (Inventory Locking Benchmark Report)

Báo cáo này lưu trữ số liệu thực nghiệm đo đạc hiệu năng và tính chính xác giữa hai chiến lược quản lý kho phòng trống dưới tải cao: **Pessimistic Locking (Khóa bi quan)** và **Atomic Update (Cập nhật nguyên tử)**.

---

## 1. Môi Trường Thử Nghiệm (Test Setup)

Thử nghiệm được thực hiện thông qua lớp kiểm thử tích hợp [InventoryLockingBenchmarkTest](../Server/src/test/java/com/omnibooking/services/booking/impl/InventoryLockingBenchmarkTest.java):

- **Số lượng yêu cầu đồng thời (Threads):** 40 luồng chạy song song thực hiện đặt phòng.
- **Số lượng phòng trống ban đầu:** 15 phòng (để kích hoạt tranh chấp tài nguyên và kiểm tra xem có bị bán vượt - overselling hay không).
- **Số lượng đặt phòng mong muốn tối đa:** 15 (mỗi luồng đặt đúng 1 phòng).
- **Cơ sở dữ liệu:** H2 database (chạy ở chế độ MVCC tương đương cơ chế khóa hàng của PostgreSQL).

---

## 2. Kết Quả Đo Đạc Thực Tế (Benchmark Metrics)

Dưới đây là bảng so sánh các chỉ số hiệu năng và độ tin cậy được trích xuất trực tiếp từ logs chạy test:

| Chỉ Số (Metric)                                     | Khóa Bi Quan - Pessimistic Lock (A) | Cập Nhật Nguyên Tử - Atomic Update (B) | Nhận Xét (Comparison)               |
| :-------------------------------------------------- | :---------------------------------- | :------------------------------------- | :---------------------------------- |
| **Tổng số yêu cầu (Total Requests)**                | 40                                  | 40                                     | Bằng nhau                           |
| **Đặt phòng thành công (Success Bookings)**         | 15                                  | 15                                     | Bằng nhau (Đúng dung lượng kho)     |
| **Số ca bán vượt (Oversell Incidents)**             | 0                                   | 0                                      | Đảm bảo an toàn 100%                |
| **Số ca âm kho (Negative Inventory)**               | 0                                   | 0                                      | Đảm bảo an toàn 100%                |
| **Lỗi Deadlock / Khóa tranh chấp**                  | 0                                   | 0                                      | Không ghi nhận                      |
| **Thời gian chờ khóa (Lock Wait Time)**             | **~37.27 ms**                       | **0 ms**                               | **Atomic không cần chờ khóa hàng**  |
| **Tỷ lệ từ chối/thất bại (Failure/Rejection Rate)** | **62.5% (25/40)**                   | **62.5% (25/40)**                      | Do hết phòng khả dụng (Hợp lệ)      |
| **Thời gian trễ trung bình (Latency)**              | **51.57 ms**                        | **14.30 ms**                           | **Atomic nhanh hơn 3.6 lần**        |
| **Thông lượng (Throughput)**                        | **519.08 RPS**                      | **2,187.52 RPS**                       | **Atomic chịu tải tốt hơn 4.2 lần** |

---

## 3. Phân Tích & Đánh Giá Chi Tiết

1. **Về tính chính xác (Correctness):**
   - Cả hai giải pháp đều đạt độ chính xác tuyệt đối: Chỉ cho phép đặt tối đa 15 phòng từ kho 15 phòng, chặn đứng 25 yêu cầu còn lại. Không xảy ra overselling hay âm kho.
2. **Về hiệu năng & Tốc độ phản hồi (Performance):**
   - **Khóa Bi Quan (A):** Khi sử dụng `SELECT ... FOR UPDATE`, mỗi thread phải chờ thread trước đó commit transaction xong mới được lock và đọc dữ liệu. Điều này tạo ra hàng đợi tuần tự hóa (serialization bottleneck) tại tầng DB, đẩy latency trung bình lên **51.57 ms** và giới hạn thông lượng ở mức **519.08 RPS**.
   - **Cập Nhật Nguyên Tử (B):** Sử dụng điều kiện lọc ở lệnh UPDATE (`available_count >= :rooms`). Database thực hiện ghi trực tiếp một cách nguyên tử mà không cần giữ khóa đọc lâu qua transaction. Latency giảm sâu xuống chỉ còn **14.30 ms** và thông lượng vọt lên **2,187.52 RPS** (gấp 4.2 lần).

---

## 4. Đề Xuất Hành Động (Recommendation)

Dựa trên số liệu đo đạc thực nghiệm rõ ràng, **Atomic Update vượt trội hoàn toàn về cả throughput lẫn latency** trong khi vẫn duy trì tính toàn vẹn dữ liệu tương đương Pessimistic Lock. Chúng tôi khuyến nghị chính thức di chuyển luồng đặt phòng sang sử dụng Atomic Update (xem chi tiết tại tài liệu quyết định kiến trúc [ADR_INVENTORY_LOCKING.md](ADR_INVENTORY_LOCKING.md)).
