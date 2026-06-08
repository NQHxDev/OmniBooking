package com.omnibooking.services.auth.impl;

import com.omnibooking.constant.EventConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.TwoFactorSetupResponse;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.UserTwoFactor;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.user.UserTwoFactorRepository;
import com.omnibooking.services.auth.TwoFactorAuthService;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.core.OutboxService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

   private static final String INCR_EXPIRE_SCRIPT =
         "local val = redis.call('incr', KEYS[1]); " +
         "if val == 1 then " +
         "  redis.call('expire', KEYS[1], tonumber(ARGV[1])); " +
         "end; " +
         "return val;";

   private final UserRepository userRepository;

   private final UserTwoFactorRepository userTwoFactorRepository;

   private final EncryptionService encryptionService;

   private final PasswordEncoder passwordEncoder;

   private final StringRedisTemplate redisTemplate;

   private final OutboxService outboxService;

   private final MailService mailService;

   private final AppProperties appProperties;

   private final ObjectMapper objectMapper;

   private final SecretGenerator secretGenerator = new DefaultSecretGenerator();

   private final TimeProvider timeProvider = new SystemTimeProvider();

   private final CodeGenerator codeGenerator = new DefaultCodeGenerator();

   private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

   @Override
   @Transactional
   public TwoFactorSetupResponse initiate2FA(UUID userId) {
      if (!is2FAFeatureEnabled()) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId).orElse(null);

      String rawSecret;
      if (user2fa == null) {
         rawSecret = secretGenerator.generate();
         String encryptedSecret = encryptionService.encrypt(rawSecret);

         user2fa = UserTwoFactor.builder()
               .user(user)
               .secretKey(encryptedSecret)
               .isEnabled(false)
               .build();
         userTwoFactorRepository.save(user2fa);
      } else {
         if (Boolean.TRUE.equals(user2fa.getIsEnabled())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
         }
         rawSecret = encryptionService.decrypt(user2fa.getSecretKey());
      }

      String qrCodeUri = String.format(
            "otpauth://totp/OmniBooking:%s?secret=%s&issuer=OmniBooking&algorithm=SHA1&digits=6&period=30",
            user.getEmail(), rawSecret);

      return TwoFactorSetupResponse.builder()
            .secretKey(rawSecret)
            .qrCodeUri(qrCodeUri)
            .build();
   }

   @Override
   @Transactional
   public List<String> enable2FA(UUID userId, String code) {
      if (!is2FAFeatureEnabled()) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      if (Boolean.TRUE.equals(user2fa.getIsEnabled())) {
         throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
      }

      String rawSecret = encryptionService.decrypt(user2fa.getSecretKey());
      if (!verifyAndConsumeTotpCode(userId, rawSecret, code)) {
         throw new AppException(ErrorCode.INVALID_OTP);
      }

      List<String> rawBackupCodes = generateBackupCodes();
      List<String> hashedBackupCodes = new ArrayList<>();
      for (String rawCode : rawBackupCodes) {
         hashedBackupCodes.add(passwordEncoder.encode(rawCode));
      }

      try {
         user2fa.setIsEnabled(true);
         user2fa.setBackupCodes(objectMapper.writeValueAsString(hashedBackupCodes));
         userTwoFactorRepository.save(user2fa);

         User user = user2fa.getUser();
         UserProfile profile = user.getProfile();
         String displayName = profile != null && profile.getDisplayName() != null ? profile.getDisplayName()
               : user.getUsername();
         EmailEvent emailEvent = mailService.buildTwoFactorEnabledEmailEvent(user.getEmail(), displayName);

         outboxService.saveEvent(
               user.getId(),
               "USER",
               EventConstants.TWO_FACTOR_ENABLED,
               emailEvent);

      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Failed to enable 2FA", e);
         throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
      }

      return rawBackupCodes;
   }

   @Override
   @Transactional
   public void disable2FA(UUID userId, String code) {
      if (!is2FAFeatureEnabled()) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      if (!Boolean.TRUE.equals(user2fa.getIsEnabled())) {
         throw new AppException(ErrorCode.NOT_FOUND);
      }

      String rawSecret = encryptionService.decrypt(user2fa.getSecretKey());
      if (!verifyAndConsumeTotpCode(userId, rawSecret, code)) {
         throw new AppException(ErrorCode.INVALID_OTP);
      }

      user2fa.setIsEnabled(false);
      userTwoFactorRepository.save(user2fa);
   }

   @Override
   @Transactional
   public void remove2FA(UUID userId, String code) {
      if (!is2FAFeatureEnabled()) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      String rawSecret = encryptionService.decrypt(user2fa.getSecretKey());
      boolean isValid = verifyAndConsumeTotpCode(userId, rawSecret, code);

      if (!isValid && code != null && code.length() == 8) {
         String backupCodesJson = user2fa.getBackupCodes();
         if (backupCodesJson != null && !backupCodesJson.isBlank()) {
            try {
               List<String> hashedBackupCodes = objectMapper.readValue(backupCodesJson,
                     new TypeReference<List<String>>() {
                     });
               for (int i = 0; i < hashedBackupCodes.size(); i++) {
                  if (passwordEncoder.matches(code, hashedBackupCodes.get(i))) {
                     isValid = true;
                     break;
                  }
               }
            } catch (Exception e) {
               log.error("Error reading backup codes", e);
            }
         }
      }

      if (!isValid) {
         throw new AppException(ErrorCode.INVALID_OTP);
      }

      userTwoFactorRepository.delete(user2fa);
   }

   @Override
   @Transactional
   public boolean verifyCode(UUID userId, String code) {
      if (!is2FAFeatureEnabled()) {
         return true;
      }

      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId).orElse(null);
      if (user2fa == null || !Boolean.TRUE.equals(user2fa.getIsEnabled())) {
         return true;
      }

      String rawSecret = encryptionService.decrypt(user2fa.getSecretKey());
      if (verifyAndConsumeTotpCode(userId, rawSecret, code)) {
         return true;
      }

      if (code != null && code.length() == 8) {
         String backupCodesJson = user2fa.getBackupCodes();
         if (backupCodesJson != null && !backupCodesJson.isBlank()) {
            try {
               List<String> hashedBackupCodes = objectMapper.readValue(backupCodesJson,
                     new TypeReference<List<String>>() {
                     });
               for (int i = 0; i < hashedBackupCodes.size(); i++) {
                  if (passwordEncoder.matches(code, hashedBackupCodes.get(i))) {
                     hashedBackupCodes.remove(i);
                     user2fa.setBackupCodes(objectMapper.writeValueAsString(hashedBackupCodes));
                     userTwoFactorRepository.save(user2fa);
                     log.info("Backup code consumed for user: {}", userId);
                     return true;
                  }
               }
            } catch (Exception e) {
               log.error("Error reading or writing backup codes for user: {}", userId, e);
            }
         }
      }

      return false;
   }

   @Override
   public boolean is2FAEnabledForUser(UUID userId) {
      if (!is2FAFeatureEnabled()) {
         return false;
      }
      return userTwoFactorRepository.findByUserId(userId)
            .map(UserTwoFactor::getIsEnabled)
            .orElse(false);
   }

   @Override
   public String get2FAStatusString(UUID userId) {
      if (!is2FAFeatureEnabled()) {
         return "UNSET";
      }
      UserTwoFactor user2fa = userTwoFactorRepository.findByUserId(userId).orElse(null);
      if (user2fa == null) {
         return "UNSET";
      }
      return Boolean.TRUE.equals(user2fa.getIsEnabled()) ? "ENABLED" : "DISABLED";
   }

   private boolean is2FAFeatureEnabled() {
      return appProperties.getSecurity().isTwoFactorEnabled();
   }

   private List<String> generateBackupCodes() {
      SecureRandom random = new SecureRandom();
      List<String> codes = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
         int num = 10000000 + random.nextInt(90000000);
         codes.add(String.valueOf(num));
      }
      return codes;
   }

   private boolean verifyAndConsumeTotpCode(UUID userId, String rawSecret, String code) {
      String lockKey = "totp:lock:" + userId;
      if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
         log.warn("Attempted 2FA verification while locked for user: {}", userId);
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      if (codeVerifier.isValidCode(rawSecret, code)) {
         String redisKey = "totp:used:" + userId + ":" + code;
         Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 60, TimeUnit.SECONDS);
         if (success == null || !success) {
            log.warn("Replay attack detected or code reused too quickly for user: {}, code: {}", userId, code);
            return false;
         }
         // Clear failed attempts upon success
         redisTemplate.delete("totp:failed:" + userId);
         return true;
      }

      // Handle failed attempt rate limiting atomically
      String failedAttemptsKey = "totp:failed:" + userId;
      Long failedCount = 0L;
      try {
         failedCount = redisTemplate.execute(
               new DefaultRedisScript<>(INCR_EXPIRE_SCRIPT, Long.class),
               Collections.singletonList(failedAttemptsKey),
               "300" // 5 minutes = 300 seconds
         );
      } catch (Exception e) {
         log.error("Failed to execute atomic failure increment for TOTP: {}", userId, e);
         try {
            failedCount = redisTemplate.opsForValue().increment(failedAttemptsKey);
            redisTemplate.expire(failedAttemptsKey, 5, TimeUnit.MINUTES);
         } catch (Exception ex) {
            log.error("Fallback TOTP increment also failed: {}", userId, ex);
         }
      }
      if (failedCount == null) {
         failedCount = 1L;
      }
      if (failedCount >= 5) {
         redisTemplate.opsForValue().set(lockKey, "locked", 15, TimeUnit.MINUTES);
         redisTemplate.delete(failedAttemptsKey);
         log.warn("User {} locked out of 2FA for 15 minutes due to 5 consecutive failures", userId);
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      return false;
   }

}
