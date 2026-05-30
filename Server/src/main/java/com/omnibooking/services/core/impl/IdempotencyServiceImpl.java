package com.omnibooking.services.core.impl;

import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.repository.ProcessedEventRepository;
import com.omnibooking.services.core.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

   private final ProcessedEventRepository processedEventRepository;

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public boolean claimEvent(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return false;
      }
      
      Optional<ProcessedEvent> existingOpt = processedEventRepository.findByIdForWrite(eventId, consumerGroup);
      
      if (existingOpt.isEmpty()) {
         try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                  .eventId(eventId)
                  .consumerGroup(consumerGroup)
                  .processedAt(Instant.now())
                  .updatedAt(Instant.now())
                  .status("PROCESSING")
                  .build();
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully claimed new event: eventId={}, consumerGroup={}", eventId, consumerGroup);
            return true;
         } catch (DataIntegrityViolationException e) {
            log.warn("Race condition: Duplicate event claim detected: eventId={}, consumerGroup={}", 
                  eventId, consumerGroup);
            return false;
         }
      }

      ProcessedEvent existing = existingOpt.get();
      if ("FAILED".equals(existing.getStatus())) {
         existing.setStatus("PROCESSING");
         existing.setUpdatedAt(Instant.now());
         processedEventRepository.saveAndFlush(existing);
         log.info("Retrying previously failed event: eventId={}, consumerGroup={}", eventId, consumerGroup);
         return true;
      }

      log.warn("Duplicate claim rejected (already COMPLETED or PROCESSING): eventId={}, consumerGroup={}, currentStatus={}", 
            eventId, consumerGroup, existing.getStatus());
      return false;
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void completeEvent(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return;
      }
      ProcessedEvent.ProcessedEventId id = new ProcessedEvent.ProcessedEventId(eventId, consumerGroup);
      processedEventRepository.findById(id).ifPresent(processedEvent -> {
         processedEvent.setStatus("COMPLETED");
         processedEvent.setUpdatedAt(Instant.now());
         processedEventRepository.saveAndFlush(processedEvent);
         log.info("Successfully marked event as COMPLETED: eventId={}, consumerGroup={}", eventId, consumerGroup);
      });
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void releaseClaim(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return;
      }
      try {
         ProcessedEvent.ProcessedEventId id = new ProcessedEvent.ProcessedEventId(eventId, consumerGroup);
         processedEventRepository.findById(id).ifPresent(processedEvent -> {
            processedEvent.setStatus("FAILED");
            processedEvent.setUpdatedAt(Instant.now());
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully released claim (marked as FAILED): eventId={}, consumerGroup={}", eventId, consumerGroup);
         });
      } catch (Exception e) {
         log.error("Failed to release claim: eventId={}, consumerGroup={}", eventId, consumerGroup, e);
      }
   }

}
