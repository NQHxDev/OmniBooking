package com.omnibooking.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
         redisTemplate.execute((RedisCallback<Object>) (connection) -> {
            connection.execute("BF.RESERVE",
                  filterName.getBytes(),
                  String.valueOf(errorRate).getBytes(),
                  String.valueOf(capacity).getBytes());
            return null;
         });
         log.info("Created Bloom Filter: {}", filterName);
      } catch (Exception e) {
         log.debug("Bloom Filter {} might already exist: {}", filterName, e.getMessage());
      }
   }

   public void add(String value) {
      try {
         redisTemplate.execute((RedisCallback<Object>) (connection) -> {
            connection.execute("BF.ADD", EMAIL_FILTER.getBytes(), value.getBytes());
            return null;
         });
      } catch (Exception e) {
         log.error("Failed to add value to Bloom Filter: {}", value, e);
      }
   }

   public boolean mightContain(String value) {
      try {
         Boolean exists = redisTemplate.execute((RedisCallback<Boolean>) (connection) -> {
            Object result = connection.execute("BF.EXISTS", EMAIL_FILTER.getBytes(), value.getBytes());
            // RedisBloom trả về 1 nếu tồn tại, 0 nếu không
            return result != null && (Long) result == 1L;
         });
         return exists != null && exists;
      } catch (Exception e) {
         log.error("Error checking Bloom Filter for: {}", value, e);
         return true; // Fallback an toàn
      }
   }

   public String getEmailFilterName() {
      return EMAIL_FILTER;
   }
}
