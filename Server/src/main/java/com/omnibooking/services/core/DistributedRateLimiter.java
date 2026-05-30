package com.omnibooking.services.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedRateLimiter {

   private final StringRedisTemplate redisTemplate;

   // Atomic Lua script implementing Token Bucket rate limiting
   private static final String LUA_LIMITER = 
         "local tokens_key = KEYS[1]\n" +
         "local last_refill_key = KEYS[2]\n" +
         "local capacity = tonumber(ARGV[1])\n" +
         "local refill_rate = tonumber(ARGV[2])\n" +
         "local now = tonumber(ARGV[3])\n" +
         "local last_refill = tonumber(redis.call('get', last_refill_key)) or now\n" +
         "local tokens = tonumber(redis.call('get', tokens_key)) or capacity\n" +
         "local time_passed = math.max(0, now - last_refill)\n" +
         "local refilled = time_passed * refill_rate\n" +
         "tokens = math.min(capacity, tokens + refilled)\n" +
         "if tokens >= 1.0 then\n" +
         "  tokens = tokens - 1.0\n" +
         "  redis.call('setex', tokens_key, 3600, tostring(tokens))\n" +
         "  redis.call('setex', last_refill_key, 3600, tostring(now))\n" +
         "  return 1\n" +
         "else\n" +
         "  return 0\n" +
         "end";

   /**
    * Checks if a request under the given key is allowed under an atomic Token Bucket rate limit.
    * Uses a Lua script to ensure atomic read-calculate-write updates across multiple pods.
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
         DefaultRedisScript<Long> script = new DefaultRedisScript<>();
         script.setScriptText(LUA_LIMITER);
         script.setResultType(Long.class);

         Long result = redisTemplate.execute(
               script,
               List.of(tokensKey, lastRefillKey),
               String.valueOf(capacity),
               String.valueOf(refillRatePerSecond),
               String.valueOf(now)
         );

         if (result != null && result == 1L) {
            return true;
         } else {
            log.warn("Atomic rate limit exceeded for key: {}. Throttling active.", key);
            return false;
         }
      } catch (Exception e) {
         log.error("Error executing atomic Redis rate limit check for key: {}. Fail-open allowed.", key, e);
         return true; // Fail-open to avoid blocking execution when Redis has issues
      }
   }

}
