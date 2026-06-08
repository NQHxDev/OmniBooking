package com.omnibooking.services.pricing;

import com.omnibooking.model.enums.RuleType;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class OccupancyRuleHandler implements PricingRuleHandler {

   @Override
   public RuleType getSupportedType() {
      return RuleType.OCCUPANCY;
   }

   @Override
   public boolean isApplicable(
         LocalDate startDate,
         LocalDate endDate,
         Integer occupancyThreshold,
         LocalDate stayDate,
         int guestCount) {
      if (occupancyThreshold == null || occupancyThreshold <= 0) {
         throw new IllegalArgumentException("Occupancy threshold must be greater than 0");
      }
      return guestCount >= occupancyThreshold;
   }

}
