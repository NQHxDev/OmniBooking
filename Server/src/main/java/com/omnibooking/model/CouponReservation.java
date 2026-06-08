package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.model.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupon_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponReservation {

   @Id
   private UUID id;

   @ManyToOne(optional = false)
   @JoinColumn(name = "coupon_id", nullable = false)
   private Coupon coupon;

   @Column(name = "booking_session_id", nullable = false, unique = true, length = 100)
   private String bookingSessionId;

   @Column(name = "reservation_token", nullable = false, unique = true, length = 255)
   private String reservationToken;

   @ManyToOne(optional = false)
   @JoinColumn(name = "customer_id", nullable = false)
   private User customer;

   @ManyToOne(optional = false)
   @JoinColumn(name = "property_id", nullable = false)
   private Property property;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private ReservationStatus status = ReservationStatus.ACTIVE;

   @Column(name = "reserved_at", nullable = false)
   private Instant reservedAt;

   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;

   @PrePersist
   public void prePersist() {
      if (this.id == null) {
         this.id = UuidCreator.getTimeOrderedEpoch();
      }
      if (this.reservedAt == null) {
         this.reservedAt = Instant.now();
      }
   }

}
