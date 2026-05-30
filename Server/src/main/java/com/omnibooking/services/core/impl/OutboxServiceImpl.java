package com.omnibooking.services.core.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.KafkaConfig;
import com.omnibooking.model.OutboxEvent;
import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.repository.OutboxEventRepository;
import com.omnibooking.services.core.OutboxEventRegistry;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.core.EventMetadataProvider;
import com.omnibooking.services.core.EventEnvelope;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.scheduling.annotation.Async;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnibooking.services.core.EventUpcaster;
import com.omnibooking.services.core.DistributedRateLimiter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

   private final OutboxEventRepository outboxEventRepository;
   private final ObjectMapper objectMapper;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final MeterRegistry meterRegistry;
   private final EventUpcaster eventUpcaster;
   private final DistributedRateLimiter distributedRateLimiter;
   private final List<EventMetadataProvider> metadataProviders;
   private final AtomicBoolean isProcessing = new AtomicBoolean(false);
   private final AtomicBoolean wakeUpPending = new AtomicBoolean(false);
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

         // Set eventId on DTO event payloads using matched EventMetadataProvider and wrap in EventEnvelope
         EventMetadataProvider matchedProvider = metadataProviders.stream()
               .filter(p -> p.supports(payload))
               .findFirst()
               .orElse(null);

         EventEnvelope envelope = new EventEnvelope(eventId, eventType, payload, matchedProvider);

         String jsonPayload = objectMapper.writeValueAsString(envelope.getPayload());
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
                  if (wakeUpPending.compareAndSet(false, true)) {
                     log.info("Transaction committed, triggering outbox processing asynchronously.");
                     if (self != null) {
                        self.processOutboxAsync();
                     }
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
         boolean hasMore = true;
         int batchCount = 0;
         while (hasMore && batchCount < 20) {
            // Reset wakeUpPending before querying the DB.
            // If any transaction commits after this point, it will see wakeUpPending as false
            // and successfully trigger another asynchronous run.
            wakeUpPending.set(false);

            List<OutboxEvent> events = self.lockAndFetchEventsToProcess(PageRequest.of(0, 50));
            if (events.isEmpty()) {
               hasMore = false;
            } else {
               batchCount++;
               log.info("Processing {} outbox events (batch {})", events.size(), batchCount);
               for (OutboxEvent event : events) {
                  if (event.getRetryCount() > 0) {
                     // Apply rate limit on retry attempts: max 2 retries per second dynamically
                     boolean allowed = distributedRateLimiter.isAllowed("outbox:retry", 10, 2);
                     if (!allowed) {
                        log.warn("Throttling retry for outbox event: {} to prevent retry storm.", event.getId());
                        self.rescheduleRetry(event.getId());
                        continue;
                     }
                  }
                  if (self != null) {
                     try {
                        self.processSingleEvent(event);
                     } catch (Exception e) {
                        log.error("Failed to process outbox event: {}", event.getId(), e);
                     }
                  }
               }
            }
         }
      } finally {
         isProcessing.set(false);
         wakeUpPending.set(false); // Safeguard
      }
   }

   @Async
   @Override
   public void processOutboxAsync() {
      processOutbox();
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

         // Deserialize and dynamically upcast JSON payload before mapping
         JsonNode payloadNode = objectMapper.readTree(event.getPayload());
         int currentVersion = event.getEventVersion() != null ? event.getEventVersion() : 1;
         int targetVersion = getTargetVersionForEvent(event.getEventType());
         
         JsonNode upcastedNode = eventUpcaster.upcast(event.getEventType(), payloadNode, currentVersion, targetVersion);
         
         Class<?> clazz = OutboxEventRegistry.getEventClass(event.getEventType());
         Object payload = objectMapper.treeToValue(upcastedNode, clazz);

         // Send to Kafka and WAIT for confirmation. Use aggregateId as partition key for ordering.
         kafkaTemplate.send(topic, event.getAggregateId().toString(), payload).get(5, java.util.concurrent.TimeUnit.SECONDS);
         log.info("Successfully pushed outbox event {} to Kafka topic {} with partition key {}", 
               event.getId(), topic, event.getAggregateId());

         // Mark as processed (within transaction)
         event.setStatus(OutboxStatus.PROCESSED);
         outboxEventRepository.saveAndFlush(event);
      } catch (Exception e) {
         log.error("Error processing single outbox event: {}", event.getId(), e);
         meterRegistry.counter("omnibooking.kafka.publish.failure").increment();
         handleFailure(event, e);
      }
   }

   private int getTargetVersionForEvent(String eventType) {
      if ("USER_REGISTERED_MAIL".equals(eventType)) {
         return 2;
      }
      return 1;
   }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsProcessed(UUID eventId) {
       outboxEventRepository.findById(eventId).ifPresent(event -> {
          event.setStatus(OutboxStatus.PROCESSED);
          outboxEventRepository.saveAndFlush(event);
       });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rescheduleRetry(UUID eventId) {
       outboxEventRepository.findById(eventId).ifPresent(event -> {
          event.setStatus(OutboxStatus.PENDING);
          event.setNextRetryAt(Instant.now().plus(Duration.ofSeconds(30)));
          outboxEventRepository.saveAndFlush(event);
          log.info("Rescheduled outbox event {} for retry in 30 seconds due to throttling", eventId);
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
      if (eventType.contains("PROPERTY_SYNC")) {
         return "omnibooking-property-sync";
      }
      return "omnibooking-default-topic";
   }

   @Override
   @Transactional
   public void purgeOldOutboxEvents() {
      Instant threshold = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
      int deleted = outboxEventRepository.deleteProcessedEventsBefore(threshold);
      if (deleted > 0) {
         log.info("Purged {} processed outbox events older than 30 days", deleted);
      }
   }

}
