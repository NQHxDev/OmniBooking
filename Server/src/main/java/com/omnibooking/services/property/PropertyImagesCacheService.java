package com.omnibooking.services.property;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyImagesCacheService {

   private final StringRedisTemplate redisTemplate;
   private final MediaRepository mediaRepository;
   private final ObjectMapper objectMapper;

   private static final String CACHE_PREFIX = "property:images:";
   private static final String LOCK_PREFIX = "lock:property:images:";
   private static final long CACHE_TTL_DAYS = 7;
   private static final long LOCK_TTL_SECONDS = 5;

   public List<String> getPropertyImageUrls(UUID propertyId) {
      String cacheKey = CACHE_PREFIX + propertyId;
      String lockKey = LOCK_PREFIX + propertyId;

      // 1. Try to read from Redis cache
      String cachedValue = redisTemplate.opsForValue().get(cacheKey);
      if (cachedValue != null) {
         try {
            return objectMapper.readValue(cachedValue, new TypeReference<List<String>>() {});
         } catch (Exception e) {
            log.error("Failed to deserialize property image URLs from Redis cache for key: {}", cacheKey, e);
         }
      }

      // 2. Cache stampede protection using a distributed lock
      Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
      if (Boolean.TRUE.equals(acquired)) {
         try {
            // Double check locking pattern: check if another thread populated the cache while we waited for lock
            cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
               try {
                  return objectMapper.readValue(cachedValue, new TypeReference<List<String>>() {});
               } catch (Exception e) {
                  log.error("Failed to deserialize property image URLs on double check", e);
               }
            }

            // Query database and populate cache
            log.info("Cache miss for property images (lock acquired). Querying DB for propertyId: {}", propertyId);
            List<String> urls = queryDatabase(propertyId);

            try {
               redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(urls), CACHE_TTL_DAYS, TimeUnit.DAYS);
            } catch (Exception e) {
               log.error("Failed to serialize property image URLs to Redis cache for key: {}", cacheKey, e);
            }

            return urls;
         } finally {
            redisTemplate.delete(lockKey);
         }
      } else {
         // Lock is held by another thread, poll and wait for cache to populate
         log.info("Lock is held by another thread. Waiting for cache population for propertyId: {}", propertyId);
         int retries = 30; // Wait up to 3 seconds (30 * 100ms)
         while (retries > 0) {
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               log.error("Polling wait interrupted", e);
               break;
            }

            cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
               try {
                  return objectMapper.readValue(cachedValue, new TypeReference<List<String>>() {});
               } catch (Exception e) {
                  log.error("Failed to deserialize property image URLs from polled cache value", e);
               }
            }
            retries--;
         }

         // Fallback if lock owner crashed or database query took too long
         log.warn("Polling timed out or failed. Falling back to direct DB query for propertyId: {}", propertyId);
         return queryDatabase(propertyId);
      }
   }

   public void evict(UUID propertyId) {
      String cacheKey = CACHE_PREFIX + propertyId;
      log.info("Evicting property images cache for key: {}", cacheKey);
      redisTemplate.delete(cacheKey);
   }

   private List<String> queryDatabase(UUID propertyId) {
      return mediaRepository.findByEntityIdAndEntityType(propertyId, "PROPERTY").stream()
            .map(com.omnibooking.model.Media::getUrl)
            .toList();
   }
}
