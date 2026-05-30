package com.omnibooking.worker;

import com.omnibooking.services.property.SearchReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchReconciliationWorker {

   private final SearchReconciliationService searchReconciliationService;

   /**
    * Runs the search reconciliation job daily at 1:00 AM.
    */
   @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
   public void runReconciliation() {
      log.info("Starting scheduled Search Reconciliation job...");
      try {
         searchReconciliationService.reconcileProperties();
         log.info("Scheduled Search Reconciliation job finished successfully.");
      } catch (Exception e) {
         log.error("Error executing Search Reconciliation job", e);
      }
   }

}
