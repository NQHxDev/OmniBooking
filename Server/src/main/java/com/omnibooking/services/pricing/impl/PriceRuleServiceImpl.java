package com.omnibooking.services.pricing.impl;

import com.omnibooking.config.RedisConfig;
import com.omnibooking.model.PriceRule;
import com.omnibooking.model.PriceRuleVersion;
import com.omnibooking.model.enums.RuleType;
import com.omnibooking.repository.pricing.PriceRuleRepository;
import com.omnibooking.repository.pricing.PriceRuleVersionRepository;
import com.omnibooking.services.pricing.PriceRuleService;
import com.omnibooking.services.pricing.AuditLogService;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PriceRuleServiceImpl implements PriceRuleService {

   private final PriceRuleRepository priceRuleRepository;

   private final PriceRuleVersionRepository priceRuleVersionRepository;

   private final AuditLogService auditLogService;

   private final CacheManager cacheManager;

   public PriceRuleServiceImpl(
         PriceRuleRepository priceRuleRepository,
         PriceRuleVersionRepository priceRuleVersionRepository,
         AuditLogService auditLogService,
         CacheManager cacheManager) {
      this.priceRuleRepository = priceRuleRepository;
      this.priceRuleVersionRepository = priceRuleVersionRepository;
      this.auditLogService = auditLogService;
      this.cacheManager = cacheManager;
   }

   private void evictPricingCache() {
      var cache = cacheManager.getCache(RedisConfig.PROPERTY_PRICING);
      if (cache != null) {
         cache.clear();
      }
   }

   @Override
   public PriceRule createRule(PriceRule rule, UUID actorId) {
      if (rule.getRuleType() == RuleType.OCCUPANCY &&
            (rule.getOccupancyThreshold() == null || rule.getOccupancyThreshold() <= 0)) {
         throw new IllegalArgumentException("Occupancy threshold must be greater than 0");
      }

      PriceRule savedRule = priceRuleRepository.save(rule);

      // Versioning: first version is 1
      PriceRuleVersion version = PriceRuleVersion.builder()
            .priceRule(savedRule)
            .version(1)
            .name(savedRule.getName())
            .ruleType(savedRule.getRuleType())
            .startDate(savedRule.getStartDate())
            .endDate(savedRule.getEndDate())
            .adjustmentType(savedRule.getAdjustmentType())
            .adjustmentValue(savedRule.getAdjustmentValue())
            .occupancyThreshold(savedRule.getOccupancyThreshold())
            .priority(savedRule.getPriority())
            .isActive(savedRule.getIsActive())
            .createdBy(actorId)
            .build();

      priceRuleVersionRepository.save(version);

      // Audit Logging
      auditLogService.logChange("PRICE_RULE", savedRule.getId(), "CREATE", actorId, null, savedRule);

      evictPricingCache();
      return savedRule;
   }

   @Override
   public PriceRule updateRule(UUID ruleId, PriceRule ruleDetails, UUID actorId) {
      PriceRule existing = priceRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Price rule not found"));

      if (ruleDetails.getRuleType() == RuleType.OCCUPANCY &&
            (ruleDetails.getOccupancyThreshold() == null || ruleDetails.getOccupancyThreshold() <= 0)) {
         throw new IllegalArgumentException("Occupancy threshold must be greater than 0");
      }

      // Clone old values for audit log before updating
      PriceRule oldCopy = PriceRule.builder()
            .name(existing.getName())
            .ruleType(existing.getRuleType())
            .startDate(existing.getStartDate())
            .endDate(existing.getEndDate())
            .adjustmentType(existing.getAdjustmentType())
            .adjustmentValue(existing.getAdjustmentValue())
            .occupancyThreshold(existing.getOccupancyThreshold())
            .priority(existing.getPriority())
            .isActive(existing.getIsActive())
            .build();
      oldCopy.setId(existing.getId());

      // Update fields
      existing.setName(ruleDetails.getName());
      existing.setRuleType(ruleDetails.getRuleType());
      existing.setStartDate(ruleDetails.getStartDate());
      existing.setEndDate(ruleDetails.getEndDate());
      existing.setAdjustmentType(ruleDetails.getAdjustmentType());
      existing.setAdjustmentValue(ruleDetails.getAdjustmentValue());
      existing.setOccupancyThreshold(ruleDetails.getOccupancyThreshold());
      existing.setPriority(ruleDetails.getPriority());
      existing.setIsActive(ruleDetails.getIsActive());

      PriceRule updated = priceRuleRepository.save(existing);

      // Increment Version
      int maxVer = priceRuleVersionRepository.findMaxVersionByPriceRuleId(ruleId);
      int nextVer = maxVer + 1;

      PriceRuleVersion version = PriceRuleVersion.builder()
            .priceRule(updated)
            .version(nextVer)
            .name(updated.getName())
            .ruleType(updated.getRuleType())
            .startDate(updated.getStartDate())
            .endDate(updated.getEndDate())
            .adjustmentType(updated.getAdjustmentType())
            .adjustmentValue(updated.getAdjustmentValue())
            .occupancyThreshold(updated.getOccupancyThreshold())
            .priority(updated.getPriority())
            .isActive(updated.getIsActive())
            .createdBy(actorId)
            .build();

      priceRuleVersionRepository.save(version);

      // Audit Logging
      auditLogService.logChange("PRICE_RULE", updated.getId(), "UPDATE", actorId, oldCopy, updated);

      evictPricingCache();
      return updated;
   }

   @Override
   public void deleteRule(UUID ruleId, UUID actorId) {
      PriceRule existing = priceRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Price rule not found"));

      auditLogService.logChange("PRICE_RULE", ruleId, "DELETE", actorId, existing, null);
      priceRuleRepository.deleteById(ruleId);
      evictPricingCache();
   }

   @Override
   @Transactional(readOnly = true)
   public List<PriceRule> getRulesByProperty(UUID propertyId) {
      return priceRuleRepository.findByPropertyId(propertyId);
   }

   @Override
   @Transactional(readOnly = true)
   public PriceRule getRuleById(UUID ruleId) {
      return priceRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Price rule not found"));
   }

}
