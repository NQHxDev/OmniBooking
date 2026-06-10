# Báo Cáo Kiểm Định Truy Vấn Pricing (Pricing Query Audit Report)

Báo cáo này trình bày kết quả đo đạc, phân tích lỗi N+1 truy vấn trong quá trình tính giá đặt phòng (`calculateStayPrice`) và kết quả tối ưu hóa thực tế.

---

## 1. Kết Quả Đo Đạc Trước Tối Ưu (Initial Measurement)

Trước khi tối ưu, mã nguồn [PriceCalculationServiceImpl](../Server/src/main/java/com/omnibooking/services/pricing/impl/PriceCalculationServiceImpl.java) thực hiện truy vấn `roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDate` đơn lẻ cho từng ngày trong vòng lặp ngày lưu trú.

Kết quả đo đạc số lượng câu lệnh SQL (`PrepareStatementCount`) thông qua Hibernate Statistics:

- **Đặt phòng 3 đêm (stayDates = 4 ngày):** **8 truy vấn**
   - 1 truy vấn RoomType (`findById`).
   - 1 truy vấn active PriceRules.
   - 4 truy vấn RoomAvailability đơn lẻ (1 truy vấn/ngày).
   - 2 truy vấn tải lười (lazy load) Property và Owner.
- **Đặt phòng 10 đêm (stayDates = 11 ngày):** **12 truy vấn**
   - 1 truy vấn active PriceRules.
   - 11 truy vấn RoomAvailability đơn lẻ (1 truy vấn/ngày).
   - (Các thông tin RoomType, Property và Owner đã được cache trong Session của Hibernate từ lượt chạy trước nên không truy vấn lại).
- **Chênh lệch:** **+4 truy vấn** (Tương đương $11 - 7 = 4$ truy vấn chênh lệch cho 7 ngày tăng thêm).

> [!WARNING]
> **Nhận xét:** Số lượng truy vấn tăng tuyến tính $O(N)$ theo số ngày lưu trú của khách hàng. Đối với các đơn đặt phòng dài ngày (ví dụ: 30 ngày), hệ thống sẽ phải thực hiện tới 30 truy vấn SELECT đơn lẻ đến bảng `room_availabilities`, gây lãng phí kết nối và tài nguyên DB.

---

## 2. Giải Pháp Tối Ưu Hóa (Optimization Strategy)

Chúng tôi đã bổ sung truy vấn theo dải ngày (Range Query) trong [RoomAvailabilityRepository](../Server/src/main/java/com/omnibooking/repository/property/RoomAvailabilityRepository.java):

```java
@Query("SELECT r FROM RoomAvailability r WHERE r.roomType.id = :roomTypeId " +
       "AND r.availabilityDate >= :startDate AND r.availabilityDate < :endDate")
List<RoomAvailability> findByRoomTypeIdAndAvailabilityDateRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
```

Và thay thế vòng lặp tải đơn lẻ trong [PriceCalculationServiceImpl](../Server/src/main/java/com/omnibooking/services/pricing/impl/PriceCalculationServiceImpl.java) bằng một lần gọi duy nhất trước vòng lặp, sau đó ánh xạ kết quả thành Map trong bộ nhớ.

---

## 3. Kết Quả Sau Tối Ưu (Post-Optimization Evidence)

Chạy lại test case kiểm định [PricingNPlusOneAuditTest](../Server/src/test/java/com/omnibooking/services/pricing/PricingNPlusOneAuditTest.java) thu được kết quả:

```text
====== N+1 QUERY AUDIT REPORT ======
Query count for 3-night stay: 5
Query count for 10-night stay: 2
Difference (queries for additional 7 days): -3
====================================
```

- **Đặt phòng 3 đêm:** **5 truy vấn** (Giảm từ 8).
- **Đặt phòng 10 đêm:** **2 truy vấn** (Giảm từ 12).
   - 1 truy vấn active PriceRules.
   - 1 truy vấn tải lô RoomAvailability theo dải ngày.
- **Chênh lệch:** **-3 truy vấn** (Số lượng truy vấn thực tế không tăng thêm khi tăng số ngày lưu trú).

> [!TIP]
> **Kết luận:** Lỗi N+1 truy vấn đã được giải quyết triệt để. Số lượng truy vấn DB cho availability check hiện tại là hằng số $O(1)$ bất kể khách đặt phòng bao lâu.
