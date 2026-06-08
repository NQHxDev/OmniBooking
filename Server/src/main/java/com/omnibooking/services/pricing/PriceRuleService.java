package com.omnibooking.services.pricing;

import com.omnibooking.model.PriceRule;
import java.util.List;
import java.util.UUID;

public interface PriceRuleService {

   PriceRule createRule(PriceRule rule, UUID actorId);

   PriceRule updateRule(UUID ruleId, PriceRule ruleDetails, UUID actorId);

   void deleteRule(UUID ruleId, UUID actorId);

   List<PriceRule> getRulesByProperty(UUID propertyId);

   PriceRule getRuleById(UUID ruleId);

}
