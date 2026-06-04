package com.omnibooking.worker;

import com.omnibooking.services.core.OutboxService;

import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
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
      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("outbox-worker", CheckInStatus.IN_PROGRESS));

      try {
         outboxService.processOutbox();
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "outbox-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Error in OutboxWorker", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "outbox-worker", CheckInStatus.ERROR));
      }
   }

   @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
   public void purgeOldEvents() {
      log.info("Starting scheduled Outbox events cleanup job...");
      try {
         outboxService.purgeOldOutboxEvents();
         log.info("Scheduled Outbox cleanup job finished successfully.");
      } catch (Exception e) {
         log.error("Error running Outbox events cleanup job", e);
      }
   }

}
