package com.omnibooking.services.core.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.KafkaConfig;
import com.omnibooking.model.OutboxEvent;
import com.omnibooking.repository.OutboxEventRepository;
import com.omnibooking.services.core.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
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
         String jsonPayload = objectMapper.writeValueAsString(payload);
         OutboxEvent event = OutboxEvent.builder()
               .aggregateId(aggregateId)
               .aggregateType(aggregateType)
               .eventType(eventType)
               .payload(jsonPayload)
               .payloadClass(payload.getClass().getName())
               .build();
         outboxEventRepository.save(event);
         log.info("Saved outbox event: {} for aggregate: {}", eventType, aggregateId);

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
      // 1. Thread-safety check to avoid concurrent runs in the same instance
      if (!isProcessing.compareAndSet(false, true)) {
         return;
      }

      try {
         // 2. Fetch unprocessed events
         List<OutboxEvent> events = outboxEventRepository.findUnprocessedForUpdate(PageRequest.of(0, 50));
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
   public void processSingleEvent(OutboxEvent event) {
      try {
         String topic = getTopicForEvent(event.getEventType());

         // Deserialize back to original class
         Class<?> clazz = Class.forName(event.getPayloadClass());
         Object payload = objectMapper.readValue(event.getPayload(), clazz);

         // Send to Kafka and WAIT for confirmation
         try {
            kafkaTemplate.send(topic, payload).get(5, java.util.concurrent.TimeUnit.SECONDS);
            log.info("Successfully pushed outbox event {} to Kafka topic {}", event.getId(), topic);
            
            // ONLY mark as processed if Kafka send was successful
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.saveAndFlush(event);
         } catch (Exception ex) {
            log.error("Failed to push outbox event {} to Kafka", event.getId(), ex);
            throw new RuntimeException("Kafka send failed", ex);
         }
      } catch (Exception e) {
         throw new RuntimeException("Error processing single outbox event", e);
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void markAsProcessed(UUID eventId) {
      outboxEventRepository.findById(eventId).ifPresent(event -> {
         event.setProcessed(true);
         event.setProcessedAt(Instant.now());
         outboxEventRepository.save(event);
      });
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
