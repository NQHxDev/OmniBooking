package com.omnibooking.services.core.impl;

import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.repository.ProcessedEventRepository;
import com.omnibooking.services.core.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

   private final ProcessedEventRepository processedEventRepository;

   @Override
   @Transactional(readOnly = true)
   public boolean isProcessed(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return false;
      }
      return processedEventRepository.existsById(new ProcessedEvent.ProcessedEventId(eventId, consumerGroup));
   }

   @Override
   @Transactional
   public void markProcessed(UUID eventId, String consumerGroup) {
      if (eventId == null) {
         return;
      }
      ProcessedEvent processedEvent = ProcessedEvent.builder()
            .eventId(eventId)
            .consumerGroup(consumerGroup)
            .processedAt(Instant.now())
            .build();
      processedEventRepository.saveAndFlush(processedEvent);
      log.info("Event marked as processed: eventId={}, consumerGroup={}", eventId, consumerGroup);
   }

}
