package com.omnibooking.services.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class BloomFilterService {

   private final StringRedisTemplate redisTemplate;

   private static final String EMAIL_FILTER = "bf:user_emails";

   /**
    * Khởi tạo Bloom Filter với cấu hình mặc định
    * (error rate 0.01, initial capacity 1000)
    * Nếu đã tồn tại thì sẽ bỏ qua.
    */
   public void createFilter(String filterName, double errorRate, long capacity) {
      try {
         org.springframework.data.redis.core.script.RedisScript<String> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>(
               "return redis.call('BF.RESERVE', KEYS[1], ARGV[1], ARGV[2])", String.class);

         redisTemplate.execute(script,
               Objects.requireNonNull(Collections.singletonList(filterName)),
               String.valueOf(errorRate),
               String.valueOf(capacity));

         log.info("Created Bloom Filter: {}", filterName);
      } catch (Exception e) {
         log.debug("Bloom Filter {} might already exist: {}", filterName, e.getMessage());
      }
   }

   public void add(String value) {
      if (value == null)
         return;
      try {
         org.springframework.data.redis.core.script.RedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>(
               "return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

         redisTemplate.execute(script, Objects.requireNonNull(Collections.singletonList(EMAIL_FILTER)), value);
      } catch (Exception e) {
         log.error("Failed to add value to Bloom Filter: {}", value, e);
      }
   }

   public boolean mightContain(String value) {
      if (value == null)
         return false;
      try {
         org.springframework.data.redis.core.script.RedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>(
               "return redis.call('BF.EXISTS', KEYS[1], ARGV[1])", Long.class);

         Long result = redisTemplate.execute(script, Objects.requireNonNull(Collections.singletonList(EMAIL_FILTER)),
               value);

         return result != null && result == 1L;
      } catch (Exception e) {
         log.error("Error checking Bloom Filter (failing open): {}", e.getMessage());
         return true; // Fail open to allow database check if Redis/Bloom is down
      }
   }

   public String getEmailFilterName() {
      return EMAIL_FILTER;
   }
}
