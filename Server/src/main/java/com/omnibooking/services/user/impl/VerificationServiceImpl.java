package com.omnibooking.services.user.impl;

import com.omnibooking.services.user.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

   private final StringRedisTemplate redisTemplate;

   private static final String VERIFY_PREFIX = "auth:verify:";

   private static final long EXPIRATION_HOURS = 2;

   private static final SecureRandom secureRandom = new SecureRandom();

   private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

   @Override
   public String createVerificationToken(UUID userId) {
      byte[] randomBytes = new byte[32];
      secureRandom.nextBytes(randomBytes);
      String token = base64Encoder.encodeToString(randomBytes);

      String key = VERIFY_PREFIX + token;
      log.debug("Creating token key: {} for user: {}", key, userId);
      redisTemplate.opsForValue().set(
            Objects.requireNonNull(key),
            Objects.requireNonNull(userId.toString()),
            EXPIRATION_HOURS,
            TimeUnit.HOURS);

      return token;
   }

   @Override
   public UUID verifyToken(String token) {
      if (token == null)
         return null;
      String cleanToken = token.trim();
      String key = VERIFY_PREFIX + cleanToken;

      log.debug("Verifying token key: {}", key);
      String userIdStr = redisTemplate.opsForValue().get(key);

      if (userIdStr == null) {
         log.debug("Token NOT found in Redis for key: {}", key);
         return null;
      }

      redisTemplate.delete(key);
      log.debug("Token verified and deleted for user: {}", userIdStr);

      return UUID.fromString(userIdStr);
   }

}
