package com.omnibooking.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.security.RedisSessionInfo;
import com.omnibooking.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

   private final AuthenticationManager authenticationManager;
   private final JWTService jwtService;
   private final HashingService hashingService;
   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final UserProfileRepository userProfileRepository;

   private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;
   private static final long REFRESH_TOKEN_EXPIRY_MS = REFRESH_TOKEN_EXPIRY_DAYS * 24 * 60 * 60 * 1000;

   @Transactional
   public AuthResponse register(RegisterRequest request) {
      if (userRepository.existsByUsername(request.getUsername())) {
         throw new AppException("USERNAME_ALREADY_EXISTS", "Username is already taken", HttpStatus.BAD_REQUEST);
      }
      if (userRepository.existsByEmail(request.getEmail())) {
         throw new AppException("EMAIL_ALREADY_EXISTS", "Email is already taken", HttpStatus.BAD_REQUEST);
      }

      Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(
                  () -> new AppException("ROLE_NOT_FOUND", "Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));

      User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(hashingService.hash(request.getPassword()))
            .isActive(true)
            .roles(Collections.singleton(userRole))
            .build();

      User savedUser;
      if (user != null) {
         savedUser = Objects.requireNonNull(userRepository.save(user));
      } else {
         throw new AppException("INTERNAL_SERVER_ERROR", "Failed to save user", HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // Create Profile
      String[] nameParts = request.getFullName().split(" ", 2);
      String firstName = nameParts[0];
      String lastName = nameParts.length > 1 ? nameParts[1] : "";

      UserProfile profile = Objects.requireNonNull(UserProfile.builder()
            .user(savedUser)
            .firstName(firstName)
            .lastName(lastName)
            .build());

      userProfileRepository.save(profile);

      return AuthResponse.builder()
            .id(savedUser.getId())
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .roles(Collections.singletonList(userRole.getName()))
            .build();
   }

   public AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response) {
      try {
         Authentication authentication = authenticationManager.authenticate(
               new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

         UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
         String role = user.getAuthorities().stream()
               .map(GrantedAuthority::getAuthority)
               .findFirst()
               .orElse("ROLE_USER");

         // Create SessionID and RefreshToken (UUIDv7)
         UUID sessionId = UuidCreator.getTimeOrderedEpoch();
         UUID refreshToken = UuidCreator.getTimeOrderedEpoch();

         // Generate Access Token
         String accessToken = jwtService.generateAccessToken(user.getId(), role, sessionId);

         // Store in Redis
         saveSessionToRedis(user.getId(), user.getUsername(), user.getEmail(), role, sessionId, refreshToken, ip,
               userAgent);

         // Set Cookies
         setAuthCookies(response, accessToken, sessionId.toString(), refreshToken.toString());

         return AuthResponse.builder()
               .id(user.getId())
               .username(user.getUsername())
               .email(user.getEmail())
               .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
               .build();

      } catch (Exception e) {
         log.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
         throw new AppException("WRONG_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED);
      }
   }

   public AuthResponse refresh(String sessionId, String rawRefreshToken, String ip, String userAgent,
         HttpServletResponse response) {
      String redisKey = "refresh:" + sessionId;
      String json = redisTemplate.opsForValue().get(redisKey);

      if (json == null)
         throw new AppException("SESSION_NOT_FOUND", "Session expired or not found", HttpStatus.UNAUTHORIZED);

      try {
         RedisSessionInfo info = objectMapper.readValue(json, RedisSessionInfo.class);

         // Verify Refresh Token
         if (!hashingService.verify(rawRefreshToken, info.getHashedRefreshToken())) {
            revokeAllUserSessions(info.getUserId());
            clearAuthCookies(response);
            throw new AppException("REFRESH_TOKEN_REUSED", "Refresh token already used. All sessions revoked.",
                  HttpStatus.FORBIDDEN);
         }

         // Rotation: Generate NEW token
         UUID newRefreshToken = UuidCreator.getTimeOrderedEpoch();
         info.setHashedRefreshToken(hashingService.hash(newRefreshToken.toString()));
         info.setIp(ip);
         info.setUserAgent(userAgent);

         // Update Redis
         redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(objectMapper.writeValueAsString(info)),
               REFRESH_TOKEN_EXPIRY_DAYS,
               TimeUnit.DAYS);

         // Generate NEW Access Token
         String newAccessToken = jwtService.generateAccessToken(info.getUserId(), info.getRole(),
               UUID.fromString(sessionId));

         // Update Cookies
         setAuthCookies(response, newAccessToken, sessionId, newRefreshToken.toString());

         return AuthResponse.builder()
               .id(info.getUserId())
               .username(info.getUsername())
               .email(info.getEmail())
               .roles(java.util.List.of(info.getRole()))
               .build();

      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Refresh failed: {}", e.getMessage());
         throw new AppException("INTERNAL_SERVER_ERROR", "Refresh failed", HttpStatus.INTERNAL_SERVER_ERROR);
      }
   }

   public void logout(UUID sessionId, UUID userId, HttpServletResponse response) {
      // 1. Remove from Redis
      redisTemplate.delete("refresh:" + Objects.requireNonNull(sessionId.toString()));
      redisTemplate.opsForZSet().remove("user_sessions:" + Objects.requireNonNull(userId.toString()),
            Objects.requireNonNull(sessionId.toString()));

      // 2. Clear Cookies
      clearAuthCookies(response);
   }

   public void revokeAllUserSessions(UUID userId) {
      String indexKey = "user_sessions:" + Objects.requireNonNull(userId.toString());
      java.util.Set<String> sessionIds = redisTemplate.opsForZSet().range(indexKey, 0, -1);

      if (sessionIds != null) {
         sessionIds.forEach(id -> redisTemplate.delete("refresh:" + Objects.requireNonNull(id)));
      }
      redisTemplate.delete(indexKey);
   }

   private void saveSessionToRedis(UUID userId, String username, String email, String role, UUID sessionId,
         UUID refreshToken, String ip,
         String userAgent) {
      try {
         String redisKey = "refresh:" + sessionId;
         RedisSessionInfo info = RedisSessionInfo.builder()
               .userId(userId)
               .username(username)
               .email(email)
               .role(role)
               .hashedRefreshToken(hashingService.hash(refreshToken.toString()))
               .ip(ip)
               .userAgent(userAgent)
               .createdAt(System.currentTimeMillis())
               .build();

         String json = Objects.requireNonNull(objectMapper.writeValueAsString(info));
         redisTemplate.opsForValue().set(redisKey, json, REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);

         // Add to User Sessions Index (Sorted Set)
         long expiryTimestamp = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS;
         String indexKey = "user_sessions:" + Objects.requireNonNull(userId.toString());
         redisTemplate.opsForZSet().add(indexKey, Objects.requireNonNull(sessionId.toString()), expiryTimestamp);
         redisTemplate.expire(indexKey, REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);

      } catch (Exception e) {
         log.error("Failed to save session to Redis: {}", e.getMessage());
         throw new AppException("INTERNAL_SERVER_ERROR", "Session storage failed", HttpStatus.INTERNAL_SERVER_ERROR);
      }
   }

   private void setAuthCookies(HttpServletResponse response, String accessToken, String sessionId,
         String refreshToken) {
      // Access Token (HttpOnly)
      Cookie accessCookie = new Cookie("access_token", accessToken);
      accessCookie.setHttpOnly(true);
      accessCookie.setPath("/");
      accessCookie.setMaxAge(15 * 60); // 15m

      // Session ID (HttpOnly)
      Cookie sessionCookie = new Cookie("session_id", sessionId);
      sessionCookie.setHttpOnly(true);
      sessionCookie.setPath("/");
      sessionCookie.setMaxAge((int) (REFRESH_TOKEN_EXPIRY_MS / 1000));

      Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
      refreshCookie.setHttpOnly(true);
      refreshCookie.setPath("/api/v1/auth/refresh"); // Only sent to refresh endpoint
      refreshCookie.setMaxAge((int) (REFRESH_TOKEN_EXPIRY_MS / 1000));

      // CSRF Token (NOT HttpOnly)
      String csrfToken = UUID.randomUUID().toString();
      Cookie csrfCookie = new Cookie("csrf_token", csrfToken);
      csrfCookie.setHttpOnly(false); // Accessible by JS
      csrfCookie.setPath("/");
      csrfCookie.setMaxAge((int) (REFRESH_TOKEN_EXPIRY_MS / 1000));

      response.addCookie(accessCookie);
      response.addCookie(sessionCookie);
      response.addCookie(refreshCookie);
      response.addCookie(csrfCookie);
   }

   private void clearAuthCookies(HttpServletResponse response) {
      String[] cookies = { "access_token", "session_id", "refresh_token", "csrf_token" };
      for (String name : cookies) {
         Cookie cookie = new Cookie(name, null);
         cookie.setPath("/");
         cookie.setHttpOnly(true);
         cookie.setMaxAge(0);
         response.addCookie(cookie);
      }
   }
}
