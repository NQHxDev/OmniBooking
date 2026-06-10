package com.omnibooking.worker;

import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.repository.infra.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyRecoveryWorker {

   private final ProcessedEventRepository processedEventRepository;
   private final MeterRegistry meterRegistry;

   /**
    * Recovers stale claims stuck in 'PROCESSING' state for longer than 5 minutes.
    * Resets their status to 'FAILED' to allow redelivery/retries.
    */
   @Scheduled(fixedDelay = 60000) // Every 1 minute
   @Transactional
   public void recoverStaleClaims() {
      Instant now = Instant.now();
      List<ProcessedEvent> staleEvents = processedEventRepository.findByStatusAndLeaseUntilBefore("PROCESSING", now);
      
      if (!staleEvents.isEmpty()) {
         log.warn("Found {} stale idempotency claims with expired leases in PROCESSING. Recovering them to FAILED...", staleEvents.size());
         for (ProcessedEvent event : staleEvents) {
            log.warn("Recovering stale claim: eventId={}, consumerGroup={}, leaseUntil={}", 
                  event.getEventId(), event.getConsumerGroup(), event.getLeaseUntil());
            int updated = processedEventRepository.recoverStaleEvent(event.getEventId(), event.getConsumerGroup(), now);
            if (updated > 0) {
               meterRegistry.counter("omnibooking.lease.expired").increment();
            }
         }
      }
   }

   /**
    * Periodically purges historical completed/failed event logs older than 30 days.
    * Runs every day at 2:00 AM.
    */
   @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
   @Transactional
   public void purgeOldClaims() {
      Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
      int deletedCount = processedEventRepository.deleteOldEvents(List.of("COMPLETED", "FAILED"), threshold);
      if (deletedCount > 0) {
         log.info("Successfully purged {} historical processed event logs older than 30 days", deletedCount);
      }
   }

}
