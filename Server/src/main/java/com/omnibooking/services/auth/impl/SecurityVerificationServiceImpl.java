package com.omnibooking.services.auth.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.auth.SecurityVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.util.OtpUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityVerificationServiceImpl implements SecurityVerificationService {

   private final StringRedisTemplate redisTemplate;

   private final MailService mailService;

   private final OutboxService outboxService;

   private static final String OTP_KEY_PREFIX = "SECURITY_OTP:";

   private static final String TRUSTED_SESSION_PREFIX = "TRUSTED_SESSION:";

   private static final int OTP_EXPIRY_MINUTES = 5;

   private static final int TRUSTED_EXPIRY_MINUTES = 30;

   @Override
   @Transactional
   public void sendSecurityOTP(UUID userId, String email) {
      String otp = OtpUtils.generateAlphanumericOtp();
      String key = OTP_KEY_PREFIX + userId.toString();

      redisTemplate.opsForValue().set(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

      // Use Outbox Pattern for reliable email delivery
      EmailEvent emailEvent = mailService.buildSecurityOtpEmailEvent(email, email, otp);
      outboxService.saveEvent(
            userId,
            "SECURITY",
            EventConstants.SECURITY_OTP_SEND,
            emailEvent);

      log.info("Security OTP recorded in outbox for userId: {}. OTP: {}", userId, otp);
   }

   @Override
   public boolean verifySecurityOTP(UUID userId, String otp) {
      String key = OTP_KEY_PREFIX + userId.toString();
      String savedOtp = redisTemplate.opsForValue().get(key);

      if (savedOtp != null && savedOtp.trim().equalsIgnoreCase(otp.trim())) {
         redisTemplate.delete(key);

         String trustedKey = TRUSTED_SESSION_PREFIX + userId.toString();
         redisTemplate.opsForValue().set(trustedKey, "true", TRUSTED_EXPIRY_MINUTES, TimeUnit.MINUTES);

         log.info("UserId: {} successfully verified security OTP. Session trusted for 30m.", userId);
         return true;
      }

      return false;
   }

   @Override
   public boolean isSessionTrusted(UUID userId) {
      String trustedKey = TRUSTED_SESSION_PREFIX + userId.toString();
      String isTrusted = redisTemplate.opsForValue().get(trustedKey);
      return "true".equals(isTrusted);
   }
}
