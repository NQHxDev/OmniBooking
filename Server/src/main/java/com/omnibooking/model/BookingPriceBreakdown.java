package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_price_breakdowns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPriceBreakdown {

   @Id
   private UUID id;

   @ManyToOne(optional = false)
   @JoinColumn(name = "booking_id", nullable = false)
   private Booking booking;

   @Column(name = "stay_date", nullable = false)
   private LocalDate stayDate;

   @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
   private BigDecimal basePrice;

   @Builder.Default
   @Column(name = "seasonal_adjustment", nullable = false, precision = 12, scale = 2)
   private BigDecimal seasonalAdjustment = BigDecimal.ZERO;

   @Builder.Default
   @Column(name = "weekend_adjustment", nullable = false, precision = 12, scale = 2)
   private BigDecimal weekendAdjustment = BigDecimal.ZERO;

   @Builder.Default
   @Column(name = "occupancy_adjustment", nullable = false, precision = 12, scale = 2)
   private BigDecimal occupancyAdjustment = BigDecimal.ZERO;

   @Builder.Default
   @Column(name = "coupon_discount", nullable = false, precision = 12, scale = 2)
   private BigDecimal couponDiscount = BigDecimal.ZERO;

   @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
   private BigDecimal finalPrice;

   @Column(name = "applied_coupon_id")
   private UUID appliedCouponId;

   @Column(name = "applied_coupon_code", length = 50)
   private String appliedCouponCode;

   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @PrePersist
   public void prePersist() {
      if (this.id == null) {
         this.id = UuidCreator.getTimeOrderedEpoch();
      }
      if (this.createdAt == null) {
         this.createdAt = Instant.now();
      }
   }

}
