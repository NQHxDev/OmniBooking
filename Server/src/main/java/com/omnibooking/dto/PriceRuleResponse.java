package com.omnibooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceRuleResponse(
      UUID id,
      UUID propertyId,
      UUID roomTypeId,
      String name,
      String ruleType,
      LocalDate startDate,
      LocalDate endDate,
      String adjustmentType,
      BigDecimal adjustmentValue,
      Integer occupancyThreshold,
      Integer priority,
      Boolean isActive) {
}
