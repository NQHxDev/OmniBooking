package com.omnibooking.consumer;

import com.omnibooking.constant.EventConstants;
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
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCDCConsumer {

   private final VerificationService verificationService;

   private final MailService mailService;

   private final OutboxService outboxService;

   private final IdempotencyService idempotencyService;

   private final MeterRegistry meterRegistry;

   private final TransactionTemplate transactionTemplate;

   @KafkaListener(topics = "omnibooking-user-cdc", groupId = "omnibooking-cdc-group")
   public void handleUserCreated(UserCreatedEvent event) {
      String consumerGroup = "omnibooking-cdc-group";
      if (event.getEventId() != null) {
         boolean claimed = idempotencyService.claimEvent(event.getEventId(), consumerGroup);
         if (!claimed) {
            log.info("Duplicate completed event skipped: eventId={}, userId={}",
                  event.getEventId(), event.getUserId());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            return;
         }
      }

      log.info("CDC Event received: New user created with ID: {} (eventId: {})",
            event.getUserId(), event.getEventId());

      try {
         // Create Verification Token (Redis network call - OUTSIDE database transaction)
         String token = verificationService.createVerificationToken(event.getUserId());

         // Build Email Event (CPU operation - OUTSIDE database transaction)
         EmailEvent emailEvent = mailService.buildVerificationEmailEvent(
               event.getEmail(),
               event.getFullName(),
               token);

         // Execute DB writes in a transaction block
         transactionTemplate.executeWithoutResult(status -> {
            // Send to Outbox (requires active transaction)
            outboxService.saveEvent(
                  event.getUserId(),
                  "USER",
                  EventConstants.USER_REGISTERED_MAIL,
                  emailEvent);

            if (event.getEventId() != null) {
               idempotencyService.completeEvent(event.getEventId(), consumerGroup);
            }
         });

         log.info("CDC Side-effects completed for user: {}", event.getEmail());
      } catch (Exception e) {
         log.error("Failed to process CDC side-effects for user: {}", event.getUserId(), e);
         if (event.getEventId() != null) {
            try {
               idempotencyService.releaseClaim(event.getEventId(), consumerGroup);
            } catch (Exception releaseEx) {
               log.error("Failed to release claim for event: {}", event.getEventId(), releaseEx);
            }
         }

         throw e;
      }
   }

}
