package com.omnibooking.services.core;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class BloomFilterWarmupTask implements CommandLineRunner {

   private final BloomFilterService bloomFilterService;

   private final BloomFilterRebuildService rebuildService;

   private final StringRedisTemplate redisTemplate;

   @Override
   public void run(String... args) {
      log.info("Performing startup validation for Bloom Filter...");

      String filterName = bloomFilterService.getEmailFilterName();
      Boolean exists = false;
      try {
         exists = redisTemplate.hasKey(filterName);
      } catch (Exception e) {
         log.error("Failed to check Bloom Filter key existence in Redis (failing open): {}", e.getMessage());
         // If Redis is down, we cannot rebuild anyway, skip and fail open
         return;
      }

      Boolean checkpointExists = false;
      try {
         checkpointExists = redisTemplate.hasKey("bloom_rebuild_checkpoint");
      } catch (Exception e) {
         log.error("Failed to check Bloom Filter checkpoint in Redis: {}", e.getMessage());
      }

      if (Boolean.FALSE.equals(exists) || Boolean.TRUE.equals(checkpointExists)) {
         log.warn("Bloom Filter key '{}' does not exist or was interrupted. Triggering rebuild...", filterName);
         try {
            rebuildService.rebuildBloomFilter();
         } catch (Exception e) {
            log.error("Startup Bloom Filter rebuild failed", e);
         }
      } else {
         log.info("Bloom Filter key '{}' already exists in Redis. Skipping startup rebuild.", filterName);
      }
   }

}
