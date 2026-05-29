package com.omnibooking.worker;

import com.omnibooking.services.core.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyWorker {

   private final CurrencyService currencyService;

   /**
    * Updates exchange rates every 4 hours!
    */
   @Scheduled(cron = "0 0 0,4,8,12,16,20 * * *")
   public void updateExchangeRates() {
      log.info("Starting scheduled exchange rate update...");

      io.sentry.protocol.SentryId checkInId = io.sentry.Sentry.captureCheckIn(
            new io.sentry.CheckIn("currency-worker", io.sentry.CheckInStatus.IN_PROGRESS)
      );

      try {
         currencyService.updateRates();
         log.info("Exchange rate update completed!");
         io.sentry.Sentry.captureCheckIn(
               new io.sentry.CheckIn(checkInId, "currency-worker", io.sentry.CheckInStatus.OK)
         );
      } catch (Exception e) {
         log.error("Failed to update exchange rates in CurrencyWorker", e);
         io.sentry.Sentry.captureCheckIn(
               new io.sentry.CheckIn(checkInId, "currency-worker", io.sentry.CheckInStatus.ERROR)
         );
      }
   }
}

