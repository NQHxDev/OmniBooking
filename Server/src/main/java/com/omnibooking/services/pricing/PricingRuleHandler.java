package com.omnibooking.services.pricing;

import com.omnibooking.model.enums.AdjustmentType;
import com.omnibooking.model.enums.RuleType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public interface PricingRuleHandler {

   RuleType getSupportedType();

   boolean isApplicable(
         LocalDate startDate,
         LocalDate endDate,
         Integer occupancyThreshold,
         LocalDate stayDate,
         int guestCount);

   default BigDecimal calculateAdjustment(
         AdjustmentType adjustmentType,
         BigDecimal adjustmentValue,
         BigDecimal basePrice) {
      if (adjustmentType == AdjustmentType.FIXED_AMOUNT) {
         return adjustmentValue;
      } else if (adjustmentType == AdjustmentType.PERCENTAGE) {
         return basePrice.multiply(adjustmentValue)
               .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      }
      return BigDecimal.ZERO;
   }

}
