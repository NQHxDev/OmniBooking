package com.omnibooking.model;

import com.omnibooking.model.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Coupon extends BaseEntity {

   @Column(nullable = false, unique = true, length = 50)
   private String code;

   @Enumerated(EnumType.STRING)
   @Column(name = "discount_type", nullable = false, length = 20)
   private DiscountType discountType;

   @Column(name = "discount_value", nullable = false)
   private BigDecimal discountValue;

   @Builder.Default
   @Column(name = "min_booking_amount")
   private BigDecimal minBookingAmount = BigDecimal.ZERO;

   @Column(name = "max_discount_amount")
   private BigDecimal maxDiscountAmount;

   @Column(name = "valid_from", nullable = false)
   private Instant validFrom;

   @Column(name = "valid_until", nullable = false)
   private Instant validUntil;

   @Column(name = "usage_limit")
   private Integer usageLimit;

   @Builder.Default
   @Column(name = "used_count")
   private Integer usedCount = 0;

   @Builder.Default
   @Column(name = "is_active")
   private Boolean isActive = true;

   @ManyToOne
   @JoinColumn(name = "property_id")
   private Property property;

   @Builder.Default
   @Column(name = "reserved_count", nullable = false)
   private Integer reservedCount = 0;

}
