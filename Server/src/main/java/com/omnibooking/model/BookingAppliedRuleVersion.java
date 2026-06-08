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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_applied_rule_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAppliedRuleVersion {

   @Id
   private UUID id;

   @ManyToOne(optional = false)
   @JoinColumn(name = "booking_breakdown_id", nullable = false)
   private BookingPriceBreakdown bookingPriceBreakdown;

   @ManyToOne(optional = false)
   @JoinColumn(name = "rule_version_id", nullable = false)
   private PriceRuleVersion priceRuleVersion;

   @Column(name = "adjustment_amount", nullable = false, precision = 12, scale = 2)
   private BigDecimal adjustmentAmount;

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
