package com.omnibooking.services.impl;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.services.AuthService;
import com.omnibooking.services.JWTService;
import com.omnibooking.services.MailService;
import com.omnibooking.services.SessionService;
import com.omnibooking.services.VerificationService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.mapper.UserMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;
import com.omnibooking.services.BloomFilterService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final UserProfileRepository userProfileRepository;
   private final PasswordEncoder passwordEncoder;
   private final JWTService jwtService;
   private final SessionService sessionService;
   private final org.springframework.context.ApplicationEventPublisher eventPublisher;
   private final VerificationService verificationService;
   private final StringRedisTemplate redisTemplate;
   private final BloomFilterService bloomFilterService;
   private final UserMapper userMapper;
   private final MailService mailService;

   @Value("${app.security.cookie-secure:false}")
   private boolean cookieSecure;

   @Override
   @Transactional
   public AuthResponse register(RegisterRequest request, String ip, String userAgent, HttpServletResponse response) {
      // 1. Check Bloom Filter first (Fast pre-check)
      if (bloomFilterService.mightContain(request.getEmail())) {
         if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
         }
      }

      Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

      User user = userMapper.toUser(request);
      user.setPassword(passwordEncoder.encode(request.getPassword()));
      user.setRoles(Collections.singleton(userRole));

      User savedUser = userRepository.save(Objects.requireNonNull(user));

      // 2. Add to Bloom Filter after successful DB save
      bloomFilterService.add(savedUser.getEmail());

      // Create Profile
      String[] nameParts = request.getFullName().split(" ", 2);
      UserProfile profile = UserProfile.builder()
            .user(savedUser)
            .firstName(nameParts[0])
            .lastName(nameParts.length > 1 ? nameParts[1] : "")
            .build();
      userProfileRepository.save(Objects.requireNonNull(profile));

      // Publish Event
      eventPublisher
            .publishEvent(new com.omnibooking.event.UserRegisteredEvent(this, savedUser, request.getFullName()));

      // Automatic Login
      Set<String> roles = Collections.singleton(userRole.getName());
      return issueTokensAndBuildResponse(savedUser, roles, profile, ip, userAgent, response);
   }

   @Override
   public AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response) {
      // 1. Check Bloom Filter (Security & Performance layer)
      // Nếu Bloom Filter nói không có, chắc chắn là không có -> Reject ngay
      if (!bloomFilterService.mightContain(request.getEmail())) {
         log.warn("Login attempt for non-existent email (blocked by Bloom): {}", request.getEmail());
         throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

      if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
         throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      Set<String> roles = user.getRoles().stream()
            .map(com.omnibooking.model.Role::getName)
            .collect(java.util.stream.Collectors.toSet());

      UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response);
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
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      // LOCKING: Ngăn chặn race condition khi nhiều request refresh cùng lúc cho 1
      // sessionId
      String lockKey = "lock:refresh:" + sId;
      Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "L", 5, TimeUnit.SECONDS);

      if (Boolean.FALSE.equals(acquired)) {
         log.warn("Refresh already in progress for session: {}", sId);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      try {
         if (!sessionService.isValidSession(sId, rToken)) {
            throw new AppException(ErrorCode.INVALID_SESSION);
         }

         RedisSessionInfo info = sessionService.getSession(sId);

         User user = userRepository.findById(Objects.requireNonNull(info.getUserId()))
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

         Set<String> roles = user.getRoles().stream()
               .map(com.omnibooking.model.Role::getName)
               .collect(java.util.stream.Collectors.toSet());

         UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

         // ROTATION: Thu hồi session cũ trước khi cấp mới
         sessionService.deleteSession(sId);
         log.info("Rotating session for user: {}", user.getEmail());

         return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response);
      } finally {
         redisTemplate.delete(lockKey);
      }
   }

   @Override
   public void logout(UUID sessionId, UUID userId, HttpServletResponse response) {
      sessionService.deleteSession(sessionId);
      CookieUtils.clearAuthCookies(response, cookieSecure);
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
   }

   /**
    * Centralized logic to issue tokens, save sessions, and set cookies.
    */
   private AuthResponse issueTokensAndBuildResponse(User user, Set<String> roles, UserProfile profile,
         String ip, String userAgent, HttpServletResponse response) {
      UUID sessionId = UuidCreator.getTimeOrderedEpoch();
      UUID refreshToken = UuidCreator.getTimeOrderedEpoch();

      String fingerprint = UuidCreator.getTimeOrderedEpoch().toString();
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);

      String accessToken = jwtService.generateAccessToken(user.getId(), roles, sessionId, fgpHash);

      String fullName = profile != null ? profile.getFirstName() + " " + profile.getLastName() : user.getUsername();

      sessionService.saveSession(user.getId(), user.getUsername(), user.getEmail(), fullName, roles,
            sessionId, refreshToken, ip, userAgent);

      CookieUtils.setAuthCookies(response, accessToken, sessionId.toString(), refreshToken.toString(), fingerprint,
            cookieSecure);

      return userMapper.toAuthResponse(user, profile, roles);
   }

   @Override
   public void clearAllCookies(HttpServletResponse response) {
      CookieUtils.clearAuthCookies(response, cookieSecure);
   }

   @Override
   @Transactional
   public AuthResponse upgradeToPartner(UUID userId, String ip, String userAgent, HttpServletResponse response) {
      User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      Role partnerRole = roleRepository.findByName("ROLE_PARTNER")
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

      // Add ROLE_PARTNER to the user's roles
      user.getRoles().add(partnerRole);
      userRepository.save(user);

      UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

      log.info("Upgrading user {} to ROLE_PARTNER", user.getEmail());

      Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

      return issueTokensAndBuildResponse(user, roles, profile, ip, userAgent, response);
   }

   @Override
   public void forgotPassword(String email) {
      // 1. Rate Limiting check (3 requests per 1 minute)
      String rateLimitKey = "rate_limit:forgot_password:" + email;
      Long count = redisTemplate.opsForValue().increment(rateLimitKey);
      
      if (count != null && count == 1) {
         redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
      }
      
      if (count != null && count > 3) {
         log.warn("Forgot password rate limit exceeded for email: {}", email);
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      // 2. Security: Always return success even if user doesn't exist
      // But we still need to fetch user to get name and send email
      userRepository.findByEmail(email).ifPresent(user -> {
         String token = UUID.randomUUID().toString();
         String redisKey = "reset_token:" + token;

         // Save to Redis (15 minutes)
         redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(email), 15, TimeUnit.MINUTES);

         UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
         String fullName = profile != null ? profile.getFirstName() + " " + profile.getLastName() : user.getUsername();

         log.info("Sending forgot password email to: {}", email);
         mailService.sendForgotPasswordEmail(email, fullName, token);
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
      userRepository.save(user);

      // 1. If requested, logout from all devices
      if (logoutAll) {
         log.info("Revoking all sessions for user: {} due to password reset", email);
         sessionService.revokeAllUserSessions(user.getId());
      }

      // 2. Invalidate token after use
      redisTemplate.delete(redisKey);
      log.info("Password reset successfully for user: {}", email);
   }

}
