package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.model.enums.AdjustmentType;
import com.omnibooking.model.enums.RuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "price_rule_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRuleVersion {

   @Id
   private UUID id;

   @ManyToOne(optional = false)
   @JoinColumn(name = "price_rule_id", nullable = false)
   private PriceRule priceRule;

   @Column(nullable = false)
   private Integer version;

   @Column(nullable = false, length = 100)
   private String name;

   @Enumerated(EnumType.STRING)
   @Column(name = "rule_type", nullable = false, length = 20)
   private RuleType ruleType;

   @Column(name = "start_date")
   private LocalDate startDate;

   @Column(name = "end_date")
   private LocalDate endDate;

   @Enumerated(EnumType.STRING)
   @Column(name = "adjustment_type", nullable = false, length = 20)
   private AdjustmentType adjustmentType;

   @Column(name = "adjustment_value", nullable = false, precision = 12, scale = 2)
   private BigDecimal adjustmentValue;

   @Column(name = "occupancy_threshold")
   private Integer occupancyThreshold;

   @Column(nullable = false)
   private Integer priority;

   @Column(name = "is_active", nullable = false)
   private Boolean isActive;

   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @Column(name = "created_by", nullable = false)
   private UUID createdBy;

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
