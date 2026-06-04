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

import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
      Instant now = Instant.now();

      if (existingOpt.isEmpty()) {
         try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                  .eventId(eventId)
                  .consumerGroup(consumerGroup)
                  .processedAt(now)
                  .updatedAt(now)
                  .leaseUntil(now.plus(Duration.ofMinutes(5)))
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
      boolean isLeaseExpired = existing.getLeaseUntil() != null && existing.getLeaseUntil().isBefore(now);

      if ("FAILED".equals(existing.getStatus()) || ("PROCESSING".equals(existing.getStatus()) && isLeaseExpired)) {
         existing.setStatus("PROCESSING");
         existing.setUpdatedAt(now);
         existing.setLeaseUntil(now.plus(Duration.ofMinutes(5)));
         processedEventRepository.saveAndFlush(existing);
         log.info("Retrying previously failed or expired/stuck event: eventId={}, consumerGroup={}", eventId,
               consumerGroup);

         return true;
      }

      if ("PROCESSING".equals(existing.getStatus())) {
         log.warn("Event is currently being processed by another consumer instance: eventId={}, consumerGroup={}",
               eventId, consumerGroup);
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      }

      log.info("Duplicate event detected (already COMPLETED): eventId={}, consumerGroup={}", eventId, consumerGroup);
      return false;
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRED)
   public void completeEvent(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return;
      }
      ProcessedEvent.ProcessedEventId id = new ProcessedEvent.ProcessedEventId(eventId, consumerGroup);
      processedEventRepository.findById(id).ifPresent(processedEvent -> {
         processedEvent.setStatus("COMPLETED");
         processedEvent.setUpdatedAt(Instant.now());
         processedEvent.setLeaseUntil(Instant.now().plus(365, ChronoUnit.DAYS));
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
            processedEvent.setLeaseUntil(Instant.now());
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully released claim (marked as FAILED): eventId={}, consumerGroup={}", eventId,
                  consumerGroup);
         });
      } catch (Exception e) {
         log.error("Failed to release claim: eventId={}, consumerGroup={}", eventId, consumerGroup, e);
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void renewLease(UUID eventId, String consumerGroup, Duration extension) {
      if (eventId == null) {
         return;
      }
      ProcessedEvent.ProcessedEventId id = new ProcessedEvent.ProcessedEventId(eventId, consumerGroup);
      processedEventRepository.findById(id).ifPresent(processedEvent -> {
         if ("PROCESSING".equals(processedEvent.getStatus())) {
            processedEvent.setLeaseUntil(Instant.now().plus(extension));
            processedEvent.setUpdatedAt(Instant.now());
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully renewed lease for event: eventId={}, consumerGroup={}, leaseUntil={}",
                  eventId, consumerGroup, processedEvent.getLeaseUntil());
         }
      });
   }

}
