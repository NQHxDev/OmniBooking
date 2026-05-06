package com.omnibooking.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserEventListener {

   @Async
   @EventListener
   public void handleUserRegisteredEvent(UserRegisteredEvent event) {
      log.info("Handling user registered event for user: {}", event.getUser().getUsername());
      // Logic gửi email, thông báo, v.v...
      // Ví dụ: mailService.sendWelcomeEmail(event.getUser());
   }

}
