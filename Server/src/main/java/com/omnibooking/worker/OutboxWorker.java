package com.omnibooking.worker;

import com.omnibooking.services.core.OutboxService;
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
      io.sentry.protocol.SentryId checkInId = io.sentry.Sentry.captureCheckIn(
            new io.sentry.CheckIn("outbox-worker", io.sentry.CheckInStatus.IN_PROGRESS)
      );

      try {
         outboxService.processOutbox();
         io.sentry.Sentry.captureCheckIn(
               new io.sentry.CheckIn(checkInId, "outbox-worker", io.sentry.CheckInStatus.OK)
         );
      } catch (Exception e) {
         log.error("Error in OutboxWorker", e);
         io.sentry.Sentry.captureCheckIn(
               new io.sentry.CheckIn(checkInId, "outbox-worker", io.sentry.CheckInStatus.ERROR)
         );
      }
   }

}

