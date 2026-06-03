package com.omnibooking.consumer;

import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.dto.event.UserCreatedEvent;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.core.IdempotencyService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omnibooking.services.core.LeaseRenewer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCDCConsumer {

   private final VerificationService verificationService;

   private final MailService mailService;

   private final OutboxService outboxService;

   private final IdempotencyService idempotencyService;

   private final MeterRegistry meterRegistry;

   @Transactional
   @KafkaListener(topics = "omnibooking-user-cdc", groupId = "omnibooking-cdc-group")
   public void handleUserCreated(UserCreatedEvent event) {
      String consumerGroup = "omnibooking-cdc-group";
      if (event.getEventId() != null) {
         boolean claimed = idempotencyService.claimEvent(event.getEventId(), consumerGroup);
         if (!claimed) {
            log.warn("[Kafka Consumer] Duplicate UserCreated CDC event detected and skipped: eventId={}, userId={}",
                  event.getEventId(), event.getUserId());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            meterRegistry.counter("omnibooking.kafka.consumer.skipped").increment();
            return;
         }
      }

      log.info("CDC Event received: New user created with ID: {} (eventId: {})",
            event.getUserId(), event.getEventId());

      try (LeaseRenewer ignored = new LeaseRenewer(idempotencyService, event.getEventId(), consumerGroup)) {
         // Create Verification Token
         String token = verificationService.createVerificationToken(event.getUserId());

         // Build Email Event
         EmailEvent emailEvent = mailService.buildVerificationEmailEvent(
               event.getEmail(),
               event.getFullName(),
               token);

         // Send to Outbox
         outboxService.saveEvent(
               event.getUserId(),
               "USER",
               "USER_REGISTERED_MAIL",
               emailEvent);

         log.info("CDC Side-effects completed for user: {}", event.getEmail());

         if (event.getEventId() != null) {
            idempotencyService.completeEvent(event.getEventId(), consumerGroup);
         }
      } catch (Exception e) {
         log.error("Failed to process CDC side-effects for user: {}", event.getUserId(), e);
         if (event.getEventId() != null) {
            try {
               idempotencyService.releaseClaim(event.getEventId(), consumerGroup);
            } catch (Exception releaseEx) {
               log.error("Failed to release claim for event: {}", event.getEventId(), releaseEx);
            }
         }
         // Kafka will retry based on configuration
         throw e;
      }
   }

}
