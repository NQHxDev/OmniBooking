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
import jakarta.servlet.http.HttpServletResponse;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.dto.RegistrationStatusResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
   public AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response) {
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

         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, request.isRememberMe(), now,
               now, null);
      } catch (AppException e) {
         if (e.getErrorEnum() == ErrorCode.INVALID_CREDENTIALS) {
            // Increment failed login counts in Redis (15-minute TTL)
            try {
               redisTemplate.opsForValue().increment(emailKey);
               redisTemplate.expire(emailKey, 15, TimeUnit.MINUTES);
               
               redisTemplate.opsForValue().increment(ipKey);
               redisTemplate.expire(ipKey, 15, TimeUnit.MINUTES);
            } catch (Exception ex) {
               log.error("Failed to increment login failure count in Redis", ex);
            }
         }
         throw e;
      }
   }

   @Override
   public AuthResponse refresh(String sessionId, String refreshToken, String ip, String userAgent,
         HttpServletResponse response) {
      UUID sId;
      UUID rToken;
      try {
         sId = UUID.fromString(sessionId);
         rToken = UUID.fromString(refreshToken);
      } catch (IllegalArgumentException e) {
         log.error("Invalid UUID format for session or refresh token: session={}, refresh={}", sessionId, refreshToken);
         CookieUtils.clearAuthCookies(response, isCookieSecure());
         throw new AppException(ErrorCode.INVALID_SESSION);
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
                     lockValue
               );
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

         if (!sessionService.isValidSession(sId, rToken)) {
            CookieUtils.clearAuthCookies(response, isCookieSecure());
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         RedisSessionInfo info = sessionService.getSession(sId);

         User user = userRepository.findById(Objects.requireNonNull(info.getUserId()))
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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

         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, info.isRememberMe(),
               info.getCreatedAt(), info.getLastAccessedAt(), sId);
      } finally {
         if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
         }
         String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
         redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey),
               lockValue);
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

      String fingerprint = UuidCreator.getTimeOrderedEpoch().toString();
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);

      String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail(), roles,
            sessionId, fgpHash, user.getTokenVersion());

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
            isCookieSecure(), (int) (finalTtlMs / 1000));

      return userMapper.toAuthResponse(user, profile, roles);
   }

   @Override
   public void clearAllCookies(HttpServletResponse response) {
      CookieUtils.clearAuthCookies(response, isCookieSecure());
   }

   @Override
   @Transactional
   public AuthResponse upgradeToPartner(UUID userId, String ip, String userAgent, HttpServletResponse response,
         boolean rememberMe) {
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
      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, rememberMe, now, now, null);
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
         HttpServletResponse response, boolean rememberMe) {
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
      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, rememberMe, now, now, null);
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
         HttpServletResponse response) {
      try {
         UUID userId = jwtService.extractUserId(accessToken);
         Set<String> roles = jwtService.extractRoles(accessToken);

         User user = userRepository.findById(userId)
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
         UserProfile profile = userProfileRepository.findById(userId).orElse(null);

         long now = System.currentTimeMillis();
         // For finalize registration, we don't have 'rememberMe' info from the original
         // request yet,
         // defaulting to false or we could pass it. Let's default to false for safety.
         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, false, now, now, null);
      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Failed to finalize registration for token", e);
         throw new AppException(ErrorCode.INVALID_TOKEN);
      }
   }

   @Override
   @Transactional
   public AuthResponse loginWith2FA(com.omnibooking.dto.TwoFactorLoginRequest request, String ip, String userAgent,
         HttpServletResponse response) {
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
      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, request.isRememberMe(), now,
            now, null);
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
         HttpServletResponse response) {
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
      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response, false, now, now, null);
   }

   @Override
   public RegistrationStatusResponse getRegistrationStatus(String requestId) {
      UUID reqId;
      try {
         reqId = UUID.fromString(requestId);
      } catch (Exception e) {
         throw new AppException(ErrorCode.INVALID_TOKEN, "Invalid requestId format");
      }

      // 1. Try Redis first
      String redisKey = "registration_result:" + requestId;
      String redisVal = redisTemplate.opsForValue().get(redisKey);

      if (redisVal != null) {
         String status = redisVal;
         String message = "Status retrieved from cache";
         if (redisVal.startsWith("FAILED")) {
            status = "FAILED";
            message = redisVal;
         }
         return RegistrationStatusResponse.builder()
               .requestId(requestId)
               .status(status)
               .message(message)
               .completedAt(null)
               .build();
      }

      // 2. Fallback to DB
      RegistrationInbox inbox = registrationInboxRepository.findById(reqId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration request not found"));

      String message = "Status retrieved from database";
      if (inbox.getStatus() == RegistrationInboxStatus.FAILED || inbox.getStatus() == RegistrationInboxStatus.FAILED_PERMANENT) {
         message = inbox.getLastError() != null ? inbox.getLastError() : "Processing failed";
      }

      return RegistrationStatusResponse.builder()
            .requestId(requestId)
            .status(inbox.getStatus().name())
            .message(message)
            .completedAt(inbox.getProcessedAt())
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
