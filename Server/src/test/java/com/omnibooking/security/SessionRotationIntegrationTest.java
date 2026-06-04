package com.omnibooking.security;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;

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
   private org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

   @MockitoBean
   private com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository propertyElasticsearchRepository;

   @MockitoBean
   private com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository destinationElasticsearchRepository;

   @MockitoBean
   private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

   @MockitoBean
   private org.springframework.kafka.core.KafkaAdmin kafkaAdmin;

   @MockitoBean
   private org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer;

   @Mock
   private ValueOperations<String, String> valueOperations;

   @Mock
   private ZSetOperations<String, String> zSetOperations;

   private AutoCloseable closeable;

   @BeforeEach
   void setUp() {
      closeable = MockitoAnnotations.openMocks(this);
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
   }

   @AfterEach
   void tearDown() throws Exception {
      if (closeable != null) {
         closeable.close();
      }
   }

   @Test
   void shouldRotateSessionSuccessfully() {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

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
            .roles(Set.of("ROLE_USER"))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      // Mock user and profile retrieval
      Role role = Role.builder().name("ROLE_USER").build();
      User user = User.builder()
            .id(userId)
            .email("test@example.com")
            .roles(Set.of(role))
            .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

      // Mock successful new session save and get
      doNothing().when(sessionService).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());

      // Verification mock: getSession(newSessionId) must return the new session info
      doAnswer(invocation -> {
         UUID id = invocation.getArgument(0);
         if (id.equals(oldSessionId)) {
            return sessionInfo;
         }
         return RedisSessionInfo.builder()
               .userId(userId)
               .username("test@example.com")
               .email("test@example.com")
               .roles(Set.of("ROLE_USER"))
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
      // Ensure saveSession was called for new session
      verify(sessionService, times(1)).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());
      // Ensure deleteSession was called on the old session
      verify(sessionService, times(1)).deleteSession(oldSessionId);
   }

   @Test
   void shouldRollbackSessionRotationWhenSaveSessionFails() {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

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
            .roles(Set.of("ROLE_USER"))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      // Mock user and profile retrieval
      Role role = Role.builder().name("ROLE_USER").build();
      User user = User.builder()
            .id(userId)
            .email("test@example.com")
            .roles(Set.of(role))
            .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Simulate verify fail (saving returns null on get)
      doAnswer(invocation -> {
         UUID id = invocation.getArgument(0);
         if (id.equals(oldSessionId)) {
            return sessionInfo;
         }
         return null; // verification fails!
      }).when(sessionService).getSession(any(UUID.class));

      // Act & Assert
      assertThatThrownBy(() -> authService.refresh(
            oldSessionId.toString(),
            oldRefreshToken.toString(),
            "127.0.0.1",
            "userAgent",
            response)).isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.INVALID_SESSION);

      // Ensure saveSession was called
      verify(sessionService, times(1)).saveSession(any(UUID.class), any(RedisSessionInfo.class), anyLong());
      // Ensure old session was NOT deleted (rollback works!)
      verify(sessionService, never()).deleteSession(oldSessionId);
      // Ensure invalid new session was cleaned up/deleted
      verify(sessionService, times(1)).deleteSession(argThat(id -> !id.equals(oldSessionId)));
   }

   @Test
   void shouldLogoutSuccessfullyWhenSessionOwnedByUser() {
      UUID sessionId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      // Mock session owned by the user
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(sessionId);
      doNothing().when(sessionService).deleteSession(sessionId);

      // Act
      authService.logout(sessionId, userId, response);

      // Assert
      verify(sessionService, times(1)).deleteSession(sessionId);
   }

   @Test
   void shouldFailLogoutWhenSessionOwnedByAnotherUser() {
      UUID sessionId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();

      // Mock session owned by another user
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(otherUserId)
            .username("other@example.com")
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(sessionId);

      // Act & Assert
      assertThatThrownBy(() -> authService.logout(sessionId, userId, response))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.UNAUTHORIZED);

      // Ensure deleteSession was NEVER called
      verify(sessionService, never()).deleteSession(sessionId);
   }

   @Test
   void shouldLogoutGracefullyWhenSessionDoesNotExist() {
      UUID sessionId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      // Mock session does not exist (returns null)
      doReturn(null).when(sessionService).getSession(sessionId);

      // Act
      authService.logout(sessionId, userId, response);

      // Assert - should not throw and should not call deleteSession
      verify(sessionService, never()).deleteSession(sessionId);
   }

   @Test
   void shouldAllowOnlyOneConcurrentRefreshToSucceed() throws Exception {
      UUID oldSessionId = UUID.randomUUID();
      UUID oldRefreshToken = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      // We simulate a real lock behavior using an AtomicBoolean
      AtomicBoolean lockAcquired = new AtomicBoolean(false);
      when(valueOperations.setIfAbsent(startsWith("lock:refresh:"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenAnswer(invocation -> !lockAcquired.getAndSet(true)); // acts like setIfAbsent NX

      // When Lua release script is executed, reset lockAcquired to false
      when(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<Long>>any(),
            ArgumentMatchers.<String>anyList(),
            ArgumentMatchers.<Object>any()))
            .thenAnswer(invocation -> {
               lockAcquired.set(false);
               return 1L;
            });

      // Mock other dependencies
      doReturn(true).when(sessionService).isValidSession(oldSessionId, oldRefreshToken);
      RedisSessionInfo sessionInfo = RedisSessionInfo.builder()
            .userId(userId)
            .username("test@example.com")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .rememberMe(false)
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .build();
      doReturn(sessionInfo).when(sessionService).getSession(oldSessionId);

      Role role = Role.builder().name("ROLE_USER").build();
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
               .roles(Set.of("ROLE_USER"))
               .build();
      }).when(sessionService).getSession(any(UUID.class));

      // Run 20 concurrent requests
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
               latch.await(); // wait for all threads to align
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

      latch.countDown(); // trigger execution of all threads simultaneously
      doneLatch.await(); // wait for all to finish
      executor.shutdown();

      // Exactly one refresh operation must succeed, and all others must fail with
      // INVALID_SESSION
      assertThat(successCount.get()).isEqualTo(1);
      assertThat(failureCount.get()).isEqualTo(threadCount - 1);
      assertThat(invalidSessionCount.get()).isEqualTo(threadCount - 1);
   }

}
