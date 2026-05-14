package com.omnibooking.worker;

import com.omnibooking.services.CurrencyService;
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
      currencyService.updateRates();
      log.info("Exchange rate update completed!");
   }
}
