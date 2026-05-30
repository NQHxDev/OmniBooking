package com.omnibooking.services.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedRateLimiter {

   private final StringRedisTemplate redisTemplate;

   /**
    * Checks if a request under the given key is allowed under a Token Bucket rate limit.
    * Uses Redis to coordinate rate limits across multiple server instances.
    * 
    * @param key the unique key for the rate limit partition
    * @param capacity the maximum capacity of the bucket (burst limit)
    * @param refillRatePerSecond how many tokens are refilled per second
    * @return true if allowed, false if throttled
    */
   public boolean isAllowed(String key, int capacity, int refillRatePerSecond) {
      String tokensKey = "rate_limit:tokens:" + key;
      String lastRefillKey = "rate_limit:last_refill:" + key;

      long now = Instant.now().getEpochSecond();

      try {
         String lastRefillStr = redisTemplate.opsForValue().get(lastRefillKey);
         String tokensStr = redisTemplate.opsForValue().get(tokensKey);

         double tokens = tokensStr != null ? Double.parseDouble(tokensStr) : capacity;
         long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

         // Calculate dynamic token refill based on elapsed time
         long timePassed = Math.max(0, now - lastRefill);
         double refilled = timePassed * refillRatePerSecond;
         tokens = Math.min(capacity, tokens + refilled);

         if (tokens >= 1.0) {
            tokens -= 1.0;
            redisTemplate.opsForValue().set(tokensKey, String.valueOf(tokens), 1, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now), 1, TimeUnit.HOURS);
            return true;
         } else {
            log.warn("Rate limit exceeded for key: {}. Throttling active.", key);
            return false;
         }
      } catch (Exception e) {
         log.error("Error executing Redis rate limit check for key: {}. Fail-open allowed.", key, e);
         return true; // Fail-open to avoid blocking execution when Redis has issues
      }
   }

}
