package com.omnibooking.services.pricing;

import com.omnibooking.model.enums.RuleType;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class SeasonalRuleHandler implements PricingRuleHandler {

   @Override
   public RuleType getSupportedType() {
      return RuleType.SEASONAL;
   }

   @Override
   public boolean isApplicable(
         LocalDate startDate,
         LocalDate endDate,
         Integer occupancyThreshold,
         LocalDate stayDate,
         int guestCount) {
      return (startDate == null || !stayDate.isBefore(startDate)) &&
            (endDate == null || !stayDate.isAfter(endDate));
   }

}
