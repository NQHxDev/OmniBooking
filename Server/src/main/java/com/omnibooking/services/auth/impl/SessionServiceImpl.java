package com.omnibooking.services.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.services.auth.SessionService;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
      RedisSessionInfo info = getSession(sessionId);
      if (info != null && info.getUserId() != null) {
         String indexKey = "user_sessions:" + info.getUserId().toString();
         redisTemplate.opsForZSet().remove(indexKey, sessionId.toString());
      }
      redisTemplate.delete("refresh:" + sessionId);
   }

   @Override
   public void revokeAllUserSessions(UUID userId) {
      String indexKey = "user_sessions:" + Objects.requireNonNull(userId.toString());
      java.util.Set<String> sessionIds = redisTemplate.opsForZSet().range(indexKey, 0, -1);

      if (sessionIds != null) {
         sessionIds.forEach(id -> redisTemplate.delete("refresh:" + Objects.requireNonNull(id)));
      }
      redisTemplate.delete(indexKey);
   }

   @Override
   public boolean isValidSession(UUID sessionId, UUID refreshToken) {
      RedisSessionInfo info = getSession(sessionId);
      return info != null && passwordEncoder.matches(refreshToken.toString(), info.getHashedRefreshToken());
   }

}
