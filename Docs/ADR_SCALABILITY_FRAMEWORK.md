# Khung Quyết Định Kiến Trúc: Thiết Kế Mở Rộng Hệ Thống (Scalability Decision Framework)

Tài liệu này đóng vai trò là Khung Quyết Định Kiến Trúc (Architecture Decision Framework) để hướng dẫn đội ngũ phát triển đưa ra các quyết định kỹ thuật nâng cấp hệ thống OmniBooking khi các ngưỡng tải và quy mô dữ liệu thực tế đạt đến giới hạn.

---

## 1. Mở Rộng Khả Năng Đọc (Read Scaling)

### Khi nào nên triển khai Read Replicas?

- **Bối cảnh:** Toàn bộ truy vấn đọc (tra cứu khách sạn, xem phòng trống, tìm lịch sử đặt phòng) và ghi đều tập trung vào DB Master duy nhất.
- **Tiêu chí kích hoạt (Triggers):**
   - Tỷ lệ đọc/ghi (Read/Write Ratio) vượt quá **4:1**.
   - CPU Utilization của Database Master thường xuyên duy trì ở mức **> 70%** trong giờ cao điểm, và trên 60% các câu lệnh đó là câu lệnh đọc (`SELECT`).
   - Latency trung bình của API đọc thông tin danh sách khách sạn tăng quá **200ms** dưới tải bình thường.
- **Quyết định (Decision):** Cấu hình định tuyến (Routing DataSource) phân tách Read/Write. Toàn bộ thao tác ghi gửi đến Master; các thao tác đọc và tra cứu gửi đến 1 hoặc nhiều database Read Replicas (áp dụng nhân bản không đồng bộ - asynchronous replication).

### Khi nào nên áp dụng mô hình CQRS (Command Query Responsibility Segregation)?

- **Bối cảnh:** Ngay cả khi có Read Replicas, các truy vấn đọc phức tạp (join nhiều bảng như Booking, Property, RoomType, User, Review) vẫn gây tải nặng và chậm.
- **Tiêu chí kích hoạt (Triggers):**
   - Nhân bản trễ (Replication Lag) giữa Master và Replicas vượt quá **2 giây** dưới tải cao do tranh chấp tài nguyên (lock/write contention).
   - Các truy vấn tổng hợp báo cáo doanh thu và lịch sử đặt phòng ảnh hưởng trực tiếp tới tốc độ tải trang quản trị của Partner.
   - Số lượng bản ghi bảng `bookings` vượt quá **10 triệu dòng**.
- **Quyết định:** Tách biệt hoàn toàn tầng ghi (Command) dùng PostgreSQL và tầng đọc (Query) dùng một Read Model riêng biệt lưu trong bộ nhớ cache Redis hoặc Elasticsearch. Dữ liệu Query được cập nhật bất đồng bộ thông qua Event Bus (Kafka).

---

## 2. Chiến Lược Tìm Kiếm (Search Strategy)

### Khi nào Elasticsearch trở nên cần thiết?

- **Bối cảnh:** Tìm kiếm khách sạn theo vị trí địa lý, khoảng giá, dịch vụ đi kèm và từ khóa tiếng Việt có dấu/không dấu (Fuzzy Search) bằng SQL `LIKE` hoặc PostgreSQL Text Search quá chậm và không tối ưu.
- **Tiêu chí kích hoạt (Triggers):**
   - Tổng số lượng khách sạn (`properties`) đang hoạt động trên hệ thống vượt quá **50.000 khách sạn**.
   - Tải lượng tìm kiếm từ người dùng (Search RPS) vượt quá **150 RPS**.
   - Các câu truy vấn tìm kiếm phức tạp (phân trang kết hợp filter thuộc tính phòng) chiếm trên **50% thời gian xử lý** của Database Master.
- **Quyết định:** Đưa Elasticsearch/OpenSearch vào làm công cụ tìm kiếm chính cho phía khách hàng. PostgreSQL giữ vai trò là Single Source of Truth (SSOT). Dữ liệu được đồng bộ từ PostgreSQL sang Elasticsearch bất đồng bộ qua luồng Kafka CDC (Kafka Connect) để đảm bảo không chặn tiến trình tạo đặt phòng.

---

## 3. Quản Lý Tăng Trưởng Dữ Liệu (Data Growth & Archival)

### Khi nào nên phân vùng bảng (Table Partitioning)?

- **Bối cảnh:** Bảng `bookings` và bảng nhật ký kho phòng `inventory_operations` tăng trưởng rất nhanh. Quét tuần tự (Seq Scan) hoặc thậm chí Index Scan trên các bảng này trở nên đắt đỏ.
- **Tiêu chí kích hoạt (Triggers):**
   - Bảng `bookings` vượt quá **20 triệu dòng** hoặc kích thước bảng vật lý trên đĩa vượt quá **20 GB**.
   - Thời gian chạy tác vụ đối soát hàng ngày của `BookingReconciliationWorker` tăng vượt quá **15 phút**.
- **Quyết định:** Triển khai Phân vùng khai báo (Declarative Partitioning) trong PostgreSQL cho bảng `bookings` và `inventory_operations` theo tháng dựa trên cột `created_at`.

### Khi nào nên thực hiện chính sách Lưu Trữ Lạnh (Archival Strategy)?

- **Bối cảnh:** Dữ liệu đặt phòng cũ từ nhiều năm trước rất ít khi được truy cập nhưng vẫn chiếm dụng không gian lưu trữ đắt đỏ của DB Master.
- **Tiêu chí kích hoạt (Triggers):**
   - Tuổi thọ dữ liệu đặt phòng vượt quá **2 năm (24 tháng)**.
   - Chi phí đĩa lưu trữ (Hot SSD Storage) cho Database Master tăng quá ngân sách dự kiến.
- **Quyết định:** Triển khai tiến trình quét tự động hàng tháng chuyển các phân vùng dữ liệu cũ hơn 24 tháng sang DB lịch sử (Historical DB) hoặc nén lưu trữ dạng tệp tin trên Cloud Object Storage (như AWS S3/GCS). Xóa hoàn toàn bản ghi khỏi DB Master sau khi đã lưu trữ lạnh thành công.

---

## 4. Mở Rộng Quy Trình Vận Hành (Operational Scaling)

### Khi nào nên chuyển đổi sang Event-Driven Architecture (EDA) qua Kafka?

- **Bối cảnh:** Khi đặt phòng thành công, hệ thống phải thực hiện nhiều side-effects: gửi mail xác nhận, tính điểm thành viên, đồng bộ thống kê, đẩy dữ liệu sang CRM đối tác. Chạy tất cả đồng bộ trong luồng API sẽ làm phình to thời gian phản hồi.
- **Tiêu chí kích hoạt (Triggers):**
   - Thời gian phản hồi trung bình của API tạo đặt phòng vượt quá **250ms**.
   - Sự cố sập dịch vụ gửi email hoặc CRM đối tác làm gián đoạn/lỗi toàn bộ tiến trình đặt phòng của khách hàng (lỗi cascade).
   - Số lượng tác vụ side-effects tăng lên **> 4 tác vụ** cho mỗi sự kiện đặt phòng.
- **Quyết định:** Sử dụng mô hình Transactional Outbox Pattern kết hợp Apache Kafka. API chỉ ghi nhận đặt phòng vào PostgreSQL, Outbox Worker đẩy sự kiện lên Kafka. Các dịch vụ khác (Email Service, Loyalty Service, Analytics Service) sẽ tiêu thụ tin nhắn từ Kafka độc lập và bất đồng bộ, cô lập lỗi hoàn toàn.
