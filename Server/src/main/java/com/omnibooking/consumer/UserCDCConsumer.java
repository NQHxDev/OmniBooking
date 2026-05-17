package com.omnibooking.consumer;

import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.dto.event.UserCreatedEvent;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.services.core.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCDCConsumer {

   private final VerificationService verificationService;

   private final MailService mailService;

   private final OutboxService outboxService;

   @Transactional
   @KafkaListener(topics = "omnibooking-user-cdc", groupId = "omnibooking-cdc-group")
   public void handleUserCreated(UserCreatedEvent event) {
      log.info("CDC Event received: New user created with ID: {}", event.getUserId());

      try {
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
      } catch (Exception e) {
         log.error("Failed to process CDC side-effects for user: {}", event.getUserId(), e);
         // Kafka will retry based on configuration
         throw e;
      }
   }
}
