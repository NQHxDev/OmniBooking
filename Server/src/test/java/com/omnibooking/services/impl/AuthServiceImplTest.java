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
import com.omnibooking.services.BloomFilterService;
import com.omnibooking.services.JWTService;
import com.omnibooking.services.SessionService;
import com.omnibooking.services.VerificationService;
import com.omnibooking.mapper.UserMapper;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

   @Mock
   private UserRepository userRepository;
   @Mock
   private RoleRepository roleRepository;
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

   @InjectMocks
   private AuthServiceImpl authService;

   @BeforeEach
   void setUp() {
      ReflectionTestUtils.setField(Objects.requireNonNull(authService), "cookieSecure", false);
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
         when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
         when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
         when(userMapper.toUser(any())).thenReturn(user);
         when(userRepository.save(Objects.requireNonNull(any(User.class)))).thenReturn(user);
         when(userProfileRepository.save(Objects.requireNonNull(any(UserProfile.class)))).thenReturn(new UserProfile());
         when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access_token");
         when(userMapper.toAuthResponse(any(), any(), any())).thenReturn(AuthResponse.builder()
               .email(request.getEmail())
               .build());

         // Act
         AuthResponse result = authService.register(request, "127.0.0.1", "agent", response);

         // Assert
         assertThat(result).isNotNull();
         assertThat(result.getEmail()).isEqualTo(request.getEmail());
         verify(userRepository, times(1)).save(Objects.requireNonNull(any(User.class)));
         verify(bloomFilterService, times(1)).add(request.getEmail());
         verify(eventPublisher, times(1)).publishEvent(Objects.requireNonNull(any()));
         verify(sessionService, times(1)).saveSession(any(), any(), any(), any(), any(), any(), any(), any(), any());
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
         assertThatThrownBy(() -> authService.register(request, "ip", "ua", response))
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
         LoginRequest request = new LoginRequest("test@example.com", "password123");
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
         when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access_token");
         when(userMapper.toAuthResponse(any(), any(), any())).thenReturn(AuthResponse.builder()
               .email(request.getEmail())
               .build());

         // Act
         AuthResponse result = authService.login(request, "ip", "ua", response);

         // Assert
         assertThat(result).isNotNull();
         assertThat(result.getEmail()).isEqualTo(request.getEmail());
         verify(sessionService).saveSession(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }

      @Test
      @DisplayName("Should reject login immediately if email not in Bloom Filter")
      void shouldReject_WhenEmailNotInBloomFilter() {
         // Arrange
         LoginRequest request = new LoginRequest("nonexistent@example.com", "password");
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
         LoginRequest request = new LoginRequest("test@example.com", "wrong_pass");
         User user = User.builder().email(request.getEmail()).password("hashed").build();

         when(bloomFilterService.mightContain(request.getEmail())).thenReturn(true);
         when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
         when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

         // Act & Assert
         assertThatThrownBy(() -> authService.login(request, "ip", "ua", response))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_CREDENTIALS);
      }
   }
}
