package com.omnibooking.event;

import com.omnibooking.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

   private final MailService mailService;
   private final com.omnibooking.services.VerificationService verificationService;

   @Async
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void handleUserRegisteredEvent(UserRegisteredEvent event) {
      var user = event.getUser();
      String fullName = event.getFullName();
      log.info("Handling user registered event for user: {}", user.getEmail());

      // Tạo token thật lưu vào Redis với thời hạn 2h
      String token = verificationService.createVerificationToken(user.getId());

      try {
         mailService.sendVerificationEmail(user.getEmail(), fullName, token);
         log.info("Verification email event pushed to Kafka for: {}", user.getEmail());
      } catch (Exception e) {
         log.error("Failed to push email event to Kafka for: {}. Error: {}", user.getEmail(), e.getMessage());
      }
   }
}
