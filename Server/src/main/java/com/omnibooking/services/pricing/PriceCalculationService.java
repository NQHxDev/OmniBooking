package com.omnibooking.services.pricing;

import com.omnibooking.model.Coupon;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PriceCalculationService {

   StayPriceResult calculateStayPrice(UUID propertyId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
         int guestCount);

   StayPriceResult calculateStayPriceWithCoupon(UUID propertyId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
         int guestCount, String couponCode);

   BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal totalStayPrice);

   record DailyPrice(
         LocalDate date,
         BigDecimal basePrice,
         BigDecimal seasonalAdjustment,
         BigDecimal weekendAdjustment,
         BigDecimal occupancyAdjustment,
         BigDecimal finalPrice,
         List<UUID> appliedRuleIds) {
   }

   record StayPriceResult(
         List<DailyPrice> dailyPrices,
         BigDecimal totalBasePrice,
         BigDecimal totalSeasonalAdjustment,
         BigDecimal totalWeekendAdjustment,
         BigDecimal totalOccupancyAdjustment,
         BigDecimal totalCouponDiscount,
         BigDecimal totalFinalPrice,
         UUID appliedCouponId,
         String appliedCouponCode) {
   }

}
