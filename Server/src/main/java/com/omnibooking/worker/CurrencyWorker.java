package com.omnibooking.worker;

import com.omnibooking.services.core.CurrencyService;

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
public class CurrencyWorker {

   private final CurrencyService currencyService;

   /**
    * Updates exchange rates every 4 hours!
    */
   @Scheduled(cron = "0 0 0,4,8,12,16,20 * * *")
   public void updateExchangeRates() {
      log.info("Starting scheduled exchange rate update...");

      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("currency-worker", CheckInStatus.IN_PROGRESS));

      try {
         currencyService.updateRates();
         log.info("Exchange rate update completed!");
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "currency-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Failed to update exchange rates in CurrencyWorker", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "currency-worker", CheckInStatus.ERROR));
      }
   }

}
