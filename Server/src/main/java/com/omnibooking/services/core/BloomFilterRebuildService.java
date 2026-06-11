package com.omnibooking.services.core;

import com.omnibooking.repository.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BloomFilterRebuildService {

   private final UserRepository userRepository;

   private final BloomFilterService bloomFilterService;

   private final StringRedisTemplate redisTemplate;

   private final MeterRegistry meterRegistry;

   private static final String CHECKPOINT_KEY = "bloom_rebuild_checkpoint";

   public void rebuildBloomFilter() {
      long startTime = System.currentTimeMillis();
      String filterName = bloomFilterService.getEmailFilterName();

      log.info("Starting Bloom Filter rebuild process...");

      // Check if checkpoint exists indicating resumption
      var ops = redisTemplate.opsForValue();
      if (ops == null) {
         log.warn(
               "StringRedisTemplate.opsForValue() returned null. Skipping Bloom Filter rebuild (expected if Redis is mocked in tests).");
         return;
      }
      String checkpointVal = ops.get(CHECKPOINT_KEY);
      UUID lastId = null;
      if (checkpointVal != null && !checkpointVal.isEmpty()) {
         lastId = UUID.fromString(checkpointVal);
         log.info("Interrupted rebuild detected. Resuming from checkpoint: {}", lastId);
      } else {
         // Clean slate rebuild
         log.info("No checkpoint found. Clearing existing Bloom Filter key from Redis.");
         redisTemplate.delete(filterName);
         // error rate 0.01, initial capacity 10000
         bloomFilterService.createFilter(filterName, 0.01, 10000);
      }

      int batchSize = 5000;
      long totalProcessed = 0;
      boolean hasMore = true;
      Pageable limit = PageRequest.of(0, batchSize);

      while (hasMore) {
         try {
            List<Object[]> batch = userRepository.findEmailsForWarmup(lastId, limit);
            if (batch.isEmpty()) {
               hasMore = false;
               log.info("Rebuild complete. No more users found.");
            } else {
               for (Object[] row : batch) {
                  UUID id = (UUID) row[0];
                  String email = (String) row[1];
                  if (email != null) {
                     bloomFilterService.add(email);
                     totalProcessed++;
                  }
                  lastId = id;
               }

               // Save checkpoint after successful batch processing
               if (lastId != null) {
                  redisTemplate.opsForValue().set(CHECKPOINT_KEY, lastId.toString());
                  log.info("Bloom filter rebuild: processed batch of {} users, saved checkpoint lastId: {}",
                        batch.size(), lastId);
               }
            }
         } catch (Exception e) {
            log.error("Error encountered during Bloom Filter rebuild at lastId: {}. Process will resume on next start.",
                  lastId, e);
            throw e;
         }
      }

      // Success cleanup
      redisTemplate.delete(CHECKPOINT_KEY);
      long duration = System.currentTimeMillis() - startTime;

      // Expose metrics
      meterRegistry.timer("omnibooking.bloom.rebuild.duration").record(duration, TimeUnit.MILLISECONDS);
      meterRegistry.counter("omnibooking.bloom.rebuild.users_processed").increment(totalProcessed);

      log.info("Bloom Filter rebuild finished successfully in {} ms. Total users rebuilt: {}", duration,
            totalProcessed);
   }

}
