package com.omnibooking.services.pricing.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.model.PricingAuditLog;
import com.omnibooking.repository.pricing.PricingAuditLogRepository;
import com.omnibooking.services.pricing.AuditLogService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

   private final PricingAuditLogRepository pricingAuditLogRepository;

   private final ObjectMapper objectMapper;

   public AuditLogServiceImpl(PricingAuditLogRepository pricingAuditLogRepository, ObjectMapper objectMapper) {
      this.pricingAuditLogRepository = pricingAuditLogRepository;
      this.objectMapper = objectMapper;
   }

   @Override
   public void logChange(String entityType, UUID entityId, String operationType, UUID actorId, Object oldValues,
         Object newValues) {
      String correlationIdStr = MDC.get("correlationId");
      UUID correlationId;
      if (correlationIdStr != null) {
         try {
            correlationId = UUID.fromString(correlationIdStr);
         } catch (IllegalArgumentException e) {
            correlationId = UuidCreator.getTimeOrderedEpoch();
         }
      } else {
         correlationId = UuidCreator.getTimeOrderedEpoch();
      }

      String oldJson = null;
      String newJson = null;

      try {
         if (oldValues != null) {
            oldJson = objectMapper.writeValueAsString(oldValues);
         }
         if (newValues != null) {
            newJson = objectMapper.writeValueAsString(newValues);
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed to serialize audit log values to JSON", e);
      }

      PricingAuditLog auditLog = PricingAuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .operationType(operationType)
            .actorId(actorId)
            .oldValues(oldJson)
            .newValues(newJson)
            .correlationId(correlationId)
            .build();

      pricingAuditLogRepository.save(auditLog);
   }

}
