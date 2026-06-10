package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_release_retries", uniqueConstraints = {
      @UniqueConstraint(name = "uq_coupon_retry_booking_coupon_status", columnNames = { "booking_id", "coupon_id",
            "status" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponReleaseRetry {

   @Id
   private UUID id;

   @Column(name = "booking_id", nullable = false)
   private UUID bookingId;

   @Column(name = "coupon_id", nullable = false)
   private UUID couponId;

   @Column(name = "user_id", nullable = false)
   private UUID userId;

   @Column(name = "attempt_count", nullable = false)
   private int attemptCount;

   @Column(name = "last_attempt_at")
   private Instant lastAttemptAt;

   @Column(name = "next_attempt_at", nullable = false)
   private Instant nextAttemptAt;

   @Column(name = "status", nullable = false, length = 20)
   private String status; // PENDING, SUCCESS, FAILED

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
