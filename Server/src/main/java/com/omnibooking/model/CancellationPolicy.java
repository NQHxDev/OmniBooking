package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CancellationPolicy extends BaseEntity {

   @Column(nullable = false, length = 100)
   private String name;

   @Column(columnDefinition = "TEXT")
   private String description;

   @Builder.Default
   @Column(name = "free_cancellation_days", nullable = false)
   private Integer freeCancellationDays = 0;

   @Builder.Default
   @Column(name = "penalty_percentage", nullable = false)
   private BigDecimal penaltyPercentage = new BigDecimal("100.00");

}
