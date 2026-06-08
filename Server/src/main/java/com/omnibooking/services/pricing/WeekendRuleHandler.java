package com.omnibooking.services.pricing;

import com.omnibooking.model.enums.RuleType;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class WeekendRuleHandler implements PricingRuleHandler {

   @Override
   public RuleType getSupportedType() {
      return RuleType.WEEKEND;
   }

   @Override
   public boolean isApplicable(
         LocalDate startDate,
         LocalDate endDate,
         Integer occupancyThreshold,
         LocalDate stayDate,
         int guestCount) {
      DayOfWeek day = stayDate.getDayOfWeek();
      return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
   }

}
