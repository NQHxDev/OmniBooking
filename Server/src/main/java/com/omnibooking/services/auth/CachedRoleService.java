package com.omnibooking.services.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Role;
import com.omnibooking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedRoleService {

   private final RoleRepository roleRepository;
   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;

   private static final String ROLE_CACHE_PREFIX = "role:";
   private static final long CACHE_TTL_HOURS = 24;

   /**
    * Retrieves a Role by name. Checks Redis cache first.
    * If not found in cache, fetches from database, serializes, and caches in Redis.
    * Uses ObjectMapper for explicit deserialization to prevent LinkedHashMap ClassCastExceptions.
    */
   public Role getRoleByName(String name) {
      String cacheKey = ROLE_CACHE_PREFIX + name;
      try {
         String cachedJson = redisTemplate.opsForValue().get(cacheKey);
         if (cachedJson != null) {
            log.debug("Cache hit for role: {}", name);
            return objectMapper.readValue(cachedJson, Role.class);
         }
      } catch (Exception e) {
         log.error("Failed to read role from Redis cache for key: {}", cacheKey, e);
      }

      log.info("Cache miss for role: {}. Querying database...", name);
      Role role = roleRepository.findByName(name)
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

      try {
         String json = objectMapper.writeValueAsString(role);
         redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
         log.info("Cached role: {} in Redis with 24h TTL", name);
      } catch (Exception e) {
         log.error("Failed to write role to Redis cache for key: {}", cacheKey, e);
      }

      return role;
   }
}
