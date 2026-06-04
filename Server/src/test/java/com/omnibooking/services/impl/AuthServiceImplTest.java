package com.omnibooking.services.impl;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.config.AppProperties;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.services.auth.SessionService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.auth.impl.AuthServiceImpl;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.mapper.UserMapper;
import com.omnibooking.security.RedisSessionInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyLong;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

   @Mock
   private AppProperties appProperties;

   @Mock
   private AppProperties.Security securityProperties;

   @Mock
   private UserRepository userRepository;

   @Mock
   private RoleRepository roleRepository;

   @Mock
   private CachedRoleService cachedRoleService;

   @Mock
   private UserProfileRepository userProfileRepository;

   @Mock
   private PasswordEncoder passwordEncoder;

   @Mock
   private JWTService jwtService;

   @Mock
   private SessionService sessionService;

   @Mock
   private ApplicationEventPublisher eventPublisher;

   @Mock
   private VerificationService verificationService;

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private BloomFilterService bloomFilterService;

   @Mock
   private HttpServletResponse response;

   @Mock
   private UserMapper userMapper;

   @Mock
   private MailService mailService;

   @Mock
   private OutboxService outboxService;

   @Mock
   private com.omnibooking.services.auth.TwoFactorAuthService twoFactorAuthService;

   @Mock
   private org.springframework.data.redis.core.ValueOperations<String, String> valueOps;

   @InjectMocks
   private AuthServiceImpl authService;

   @BeforeEach
   void setUp() {
      lenient().when(appProperties.getSecurity()).thenReturn(securityProperties);
      lenient().when(securityProperties.isCookieSecure()).thenReturn(false);
      lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
   }

   @Nested
   @DisplayName("Registration Tests")
   class RegistrationTests {

      @Test
      @DisplayName("Should register successfully when data is valid")
      void shouldRegister_Success() {
         // Arrange
         RegisterRequest request = RegisterRequest.builder()
               .email("test@example.com")
               .password("password123")
               .fullName("Test User")
               .build();
         Role userRole = Role.builder().name("ROLE_USER").build();
         User user = User.builder()
               .id(UUID.randomUUID())
               .email(request.getEmail())
               .username(request.getEmail())
               .roles(Collections.singleton(userRole))
               .build();

         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(false);
         when(cachedRoleService.getRoleByName("ROLE_USER")).thenReturn(userRole);
         when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
         when(userMapper.toUser(any())).thenReturn(user);
         when(userRepository.save(any(User.class))).thenReturn(user);
         when(userProfileRepository.save(any(UserProfile.class))).thenReturn(new UserProfile());
         when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access_token");
         when(sessionService.getSession(any())).thenReturn(RedisSessionInfo.builder().userId(user.getId()).build());
         when(userMapper.toAuthResponse(any(), any(), any())).thenReturn(AuthResponse.builder()
               .email(request.getEmail())
               .build());

         // Act
         AuthResponse result = authService.register(request, "127.0.0.1", "agent", response, false);

         // Assert
         assertThat(result).isNotNull();
         assertThat(result.getEmail()).isEqualTo(request.getEmail());
         verify(userRepository, times(1)).save(any(User.class));
         verify(bloomFilterService, times(1)).add(request.getEmail());
         verify(sessionService, times(1)).saveSession(any(), any(), anyLong());
         verify(outboxService, times(1)).saveEvent(any(), eq("USER"), eq("USER_REGISTERED"), any());
      }

      @Test
      @DisplayName("Should throw exception when email already exists (Bloom Filter hit)")
      void shouldThrowException_WhenEmailExists() {
         // Arrange
         RegisterRequest request = RegisterRequest.builder()
               .email("exists@example.com")
               .password("password")
               .fullName("Name")
               .build();
         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(true);
         when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

         // Act & Assert
         assertThatThrownBy(() -> authService.register(request, "ip", "ua", response, false))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.EMAIL_ALREADY_EXISTS);
      }
   }

   @Nested
   @DisplayName("Login Tests")
   class LoginTests {

      @Test
      @DisplayName("Should login successfully with valid credentials")
      void shouldLogin_Success() {
         // Arrange
         LoginRequest request = LoginRequest.builder()
               .email("test@example.com")
               .password("password123")
               .rememberMe(false)
               .build();
         Role role = Role.builder().name("ROLE_USER").build();
         User user = User.builder()
               .id(UUID.randomUUID())
               .email(request.getEmail())
               .password("hashed_password")
               .roles(Set.of(role))
               .build();

         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(true);
         when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
         when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
         when(twoFactorAuthService.is2FAEnabledForUser(user.getId())).thenReturn(false);
         when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access_token");
         when(sessionService.getSession(any())).thenReturn(RedisSessionInfo.builder().userId(user.getId()).build());
         when(userMapper.toAuthResponse(any(), any(), any())).thenReturn(AuthResponse.builder()
               .email(request.getEmail())
               .build());

         // Act
         AuthResponse result = authService.login(request, "ip", "ua", response);

         // Assert
         assertThat(result).isNotNull();
         assertThat(result.getEmail()).isEqualTo(request.getEmail());
         verify(sessionService).saveSession(any(), any(), anyLong());
      }

      @Test
      @DisplayName("Should reject login immediately if email not in Bloom Filter")
      void shouldReject_WhenEmailNotInBloomFilter() {
         // Arrange
         LoginRequest request = LoginRequest.builder()
               .email("nonexistent@example.com")
               .password("password")
               .build();
         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(false);

         // Act & Assert
         assertThatThrownBy(() -> authService.login(request, "ip", "ua", response))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_CREDENTIALS);

         verify(userRepository, never()).findByEmail(anyString());
      }

      @Test
      @DisplayName("Should throw exception for invalid password")
      void shouldThrow_WhenPasswordInvalid() {
         // Arrange
         LoginRequest request = LoginRequest.builder()
               .email("test@example.com")
               .password("wrong_pass")
               .build();
         User user = User.builder().email(request.getEmail()).password("hashed").build();

         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(true);
         when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
         when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

         // Act & Assert
         assertThatThrownBy(() -> authService.login(request, "ip", "ua", response))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_CREDENTIALS);
      }

      @Test
      @DisplayName("Should throw TWO_FACTOR_REQUIRED when user has 2FA enabled")
      void shouldThrowTwoFactorRequired_When2FAEnabled() {
         // Arrange
         LoginRequest request = LoginRequest.builder()
               .email("test@example.com")
               .password("password123")
               .rememberMe(false)
               .build();
         Role role = Role.builder().name("ROLE_USER").build();
         User user = User.builder()
               .id(UUID.randomUUID())
               .email(request.getEmail())
               .password("hashed_password")
               .roles(Set.of(role))
               .build();

         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(true);
         when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
         when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
         when(twoFactorAuthService.is2FAEnabledForUser(user.getId())).thenReturn(true);

         // Act & Assert
         assertThatThrownBy(() -> authService.login(request, "ip", "ua", response))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.TWO_FACTOR_REQUIRED);
      }
   }

   @Nested
   @DisplayName("Forgot Password Tests")
   class ForgotPasswordTests {

      @Test
      @DisplayName("Should send forgot password email and set token in redis")
      void shouldForgotPassword_Success() {
         // Arrange
         String email = "test@example.com";
         User user = User.builder().id(UUID.randomUUID()).email(email).username(email).build();

         lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

         when(valueOps.increment(anyString())).thenReturn(1L);

         // Act
         authService.forgotPassword(email);

         // Assert
         // Verify rate limit incremented
         verify(valueOps, times(1)).increment(startsWith("rate_limit:"));
         // Verify reset token set
         verify(valueOps, times(1)).set(startsWith("reset_token:"), eq(email), eq(15L), any(TimeUnit.class));
         // Verify outbox event recorded
         verify(outboxService, times(1)).saveEvent(any(), eq("USER"), eq("FORGOT_PASSWORD"), any());
      }

      @Test
      @DisplayName("Should throw RATE_LIMIT_EXCEEDED when request is too frequent")
      void shouldThrow_WhenRateLimitExceeded() {
         // Arrange
         String email = "spam@example.com";
         when(valueOps.increment("rate_limit:forgot_password:" + email)).thenReturn(4L);

         // Act & Assert
         assertThatThrownBy(() -> authService.forgotPassword(email))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.RATE_LIMIT_EXCEEDED);

         verify(userRepository, never()).findByEmail(anyString());
      }
   }

   @Nested
   @DisplayName("Reset Password Tests")
   class ResetPasswordTests {

      @Test
      @DisplayName("Should update password and invalidate token on success")
      void shouldResetPassword_Success() {
         // Arrange
         String token = "valid_token";
         String email = "test@example.com";
         String newPassword = "new_password_123";
         User user = User.builder().email(email).build();

         when(valueOps.get("reset_token:" + token)).thenReturn(email);
         when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
         when(passwordEncoder.encode(newPassword)).thenReturn("hashed_new_password");

         // Act
         authService.resetPassword(token, newPassword, false);

         // Assert
         verify(userRepository).save(Objects.requireNonNull(user));
         assertThat(user.getPassword()).isEqualTo("hashed_new_password");
         verify(redisTemplate).delete("reset_token:" + token);
      }

      @Test
      @DisplayName("Should revoke all sessions when logoutAll is true")
      void shouldRevokeSessions_WhenLogoutAllIsTrue() {
         // Arrange
         String token = "valid_token";
         String email = "test@example.com";
         String newPassword = "new_password_123";
         User user = User.builder().id(UUID.randomUUID()).email(email).build();

         when(valueOps.get("reset_token:" + token)).thenReturn(email);
         when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
         when(passwordEncoder.encode(newPassword)).thenReturn("hashed_new_password");

         // Act
         authService.resetPassword(token, newPassword, true);

         // Assert
         verify(sessionService).revokeAllUserSessions(user.getId());
         verify(userRepository).save(any(User.class));
      }

      @Test
      @DisplayName("Should throw INVALID_RESET_TOKEN for invalid/expired token")
      void shouldThrow_WhenTokenInvalid() {
         // Arrange
         String token = "invalid_token";
         when(valueOps.get(anyString())).thenReturn(null);

         // Act & Assert
         assertThatThrownBy(() -> authService.resetPassword(token, "pass", false))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_RESET_TOKEN);
      }
   }
}
