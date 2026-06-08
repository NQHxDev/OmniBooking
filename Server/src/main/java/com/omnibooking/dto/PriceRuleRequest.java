package com.omnibooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceRuleRequest(
      @NotNull(message = "Property ID cannot be null") UUID propertyId,

      UUID roomTypeId,

      @NotBlank(message = "Rule name cannot be blank") String name,

      @NotBlank(message = "Rule type cannot be blank") String ruleType,

      LocalDate startDate,

      LocalDate endDate,

      @NotBlank(message = "Adjustment type cannot be blank") String adjustmentType,

      @NotNull(message = "Adjustment value cannot be null") BigDecimal adjustmentValue,

      Integer occupancyThreshold,

      Integer priority,

      Boolean isActive) {
}
