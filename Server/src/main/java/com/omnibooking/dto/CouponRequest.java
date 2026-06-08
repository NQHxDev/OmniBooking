package com.omnibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponRequest(
      @NotBlank(message = "Coupon code cannot be blank") String code,

      @NotBlank(message = "Discount type cannot be blank") String discountType,

      @NotNull(message = "Discount value cannot be null") BigDecimal discountValue,

      BigDecimal minBookingAmount,

      BigDecimal maxDiscountAmount,

      @NotNull(message = "Valid from cannot be null") Instant validFrom,

      @NotNull(message = "Valid until cannot be null") Instant validUntil,

      Integer usageLimit,

      UUID propertyId,

      Boolean isActive) {
}
