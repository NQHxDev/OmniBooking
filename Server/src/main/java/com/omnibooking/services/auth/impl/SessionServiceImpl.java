package com.omnibooking.services.auth.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.services.auth.SessionService;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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

   private Map<String, String> convertToMap(RedisSessionInfo info) {
      Map<String, String> map = new HashMap<>();
      if (info.getUserId() != null)
         map.put("userId", info.getUserId().toString());
      if (info.getUsername() != null)
         map.put("username", info.getUsername());
      if (info.getEmail() != null)
         map.put("email", info.getEmail());
      if (info.getFullName() != null)
         map.put("fullName", info.getFullName());
      if (info.getRoles() != null) {
         try {
            map.put("roles", objectMapper.writeValueAsString(info.getRoles()));
         } catch (Exception e) {
            map.put("roles", "[]");
         }
      }
      if (info.getHashedRefreshToken() != null)
         map.put("hashedRefreshToken", info.getHashedRefreshToken());
      if (info.getIp() != null)
         map.put("ip", info.getIp());
      if (info.getUserAgent() != null)
         map.put("userAgent", info.getUserAgent());
      map.put("createdAt", String.valueOf(info.getCreatedAt()));
      map.put("lastAccessedAt", String.valueOf(info.getLastAccessedAt()));
      map.put("rememberMe", String.valueOf(info.isRememberMe()));
      if (info.getDeviceVersion() != null)
         map.put("deviceVersion", String.valueOf(info.getDeviceVersion()));
      if (info.getPlatform() != null)
         map.put("platform", info.getPlatform());
      if (info.getBrowserFamily() != null)
         map.put("browserFamily", info.getBrowserFamily());
      if (info.getCsrfNonce() != null)
         map.put("csrfNonce", info.getCsrfNonce());
      if (info.getRefreshFamilyId() != null)
         map.put("refreshFamilyId", info.getRefreshFamilyId().toString());
      if (info.getRefreshTokenId() != null)
         map.put("refreshTokenId", info.getRefreshTokenId().toString());
      if (info.getParentTokenId() != null)
         map.put("parentTokenId", info.getParentTokenId().toString());
      map.put("used", String.valueOf(info.isUsed()));
      if (info.getChildSessionId() != null)
         map.put("childSessionId", info.getChildSessionId().toString());
      if (info.getRotationTimestamp() != null)
         map.put("rotationTimestamp", String.valueOf(info.getRotationTimestamp()));
      if (info.getEncryptedChildCredentials() != null)
         map.put("encryptedChildCredentials", info.getEncryptedChildCredentials());
      if (info.getSessionVersion() != null)
         map.put("sessionVersion", String.valueOf(info.getSessionVersion()));
      map.put("active", String.valueOf(info.isActive()));
      return map;
   }

   private RedisSessionInfo convertFromMap(Map<Object, Object> map) {
      if (map == null || map.isEmpty())
         return null;
      try {
         RedisSessionInfo.RedisSessionInfoBuilder builder = RedisSessionInfo.builder();
         if (map.get("userId") != null)
            builder.userId(UUID.fromString((String) map.get("userId")));
         builder.username((String) map.get("username"));
         builder.email((String) map.get("email"));
         builder.fullName((String) map.get("fullName"));
         if (map.get("roles") != null) {
            try {
               builder.roles(objectMapper.readValue((String) map.get("roles"),
                     new TypeReference<Set<String>>() {
                     }));
            } catch (Exception e) {
               builder.roles(Collections.emptySet());
            }
         }
         builder.hashedRefreshToken((String) map.get("hashedRefreshToken"));
         builder.ip((String) map.get("ip"));
         builder.userAgent((String) map.get("userAgent"));
         if (map.get("createdAt") != null)
            builder.createdAt(Long.parseLong((String) map.get("createdAt")));
         if (map.get("lastAccessedAt") != null)
            builder.lastAccessedAt(Long.parseLong((String) map.get("lastAccessedAt")));
         if (map.get("rememberMe") != null)
            builder.rememberMe(Boolean.parseBoolean((String) map.get("rememberMe")));
         if (map.get("deviceVersion") != null)
            builder.deviceVersion(Integer.parseInt((String) map.get("deviceVersion")));
         builder.platform((String) map.get("platform"));
         builder.browserFamily((String) map.get("browserFamily"));
         builder.csrfNonce((String) map.get("csrfNonce"));
         if (map.get("refreshFamilyId") != null)
            builder.refreshFamilyId(UUID.fromString((String) map.get("refreshFamilyId")));
         if (map.get("refreshTokenId") != null)
            builder.refreshTokenId(UUID.fromString((String) map.get("refreshTokenId")));
         if (map.get("parentTokenId") != null)
            builder.parentTokenId(UUID.fromString((String) map.get("parentTokenId")));
         if (map.get("used") != null)
            builder.used(Boolean.parseBoolean((String) map.get("used")));
         if (map.get("childSessionId") != null)
            builder.childSessionId(UUID.fromString((String) map.get("childSessionId")));
         if (map.get("rotationTimestamp") != null)
            builder.rotationTimestamp(Long.parseLong((String) map.get("rotationTimestamp")));
         builder.encryptedChildCredentials((String) map.get("encryptedChildCredentials"));
         if (map.get("sessionVersion") != null)
            builder.sessionVersion(Integer.parseInt((String) map.get("sessionVersion")));
         if (map.get("active") != null)
            builder.active(Boolean.parseBoolean((String) map.get("active")));
         return builder.build();
      } catch (Exception e) {
         log.error("Failed to parse RedisSessionInfo from map", e);
         return null;
      }
   }

   @Override
   public void saveSession(UUID sessionId, RedisSessionInfo info, long expiryMs) {
      try {
         String redisKey = "refresh:" + sessionId;
         Map<String, String> map = convertToMap(info);
         redisTemplate.opsForHash().putAll(redisKey, map);
         redisTemplate.expire(redisKey, expiryMs, TimeUnit.MILLISECONDS);

         // Add or remove from pending_sessions ZSET
         if (!info.isActive()) {
            redisTemplate.opsForZSet().add("pending_sessions", sessionId.toString(), System.currentTimeMillis());
         } else {
            redisTemplate.opsForZSet().remove("pending_sessions", sessionId.toString());
         }

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
         String redisKey = "refresh:" + sessionId;
         Map<Object, Object> map = redisTemplate.opsForHash().entries(redisKey);
         if (map == null || map.isEmpty())
            return null;
         return convertFromMap(map);
      } catch (DataAccessException e) {
         throw e;
      } catch (Exception e) {
         log.error("Failed to parse session info from Redis for sessionId: {}", sessionId, e);
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
         redisTemplate.opsForZSet().remove("pending_sessions", sessionId.toString());
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
               "    redis.call('zrem', 'pending_sessions', id)\n" +
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
            Cursor<byte[]> cursor = connection.keyCommands().scan(
                  ScanOptions.scanOptions()
                        .match("user_sessions:*")
                        .count(100)
                        .build());

            long now = System.currentTimeMillis();
            long cleanedCount = 0;

            while (cursor.hasNext()) {
               String indexKey = new String(cursor.next(), StandardCharsets.UTF_8);
               try {
                  // Xóa các session con có score (expiry time) <= now
                  Long removed = redisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, now);
                  if (removed != null && removed > 0) {
                     cleanedCount += removed;
                  }

                  Set<String> sessionIds = redisTemplate.opsForZSet().range(indexKey, 0, -1);
                  if (sessionIds != null) {
                     for (String sId : sessionIds) {
                        if (Boolean.FALSE.equals(redisTemplate.hasKey("refresh:" + sId))) {
                           redisTemplate.opsForZSet().remove(indexKey, sId);
                        }
                     }
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

   /**
    * Tác vụ dọn dẹp các session pending mồ côi (active = false quá 5 phút)
    * Sử dụng Lua script để xóa nguyên tử, tránh race condition với luồng khôi phục
    * (P8 & P22).
    */
   @Scheduled(cron = "0 */5 * * * *")
   public void cleanupPendingSessions() {
      try {
         long threshold = System.currentTimeMillis() - 5 * 60 * 1000; // 5 minutes ago
         Set<String> sessionIds = redisTemplate.opsForZSet().rangeByScore("pending_sessions", 0, threshold);
         if (sessionIds == null || sessionIds.isEmpty()) {
            return;
         }
         String script = "local exists = redis.call('exists', KEYS[1])\n" +
               "if exists == 0 then\n" +
               "    redis.call('zrem', KEYS[2], ARGV[1])\n" +
               "    return 1\n" +
               "end\n" +
               "local active = redis.call('hget', KEYS[1], 'active')\n" +
               "if active == 'false' then\n" +
               "    redis.call('del', KEYS[1])\n" +
               "    redis.call('zrem', KEYS[2], ARGV[1])\n" +
               "    return 2\n" +
               "else\n" +
               "    redis.call('zrem', KEYS[2], ARGV[1])\n" +
               "    return 0\n" +
               "end";
         for (String sId : sessionIds) {
            redisTemplate.execute(
                  new DefaultRedisScript<>(script, Long.class),
                  List.of("refresh:" + sId, "pending_sessions"),
                  sId);
         }
      } catch (Exception e) {
         log.error("Failed to run background pending sessions cleanup", e);
      }
   }

}
