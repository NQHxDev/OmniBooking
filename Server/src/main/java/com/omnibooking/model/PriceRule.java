package com.omnibooking.model;

import com.omnibooking.model.enums.AdjustmentType;
import com.omnibooking.model.enums.RuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "price_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PriceRule extends BaseEntity {

   @ManyToOne(optional = false)
   @JoinColumn(name = "property_id", nullable = false)
   private Property property;

   @ManyToOne
   @JoinColumn(name = "room_type_id")
   private RoomType roomType;

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

   @Builder.Default
   @Column(nullable = false)
   private Integer priority = 0;

   @Builder.Default
   @Column(name = "is_active", nullable = false)
   private Boolean isActive = true;

}
