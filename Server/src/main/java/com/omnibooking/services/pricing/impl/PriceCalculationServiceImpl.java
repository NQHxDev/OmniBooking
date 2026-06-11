package com.omnibooking.services.pricing.impl;

import com.omnibooking.config.RedisConfig;
import com.omnibooking.model.BaseEntity;
import com.omnibooking.model.Coupon;
import com.omnibooking.model.PriceRule;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.enums.DiscountType;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.pricing.PriceRuleRepository;
import com.omnibooking.services.pricing.PriceCalculationService;
import com.omnibooking.services.pricing.PricingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PriceCalculationServiceImpl implements PriceCalculationService {

   private final RoomTypeRepository roomTypeRepository;

   private final PriceRuleRepository priceRuleRepository;

   private final RoomAvailabilityRepository roomAvailabilityRepository;

   private final CouponRepository couponRepository;

   private final PricingEngine pricingEngine;

   @Autowired
   @Lazy
   private PriceCalculationService self;

   public PriceCalculationServiceImpl(
         RoomTypeRepository roomTypeRepository,
         PriceRuleRepository priceRuleRepository,
         RoomAvailabilityRepository roomAvailabilityRepository,
         CouponRepository couponRepository,
         PricingEngine pricingEngine) {
      this.roomTypeRepository = roomTypeRepository;
      this.priceRuleRepository = priceRuleRepository;
      this.roomAvailabilityRepository = roomAvailabilityRepository;
      this.couponRepository = couponRepository;
      this.pricingEngine = pricingEngine;
   }

   @Override
   @Cacheable(value = RedisConfig.PROPERTY_PRICING, key = "#propertyId.toString() + ':' + #roomTypeId.toString() + ':' + #checkIn.toString() + ':' + #checkOut.toString() + ':' + #guestCount")
   public StayPriceResult calculateStayPrice(UUID propertyId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
         int guestCount) {
      if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
         throw new IllegalArgumentException("Check-out date must be after check-in date");
      }

      RoomType roomType = roomTypeRepository.findById(roomTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Room type not found"));

      if (!roomType.getProperty().getId().equals(propertyId)) {
         throw new IllegalArgumentException("Room type does not belong to the specified property");
      }

      List<PriceRule> rules = priceRuleRepository.findByPropertyIdAndIsActiveTrue(propertyId);
      List<PriceRule> applicableRulesForRoom = rules.stream()
            .filter(r -> r.getRoomType() == null || r.getRoomType().getId().equals(roomTypeId))
            .collect(Collectors.toList());

      List<LocalDate> stayDates = checkIn.datesUntil(checkOut).collect(Collectors.toList());

      // Batch load availability records for the entire stay range
      List<RoomAvailability> availabilities = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDateRange(roomTypeId, checkIn, checkOut);

      java.util.Map<LocalDate, BigDecimal> priceOverrides = availabilities.stream()
            .filter(a -> a.getPriceOverride() != null)
            .collect(Collectors.toMap(RoomAvailability::getAvailabilityDate, RoomAvailability::getPriceOverride));

      List<DailyPrice> dailyPrices = new ArrayList<>();

      BigDecimal totalBasePrice = BigDecimal.ZERO;
      BigDecimal totalSeasonal = BigDecimal.ZERO;
      BigDecimal totalWeekend = BigDecimal.ZERO;
      BigDecimal totalOccupancy = BigDecimal.ZERO;
      BigDecimal totalFinal = BigDecimal.ZERO;

      for (LocalDate date : stayDates) {
         BigDecimal basePrice = priceOverrides.getOrDefault(date, roomType.getBasePrice());

         PricingEngine.PricingResult result = pricingEngine.calculatePrice(
               applicableRulesForRoom,
               date,
               basePrice,
               guestCount);

         List<UUID> appliedRuleIds = result.appliedRules().stream()
               .map(BaseEntity::getId)
               .collect(Collectors.toList());

         dailyPrices.add(new DailyPrice(
               date,
               basePrice,
               result.seasonalAdjustment(),
               result.weekendAdjustment(),
               result.occupancyAdjustment(),
               result.finalPrice(),
               appliedRuleIds));

         totalBasePrice = totalBasePrice.add(basePrice);
         totalSeasonal = totalSeasonal.add(result.seasonalAdjustment());
         totalWeekend = totalWeekend.add(result.weekendAdjustment());
         totalOccupancy = totalOccupancy.add(result.occupancyAdjustment());
         totalFinal = totalFinal.add(result.finalPrice());
      }

      return new StayPriceResult(
            dailyPrices,
            totalBasePrice,
            totalSeasonal,
            totalWeekend,
            totalOccupancy,
            BigDecimal.ZERO,
            totalFinal,
            null,
            null);
   }

   @Override
   public StayPriceResult calculateStayPriceWithCoupon(UUID propertyId, UUID roomTypeId, LocalDate checkIn,
         LocalDate checkOut, int guestCount, String couponCode) {
      StayPriceResult stayPrice = self.calculateStayPrice(propertyId, roomTypeId, checkIn, checkOut, guestCount);
      if (couponCode == null || couponCode.trim().isEmpty()) {
         return stayPrice;
      }

      Coupon coupon = couponRepository.findActiveCouponByCodeAndProperty(couponCode.trim(), propertyId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive coupon code"));

      Instant now = Instant.now();
      if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
         throw new IllegalArgumentException("Coupon is expired or not yet active");
      }

      if (coupon.getUsageLimit() != null
            && (coupon.getUsedCount() + coupon.getReservedCount()) >= coupon.getUsageLimit()) {
         throw new IllegalArgumentException("Coupon usage limit has been reached");
      }

      if (coupon.getMinBookingAmount() != null
            && stayPrice.totalFinalPrice().compareTo(coupon.getMinBookingAmount()) < 0) {
         throw new IllegalArgumentException("Minimum booking amount of " + coupon.getMinBookingAmount() + " not met");
      }

      BigDecimal totalDiscount = calculateCouponDiscount(coupon, stayPrice.totalFinalPrice());

      List<DailyPrice> updatedDailyPrices = new ArrayList<>();
      BigDecimal allocatedDiscountSum = BigDecimal.ZERO;

      for (int i = 0; i < stayPrice.dailyPrices().size(); i++) {
         DailyPrice dp = stayPrice.dailyPrices().get(i);
         BigDecimal dayDiscount;
         if (i == stayPrice.dailyPrices().size() - 1) {
            dayDiscount = totalDiscount.subtract(allocatedDiscountSum);
         } else {
            if (stayPrice.totalFinalPrice().compareTo(BigDecimal.ZERO) > 0) {
               dayDiscount = dp.finalPrice().multiply(totalDiscount)
                     .divide(stayPrice.totalFinalPrice(), 2, RoundingMode.HALF_UP);
            } else {
               dayDiscount = BigDecimal.ZERO;
            }
            allocatedDiscountSum = allocatedDiscountSum.add(dayDiscount);
         }

         BigDecimal dayFinalPrice = dp.finalPrice().subtract(dayDiscount);
         if (dayFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
            dayFinalPrice = BigDecimal.ZERO;
         }

         updatedDailyPrices.add(new DailyPrice(
               dp.date(),
               dp.basePrice(),
               dp.seasonalAdjustment(),
               dp.weekendAdjustment(),
               dp.occupancyAdjustment(),
               dayFinalPrice,
               dp.appliedRuleIds()));
      }

      BigDecimal finalTotalPrice = stayPrice.totalFinalPrice().subtract(totalDiscount);
      if (finalTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
         finalTotalPrice = BigDecimal.ZERO;
      }

      return new StayPriceResult(
            updatedDailyPrices,
            stayPrice.totalBasePrice(),
            stayPrice.totalSeasonalAdjustment(),
            stayPrice.totalWeekendAdjustment(),
            stayPrice.totalOccupancyAdjustment(),
            totalDiscount,
            finalTotalPrice,
            coupon.getId(),
            coupon.getCode());
   }

   @Override
   public BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal totalStayPrice) {
      if (coupon == null) {
         return BigDecimal.ZERO;
      }
      BigDecimal discount = BigDecimal.ZERO;
      if (coupon.getDiscountType() == DiscountType.PERCENT) {
         discount = totalStayPrice.multiply(coupon.getDiscountValue())
               .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
         if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
         }
      } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
         discount = coupon.getDiscountValue();
      }

      if (discount.compareTo(totalStayPrice) > 0) {
         discount = totalStayPrice;
      }
      return discount;
   }

   @Override
   public Map<UUID, BigDecimal> calculateStayPricesForRoomTypes(UUID propertyId, List<UUID> roomTypeIds,
         LocalDate checkIn, LocalDate checkOut, int guestCount) {
      if (roomTypeIds == null || roomTypeIds.isEmpty()) {
         return Collections.emptyMap();
      }
      if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
         throw new IllegalArgumentException("Check-out date must be after check-in date");
      }

      List<RoomType> roomTypes = roomTypeRepository.findAllById(roomTypeIds);
      for (RoomType rt : roomTypes) {
         if (!rt.getProperty().getId().equals(propertyId)) {
            throw new IllegalArgumentException("Room type " + rt.getId() + " does not belong to the specified property");
         }
      }

      List<PriceRule> rules = priceRuleRepository.findByPropertyIdAndIsActiveTrue(propertyId);
      List<RoomAvailability> availabilities = roomAvailabilityRepository
            .findByRoomTypeIdsAndAvailabilityDateRange(roomTypeIds, checkIn, checkOut);

      Map<UUID, List<RoomAvailability>> availabilityMap = availabilities.stream()
            .collect(Collectors.groupingBy(a -> a.getRoomType().getId()));

      List<LocalDate> stayDates = checkIn.datesUntil(checkOut).collect(Collectors.toList());
      Map<UUID, BigDecimal> resultPriceMap = new HashMap<>();

      for (RoomType roomType : roomTypes) {
         UUID roomTypeId = roomType.getId();
         List<PriceRule> applicableRulesForRoom = rules.stream()
               .filter(r -> r.getRoomType() == null || r.getRoomType().getId().equals(roomTypeId))
               .collect(Collectors.toList());

         List<RoomAvailability> roomAvailabilities = availabilityMap.getOrDefault(roomTypeId, List.of());
         Map<LocalDate, BigDecimal> priceOverrides = roomAvailabilities.stream()
               .filter(a -> a.getPriceOverride() != null)
               .collect(Collectors.toMap(RoomAvailability::getAvailabilityDate, RoomAvailability::getPriceOverride));

         BigDecimal totalFinal = BigDecimal.ZERO;
         for (LocalDate date : stayDates) {
            BigDecimal basePrice = priceOverrides.getOrDefault(date, roomType.getBasePrice());

            PricingEngine.PricingResult result = pricingEngine.calculatePrice(
                  applicableRulesForRoom,
                  date,
                  basePrice,
                  guestCount);

            totalFinal = totalFinal.add(result.finalPrice());
         }
         resultPriceMap.put(roomTypeId, totalFinal);
      }

      return resultPriceMap;
   }

}
