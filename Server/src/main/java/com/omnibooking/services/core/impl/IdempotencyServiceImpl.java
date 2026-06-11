package com.omnibooking.services.core.impl;

import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.model.enums.IdempotencyStatus;
import com.omnibooking.repository.infra.ProcessedEventRepository;
import com.omnibooking.services.core.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;

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

   private final MeterRegistry meterRegistry;

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
                  .status(IdempotencyStatus.PROCESSING)
                  .build();
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully claimed new event: eventId={}, consumerGroup={}", eventId, consumerGroup);

            return true;
         } catch (DataIntegrityViolationException e) {
            log.warn("Race condition: Duplicate event claim detected: eventId={}, consumerGroup={}",
                  eventId, consumerGroup);
            meterRegistry.counter("omnibooking.event.duplicate").increment();
            return false;
         }
      }

      ProcessedEvent existing = existingOpt.get();
      boolean isLeaseExpired = existing.getLeaseUntil() != null && existing.getLeaseUntil().isBefore(now);

      if (existing.getStatus() == IdempotencyStatus.FAILED || (existing.getStatus() == IdempotencyStatus.PROCESSING && isLeaseExpired)) {
         if (existing.getStatus() == IdempotencyStatus.PROCESSING && isLeaseExpired) {
            meterRegistry.counter("omnibooking.lease.takeover").increment();
         }
         existing.setStatus(IdempotencyStatus.PROCESSING);
         existing.setUpdatedAt(now);
         existing.setLeaseUntil(now.plus(Duration.ofMinutes(5)));
         processedEventRepository.saveAndFlush(existing);
         log.info("Retrying previously failed or expired/stuck event: eventId={}, consumerGroup={}", eventId,
               consumerGroup);

         return true;
      }

      if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
         log.warn("Event is currently being processed by another consumer instance: eventId={}, consumerGroup={}",
               eventId, consumerGroup);
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      }

      log.info("Duplicate event detected (already COMPLETED): eventId={}, consumerGroup={}", eventId, consumerGroup);
      meterRegistry.counter("omnibooking.event.duplicate").increment();
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
         processedEvent.setStatus(IdempotencyStatus.COMPLETED);
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
            processedEvent.setStatus(IdempotencyStatus.FAILED);
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
      Instant now = Instant.now();
      Instant newLeaseUntil = now.plus(extension);
      int updated = processedEventRepository.renewLeaseOpt(eventId, consumerGroup, newLeaseUntil, now);
      if (updated > 0) {
         log.info("Successfully renewed lease for event: eventId={}, consumerGroup={}, leaseUntil={}",
               eventId, consumerGroup, newLeaseUntil);
      } else {
         log.warn(
               "Failed to renew lease for event (not in PROCESSING or lease already expired): eventId={}, consumerGroup={}",
               eventId, consumerGroup);
      }
   }

}
