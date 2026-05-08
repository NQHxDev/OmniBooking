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
import com.omnibooking.services.HashingService;
import com.omnibooking.services.JWTService;
import com.omnibooking.services.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final UserProfileRepository userProfileRepository;
   private final HashingService hashingService;
   private final JWTService jwtService;
   private final SessionService sessionService;
   private final org.springframework.context.ApplicationEventPublisher eventPublisher;
   private final com.omnibooking.services.VerificationService verificationService;

   @Override
   @Transactional
   public AuthResponse register(RegisterRequest request, String ip, String userAgent, HttpServletResponse response) {

      if (userRepository.existsByEmail(request.getEmail())) {
         throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }

      Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

      User user = User.builder()
            .username(request.getEmail())
            .email(request.getEmail())
            .password(hashingService.hash(request.getPassword()))
            .isActive(true)
            .roles(Collections.singleton(userRole))
            .build();

      if (user == null) {
         throw new AppException("BAD_REQUEST", "User not found", HttpStatus.BAD_REQUEST);
      }

      User savedUser = userRepository.save(user);

      // Create Profile
      String[] nameParts = request.getFullName().split(" ", 2);
      String firstName = nameParts[0];
      String lastName = nameParts.length > 1 ? nameParts[1] : "";

      UserProfile profile = UserProfile.builder()
            .user(savedUser)
            .firstName(firstName)
            .lastName(lastName)
            .build();

      if (profile == null) {
         throw new AppException("BAD_REQUEST", "Profile not found", HttpStatus.BAD_REQUEST);
      }

      userProfileRepository.save(profile);

      // Publish Event (For background email)
      eventPublisher.publishEvent(new com.omnibooking.event.UserRegisteredEvent(this, savedUser, request.getFullName()));

      // --- AUTOMATIC LOGIN LOGIC ---
      UUID sessionId = UUID.randomUUID();
      UUID refreshToken = UUID.randomUUID();
      String roleName = userRole.getName();

      String accessToken = jwtService.generateAccessToken(savedUser.getId(), roleName, sessionId);

      // Store in Redis
      sessionService.saveSession(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(),
            request.getFullName(),
            roleName, sessionId, refreshToken, ip, userAgent);

      // Set Cookies
      setAuthCookies(response, accessToken, sessionId.toString(), refreshToken.toString());

      return AuthResponse.builder()
            .id(savedUser.getId())
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .fullName(request.getFullName())
            .roles(Collections.singletonList(roleName))
            .build();
   }

   @Override
   public AuthResponse login(LoginRequest request, String ip, String userAgent, HttpServletResponse response) {
      User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(
                  () -> new AppException(ErrorCode.INVALID_CREDENTIALS));

      if (!hashingService.verify(request.getPassword(), user.getPassword())) {
         throw new AppException(ErrorCode.INVALID_CREDENTIALS);
      }

      String role = user.getRoles().iterator().next().getName();
      UUID sessionId = UUID.randomUUID();
      UUID refreshToken = UUID.randomUUID();

      String accessToken = jwtService.generateAccessToken(user.getId(), role, sessionId);

      // Get full name from profile
      UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
      String fullName = profile != null ? profile.getFirstName() + " " + profile.getLastName() : user.getUsername();

      // Store in Redis using SessionService
      sessionService.saveSession(user.getId(), user.getUsername(), user.getEmail(), fullName, role, sessionId,
            refreshToken, ip,
            userAgent);

      // Set Cookies
      setAuthCookies(response, accessToken, sessionId.toString(), refreshToken.toString());

      return AuthResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(fullName)
            .roles(Collections.singletonList(role))
            .build();
   }

   @Override
   public AuthResponse refresh(String sessionId, String refreshToken, String ip, String userAgent,
         HttpServletResponse response) {
      UUID sId = UUID.fromString(sessionId);
      UUID rToken = UUID.fromString(refreshToken);

      if (!sessionService.isValidSession(sId, rToken)) {
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      RedisSessionInfo info = sessionService.getSession(sId);
      String role = info.getRole();
      String accessToken = jwtService.generateAccessToken(info.getUserId(), role, sId);

      // Optional: Refresh the session in Redis (update timestamp or rotate token)
      // For now, just issue new access token
      setAuthCookies(response, accessToken, sessionId, refreshToken);

      return AuthResponse.builder()
            .id(info.getUserId())
            .username(info.getUsername())
            .email(info.getEmail())
            .fullName(info.getFullName())
            .roles(Collections.singletonList(role))
            .build();
   }

   @Override
   public void logout(UUID sessionId, UUID userId, HttpServletResponse response) {
      sessionService.deleteSession(sessionId);
      clearAuthCookies(response);
   }

   @Override
   @Transactional
   public void verifyEmail(String token) {
      UUID userId = verificationService.verifyToken(token);
      
      if (userId == null) {
         throw new AppException(ErrorCode.INVALID_TOKEN);
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      user.setIsActive(true);
      userRepository.save(user);
      
      log.info("User {} verified successfully", user.getEmail());
   }

   private void setAuthCookies(HttpServletResponse response, String accessToken, String sessionId,
         String refreshToken) {
      addCookie(response, "access_token", accessToken, 15 * 60); // 15 mins
      addCookie(response, "session_id", sessionId, 7 * 24 * 60 * 60); // 7 days
      addCookie(response, "refresh_token", refreshToken, 7 * 24 * 60 * 60); // 7 days
   }

   private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
      Cookie cookie = new Cookie(name, value);
      cookie.setHttpOnly(true);
      cookie.setSecure(false); // Set to true in production
      cookie.setPath("/");
      cookie.setMaxAge(maxAge);
      response.addCookie(cookie);
   }

   private void clearAuthCookies(HttpServletResponse response) {
      deleteCookie(response, "access_token");
      deleteCookie(response, "session_id");
      deleteCookie(response, "refresh_token");
      deleteCookie(response, "csrf_token");
   }

   private void deleteCookie(HttpServletResponse response, String name) {
      Cookie cookie = new Cookie(name, "");
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setMaxAge(0);
      response.addCookie(cookie);
   }
}
