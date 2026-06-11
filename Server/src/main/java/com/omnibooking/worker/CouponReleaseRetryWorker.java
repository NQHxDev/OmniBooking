package com.omnibooking.worker;

import com.omnibooking.services.pricing.CouponReleaseRetryService;
import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponReleaseRetryWorker {

   private final CouponReleaseRetryService couponReleaseRetryService;

   @Scheduled(fixedDelay = 30000) // Every 30 seconds
   @SchedulerLock(name = "couponReleaseRetry", lockAtMostFor = "PT25S", lockAtLeastFor = "PT5S")
   public void processRetries() {
      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("coupon-release-retry-worker", CheckInStatus.IN_PROGRESS));

      try {
         couponReleaseRetryService.processPendingRetries();
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "coupon-release-retry-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Error in CouponReleaseRetryWorker while processing retries", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "coupon-release-retry-worker", CheckInStatus.ERROR));
      }
   }

   @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
   @SchedulerLock(name = "couponReleaseRetryCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
   public void cleanup() {
      log.info("Starting scheduled Coupon Release Retry cleanup job...");
      try {
         couponReleaseRetryService.purgeOldRetryRecords();
         log.info("Scheduled Coupon Release Retry cleanup job finished successfully.");
      } catch (Exception e) {
         log.error("Error running Coupon Release Retry cleanup job", e);
      }
   }

}
