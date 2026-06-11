package com.omnibooking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(

      UUID id,

      String code,

      String discountType,

      BigDecimal discountValue,

      BigDecimal minBookingAmount,

      BigDecimal maxDiscountAmount,

      Instant validFrom,

      Instant validUntil,

      Integer usageLimit,

      Integer usedCount,

      Integer reservedCount,

      UUID propertyId,

      Boolean isActive) {
}
