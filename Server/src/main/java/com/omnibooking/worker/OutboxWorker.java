package com.omnibooking.worker;

import com.omnibooking.services.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

   private final OutboxService outboxService;

   @Scheduled(fixedDelay = 5000) // Every 5 seconds
   public void run() {
      try {
         outboxService.processOutbox();
      } catch (Exception e) {
         log.error("Error in OutboxWorker", e);
      }
   }
}
