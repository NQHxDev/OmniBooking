package com.omnibooking.worker;

import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyKeyCleanupWorker {

   private final IdempotencyKeyRepository idempotencyKeyRepository;

   private static final int BATCH_SIZE = 1000;

   @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
   public void cleanupExpiredKeys() {
      log.info("Starting batch cleanup of expired idempotency keys...");
      Instant now = Instant.now();
      int totalDeleted = 0;
      int deleted;
      do {
         try {
            deleted = idempotencyKeyRepository.deleteExpiredKeysBatch(now, BATCH_SIZE);
            totalDeleted += deleted;
         } catch (Exception e) {
            log.error("Error occurred while deleting expired idempotency keys batch", e);
            break;
         }
      } while (deleted > 0);
      log.info("Finished batch cleanup. Deleted {} expired keys in total.", totalDeleted);
   }

}
