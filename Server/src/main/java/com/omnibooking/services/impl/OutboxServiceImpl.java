package com.omnibooking.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.model.OutboxEvent;
import com.omnibooking.repository.OutboxEventRepository;
import com.omnibooking.services.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                  processOutbox();
               }
            });
         }
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize outbox event payload", e);
         throw new RuntimeException("Serialization error", e);
      }
   }

   @Override
   @Transactional
   public void processOutbox() {
      // 1. Thread-safety check to avoid concurrent runs in the same instance
      if (!isProcessing.compareAndSet(false, true)) {
         return;
      }

      try {
         // 2. Fetch using FOR UPDATE SKIP LOCKED for multi-instance safety
         List<OutboxEvent> events = outboxEventRepository.findUnprocessedForUpdate(PageRequest.of(0, 50));
         if (events.isEmpty())
            return;

      log.info("Processing {} outbox events", events.size());
      for (OutboxEvent event : events) {
         try {
            String topic = getTopicForEvent(event.getEventType());

            // Deserialize back to original class to ensure correct Kafka
            // headers/serialization
            Class<?> clazz = Class.forName(event.getPayloadClass());
            Object payload = objectMapper.readValue(event.getPayload(), clazz);

            kafkaTemplate.send(topic, payload).whenComplete((result, ex) -> {
               if (ex == null) {
                  log.info("Successfully pushed outbox event {} to Kafka topic {}", event.getId(), topic);
               } else {
                  log.error("Failed to push outbox event {} to Kafka", event.getId(), ex);
               }
            });

            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
         } catch (Exception e) {
            log.error("Failed to process outbox event: {}", event.getId(), e);
         }
      }
   } finally {
      isProcessing.set(false);
   }
}

   private String getTopicForEvent(String eventType) {
      if (eventType.contains("MAIL") || eventType.contains("REGISTERED") || eventType.contains("PASSWORD")) {
         return com.omnibooking.config.KafkaConfig.MAIL_TOPIC;
      }
      if (eventType.contains("MEDIA")) {
         return com.omnibooking.config.KafkaConfig.MEDIA_TOPIC;
      }
      return "omnibooking-default-topic";
   }
}
