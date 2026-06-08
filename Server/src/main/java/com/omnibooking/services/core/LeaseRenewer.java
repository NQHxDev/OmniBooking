package com.omnibooking.services.core;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Helper class that periodically renews the database idempotency claim lease
 * for long-running processes.
 * Implements {@link AutoCloseable} to clean up its background worker thread
 * upon exit in a try-with-resources.
 */
@Slf4j
public class LeaseRenewer implements AutoCloseable {

   private final ScheduledExecutorService scheduler;

   private final ScheduledFuture<?> future;

   public LeaseRenewer(IdempotencyService idempotencyService, UUID eventId, String consumerGroup) {
      if (eventId == null) {
         this.scheduler = null;
         this.future = null;
         return;
      }
      this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
         Thread thread = new Thread(runnable, "lease-renewer-" + eventId);
         thread.setDaemon(true);
         return thread;
      });
      // Periodically renew the lease every 2 minutes, extending the lease to 5
      // minutes from now
      this.future = this.scheduler.scheduleAtFixedRate(() -> {
         try {
            idempotencyService.renewLease(eventId, consumerGroup, java.time.Duration.ofMinutes(5));
         } catch (Exception e) {
            log.error("Failed to renew lease for eventId={} in background thread", eventId, e);
         }
      }, 2, 2, TimeUnit.MINUTES);
   }

   @Override
   public void close() {
      if (future != null) {
         future.cancel(true);
      }
      if (scheduler != null) {
         scheduler.shutdown();
         try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
               scheduler.shutdownNow();
            }
         } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

}
