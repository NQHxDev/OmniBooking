package com.omnibooking.services.auth.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.user.UserProfileRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.config.AppProperties;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.services.auth.TurnstileService;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.core.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import com.omnibooking.services.auth.SessionService;
import com.omnibooking.services.auth.TwoFactorAuthService;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.mapper.UserMapper;
import com.omnibooking.model.SocialAccount;
import com.omnibooking.repository.user.SocialAccountRepository;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.dto.RegistrationStatusResponse;
import com.omnibooking.dto.TwoFactorLoginRequest;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;
import com.omnibooking.services.core.BloomFilterService;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

   private static final SecureRandom secureRandom = new SecureRandom();

   private static final String INCR_EXPIRE_SCRIPT = "local val = redis.call('incr', KEYS[1]); " +
         "if val == 1 then " +
         "  redis.call('expire', KEYS[1], tonumber(ARGV[1])); " +
         "end; " +
         "return val;";

   private String getOsFamily(String ua) {
      if (ua == null)
         return "OTHER";
      String uaLower = ua.toLowerCase();
      if (uaLower.contains("windows"))
         return "WINDOWS";
      if (uaLower.contains("macintosh") || uaLower.contains("mac os"))
         return "MAC";
      if (uaLower.contains("iphone") || uaLower.contains("ipad") || uaLower.contains("ipod"))
         return "IOS";
      if (uaLower.contains("android"))
         return "ANDROID";
      if (uaLower.contains("linux"))
         return "LINUX";
      return "OTHER";
   }

   private String getBrowserFamily(String ua) {
      if (ua == null)
         return "OTHER";
      String uaLower = ua.toLowerCase();
      if (uaLower.contains("edg/"))
         return "EDGE";
      if (uaLower.contains("chrome/") || uaLower.contains("crios/"))
         return "CHROME";
      if (uaLower.contains("firefox/") || uaLower.contains("fxios/"))
         return "FIREFOX";
      if (uaLower.contains("safari/") && !uaLower.contains("chrome") && !uaLower.contains("chromium"))
         return "SAFARI";
      if (uaLower.contains("postman"))
         return "POSTMAN";
      if (uaLower.contains("curl"))
         return "CURL";
      return "OTHER";
   }

   private boolean isSignificantUserAgentChange(String oldPlatform, String newPlatform, String oldBrowser,
         String newBrowser) {
      if (oldPlatform == null || newPlatform == null || oldBrowser == null || newBrowser == null) {
         return true;
      }
      return !oldPlatform.equalsIgnoreCase(newPlatform) || !oldBrowser.equalsIgnoreCase(newBrowser);
   }

   private void incrementFailureCounter(String key, long timeoutSeconds) {
      try {
         redisTemplate.execute(
               new DefaultRedisScript<>(INCR_EXPIRE_SCRIPT, Long.class),
               Collections.singletonList(key),
               String.valueOf(timeoutSeconds));
      } catch (Exception e) {
         log.error("Failed to execute atomic increment for key: {}", key, e);
         try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
         } catch (Exception ex) {
            log.error("Fallback increment also failed for key: {}", key, ex);
         }
      }
   }

   private final ScheduledExecutorService lockRenewalScheduler = Executors.newScheduledThreadPool(4,
         runnable -> {
            Thread thread = Thread.ofVirtual().unstarted(runnable);
            thread.setName("lock-renewal-heartbeat-" + thread.threadId());
            return thread;
         });

   private final UserRepository userRepository;

   private final CachedRoleService cachedRoleService;

   private final AppProperties appProperties;

   private final UserProfileRepository userProfileRepository;

   private final PasswordEncoder passwordEncoder;

   private final JWTService jwtService;

   private final SessionService sessionService;

   private final VerificationService verificationService;

   private final StringRedisTemplate redisTemplate;

   private final BloomFilterService bloomFilterService;

   private final UserMapper userMapper;

   private final MailService mailService;

   private final OutboxService outboxService;

   private final SocialAccountRepository socialAccountRepository;

   private final TwoFactorAuthService twoFactorAuthService;

   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   private final RegistrationInboxRepository registrationInboxRepository;

   private final TurnstileService turnstileService;

   private final EncryptionService encryptionService;

   private final ObjectMapper objectMapper;

   private static final long SESSION_SLIDING_NORMAL_MS = 1 * 24 * 60 * 60 * 1000L;

   private static final long SESSION_SLIDING_REMEMBER_ME_MS = 7 * 24 * 60 * 60 * 1000L;

   private static final long SESSION_HARD_CAP_NORMAL_MS = 3 * 24 * 60 * 60 * 1000L;

   private static final long SESSION_HARD_CAP_REMEMBER_ME_MS = 30 * 24 * 60 * 60 * 1000L;

   private boolean isCookieSecure() {
      return appProperties.getSecurity().isCookieSecure();
   }

   @Override
   @Transactional
   public AuthResponse register(RegisterRequest request, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe) {

      // Check Bloom Filter
      if (bloomFilterService.mightContain(request.getEmail())) {
         if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
         }
      }

      Role userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);

      User user = userMapper.toUser(request);
      user.setPassword(passwordEncoder.encode(request.getPassword()));
      user.setRoles(Collections.singleton(userRole));

      User savedUser = userRepository.save(Objects.requireNonNull(user));

      // Add to Bloom Filter after successful DB save
      bloomFilterService.add(savedUser.getEmail());

      // Create Profile
      UserProfile profile = UserProfile.builder()
            .user(savedUser)
            .displayName(request.getFullName())
            .build();
      userProfileRepository.save(Objects.requireNonNull(profile));

      // Reliable Email Delivery via Outbox
      String token = verificationService.createVerificationToken(savedUser.getId());

      // We use MailService to build the DTO but NOT send it to Kafka yet
      EmailEvent emailEvent = mailService.buildVerificationEmailEvent(
            savedUser.getEmail(),
            request.getFullName(),
            token);

      outboxService.saveEvent(
            savedUser.getId(),
            "USER",
            EventConstants.USER_REGISTERED,
            emailEvent);

      // Automatic Login
      Set<String> roles = Collections.singleton(userRole.getName());
      long now = System.currentTimeMillis();

      return issueTokensAndBuildResponse(savedUser, roles, profile, ip, userAgent, response, rememberMe, now, now,
            null);
   }

   @Override
   public AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response,
         String oldSessionId) {
      String emailClean = request.getEmail().trim().toLowerCase();
      String emailKey = "login_failures:" + emailClean;
      String ipKey = "login_failures_ip:" + ip;

      String emailFailuresStr = redisTemplate.opsForValue().get(emailKey);
      String ipFailuresStr = redisTemplate.opsForValue().get(ipKey);
      int emailFailures = emailFailuresStr != null ? Integer.parseInt(emailFailuresStr) : 0;
      int ipFailures = ipFailuresStr != null ? Integer.parseInt(ipFailuresStr) : 0;
      int maxFailures = Math.max(emailFailures, ipFailures);

      if (maxFailures >= 6) {
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      if (maxFailures == 4 || maxFailures == 5) {
         turnstileService.verifyToken(request.getTurnstileToken(), ip);
      }

      try {
         // Check Bloom Filter
         if (!bloomFilterService.mightContain(emailClean)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
         }

         User user = userRepository.findByEmail(emailClean)
               .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

         if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
         }

         // Credentials matched! Clear failure counts from Redis
         try {
            redisTemplate.delete(emailKey);
            redisTemplate.delete(ipKey);
         } catch (Exception ex) {
            log.error("Failed to delete login failure keys in Redis", ex);
         }

         Set<String> roles = user.getRoles().stream()
               .map(Role::getName)
               .collect(Collectors.toSet());

         if (twoFactorAuthService.is2FAEnabledForUser(user.getId())) {
            throw new AppException(ErrorCode.TWO_FACTOR_REQUIRED);
         }

         UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
         long now = System.currentTimeMillis();

         UUID oldSessionUuid = null;
         if (oldSessionId != null && !oldSessionId.isBlank()) {
            try {
               oldSessionUuid = UUID.fromString(oldSessionId);
            } catch (Exception ex) {
               log.warn("Invalid oldSessionId format in login: {}", oldSessionId);
            }
         }

         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, request.isRememberMe(), now,
               now, oldSessionUuid);
      } catch (AppException e) {
         if (e.getErrorEnum() == ErrorCode.INVALID_CREDENTIALS) {
            // Increment failed login counts in Redis atomically (15-minute TTL = 900
            // seconds)
            incrementFailureCounter(emailKey, 900);
            incrementFailureCounter(ipKey, 900);
         }
         throw e;
      }
   }

   @Override
   public AuthResponse refresh(String sessionId, String refreshToken, String ip, String userAgent,
         HttpServletResponse response) {
      long startTime = System.currentTimeMillis();
      try {
         UUID sId;
         UUID rToken;
         try {
            sId = UUID.fromString(sessionId);
            rToken = UUID.fromString(refreshToken);
         } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for session or refresh token: session={}, refresh={}", sessionId,
                  refreshToken);
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         // Double-Layer Token Bucket Rate Limiting
         String sessionLimitKey = "rate_limit:refresh:session:" + sId;
         String ipLimitKey = "rate_limit:refresh:ip:" + ip;
         if (!checkRateLimit(sessionLimitKey, 10, 0.1667)) {
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
         }
         if (!checkRateLimit(ipLimitKey, 60, 1.0)) {
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
         }

         String lockKey = "lock:refresh:" + sId;
         String lockValue = UUID.randomUUID().toString();
         Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);

         if (Boolean.FALSE.equals(acquired)) {
            log.warn("Refresh already in progress for session: {}", sId);
            meterRegistry.counter("omnibooking.auth.lock.contention").increment();
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         ScheduledFuture<?> heartbeatTask = null;
         try {
            heartbeatTask = lockRenewalScheduler.scheduleAtFixedRate(() -> {
               try {
                  String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], 10) else return 0 end";
                  Long result = redisTemplate.execute(
                        new DefaultRedisScript<>(script, Long.class),
                        Collections.singletonList(lockKey),
                        lockValue);
                  if (result != null && result > 0) {
                     meterRegistry.counter("omnibooking.auth.refresh.lock.renewal").increment();
                  } else {
                     meterRegistry.counter("omnibooking.auth.refresh.lock.timeout").increment();
                     log.warn("Failed to renew refresh lock for session: {}", sId);
                  }
               } catch (Exception ex) {
                  log.error("Error renewing refresh lock for session: {}", sId, ex);
               }
            }, 2, 2, TimeUnit.SECONDS);

            // Load session S1 from Redis
            RedisSessionInfo info = sessionService.getSession(sId);
            if (info == null) {
               CookieUtils.clearAuthCookies(response, isCookieSecure());
               throw new AppException(ErrorCode.INVALID_SESSION);
            }

            // Load request for Access Token parsing
            HttpServletRequest request = null;
            try {
               request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            } catch (Exception ex) {
               log.warn("Request attributes not available for JWT extraction", ex);
            }

            // Validate Session Version during Refresh
            if (request != null) {
               String accessToken = CookieUtils.getCookieValue(request, CookieUtils.ACCESS_TOKEN);
               if (accessToken != null) {
                  Claims claims = null;
                  try {
                     claims = jwtService.extractAllClaims(accessToken);
                  } catch (ExpiredJwtException ex) {
                     claims = ex.getClaims();
                  } catch (Exception ex) {
                     log.warn("Failed to parse claims from access token", ex);
                  }

                  if (claims != null) {
                     // Validate session ownership (P1)
                     String subject = claims.getSubject();
                     if (subject == null || !subject.equals(info.getUserId().toString())) {
                        log.warn("Session ownership mismatch during refresh. JWT subject = {}, Redis owner = {}",
                              subject, info.getUserId());
                        writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(),
                              info.getRefreshTokenId(), "SESSION_OWNERSHIP_MISMATCH", ip, userAgent);
                        CookieUtils.clearAuthCookies(response, isCookieSecure());
                        throw new AppException(ErrorCode.INVALID_SESSION);
                     }

                     Integer jwtSessionVersion = claims.get("sv", Integer.class);
                     if (jwtSessionVersion == null) {
                        jwtSessionVersion = 1;
                     }
                     if (!jwtSessionVersion.equals(info.getSessionVersion())) {
                        log.warn("Session version mismatch during refresh for user: {}. Expected {}, got {}",
                              info.getUserId(), info.getSessionVersion(), jwtSessionVersion);
                        writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(),
                              info.getRefreshTokenId(), "SESSION_VERSION_MISMATCH", ip, userAgent);
                        sessionService.deleteSession(sId);
                        CookieUtils.clearAuthCookies(response, isCookieSecure());
                        throw new AppException(ErrorCode.INVALID_SESSION);
                     }
                  }
               }
            }

            User user = userRepository.findById(Objects.requireNonNull(info.getUserId()))
                  .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // If parent session was already rotated (used == true) (Benign Concurrency &
            // Recovery)
            if (info.isUsed()) {
               long elapsed = System.currentTimeMillis() - info.getRotationTimestamp();
               long gracePeriodMs = appProperties.getSecurity().getRefreshGracePeriodMs();

               // Extract fingerprint from cookie/header
               String fingerprintFromCookie = null;
               if (request != null) {
                  fingerprintFromCookie = CookieUtils.getCookieValue(request, CookieUtils.FINGERPRINT);
                  if (fingerprintFromCookie == null) {
                     fingerprintFromCookie = request.getHeader("x-fgp");
                  }
               }

               // Verify fingerprint with versioned pepper
               String fgh_v = null;
               if (request != null) {
                  String accessToken = CookieUtils.getCookieValue(request, CookieUtils.ACCESS_TOKEN);
                  if (accessToken != null) {
                     try {
                        fgh_v = jwtService.extractFingerprintPepperVersion(accessToken);
                     } catch (ExpiredJwtException ex) {
                        fgh_v = ex.getClaims().get("fgh_v", String.class);
                     } catch (Exception ex) {
                        log.warn("Failed to extract fingerprint pepper version from token", ex);
                     }
                  }
               }
               String pepper = null;
               if (fgh_v != null && appProperties.getSecurity().getFingerprintPeppers() != null) {
                  pepper = appProperties.getSecurity().getFingerprintPeppers().get(fgh_v);
               }
               String expectedHash = SecurityUtils.hashFingerprint(fingerprintFromCookie, pepper);

               boolean fingerprintMatches = fingerprintFromCookie != null && expectedHash.equals(
                     jwtService.extractFingerprintHash(CookieUtils.getCookieValue(request, CookieUtils.ACCESS_TOKEN)));

               if (elapsed < gracePeriodMs && fingerprintMatches) {
                  // Benign Concurrency!
                  long abuseCount = redisTemplate.opsForHash().increment("refresh:" + sId, "concurrencyCount", 1);
                  if (abuseCount > 3) {
                     meterRegistry.counter("auth_concurrent_refresh_abuse_total").increment();
                     writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(), info.getRefreshTokenId(),
                           "GRACE_PERIOD_ABUSE", ip, userAgent);
                  }

                  UUID childId = info.getChildSessionId();
                  if (childId == null) {
                     // No child session stored, corrupted
                     writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(), info.getRefreshTokenId(),
                           "UNKNOWN_CHILD_SESSION", ip, userAgent);
                     sessionService.revokeAllUserSessions(info.getUserId());
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  // Recover and activate child session atomically (Lua CAS - P21 & P25)
                  long activateResult = activateChildSession(sId, childId);
                  if (activateResult != 1 && activateResult != 0) {
                     log.error("CAS Activation failed in recovery, result: {}", activateResult);
                     if (activateResult == -3) {
                        handleSecurityIntegrityViolation(info, info, "SECURITY_CHAIN_INTEGRITY_VIOLATION", ip,
                              userAgent);
                     } else {
                        writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(),
                              info.getRefreshTokenId(), "UNKNOWN_CHILD_SESSION", ip, userAgent);
                        sessionService.revokeAllUserSessions(info.getUserId());
                     }
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  // Decrypt child credentials
                  String storedBlob = info.getEncryptedChildCredentials();
                  if (storedBlob == null || !storedBlob.contains(":")) {
                     log.error("Stored encrypted child credentials blob is invalid or missing");
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  int colonIndex = storedBlob.indexOf(':');
                  String credVersion = storedBlob.substring(0, colonIndex);
                  String cipherText = storedBlob.substring(colonIndex + 1);

                  String decryptedJson;
                  try {
                     decryptedJson = encryptionService.decrypt(cipherText, credVersion);
                  } catch (Exception e) {
                     log.error("Failed to decrypt child credentials with key version {}", credVersion, e);
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  String childAccess;
                  String childRefresh;
                  try {
                     Map<?, ?> creds = objectMapper.readValue(decryptedJson, Map.class);
                     childAccess = (String) creds.get("accessToken");
                     childRefresh = (String) creds.get("refreshToken");
                  } catch (Exception e) {
                     log.error("Failed to parse child credentials JSON", e);
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  // Load child session to get cookie expiry
                  RedisSessionInfo childSession = sessionService.getSession(childId);
                  if (childSession == null) {
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  // Refresh Family Lineage Verification
                  boolean chainValid = childSession.getParentTokenId() != null
                        && childSession.getParentTokenId().equals(info.getRefreshTokenId())
                        && childSession.getRefreshFamilyId() != null
                        && childSession.getRefreshFamilyId().equals(info.getRefreshFamilyId());
                  if (!chainValid) {
                     handleSecurityIntegrityViolation(info, childSession, "SECURITY_CHAIN_INTEGRITY_VIOLATION", ip,
                           userAgent);
                     CookieUtils.clearAuthCookies(response, isCookieSecure());
                     throw new AppException(ErrorCode.INVALID_SESSION);
                  }

                  // Return already-issued child session credentials
                  CookieUtils.setAuthCookies(response, childAccess, childId.toString(),
                        childRefresh, fingerprintFromCookie, childSession.getCsrfNonce(), isCookieSecure(),
                        (int) ((childSession.getCreatedAt()
                              + (info.isRememberMe() ? SESSION_HARD_CAP_REMEMBER_ME_MS : SESSION_HARD_CAP_NORMAL_MS)
                              - System.currentTimeMillis()) / 1000));

                  return AuthResponse.builder()
                        .accessToken(childAccess)
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(childSession.getFullName())
                        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .build();
               } else {
                  // Replay attack!
                  String classification = "TOKEN_REUSE_AFTER_ROTATION";
                  if (elapsed >= gracePeriodMs) {
                     classification = "EXPIRED_GRACE_PERIOD";
                  } else if (!fingerprintMatches) {
                     classification = "FINGERPRINT_MISMATCH";
                  }
                  writeSecurityAuditEvent(info.getUserId(), sId, info.getRefreshFamilyId(), info.getRefreshTokenId(),
                        classification, ip, userAgent);
                  meterRegistry.counter("auth_refresh_replay_detected_total").increment();
                  sessionService.revokeAllUserSessions(info.getUserId());
                  CookieUtils.clearAuthCookies(response, isCookieSecure());
                  throw new AppException(ErrorCode.INVALID_SESSION);
               }
            }

            // Verify parent session refresh token match
            if (!passwordEncoder.matches(rToken.toString(), info.getHashedRefreshToken())) {
               CookieUtils.clearAuthCookies(response, isCookieSecure());
               throw new AppException(ErrorCode.INVALID_SESSION);
            }

            // Device Signature Verification with Backward Compatibility
            if (info.getPlatform() == null || info.getBrowserFamily() == null) {
               log.info("Legacy session detected during refresh for user: {}. Bypassing DeviceSignature validation.",
                     user.getEmail());
            } else {
               String currentPlatform = getOsFamily(userAgent);
               String currentBrowserFamily = getBrowserFamily(userAgent);
               boolean suspicious = isSignificantUserAgentChange(info.getPlatform(), currentPlatform,
                     info.getBrowserFamily(), currentBrowserFamily);
               if (suspicious) {
                  log.warn("Significant device binding change detected during refresh (Suspicious device takeover). " +
                        "User: {}, Stored: [{}, {}], Current: [{}, {}]",
                        user.getEmail(), info.getPlatform(), info.getBrowserFamily(), currentPlatform,
                        currentBrowserFamily);
                  CookieUtils.clearAuthCookies(response, isCookieSecure());
                  sessionService.deleteSession(sId);
                  throw new AppException(ErrorCode.INVALID_SESSION);
               }
            }

            Set<String> roles = user.getRoles().stream()
                  .map(Role::getName)
                  .collect(Collectors.toSet());

            UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

            // Check hard cap
            long now = System.currentTimeMillis();
            long elapsed = now - info.getCreatedAt();
            long hardCap = info.isRememberMe() ? SESSION_HARD_CAP_REMEMBER_ME_MS : SESSION_HARD_CAP_NORMAL_MS;

            if (elapsed >= hardCap) {
               log.warn("Session hard cap reached for user: {} ({} ms elapsed)", user.getEmail(), elapsed);
               CookieUtils.clearAuthCookies(response, isCookieSecure());
               sessionService.deleteSession(sId);
               throw new AppException(ErrorCode.INVALID_SESSION);
            }

            log.info("Rotating session for user: {}", user.getEmail());

            return rotateSessionAndBuildResponse(user, roles, profile, ip, userAgent, response, info, sId);
         } finally {
            if (heartbeatTask != null) {
               heartbeatTask.cancel(true);
            }
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey),
                  lockValue);
         }
      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Redis or Database error during token refresh operation", e);
         meterRegistry.counter("auth_redis_fail_closed_total").increment();
         long durationMs = System.currentTimeMillis() - startTime;
         meterRegistry.timer("auth_redis_unavailable_duration_seconds")
               .record(durationMs, TimeUnit.MILLISECONDS);
         CookieUtils.clearAuthCookies(response, isCookieSecure());
         throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
      }
   }

   @Override
   public void logout(UUID sessionId, UUID userId, HttpServletResponse response) {
      RedisSessionInfo info = sessionService.getSession(sessionId);
      if (info != null) {
         if (!userId.equals(info.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
         }
         sessionService.deleteSession(sessionId);
      }
      CookieUtils.clearAuthCookies(response, isCookieSecure());
   }

   @Override
   @Transactional
   public void verifyEmail(String token) {
      UUID userId = verificationService.verifyToken(token);
      if (userId == null)
         throw new AppException(ErrorCode.INVALID_TOKEN);

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      user.setIsActive(true);
      userRepository.save(user);

      // Update Profile verification status
      userProfileRepository.findById(userId).ifPresent(p -> {
         p.setIsVerified(true);
         userProfileRepository.save(p);
      });
   }

   @Override
   @Transactional
   public void resendVerification(UUID userId) {
      // Rate limiting check
      String rateLimitKey = "auth:resend-limit:" + userId;
      String lastSent = redisTemplate.opsForValue().get(rateLimitKey);
      if (lastSent != null) {
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      if (Boolean.TRUE.equals(profile.getIsVerified())) {
         log.info("User {} already verified, skipping resend", user.getEmail());
         return;
      }

      // Create new verification token
      String token = verificationService.createVerificationToken(user.getId());

      // Save to Outbox to be picked up and sent to Kafka
      log.info("Recording resend verification outbox event for: {}", user.getEmail());
      outboxService.saveEvent(
            user.getId(),
            "USER",
            EventConstants.RESEND_VERIFICATION,
            mailService.buildVerificationEmailEvent(user.getEmail(), profile.getDisplayName(), token));

      // Set rate limit in Redis for 30 seconds
      redisTemplate.opsForValue().set(rateLimitKey, "true", 30, TimeUnit.SECONDS);
   }

   /**
    * Centralized logic to issue tokens, save sessions, and set cookies.
    */
   private AuthResponse issueTokensAndBuildResponse(User user, Set<String> roles, UserProfile profile,
         String ip, String userAgent, HttpServletResponse response, boolean rememberMe, long createdAt,
         long lastAccessedAt, UUID oldSessionId) {
      UUID sessionId = UuidCreator.getTimeOrderedEpoch();
      UUID refreshToken = UuidCreator.getTimeOrderedEpoch();

      byte[] randomBytes = new byte[32];
      secureRandom.nextBytes(randomBytes);
      String fingerprint = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);

      String pepperVer = appProperties.getSecurity().getActiveFingerprintPepperVersion();
      String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail(), roles,
            sessionId, fgpHash, user.getTokenVersion(), 1, pepperVer);

      // Cache token version in Redis (Task 3)
      try {
         String versionKey = "user_token_version:" + user.getId();
         redisTemplate.opsForValue().set(versionKey, String.valueOf(user.getTokenVersion()), 30, TimeUnit.DAYS);
      } catch (Exception ex) {
         log.error("Failed to populate token version cache in Redis: {}", ex.getMessage());
      }

      String fullName;
      if (profile != null && profile.getDisplayName() != null) {
         fullName = profile.getDisplayName();
      } else {
         fullName = user.getUsername();
      }

      long now = System.currentTimeMillis();
      long slidingMs;

      if (rememberMe) {
         // Flexible Sliding Window for Remember Me
         long maxDays = SESSION_SLIDING_REMEMBER_ME_MS / (24 * 60 * 60 * 1000L);
         long offMs = now - lastAccessedAt;
         long offDays = offMs / (24 * 60 * 60 * 1000L);
         long extensionDays = Math.max(1, maxDays - offDays + 1);
         if (extensionDays > maxDays)
            extensionDays = maxDays;

         slidingMs = extensionDays * 24 * 60 * 60 * 1000L;
         // log.info("Flexible sliding window for {}: offDays={}, extensionDays={}",
         // user.getEmail(), offDays,
         // extensionDays);
      } else {
         slidingMs = SESSION_SLIDING_NORMAL_MS;
      }

      long hardCapMs = rememberMe ? SESSION_HARD_CAP_REMEMBER_ME_MS : SESSION_HARD_CAP_NORMAL_MS;
      long remainingHardCapMs = hardCapMs - (now - createdAt);

      long finalTtlMs = Math.min(slidingMs, remainingHardCapMs);
      if (finalTtlMs < 0)
         finalTtlMs = 0;

      String platform = getOsFamily(userAgent);
      String browserFamily = getBrowserFamily(userAgent);
      String csrfNonce = UUID.randomUUID().toString();

      // Build Session Info Object
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(fullName)
            .roles(roles)
            .hashedRefreshToken(passwordEncoder.encode(refreshToken.toString()))
            .ip(ip)
            .userAgent(userAgent)
            .createdAt(createdAt)
            .lastAccessedAt(now) // Current time is the new lastAccessedAt
            .rememberMe(rememberMe)
            .deviceVersion(1)
            .platform(platform)
            .browserFamily(browserFamily)
            .csrfNonce(csrfNonce)
            .refreshFamilyId(sessionId) // First session in family uses its own ID
            .refreshTokenId(refreshToken)
            .parentTokenId(null)
            .used(false)
            .sessionVersion(1)
            .active(true)
            .build();

      // Create and Save New Session
      try {
         sessionService.saveSession(sessionId, sessionInfo, finalTtlMs);
      } catch (Exception e) {
         log.error("Failed to save new session in Redis for user: {}", user.getEmail(), e);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      // Verify Success
      try {
         RedisSessionInfo savedInfo = sessionService.getSession(sessionId);
         if (savedInfo == null || !user.getId().equals(savedInfo.getUserId())) {
            log.error("Verification failed for newly saved session: {}", sessionId);
            try {
               sessionService.deleteSession(sessionId);
            } catch (Exception ex) {
               log.error("Rollback: Failed to delete invalid new session {}", sessionId, ex);
            }
            throw new AppException(ErrorCode.INVALID_SESSION);
         }
      } catch (Exception e) {
         log.error("Failed to verify saved session or rollback: {}", sessionId, e);
         if (e instanceof AppException) {
            throw (AppException) e;
         }
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      // Delete Old Session
      if (oldSessionId != null) {
         try {
            sessionService.deleteSession(oldSessionId);
            // log.info("Successfully rotated session: deleted old session {} and issued new
            // session {}", oldSessionId,
            // sessionId);
         } catch (Exception e) {
            log.error("Failed to delete old session {} during rotation. Rolling back new session {}", oldSessionId,
                  sessionId, e);
            try {
               sessionService.deleteSession(sessionId);
            } catch (Exception ex) {
               log.error("Rollback: Failed to clean up new session {} after old session deletion failure", sessionId,
                     ex);
            }
            throw new AppException(ErrorCode.INVALID_SESSION);
         }
      }

      CookieUtils.setAuthCookies(response, accessToken, sessionId.toString(), refreshToken.toString(), fingerprint,
            csrfNonce, isCookieSecure(), (int) (finalTtlMs / 1000));

      return userMapper.toAuthResponse(user, profile, roles);
   }

   @Override
   public void clearAllCookies(HttpServletResponse response) {
      CookieUtils.clearAuthCookies(response, isCookieSecure());
   }

   @Override
   @Transactional
   public AuthResponse upgradeToPartner(UUID userId, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe, String oldSessionId) {
      User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      Role partnerRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.PARTNER);

      // Add ROLE_PARTNER to the user's roles
      user.getRoles().add(partnerRole);
      userRepository.save(user);

      UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

      log.info("Upgrading user {} to ROLE_PARTNER", user.getEmail());

      Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

      long now = System.currentTimeMillis();

      UUID oldSessionUuid = null;
      if (oldSessionId != null && !oldSessionId.isBlank()) {
         try {
            oldSessionUuid = UUID.fromString(oldSessionId);
         } catch (Exception ex) {
            log.warn("Invalid oldSessionId format in upgradeToPartner: {}", oldSessionId);
         }
      }

      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, rememberMe, now, now,
            oldSessionUuid);
   }

   @Override
   public void forgotPassword(String email) {
      // Rate Limiting check (3 requests per 1 minute)
      String rateLimitKey = "rate_limit:forgot_password:" + email;
      Long count = redisTemplate.opsForValue().increment(rateLimitKey);

      if (count != null && count == 1) {
         redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
      }

      if (count != null && count > 3) {
         log.warn("Forgot password rate limit exceeded for email: {}", email);
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      // Security: Always return success even if user doesn't exist
      // But we still need to fetch user to get name and send email
      userRepository.findByEmail(email).ifPresent(user -> {
         String token = UUID.randomUUID().toString();
         String redisKey = "reset_token:" + token;

         // Save to Redis (15 minutes)
         redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(email), 15, TimeUnit.MINUTES);

         UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
         String fullName;
         if (profile != null && profile.getDisplayName() != null) {
            fullName = profile.getDisplayName();
         } else {
            fullName = user.getUsername();
         }

         // log.info("Recording forgot password outbox event for: {}", email);
         outboxService.saveEvent(
               user.getId(),
               "USER",
               EventConstants.FORGOT_PASSWORD,
               mailService.buildForgotPasswordEmailEvent(email, fullName, token));
      });
   }

   @Override
   @Transactional
   public void resetPassword(String token, String newPassword, boolean logoutAll) {
      String redisKey = "reset_token:" + token;
      String email = redisTemplate.opsForValue().get(redisKey);

      if (email == null) {
         log.warn("Invalid or expired reset token attempt: {}", token);
         throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
      }

      User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      user.setPassword(passwordEncoder.encode(newPassword));
      user.setTokenVersion(user.getTokenVersion() + 1);
      userRepository.save(user);

      // Update Redis cache (Task 3)
      try {
         String versionKey = "user_token_version:" + user.getId();
         redisTemplate.opsForValue().set(versionKey, String.valueOf(user.getTokenVersion()), 30, TimeUnit.DAYS);
      } catch (Exception ex) {
         log.error("Failed to update token version cache in Redis: {}", ex.getMessage());
      }

      // If requested, logout from all devices
      if (logoutAll) {
         log.info("Revoking all sessions for user: {} due to password reset", email);
         sessionService.revokeAllUserSessions(user.getId());
      }

      // Invalidate token after use
      redisTemplate.delete(redisKey);
      log.info("Password reset successfully for user: {}", email);
   }

   @Override
   @Transactional
   public AuthResponse loginWithOAuth2(String provider, OAuth2UserInfo userInfo, String ip, String userAgent,
         HttpServletResponse response, boolean rememberMe, String oldSessionId) {
      // Check if social account already exists
      String providerUpper = provider.toUpperCase();
      SocialAccount socialAccount = socialAccountRepository.findByProviderAndProviderId(providerUpper, userInfo.getId())
            .orElse(null);

      User user;
      UserProfile profile = null;
      Set<String> roles;

      if (socialAccount != null) {
         // User already exists, fetch them
         user = socialAccount.getUser();
         roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
         profile = userProfileRepository.findById(user.getId()).orElse(null);

         // Sync profile info if changed
         if (profile != null) {
            boolean changed = false;
            if (userInfo.getPicture() != null && !userInfo.getPicture().equals(profile.getAvatarUrl())) {
               if (profile.getAvatarUrl() == null || profile.getAvatarUrl().contains("googleusercontent.com")) {
                  profile.setAvatarUrl(userInfo.getPicture());
                  changed = true;
               }
            }
            if (userInfo.getName() != null && !userInfo.getName().equals(profile.getDisplayName())) {
               profile.setDisplayName(userInfo.getName());
               changed = true;
            }
            // Google users are always verified
            if (Boolean.FALSE.equals(profile.getIsVerified())) {
               profile.setIsVerified(true);
               changed = true;
            }
            if (changed) {
               profile = userProfileRepository.save(Objects.requireNonNull(profile));
            }
         }
      } else {
         // Check if user with this email already exists
         user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);

         if (user == null) {
            // New User
            Role userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);

            user = User.builder()
                  .username(userInfo.getEmail()) // Use email as default username
                  .email(userInfo.getEmail())
                  .isActive(true)
                  .roles(new HashSet<>(Collections.singleton(userRole)))
                  .build();
            user = userRepository.save(Objects.requireNonNull(user));

            // Update Bloom Filter
            bloomFilterService.add(user.getEmail());

            // Create Profile
            profile = UserProfile.builder()
                  .user(user)
                  .displayName(userInfo.getName())
                  .avatarUrl(userInfo.getPicture())
                  .isVerified(true)
                  .build();
            profile = userProfileRepository.save(Objects.requireNonNull(profile));
         } else {
            // Existing user, just link social account
            profile = userProfileRepository.findById(user.getId()).orElse(null);
         }

         // Link social account
         SocialAccount newSocialAccount = SocialAccount.builder()
               .user(user)
               .provider(providerUpper)
               .providerId(userInfo.getId())
               .build();

         socialAccountRepository.save(Objects.requireNonNull(newSocialAccount));

         roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
      }

      long now = System.currentTimeMillis();

      UUID oldSessionUuid = null;
      if (oldSessionId != null && !oldSessionId.isBlank()) {
         try {
            oldSessionUuid = UUID.fromString(oldSessionId);
         } catch (Exception ex) {
            log.warn("Invalid oldSessionId format in loginWithOAuth2: {}", oldSessionId);
         }
      }

      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, rememberMe, now, now,
            oldSessionUuid);
   }

   @Override
   public RedisSessionInfo getSessionInfo(String sessionId) {
      try {
         return sessionService.getSession(UUID.fromString(sessionId));
      } catch (Exception e) {
         return null;
      }
   }

   @Override
   @Transactional
   public AuthResponse finalizeRegistration(String accessToken, String ip, String userAgent,
         HttpServletResponse response, String oldSessionId) {
      try {
         UUID userId = jwtService.extractUserId(accessToken);
         Set<String> roles = jwtService.extractRoles(accessToken);

         User user = userRepository.findById(userId)
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
         UserProfile profile = userProfileRepository.findById(userId).orElse(null);

         // Clean up registration cache keys in Redis
         try {
            UUID originalRequestId = jwtService.extractSessionId(accessToken);
            if (originalRequestId != null) {
               String reqIdStr = originalRequestId.toString();
               redisTemplate.delete("registration_result:" + reqIdStr);
               redisTemplate.delete("registration_token:" + reqIdStr);
            }
         } catch (Exception ex) {
            log.warn("Failed to clean up registration Redis cache: {}", ex.getMessage());
         }

         long now = System.currentTimeMillis();
         // For finalize registration, we don't have 'rememberMe' info from the original
         // request yet,
         // defaulting to false or we could pass it. Let's default to false for safety.
         UUID oldSessionUuid = null;
         if (oldSessionId != null && !oldSessionId.isBlank()) {
            try {
               oldSessionUuid = UUID.fromString(oldSessionId);
            } catch (Exception ex) {
               log.warn("Invalid oldSessionId format in finalizeRegistration: {}", oldSessionId);
            }
         }
         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, false, now, now,
               oldSessionUuid);
      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Failed to finalize registration for token", e);
         throw new AppException(ErrorCode.INVALID_TOKEN);
      }
   }

   @Override
   @Transactional
   public AuthResponse loginWith2FA(TwoFactorLoginRequest request, String ip, String userAgent,
         HttpServletResponse response, String oldSessionId) {
      if (!bloomFilterService.mightContain(request.getEmail())) {
         throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

      if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
         throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      if (!twoFactorAuthService.verifyCode(user.getId(), request.getCode())) {
         throw new AppException(ErrorCode.INVALID_OTP);
      }

      // OTP code verified successfully! Clear Redis failure keys
      try {
         String emailClean = request.getEmail().trim().toLowerCase();
         redisTemplate.delete("login_failures:" + emailClean);
         redisTemplate.delete("login_failures_ip:" + ip);
      } catch (Exception ex) {
         log.error("Failed to delete login failure keys in Redis during 2FA", ex);
      }

      Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

      UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
      long now = System.currentTimeMillis();

      UUID oldSessionUuid = null;
      if (oldSessionId != null && !oldSessionId.isBlank()) {
         try {
            oldSessionUuid = UUID.fromString(oldSessionId);
         } catch (Exception ex) {
            log.warn("Invalid oldSessionId format in loginWith2FA: {}", oldSessionId);
         }
      }

      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, request.isRememberMe(), now,
            now, oldSessionUuid);
   }

   @Override
   public boolean checkEmail(String email) {
      if (email == null || email.isBlank()) {
         return false;
      }
      String cleanEmail = email.trim().toLowerCase();
      if (bloomFilterService.mightContain(cleanEmail)) {
         return userRepository.findByEmail(cleanEmail)
               .map(User::getIsActive)
               .orElse(false);
      }
      return false;
   }

   @Override
   @Transactional
   public AuthResponse activateGuest(String token, String password, String ip, String userAgent,
         HttpServletResponse response, String oldSessionId) {
      UUID userId = verificationService.verifyToken(token);
      if (userId == null) {
         throw new AppException(ErrorCode.INVALID_TOKEN);
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      user.setPassword(passwordEncoder.encode(password));
      user.setIsActive(true);
      userRepository.save(user);

      UserProfile profile = userProfileRepository.findById(userId).orElse(null);
      if (profile != null) {
         profile.setIsVerified(true);
         userProfileRepository.save(profile);
      }

      Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

      long now = System.currentTimeMillis();

      UUID oldSessionUuid = null;
      if (oldSessionId != null && !oldSessionId.isBlank()) {
         try {
            oldSessionUuid = UUID.fromString(oldSessionId);
         } catch (Exception ex) {
            log.warn("Invalid oldSessionId format in activateGuest: {}", oldSessionId);
         }
      }

      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, false, now, now,
            oldSessionUuid);
   }

   @Override
   public RegistrationStatusResponse getRegistrationStatus(String requestId) {
      UUID reqId;
      try {
         reqId = UUID.fromString(requestId);
      } catch (Exception e) {
         throw new AppException(ErrorCode.INVALID_TOKEN, "Invalid requestId format");
      }

      // 1. Rate Limiting Check: 10 requests per 1 minute per requestId
      String limitKey = "rate_limit:registration_status:" + requestId;
      // Capacity = 10, refillRate = 10.0 / 60.0 = 0.1667 (tokens per second)
      if (!checkRateLimit(limitKey, 10, 0.1667)) {
         meterRegistry.counter("registration_status_rate_limited_total").increment();
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED, "Too many status check requests");
      }

      // 2. Track Polling Fallback (first status check counts as fallback initiation)
      String pollingKey = "registration_polling_started:" + requestId;
      Boolean isFirstPoll = redisTemplate.opsForValue().setIfAbsent(pollingKey, "true", 10, TimeUnit.MINUTES);
      if (Boolean.TRUE.equals(isFirstPoll)) {
         meterRegistry.counter("registration_polling_fallback_total").increment();
      }

      // 3. Try Redis first
      String redisKey = "registration_result:" + requestId;
      String redisVal = redisTemplate.opsForValue().get(redisKey);

      RegistrationStatusResponse response;
      if (redisVal != null) {
         String status = redisVal;
         String message = "Status retrieved from cache";
         Instant completedAt = null;

         if (redisVal.startsWith("{")) {
            try {
               Map<?, ?> map = objectMapper.readValue(redisVal, Map.class);
               status = (String) map.get("status");
               if (map.containsKey("message")) {
                  message = (String) map.get("message");
               }
               if (map.containsKey("completedAt")) {
                  completedAt = Instant.parse((String) map.get("completedAt"));
               }
            } catch (Exception e) {
               log.error("Failed to parse registration result JSON from Redis", e);
            }
         } else {
            if (redisVal.startsWith("FAILED")) {
               status = "FAILED";
               message = redisVal;
            }
         }

         response = RegistrationStatusResponse.builder()
               .requestId(requestId)
               .status(status)
               .message(message)
               .completedAt(completedAt)
               .build();
      } else {
         // 4. Fallback to DB
         RegistrationInbox inbox = registrationInboxRepository.findById(reqId)
               .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration request not found"));

         String message = "Status retrieved from database";
         if (inbox.getStatus() == RegistrationInboxStatus.FAILED
               || inbox.getStatus() == RegistrationInboxStatus.FAILED_PERMANENT) {
            message = inbox.getLastError() != null ? inbox.getLastError() : "Processing failed";
         }

         response = RegistrationStatusResponse.builder()
               .requestId(requestId)
               .status(inbox.getStatus().name())
               .message(message)
               .completedAt(inbox.getProcessedAt())
               .build();
      }

      // 5. Track Polling Success (first successful status check completion)
      if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
         String successPolledKey = "registration_polling_success_tracked:" + requestId;
         Boolean isFirstSuccess = redisTemplate.opsForValue().setIfAbsent(successPolledKey, "true", 10,
               TimeUnit.MINUTES);
         if (Boolean.TRUE.equals(isFirstSuccess)) {
            meterRegistry.counter("registration_polling_success_total").increment();
         }
      }

      return response;
   }

   private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

   private boolean checkRateLimit(String limitKey, int capacity, double refillRate) {
      String script = "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refill_rate = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = 1\n" +
            "local bucket = redis.call('hmget', key, 'tokens', 'last_updated')\n" +
            "local tokens = tonumber(bucket[1])\n" +
            "local last_updated = tonumber(bucket[2])\n" +
            "if not tokens then\n" +
            "    tokens = capacity\n" +
            "    last_updated = now\n" +
            "else\n" +
            "    local delta = math.max(0, now - last_updated)\n" +
            "    tokens = math.min(capacity, tokens + (delta * refill_rate / 1000.0))\n" +
            "end\n" +
            "if tokens >= requested then\n" +
            "    tokens = tokens - requested\n" +
            "    redis.call('hset', key, 'tokens', tokens, 'last_updated', now)\n" +
            "    redis.call('pexpire', key, math.ceil(capacity * 1000.0 / refill_rate))\n" +
            "    return 1\n" +
            "else\n" +
            "    redis.call('hset', key, 'tokens', tokens, 'last_updated', now)\n" +
            "    return 0\n" +
            "end";
      try {
         Long result = redisTemplate.execute(
               new DefaultRedisScript<>(script, Long.class),
               Collections.singletonList(limitKey),
               String.valueOf(capacity),
               String.valueOf(refillRate),
               String.valueOf(System.currentTimeMillis()));
         return result != null && result == 1;
      } catch (Exception e) {
         log.error("Failed to execute rate limit Lua script for key: {}", limitKey, e);
         meterRegistry.counter("auth_redis_fail_closed_total").increment();
         throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
      }
   }

   private long activateChildSession(UUID parentId, UUID childId) {
      String parentKey = "refresh:" + parentId;
      String childKey = "refresh:" + childId;
      String pendingKey = "pending_sessions";
      String script = "local parentExists = redis.call('exists', KEYS[1])\n" +
            "local childExists = redis.call('exists', KEYS[2])\n" +
            "if parentExists == 0 or childExists == 0 then\n" +
            "    return -1\n" +
            "end\n" +
            "local linkedChildId = redis.call('hget', KEYS[1], 'childSessionId')\n" +
            "if linkedChildId ~= ARGV[1] then\n" +
            "    return -3\n" +
            "end\n" +
            "local active = redis.call('hget', KEYS[2], 'active')\n" +
            "if active == 'false' then\n" +
            "    redis.call('hset', KEYS[2], 'active', 'true')\n" +
            "    redis.call('zrem', KEYS[3], ARGV[1])\n" +
            "    return 1\n" +
            "elseif active == 'true' then\n" +
            "    redis.call('zrem', KEYS[3], ARGV[1])\n" +
            "    return 0\n" +
            "end\n" +
            "return -2";
      try {
         Long result = redisTemplate.execute(
               new DefaultRedisScript<>(script, Long.class),
               Arrays.asList(parentKey, childKey, pendingKey),
               childId.toString());
         return result != null ? result : -2;
      } catch (Exception e) {
         log.error("Failed to execute activateChildSession Lua script", e);
         return -2;
      }
   }

   private void handleSecurityIntegrityViolation(RedisSessionInfo parent, RedisSessionInfo child, String classification,
         String ip, String userAgent) {
      log.error("SECURITY INTEGRITY VIOLATION DETECTED: Classification = {}. Parent = {}, Child = {}",
            classification, parent != null ? parent.getRefreshTokenId() : "null",
            child != null ? child.getRefreshTokenId() : "null");
      UUID userId = parent != null ? parent.getUserId() : (child != null ? child.getUserId() : null);
      if (userId != null) {
         sessionService.revokeAllUserSessions(userId);
      }
      writeSecurityAuditEvent(userId, parent != null ? parent.getRefreshTokenId() : null,
            parent != null ? parent.getRefreshFamilyId() : null,
            parent != null ? parent.getRefreshTokenId() : null,
            classification, ip, userAgent);
      meterRegistry.counter("auth_refresh_replay_detected_total").increment();
   }

   private void writeSecurityAuditEvent(UUID userId, UUID sessionId, UUID refreshFamilyId, UUID refreshTokenId,
         String classification, String ip, String userAgent) {
      long timestamp = System.currentTimeMillis();
      String auditSecret = appProperties.getSecurity().getAuditSecret();

      String input = (userId != null ? userId.toString() : "null") + ":"
            + (sessionId != null ? sessionId.toString() : "null") + ":"
            + timestamp + ":"
            + classification;

      String auditHash = "";
      try {
         Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
         SecretKeySpec secretKeySpec = new SecretKeySpec(
               auditSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
         sha256_HMAC.init(secretKeySpec);
         byte[] hash = sha256_HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8));
         auditHash = HexFormat.of().formatHex(hash);
      } catch (Exception e) {
         log.error("Failed to generate audit hash", e);
      }

      Map<String, Object> logPayload = new HashMap<>();
      logPayload.put("userId", userId);
      logPayload.put("sessionId", sessionId);
      logPayload.put("refreshFamilyId", refreshFamilyId);
      logPayload.put("refreshTokenId", refreshTokenId);
      logPayload.put("classification", classification);
      logPayload.put("ip", ip);
      logPayload.put("userAgent", userAgent);
      logPayload.put("timestamp", timestamp);
      logPayload.put("auditHash", auditHash);

      try {
         auditLogger.info(objectMapper.writeValueAsString(logPayload));
      } catch (Exception e) {
         log.error("Failed to write structured security audit log", e);
      }
   }

   private AuthResponse rotateSessionAndBuildResponse(User user, Set<String> roles, UserProfile profile,
         String ip, String userAgent, HttpServletResponse response, RedisSessionInfo info, UUID oldSessionId) {
      UUID childSessionId = UuidCreator.getTimeOrderedEpoch();
      UUID childRefreshToken = UuidCreator.getTimeOrderedEpoch();

      byte[] randomBytes = new byte[32];
      secureRandom.nextBytes(randomBytes);
      String childFingerprint = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
      String fgpHash = SecurityUtils.hashFingerprint(childFingerprint);

      String childAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getUsername(), user.getEmail(), roles,
            childSessionId, fgpHash, user.getTokenVersion(),
            info.getSessionVersion() + 1,
            appProperties.getSecurity().getActiveFingerprintPepperVersion());

      String encryptedChildCredentials;
      try {
         Map<String, String> credsMap = new HashMap<>();
         credsMap.put("accessToken", childAccessToken);
         credsMap.put("refreshToken", childRefreshToken.toString());
         String plainText = objectMapper.writeValueAsString(credsMap);
         String keyVer = appProperties.getSecurity().getCredentialEncryptionKeyVersion();
         String cipherText = encryptionService.encrypt(plainText, keyVer);
         encryptedChildCredentials = keyVer + ":" + cipherText;
      } catch (Exception e) {
         log.error("Failed to prepare and encrypt child credentials", e);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      String childCsrfNonce = UUID.randomUUID().toString();

      long now = System.currentTimeMillis();
      long slidingMs = info.isRememberMe() ? SESSION_SLIDING_REMEMBER_ME_MS : SESSION_SLIDING_NORMAL_MS;
      long hardCapMs = info.isRememberMe() ? SESSION_HARD_CAP_REMEMBER_ME_MS : SESSION_HARD_CAP_NORMAL_MS;
      long remainingHardCapMs = hardCapMs - (now - info.getCreatedAt());
      long finalTtlMs = Math.min(slidingMs, remainingHardCapMs);
      if (finalTtlMs < 0) {
         finalTtlMs = 0;
      }

      RedisSessionInfo childSession = RedisSessionInfo.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(info.getFullName())
            .roles(roles)
            .hashedRefreshToken(passwordEncoder.encode(childRefreshToken.toString()))
            .ip(ip)
            .userAgent(userAgent)
            .createdAt(info.getCreatedAt())
            .lastAccessedAt(now)
            .rememberMe(info.isRememberMe())
            .deviceVersion(1)
            .platform(getOsFamily(userAgent))
            .browserFamily(getBrowserFamily(userAgent))
            .csrfNonce(childCsrfNonce)
            .refreshFamilyId(info.getRefreshFamilyId() != null ? info.getRefreshFamilyId() : oldSessionId)
            .refreshTokenId(childRefreshToken)
            .parentTokenId(info.getRefreshTokenId())
            .used(false)
            .sessionVersion(info.getSessionVersion() + 1)
            .active(false) // Pending state
            .build();

      try {
         sessionService.saveSession(childSessionId, childSession, finalTtlMs);
      } catch (Exception e) {
         log.error("Failed to save pending child session", e);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      String rotateScript = "local oldExists = redis.call('exists', KEYS[1])\n" +
            "if oldExists == 0 then return -1 end\n" +
            "local used = redis.call('hget', KEYS[1], 'used')\n" +
            "if used == 'true' then return -2 end\n" +
            "redis.call('hset', KEYS[1], 'used', 'true')\n" +
            "redis.call('hset', KEYS[1], 'childSessionId', ARGV[1])\n" +
            "redis.call('hset', KEYS[1], 'rotationTimestamp', ARGV[3])\n" +
            "redis.call('hset', KEYS[1], 'encryptedChildCredentials', ARGV[4])\n" +
            "redis.call('pexpire', KEYS[1], tonumber(ARGV[2]))\n" +
            "return 1";

      long gracePeriodMs = appProperties.getSecurity().getRefreshGracePeriodMs();
      Long rotateResult;
      try {
         rotateResult = redisTemplate.execute(
               new DefaultRedisScript<>(rotateScript, Long.class),
               Collections.singletonList("refresh:" + oldSessionId),
               childSessionId.toString(),
               String.valueOf(gracePeriodMs),
               String.valueOf(now),
               encryptedChildCredentials);
      } catch (Exception e) {
         log.error("Failed to execute rotate_session Lua script", e);
         sessionService.deleteSession(childSessionId);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      if (rotateResult == null || rotateResult == -1) {
         sessionService.deleteSession(childSessionId);
         CookieUtils.clearAuthCookies(response, isCookieSecure());
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      if (rotateResult == -2) {
         // Concurrency collision! Another thread rotated this session. Clean up
         // childSession.
         sessionService.deleteSession(childSessionId);

         // Reload parent session to decrypt credentials issued by the winning thread
         RedisSessionInfo updatedParent = sessionService.getSession(oldSessionId);
         if (updatedParent == null || updatedParent.getEncryptedChildCredentials() == null) {
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         String storedBlob = updatedParent.getEncryptedChildCredentials();
         int colonIndex = storedBlob.indexOf(':');
         String credVersion = storedBlob.substring(0, colonIndex);
         String cipherText = storedBlob.substring(colonIndex + 1);

         String decryptedJson;
         try {
            decryptedJson = encryptionService.decrypt(cipherText, credVersion);
         } catch (Exception e) {
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         String childAccess;
         String childRefresh;
         try {
            Map<?, ?> creds = objectMapper.readValue(decryptedJson, Map.class);
            childAccess = (String) creds.get("accessToken");
            childRefresh = (String) creds.get("refreshToken");
         } catch (Exception e) {
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         UUID actualChildId = updatedParent.getChildSessionId();
         RedisSessionInfo actualChildSession = sessionService.getSession(actualChildId);
         if (actualChildSession == null) {
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         // Try to activate it in case it hasn't been activated by the other thread yet
         activateChildSession(oldSessionId, actualChildId);

         CookieUtils.setAuthCookies(response, childAccess, actualChildId.toString(),
               childRefresh, childFingerprint, actualChildSession.getCsrfNonce(), isCookieSecure(),
               (int) ((actualChildSession.getCreatedAt()
                     + (info.isRememberMe() ? SESSION_HARD_CAP_REMEMBER_ME_MS : SESSION_HARD_CAP_NORMAL_MS)
                     - System.currentTimeMillis()) / 1000));

         return AuthResponse.builder()
               .accessToken(childAccess)
               .id(user.getId())
               .email(user.getEmail())
               .fullName(actualChildSession.getFullName())
               .roles(new java.util.ArrayList<>(roles))
               .build();
      }

      // Activate Child Session (Lua CAS)
      long activateResult = activateChildSession(oldSessionId, childSessionId);
      if (activateResult != 1 && activateResult != 0) {
         log.error("Failed to activate child session via CAS, result: {}", activateResult);
         sessionService.deleteSession(childSessionId);
         if (activateResult == -3) {
            handleSecurityIntegrityViolation(info, childSession, "SECURITY_CHAIN_INTEGRITY_VIOLATION", ip, userAgent);
         } else {
            writeSecurityAuditEvent(info.getUserId(), oldSessionId, info.getRefreshFamilyId(), info.getRefreshTokenId(),
                  "UNKNOWN_CHILD_SESSION", ip, userAgent);
            sessionService.revokeAllUserSessions(info.getUserId());
         }
         CookieUtils.clearAuthCookies(response, isCookieSecure());
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      CookieUtils.setAuthCookies(response, childAccessToken, childSessionId.toString(),
            childRefreshToken.toString(), childFingerprint, childCsrfNonce, isCookieSecure(),
            (int) (finalTtlMs / 1000));

      return AuthResponse.builder()
            .accessToken(childAccessToken)
            .id(user.getId())
            .email(user.getEmail())
            .fullName(childSession.getFullName())
            .roles(new java.util.ArrayList<>(roles))
            .build();
   }

   @PreDestroy
   public void shutdownScheduler() {
      log.info("Shutting down lockRenewalScheduler...");
      lockRenewalScheduler.shutdown();
      try {
         if (!lockRenewalScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            lockRenewalScheduler.shutdownNow();
         }
      } catch (InterruptedException e) {
         lockRenewalScheduler.shutdownNow();
         Thread.currentThread().interrupt();
      }
   }

}
