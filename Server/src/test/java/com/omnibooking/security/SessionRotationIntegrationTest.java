package com.omnibooking.security;

import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.security.RoleRepository;
import com.omnibooking.repository.user.UserProfileRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.SessionService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
public class SessionRotationIntegrationTest {

   @Autowired
   private AuthService authService;

   @MockitoSpyBean
   private SessionService sessionService;

   @MockitoBean
   private UserRepository userRepository;

   @MockitoBean
   private RoleRepository roleRepository;

   @MockitoBean
   private UserProfileRepository userProfileRepository;

   @MockitoBean
   private StringRedisTemplate redisTemplate;

   @MockitoBean
   private HttpServletResponse response;

   @MockitoBean
   private EncryptionService encryptionService;

   @MockitoSpyBean
   private com.omnibooking.services.auth.JWTService jwtService;

   @Autowired
   private PasswordEncoder passwordEncoder;

   @MockitoBean
   private ElasticsearchOperations elasticsearchOperations;

   @MockitoBean
   private PropertyElasticsearchRepository propertyElasticsearchRepository;

   @MockitoBean
   private DestinationElasticsearchRepository destinationElasticsearchRepository;

   @MockitoBean
   private KafkaTemplate<String, Object> kafkaTemplate;

   @MockitoBean
   private KafkaAdmin kafkaAdmin;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   @Mock
   private ValueOperations<String, String> valueOperations;

   @Mock
   private ZSetOperations<String, String> zSetOperations;

   @Mock
   private HashOperations<String, Object, Object> hashOperations;

   private AutoCloseable closeable;

   private HttpServletRequest mockRequest;

   private final AtomicBoolean lockAcquired = new AtomicBoolean(false);

   @BeforeEach
   void setUp() {
      closeable = MockitoAnnotations.openMocks(this);
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
      when(redisTemplate.opsForHash()).thenReturn(hashOperations);

      lockAcquired.set(false);

      // Default mock for rate limiting and locks. Only reset lock status when
      // delete/release lock script is executed
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            any(Object[].class)))
            .thenAnswer(invocation -> {
               RedisScript<?> script = invocation.getArgument(0);
               String scriptStr = script.getScriptAsString();
               if (scriptStr != null && scriptStr.contains("redis.call('del'")) {
                  lockAcquired.set(false);
               }
               return 1L;
            });

      // Mock EncryptionService to prevent key id dependency errors
      doReturn("mockCipherText").when(encryptionService).encrypt(anyString(), anyString());
      doReturn("{\"accessToken\":\"newAccess\",\"refreshToken\":\"newRefresh\"}").when(encryptionService)
            .decrypt(anyString(), anyString());

      // Mock request context
      mockRequest = mock(HttpServletRequest.class);
      ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
      RequestContextHolder.setRequestAttributes(attrs);

      // Setup default mock cookies
      Cookie accessTokenCookie = new Cookie(CookieUtils.ACCESS_TOKEN, "mockAccessToken");
      Cookie fingerprintCookie = new Cookie(CookieUtils.FINGERPRINT, "mockFingerprint");
      when(mockRequest.getCookies()).thenReturn(new Cookie[] { accessTokenCookie, fingerprintCookie });

      Claims defaultClaims = Jwts.claims()
            .subject(UUID.randomUUID().toString())
            .add("sv", 1)
            .add("fgh_v", "v1")
            .add("fgh", SecurityUtils.hashFingerprint("mockFingerprint"))
            .build();

      doReturn(defaultClaims).when(jwtService).extractAllClaims(anyString());
      doReturn(SecurityUtils.hashFingerprint("mockFingerprint")).when(jwtService)
            .extractFingerprintHash(anyString());
      doReturn("v1").when(jwtService).extractFingerprintPepperVersion(anyString());
   }

   @AfterEach
   void tearDown() throws Exception {
      RequestContextHolder.resetRequestAttributes();
      if (closeable != null) {
         closeable.close();
      }
   }

   private void mockJwtClaimsForUser(UUID userId, int sessionVersion) {
      Claims claims = Jwts.claims()
            .subject(userId.toString())
            .add("sv", sessionVersion)
            .add("fgh_v", "v1")
            .add("fgh", SecurityUtils.hashFingerprint("mockFingerprint"))
            .build();
      doReturn(claims).when(jwtService).extractAllClaims(anyString());
   }

   @Test
   void shouldRotateSessionSuccessfully() {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      // Mock lock acquisition
      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      // Mock valid session
      doReturn(true).when(sessionService).isValidSession(oldSessionId, oldRefreshToken);

      // Mock RedisSessionInfo
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .sessionVersion(1)
            .active(true)
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      // Mock user and profile retrieval
      Role role = Role.builder().name(SecurityConstants.Roles.USER).build();
      User user = User.builder()
            .id(userId)
            .email("test@example.com")
            .roles(Set.of(role))
            .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

      // Mock successful new session save and get
      doNothing().when(sessionService).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());

      doAnswer(invocation -> {
         UUID id = invocation.getArgument(0);
         if (id.equals(oldSessionId)) {
            return sessionInfo;
         }
         return RedisSessionInfo.builder()
               .userId(userId)
               .username("test@example.com")
               .email("test@example.com")
               .roles(Set.of(SecurityConstants.Roles.USER))
               .createdAt(sessionInfo.getCreatedAt())
               .sessionVersion(2)
               .active(true)
               .build();
      }).when(sessionService).getSession(any(UUID.class));

      // Act
      AuthResponse authResponse = authService.refresh(
            oldSessionId.toString(),
            oldRefreshToken.toString(),
            "127.0.0.1",
            "userAgent",
            response);

      // Assert
      assertThat(authResponse).isNotNull();
      verify(sessionService, times(1)).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());
   }

   @Test
   void shouldEncryptChildCredentialsInRedis_WithKeyVersion() {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
      doReturn(true).when(sessionService).isValidSession(oldSessionId, oldRefreshToken);

      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .sessionVersion(1)
            .active(true)
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      ArgumentCaptor<Object> scriptArgsCaptor = ArgumentCaptor.forClass(Object.class);

      // Act
      authService.refresh(oldSessionId.toString(), oldRefreshToken.toString(), "127.0.0.1", "userAgent", response);

      // Capture arguments of rotate Lua script
      verify(redisTemplate).execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            scriptArgsCaptor.capture(),
            scriptArgsCaptor.capture(),
            scriptArgsCaptor.capture(),
            scriptArgsCaptor.capture());

      List<Object> args = scriptArgsCaptor.getAllValues();
      String encryptedBlob = (String) args.get(3);
      assertThat(encryptedBlob).startsWith("aes-v1:"); // Key version prepended
   }

   @Test
   void shouldRecoverPendingSessionAfterJvmCrash_WithParentLinkValidation() throws Exception {
      UUID parentId = UUID.randomUUID();
      UUID childId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      // Parent session rotated, but child not active yet (used = true)
      String encryptedChildCredentials = "v1:mockCipherText";

      RedisSessionInfo parentSession = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis() - 5000)
            .lastAccessedAt(System.currentTimeMillis())
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .sessionVersion(1)
            .used(true)
            .childSessionId(childId)
            .rotationTimestamp(System.currentTimeMillis() - 1000) // Within grace period
            .encryptedChildCredentials(encryptedChildCredentials)
            .refreshFamilyId(parentId)
            .refreshTokenId(oldRefreshToken)
            .active(true)
            .build();
      doReturn(parentSession).when(sessionService).getSession(parentId);

      RedisSessionInfo childSession = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .createdAt(parentSession.getCreatedAt())
            .sessionVersion(2)
            .parentTokenId(oldRefreshToken)
            .refreshFamilyId(parentId)
            .csrfNonce("childNonce")
            .active(false) // Still pending
            .build();
      doReturn(childSession).when(sessionService).getSession(childId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // CAS activation returns 1 (first-time activation success)
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(Arrays.asList("refresh:" + parentId, "refresh:" + childId, "pending_sessions")),
            any(Object[].class)))
            .thenReturn(1L);

      // Act
      AuthResponse authResponse = authService.refresh(parentId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response);

      // Assert
      assertThat(authResponse).isNotNull();
      assertThat(authResponse.getAccessToken()).isEqualTo("newAccess");
   }

   @Test
   void shouldRejectRecovery_WhenParentChildLinkMismatched() {
      UUID parentId = UUID.randomUUID();
      UUID childId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      RedisSessionInfo parentSession = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .used(true)
            .childSessionId(childId)
            .rotationTimestamp(System.currentTimeMillis() - 1000)
            .build();
      doReturn(parentSession).when(sessionService).getSession(parentId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // CAS activation returns -3 (parent.childSessionId mismatch)
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            eq(Arrays.asList("refresh:" + parentId, "refresh:" + childId, "pending_sessions")),
            any(Object[].class)))
            .thenReturn(-3L);

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(parentId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_SESSION);

      verify(sessionService).revokeAllUserSessions(userId);
   }

   @Test
   void shouldRejectRotation_WhenRefreshFamilyChainTampered() throws Exception {
      UUID parentId = UUID.randomUUID();
      UUID childId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      String encryptedChildCredentials = "v1:mockCipherText";

      RedisSessionInfo parentSession = RedisSessionInfo.builder()
            .userId(userId)
            .used(true)
            .childSessionId(childId)
            .rotationTimestamp(System.currentTimeMillis() - 1000)
            .encryptedChildCredentials(encryptedChildCredentials)
            .refreshFamilyId(parentId)
            .refreshTokenId(oldRefreshToken)
            .build();
      doReturn(parentSession).when(sessionService).getSession(parentId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Child session refreshFamilyId does not match parent session
      RedisSessionInfo childSession = RedisSessionInfo.builder()
            .userId(userId)
            .parentTokenId(oldRefreshToken)
            .refreshFamilyId(UUID.randomUUID()) // Mismatch!
            .active(false)
            .build();
      doReturn(childSession).when(sessionService).getSession(childId);

      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            any(Object[].class)))
            .thenReturn(1L); // CAS succeeds but lineage validation fails in Java

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(parentId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_SESSION);

      verify(sessionService).revokeAllUserSessions(userId);
   }

   @Test
   void shouldRejectRefresh_WhenSessionVersionMismatched() {
      UUID sessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1); // Token has version = 1

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      // Redis Session Version = 2 (incremented after password change or revoke)
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .sessionVersion(2)
            .active(true)
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(sessionId);

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(sessionId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_SESSION);

      verify(sessionService).deleteSession(sessionId);
   }

   @Test
   void shouldRateLimitViaTokenBucket() {
      UUID sessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();

      // Mock rate limit check to return 0 (limit exceeded)
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            any(Object[].class)))
            .thenReturn(0L);

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(sessionId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.RATE_LIMIT_EXCEEDED);
   }

   @Test
   void shouldFailClosedOnRedisOutage() {
      UUID sessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();

      // Simulate Redis Connection Failure during rate limit check
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            any(Object[].class)))
            .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection refused"));

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(sessionId.toString(), oldRefreshToken.toString(), "127.0.0.1",
            "userAgent", response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.SERVICE_UNAVAILABLE);
   }

   @Test
   void shouldDetectGracePeriodAbuse() throws Exception {
      UUID parentId = UUID.randomUUID();
      UUID childId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      String encryptedChildCredentials = "v1:mockCipherText";

      RedisSessionInfo parentSession = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .used(true)
            .childSessionId(childId)
            .rotationTimestamp(System.currentTimeMillis() - 1000)
            .encryptedChildCredentials(encryptedChildCredentials)
            .refreshFamilyId(parentId)
            .refreshTokenId(oldRefreshToken)
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .build();
      doReturn(parentSession).when(sessionService).getSession(parentId);

      RedisSessionInfo childSession = RedisSessionInfo.builder()
            .userId(userId)
            .createdAt(parentSession.getCreatedAt())
            .sessionVersion(2)
            .parentTokenId(oldRefreshToken)
            .refreshFamilyId(parentId)
            .active(true)
            .build();
      doReturn(childSession).when(sessionService).getSession(childId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Mock abuse count = 4 (> 3)
      when(hashOperations.increment("refresh:" + parentId, "concurrencyCount", 1))
            .thenReturn(4L);

      // Act
      authService.refresh(parentId.toString(), oldRefreshToken.toString(), "127.0.0.1", "userAgent", response);

      // Verify abuse meter incremented
      verify(redisTemplate.opsForHash()).increment("refresh:" + parentId, "concurrencyCount", 1);
   }

   @Test
   void shouldPreserveSessionExpirationAcrossRotation() {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
      doReturn(true).when(sessionService).isValidSession(oldSessionId, oldRefreshToken);

      long originalCreatedAt = System.currentTimeMillis() - 10000; // Created 10s ago

      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .rememberMe(false)
            .createdAt(originalCreatedAt)
            .lastAccessedAt(System.currentTimeMillis())
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .sessionVersion(1)
            .active(true)
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      User user = User.builder().id(userId).email("test@example.com")
            .roles(Set.of(Role.builder().name(SecurityConstants.Roles.USER).build())).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      ArgumentCaptor<RedisSessionInfo> sessionInfoCaptor = ArgumentCaptor.forClass(RedisSessionInfo.class);

      // Act
      authService.refresh(oldSessionId.toString(), oldRefreshToken.toString(), "127.0.0.1", "userAgent", response);

      // Capture and verify child session expiration is calculated correctly
      verify(sessionService).saveSession(any(UUID.class), sessionInfoCaptor.capture(), anyLong());
      RedisSessionInfo savedChild = sessionInfoCaptor.getValue();
      assertThat(savedChild.getCreatedAt()).isEqualTo(originalCreatedAt); // Expiration preserved
   }

   @Test
   void shouldAllowOnlyOneConcurrentRefreshToSucceed() throws Exception {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      mockJwtClaimsForUser(userId, 1);

      lockAcquired.set(false);

      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenAnswer(invocation -> !lockAcquired.getAndSet(true));

      // Mock execute method to release the lock correctly and clear lock status
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            anyList(),
            any(Object[].class)))
            .thenAnswer(invocation -> {
               RedisScript<?> script = invocation.getArgument(0);
               String scriptStr = script.getScriptAsString();
               if (scriptStr != null && scriptStr.contains("redis.call('del'")) {
                  lockAcquired.set(false);
               }
               return 1L;
            });

      doReturn(true).when(sessionService).isValidSession(oldSessionId, oldRefreshToken);
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of(SecurityConstants.Roles.USER))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .hashedRefreshToken(passwordEncoder.encode(oldRefreshToken.toString()))
            .sessionVersion(1)
            .active(true)
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      Role role = Role.builder().name(SecurityConstants.Roles.USER).build();
      User user = User.builder()
            .id(userId)
            .email("test@example.com")
            .roles(Set.of(role))
            .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());
      doNothing().when(sessionService).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());

      doAnswer(invocation -> {
         UUID id = invocation.getArgument(0);
         if (id.equals(oldSessionId)) {
            return sessionInfo;
         }
         return RedisSessionInfo.builder()
               .userId(userId)
               .username("test@example.com")
               .email("test@example.com")
               .roles(Set.of(SecurityConstants.Roles.USER))
               .active(true)
               .build();
      }).when(sessionService).getSession(any(UUID.class));

      int threadCount = 20;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threadCount);

      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failureCount = new AtomicInteger(0);
      AtomicInteger invalidSessionCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
         executor.submit(() -> {
            try {
               latch.await();
               authService.refresh(
                     oldSessionId.toString(),
                     oldRefreshToken.toString(),
                     "127.0.0.1",
                     "userAgent",
                     response);
               successCount.incrementAndGet();
            } catch (AppException ex) {
               failureCount.incrementAndGet();
               if (ex.getErrorEnum() == ErrorCode.INVALID_SESSION) {
                  invalidSessionCount.incrementAndGet();
               }
            } catch (Exception e) {
               failureCount.incrementAndGet();
            } finally {
               doneLatch.countDown();
            }
         });
      }

      latch.countDown();
      doneLatch.await();
      executor.shutdown();

      assertThat(successCount.get()).isEqualTo(1);
      assertThat(failureCount.get()).isEqualTo(threadCount - 1);
      assertThat(invalidSessionCount.get()).isEqualTo(threadCount - 1);
   }

}
