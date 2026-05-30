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
      try {
         ProcessedEvent processedEvent = ProcessedEvent.builder()
               .eventId(eventId)
               .consumerGroup(consumerGroup)
               .processedAt(Instant.now())
               .build();
         processedEventRepository.saveAndFlush(processedEvent);
         log.info("Successfully claimed event: eventId={}, consumerGroup={}", eventId, consumerGroup);
         return true;
      } catch (DataIntegrityViolationException e) {
         log.warn("Duplicate event claim detected (already claimed or processed): eventId={}, consumerGroup={}", 
               eventId, consumerGroup);
         return false;
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void releaseClaim(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return;
      }
      try {
         ProcessedEvent.ProcessedEventId id = new ProcessedEvent.ProcessedEventId(eventId, consumerGroup);
         processedEventRepository.deleteById(id);
         processedEventRepository.flush();
         log.info("Successfully released claim: eventId={}, consumerGroup={}", eventId, consumerGroup);
      } catch (Exception e) {
         log.error("Failed to release claim: eventId={}, consumerGroup={}", eventId, consumerGroup, e);
      }
   }

}
