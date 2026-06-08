package com.omnibooking.services.pricing;

import com.omnibooking.model.PriceRule;
import com.omnibooking.model.PriceRuleVersion;
import com.omnibooking.model.enums.RuleType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PricingEngine {

   private final Map<RuleType, PricingRuleHandler> handlers = new EnumMap<>(RuleType.class);

   public PricingEngine(List<PricingRuleHandler> ruleHandlers) {
      for (PricingRuleHandler handler : ruleHandlers) {
         handlers.put(handler.getSupportedType(), handler);
      }
   }

   public PricingRuleHandler getHandler(RuleType type) {
      return handlers.get(type);
   }

   public PricingResult calculatePrice(
         Collection<PriceRule> rules,
         LocalDate stayDate,
         BigDecimal basePrice,
         int guestCount) {
      BigDecimal seasonalAdjustment = BigDecimal.ZERO;
      BigDecimal weekendAdjustment = BigDecimal.ZERO;
      BigDecimal occupancyAdjustment = BigDecimal.ZERO;

      List<PriceRule> sortedRules = rules.stream()
            .filter(PriceRule::getIsActive)
            .sorted(Comparator.comparing(PriceRule::getPriority).reversed())
            .collect(Collectors.toList());

      List<PriceRule> appliedRules = new java.util.ArrayList<>();

      for (PriceRule rule : sortedRules) {
         PricingRuleHandler handler = handlers.get(rule.getRuleType());
         if (handler != null && handler.isApplicable(
               rule.getStartDate(),
               rule.getEndDate(),
               rule.getOccupancyThreshold(),
               stayDate,
               guestCount)) {
            BigDecimal adjustment = handler.calculateAdjustment(rule.getAdjustmentType(), rule.getAdjustmentValue(),
                  basePrice);

            if (rule.getRuleType() == RuleType.SEASONAL) {
               seasonalAdjustment = seasonalAdjustment.add(adjustment);
            } else if (rule.getRuleType() == RuleType.WEEKEND) {
               weekendAdjustment = weekendAdjustment.add(adjustment);
            } else if (rule.getRuleType() == RuleType.OCCUPANCY) {
               occupancyAdjustment = occupancyAdjustment.add(adjustment);
            }
            appliedRules.add(rule);
         }
      }

      BigDecimal finalPrice = basePrice.add(seasonalAdjustment).add(weekendAdjustment).add(occupancyAdjustment);
      if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
         finalPrice = BigDecimal.ZERO;
      }

      return new PricingResult(basePrice, seasonalAdjustment, weekendAdjustment, occupancyAdjustment, finalPrice,
            appliedRules);
   }

   public PricingResultVersion calculatePriceFromVersions(
         Collection<PriceRuleVersion> ruleVersions,
         LocalDate stayDate,
         BigDecimal basePrice,
         int guestCount) {
      BigDecimal seasonalAdjustment = BigDecimal.ZERO;
      BigDecimal weekendAdjustment = BigDecimal.ZERO;
      BigDecimal occupancyAdjustment = BigDecimal.ZERO;

      List<PriceRuleVersion> sortedRules = ruleVersions.stream()
            .filter(PriceRuleVersion::getIsActive)
            .sorted(Comparator.comparing(PriceRuleVersion::getPriority).reversed())
            .collect(Collectors.toList());

      List<PriceRuleVersion> appliedVersions = new java.util.ArrayList<>();

      for (PriceRuleVersion version : sortedRules) {
         PricingRuleHandler handler = handlers.get(version.getRuleType());
         if (handler != null && handler.isApplicable(
               version.getStartDate(),
               version.getEndDate(),
               version.getOccupancyThreshold(),
               stayDate,
               guestCount)) {
            BigDecimal adjustment = handler.calculateAdjustment(version.getAdjustmentType(),
                  version.getAdjustmentValue(), basePrice);

            if (version.getRuleType() == RuleType.SEASONAL) {
               seasonalAdjustment = seasonalAdjustment.add(adjustment);
            } else if (version.getRuleType() == RuleType.WEEKEND) {
               weekendAdjustment = weekendAdjustment.add(adjustment);
            } else if (version.getRuleType() == RuleType.OCCUPANCY) {
               occupancyAdjustment = occupancyAdjustment.add(adjustment);
            }
            appliedVersions.add(version);
         }
      }

      BigDecimal finalPrice = basePrice.add(seasonalAdjustment).add(weekendAdjustment).add(occupancyAdjustment);
      if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
         finalPrice = BigDecimal.ZERO;
      }

      return new PricingResultVersion(basePrice, seasonalAdjustment, weekendAdjustment, occupancyAdjustment, finalPrice,
            appliedVersions);
   }

   public record PricingResult(
         BigDecimal basePrice,
         BigDecimal seasonalAdjustment,
         BigDecimal weekendAdjustment,
         BigDecimal occupancyAdjustment,
         BigDecimal finalPrice,
         List<PriceRule> appliedRules) {
   }

   public record PricingResultVersion(
         BigDecimal basePrice,
         BigDecimal seasonalAdjustment,
         BigDecimal weekendAdjustment,
         BigDecimal occupancyAdjustment,
         BigDecimal finalPrice,
         List<PriceRuleVersion> appliedVersions) {
   }

}
