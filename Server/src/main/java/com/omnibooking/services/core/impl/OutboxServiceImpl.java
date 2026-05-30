package com.omnibooking.services.core.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.KafkaConfig;
import com.omnibooking.model.OutboxEvent;
import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.repository.OutboxEventRepository;
import com.omnibooking.services.core.OutboxEventRegistry;
import com.omnibooking.services.core.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

   private final OutboxEventRepository outboxEventRepository;
   private final ObjectMapper objectMapper;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final AtomicBoolean isProcessing = new AtomicBoolean(false);
   private OutboxService self;

   @Lazy
   @Autowired
   public void setSelf(OutboxService self) {
      this.self = self;
   }

   @Override
   @Transactional(propagation = Propagation.MANDATORY)
   public void saveEvent(UUID aggregateId, String aggregateType, String eventType, Object payload) {
      try {
         // Generate eventId beforehand using time-ordered UUID v7
         UUID eventId = com.github.f4b6a3.uuid.UuidCreator.getTimeOrderedEpoch();

         // Set eventId on DTO event payloads if they match known classes
         if (payload instanceof com.omnibooking.dto.event.EmailEvent) {
            ((com.omnibooking.dto.event.EmailEvent) payload).setEventId(eventId);
         } else if (payload instanceof com.omnibooking.dto.event.MediaUploadEvent) {
            ((com.omnibooking.dto.event.MediaUploadEvent) payload).setEventId(eventId);
         } else if (payload instanceof com.omnibooking.dto.event.PropertySyncEvent) {
            ((com.omnibooking.dto.event.PropertySyncEvent) payload).setEventId(eventId);
         }

         String jsonPayload = objectMapper.writeValueAsString(payload);
         OutboxEvent event = OutboxEvent.builder()
               .id(eventId) // Align OutboxEvent entity ID with eventId
               .aggregateId(aggregateId)
               .aggregateType(aggregateType)
               .eventType(eventType)
               .payload(jsonPayload)
               .build();
         outboxEventRepository.save(event);
         log.info("Saved outbox event: {} for aggregate: {} (eventId: {})", eventType, aggregateId, eventId);

         // Register synchronization to wake up worker AFTER commit
         if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
               @Override
               public void afterCommit() {
                  log.info("Transaction committed, triggering outbox processing immediately.");
                  if (self != null) {
                     self.processOutbox();
                  }
               }
            });
         }
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize outbox event payload", e);
         throw new RuntimeException("Serialization error", e);
      }
   }

   @Override
   public void processOutbox() {
      // 1. Thread-safety check inside JVM (still good as an optimization)
      if (!isProcessing.compareAndSet(false, true)) {
         return;
      }

      try {
         // 2. Fetch & lock events in a short transaction
         List<OutboxEvent> events = self.lockAndFetchEventsToProcess(PageRequest.of(0, 50));
         if (events.isEmpty()) return;

         log.info("Processing {} outbox events", events.size());
         for (OutboxEvent event : events) {
            if (self != null) {
               try {
                  self.processSingleEvent(event);
               } catch (Exception e) {
                  log.error("Failed to process outbox event: {}", event.getId(), e);
               }
            }
         }
      } finally {
         isProcessing.set(false);
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public List<OutboxEvent> lockAndFetchEventsToProcess(Pageable pageable) {
      Instant now = Instant.now();
      List<OutboxEvent> events = outboxEventRepository.findEventsToProcess(now, pageable);
      if (events.isEmpty()) {
         return List.of();
      }
      Instant lockExpiry = now.plus(Duration.ofMinutes(5));
      for (OutboxEvent event : events) {
         event.setStatus(OutboxStatus.PROCESSING);
         event.setNextRetryAt(lockExpiry);
      }
      return outboxEventRepository.saveAllAndFlush(events);
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void processSingleEvent(OutboxEvent event) {
      try {
         String topic = getTopicForEvent(event.getEventType());

         // Deserialize back to original class using registry instead of payloadClass reflection
         Class<?> clazz = OutboxEventRegistry.getEventClass(event.getEventType());
         Object payload = objectMapper.readValue(event.getPayload(), clazz);

         // Send to Kafka and WAIT for confirmation
         kafkaTemplate.send(topic, payload).get(5, java.util.concurrent.TimeUnit.SECONDS);
         log.info("Successfully pushed outbox event {} to Kafka topic {}", event.getId(), topic);

         // Mark as processed (within transaction)
         event.setStatus(OutboxStatus.PROCESSED);
         outboxEventRepository.saveAndFlush(event);
      } catch (Exception e) {
         log.error("Error processing single outbox event: {}", event.getId(), e);
         handleFailure(event, e);
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void markAsProcessed(UUID eventId) {
      outboxEventRepository.findById(eventId).ifPresent(event -> {
         event.setStatus(OutboxStatus.PROCESSED);
         outboxEventRepository.saveAndFlush(event);
      });
   }

   private void handleFailure(OutboxEvent event, Exception ex) {
      int nextRetryCount = event.getRetryCount() + 1;
      event.setRetryCount(nextRetryCount);
      event.setLastError(getStackTraceAsString(ex));

      if (nextRetryCount >= 5) { // Max retries = 5
         event.setStatus(OutboxStatus.DEAD);
         event.setNextRetryAt(Instant.now());
         log.warn("Outbox event {} reached max retries. Marked as DEAD.", event.getId());
      } else {
         event.setStatus(OutboxStatus.PENDING);
         long backoffMinutes = getBackoffMinutes(nextRetryCount);
         event.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(backoffMinutes)));
         log.info("Outbox event {} failed. Retrying in {} minutes (attempt {}/5).", 
                  event.getId(), backoffMinutes, nextRetryCount);
      }
      outboxEventRepository.saveAndFlush(event);
   }

   private long getBackoffMinutes(int retryCount) {
      return switch (retryCount) {
         case 1 -> 1;
         case 2 -> 5;
         case 3 -> 15;
         case 4 -> 60;
         default -> 60;
      };
   }

   private String getStackTraceAsString(Exception e) {
      java.io.StringWriter sw = new java.io.StringWriter();
      java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      e.printStackTrace(pw);
      return sw.toString();
   }

   private String getTopicForEvent(String eventType) {
      if (eventType.contains("MAIL") || eventType.contains("REGISTERED") || 
          eventType.contains("PASSWORD") || eventType.contains("VERIFICATION") ||
          eventType.contains("OTP")) {
         return KafkaConfig.MAIL_TOPIC;
      }
      if (eventType.contains("MEDIA")) {
         return KafkaConfig.MEDIA_TOPIC;
      }
      return "omnibooking-default-topic";
   }

}
