package com.omnibooking.services.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.RegistrationDlt;
import com.omnibooking.model.RegistrationDltAudit;
import com.omnibooking.model.enums.RegistrationDltStatus;
import com.omnibooking.repository.registration.RegistrationDltAuditRepository;
import com.omnibooking.repository.registration.RegistrationDltRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationDltReplayService {

   private final RegistrationDltRepository dltRepository;
   private final RegistrationDltAuditRepository auditRepository;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final ObjectMapper objectMapper;
   private final MeterRegistry meterRegistry;

   @Value("${omnibooking.kafka.registration.topic-name:registration-request-topic}")
   private String mainTopicName;

   @Transactional
   public boolean replayRequest(UUID requestId, String initiator) {
      RegistrationDlt dltRecord = dltRepository.findById(requestId).orElse(null);
      if (dltRecord == null) {
         log.warn("DLT record not found for requestId: {}", requestId);
         return false;
      }

      log.info("Initiating DLT replay for requestId: {} by {}", requestId, initiator);
      String result = "SUCCESS";
      String errorMessage = null;

      try {
         RegistrationMessage message = objectMapper.readValue(dltRecord.getPayload(), RegistrationMessage.class);

         // Send back to main topic, using email as partition key
         kafkaTemplate.send(mainTopicName, message.getEmail(), message).get(); // Block to ensure send succeeds

         // Update DLT record status
         dltRecord.setStatus(RegistrationDltStatus.REPLAYED);
         dltRecord.setLastReplayedAt(Instant.now());
         dltRepository.save(dltRecord);

         meterRegistry.counter("omnibooking.dlt.replayed").increment();
         log.info("Successfully replayed DLT record for requestId: {}", requestId);

      } catch (Exception e) {
         log.error("Failed to replay DLT record for requestId: {}", requestId, e);
         result = "FAILED";
         errorMessage = e.getMessage();

         dltRecord.setStatus(RegistrationDltStatus.FAILED);
         dltRepository.save(dltRecord);

         meterRegistry.counter("omnibooking.dlt.failed").increment();
      }

      // Write audit trail
      RegistrationDltAudit audit = RegistrationDltAudit.builder()
            .id(UUID.randomUUID())
            .requestId(requestId)
            .replayedBy(initiator)
            .replayedAt(Instant.now())
            .originalError(dltRecord.getOriginalError())
            .replayResult(result)
            .errorMessage(errorMessage)
            .build();
      auditRepository.save(audit);

      return "SUCCESS".equals(result);
   }

   @Transactional
   public int replayBatch(List<UUID> requestIds, String initiator) {
      int successCount = 0;
      for (UUID reqId : requestIds) {
         if (replayRequest(reqId, initiator)) {
            successCount++;
         }
      }
      return successCount;
   }

   @Transactional
   public int replayPartition(int partitionId, String initiator) {
      List<RegistrationDlt> pendingRecords = dltRepository.findByPartitionIdAndStatus(partitionId, RegistrationDltStatus.PENDING);
      List<RegistrationDlt> failedRecords = dltRepository.findByPartitionIdAndStatus(partitionId, RegistrationDltStatus.FAILED);
      
      int successCount = 0;
      for (RegistrationDlt record : pendingRecords) {
         if (replayRequest(record.getRequestId(), initiator)) successCount++;
      }
      for (RegistrationDlt record : failedRecords) {
         if (replayRequest(record.getRequestId(), initiator)) successCount++;
      }
      return successCount;
   }

   @Transactional
   public int replayAll(String initiator) {
      List<RegistrationDlt> pendingRecords = dltRepository.findByStatus(RegistrationDltStatus.PENDING);
      List<RegistrationDlt> failedRecords = dltRepository.findByStatus(RegistrationDltStatus.FAILED);

      int successCount = 0;
      for (RegistrationDlt record : pendingRecords) {
         if (replayRequest(record.getRequestId(), initiator)) successCount++;
      }
      for (RegistrationDlt record : failedRecords) {
         if (replayRequest(record.getRequestId(), initiator)) successCount++;
      }
      return successCount;
   }

}
