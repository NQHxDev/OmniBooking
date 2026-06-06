package com.omnibooking.services.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.services.auth.SessionService;
import java.util.Objects;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final PasswordEncoder passwordEncoder;

   @Override
   public void saveSession(UUID sessionId, RedisSessionInfo info, long expiryMs) {
      try {
         String redisKey = "refresh:" + sessionId;
         String json = Objects.requireNonNull(objectMapper.writeValueAsString(info));
         redisTemplate.opsForValue().set(redisKey, json, expiryMs, TimeUnit.MILLISECONDS);

         // Add to User Sessions Index
         String indexKey = "user_sessions:" + Objects.requireNonNull(info.getUserId().toString());

         // Inline Cleanup: Remove expired session references first (Score = Expiry
         // Timestamp)
         redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, System.currentTimeMillis());

         redisTemplate.opsForZSet().add(indexKey, Objects.requireNonNull(sessionId.toString()),
               System.currentTimeMillis() + expiryMs);
         redisTemplate.expire(indexKey, 30, TimeUnit.DAYS); // Hard index expiry

      } catch (Exception e) {
         log.error("Failed to save session to Redis", e);
      }
   }

   @Override
   public RedisSessionInfo getSession(UUID sessionId) {
      try {
         String json = redisTemplate.opsForValue().get("refresh:" + sessionId);
         if (json == null)
            return null;
         return objectMapper.readValue(json, RedisSessionInfo.class);
      } catch (Exception e) {
         return null;
      }
   }

   @Override
   public void deleteSession(UUID sessionId) {
      if (sessionId == null)
         return;
      try {
         RedisSessionInfo info = getSession(sessionId);
         if (info != null && info.getUserId() != null) {
            String indexKey = "user_sessions:" + info.getUserId().toString();
            redisTemplate.opsForZSet().remove(indexKey, sessionId.toString());
            // Inline Cleanup: Remove expired session references
            redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, System.currentTimeMillis());
         }
      } catch (Exception e) {
         log.error("Failed to clean up user session index during deletion for session: {}", sessionId, e);
      }
      try {
         redisTemplate.delete("refresh:" + sessionId);
      } catch (Exception e) {
         log.error("Failed to delete refresh session key from Redis: {}", sessionId, e);
      }
   }

   @Override
   public void revokeAllUserSessions(UUID userId) {
      if (userId == null)
         return;
      try {
         String indexKey = "user_sessions:" + userId.toString();
         String script = "local sessionIds = redis.call('zrange', KEYS[1], 0, -1)\n" +
               "for _, id in ipairs(sessionIds) do\n" +
               "    redis.call('del', ARGV[1] .. id)\n" +
               "end\n" +
               "return redis.call('del', KEYS[1])";
         redisTemplate.execute(
               new DefaultRedisScript<>(script, Long.class),
               Collections.singletonList(indexKey),
               "refresh:");
      } catch (Exception e) {
         log.error("Failed to revoke all sessions for user: {}", userId, e);
      }
   }

   @Override
   public boolean isValidSession(UUID sessionId, UUID refreshToken) {
      RedisSessionInfo info = getSession(sessionId);
      return info != null && passwordEncoder.matches(refreshToken.toString(), info.getHashedRefreshToken());
   }

   /**
    * Tác vụ nền chạy quét dọn dẹp các session mồ côi định kỳ mỗi 6 giờ.
    * Được bảo vệ bởi khóa phân tán lock:session-cleanup sử dụng giải phóng qua
    * owner token để an toàn cho multi-instance.
    */
   @Scheduled(cron = "0 0 */6 * * *")
   public void cleanupExpiredSessionIndexes() {
      String lockKey = "lock:session-cleanup";
      String lockValue = UUID.randomUUID().toString();
      Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.MINUTES);

      if (Boolean.FALSE.equals(acquired)) {
         return; // Khóa đã được lấy bởi instance khác
      }

      log.info("Starting background scheduled cleanup of expired session indexes...");
      try {
         redisTemplate.executeWithStickyConnection(connection -> {
            org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.keyCommands().scan(
                  org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("user_sessions:*")
                        .count(100)
                        .build());

            long now = System.currentTimeMillis();
            long cleanedCount = 0;

            while (cursor.hasNext()) {
               String indexKey = new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8);
               try {
                  // Xóa các session con có score (expiry time) <= now
                  Long removed = redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, now);
                  if (removed != null && removed > 0) {
                     cleanedCount += removed;
                  }
                  // Nếu Sorted Set rỗng, giải phóng key để tối ưu hóa bộ nhớ
                  Long size = redisTemplate.opsForZSet().zCard(indexKey);
                  if (size == null || size == 0) {
                     redisTemplate.delete(indexKey);
                  }
               } catch (Exception ex) {
                  log.error("Error cleaning session index key: {}", indexKey, ex);
               }
            }
            log.info("Finished background session cleanup. Removed {} expired references.", cleanedCount);
            return null;
         });
      } catch (Exception e) {
         log.error("Failed to run scheduled session cleanup", e);
      } finally {
         try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey),
                  lockValue);
         } catch (Exception e) {
            log.error("Failed to release session cleanup distributed lock", e);
         }
      }
   }

}
