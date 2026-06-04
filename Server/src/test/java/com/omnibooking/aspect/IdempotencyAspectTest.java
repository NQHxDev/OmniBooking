package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

public class IdempotencyAspectTest {

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private ValueOperations<String, String> valueOperations;

   private ObjectMapper objectMapper;

   private IdempotencyAspect idempotencyAspect;

   @Mock
   private ProceedingJoinPoint joinPoint;

   @Mock
   private MethodSignature methodSignature;

   @Mock
   private HttpServletRequest request;

   @Mock
   private ServletRequestAttributes requestAttributes;

   private com.omnibooking.annotation.Idempotent idempotentAnnotation;

   private MockedStatic<RequestContextHolder> mockedRequestContextHolder;

   private MockedStatic<SecurityUtils> mockedSecurityUtils;

   private AutoCloseable closeable;

   @BeforeEach
   void setUp() {
      closeable = org.mockito.MockitoAnnotations.openMocks(this);
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      objectMapper = new ObjectMapper();

      idempotencyAspect = new IdempotencyAspect(redisTemplate, objectMapper);

      // Instantiate annotation as an anonymous class to avoid Mockito proxying issues
      // with annotations
      idempotentAnnotation = new com.omnibooking.annotation.Idempotent() {
         @Override
         public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return com.omnibooking.annotation.Idempotent.class;
         }

         @Override
         public long expiration() {
            return 24L;
         }

         @Override
         public TimeUnit timeUnit() {
            return TimeUnit.HOURS;
         }
      };

      when(joinPoint.getSignature()).thenReturn(methodSignature);
      when(requestAttributes.getRequest()).thenReturn(request);

      mockedRequestContextHolder = Mockito.mockStatic(RequestContextHolder.class);
      mockedRequestContextHolder.when(RequestContextHolder::currentRequestAttributes).thenReturn(requestAttributes);

      mockedSecurityUtils = Mockito.mockStatic(SecurityUtils.class);
   }

   @AfterEach
   void tearDown() throws Exception {
      mockedRequestContextHolder.close();
      mockedSecurityUtils.close();
      if (closeable != null) {
         closeable.close();
      }
   }

   @Test
   void shouldThrowExceptionWhenIdempotencyKeyIsMissing() {
      when(request.getHeader("X-Idempotency-Key")).thenReturn(null);

      AppException exception = assertThrows(AppException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      assertEquals(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.getCode(), exception.getErrorCode());
   }

   @Test
   void shouldProceedAndCacheResponseForNewRequestWithResponseEntity() throws Throwable {
      String key = "test-key-1";
      UUID userId = UUID.randomUUID();
      when(request.getHeader("X-Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");
      mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

      String expectedRedisKey = "idempotency:POST:/api/test:" + userId + ":" + key;

      // Lock succeeds (is new key)
      when(valueOperations.setIfAbsent(eq(expectedRedisKey), eq("PROCESSING"), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      // Business logic returns ResponseEntity
      ResponseEntity<String> businessResult = ResponseEntity.ok("Success Body");
      when(joinPoint.proceed()).thenReturn(businessResult);

      // Execute
      Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation);

      // Assertions
      assertNotNull(result);
      assertTrue(result instanceof ResponseEntity);
      assertEquals(businessResult, result);

      // Verify stored in Redis with CacheValue format
      verify(valueOperations).set(eq(expectedRedisKey), contains("\"statusCode\":200"), eq(24L), eq(TimeUnit.HOURS));
   }

   @Test
   void shouldReturnCachedResponseEntityForDuplicateRequest() throws Throwable {
      String key = "test-key-2";
      UUID userId = UUID.randomUUID();
      when(request.getHeader("X-Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");
      mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

      String expectedRedisKey = "idempotency:POST:/api/test:" + userId + ":" + key;

      // Lock fails (key already exists)
      when(valueOperations.setIfAbsent(eq(expectedRedisKey), eq("PROCESSING"), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);

      // Cached response exists
      String cachedJson = "{\"statusCode\":202,\"body\":\"\\\"Cached Data\\\"\",\"responseEntity\":true}";
      when(valueOperations.get(expectedRedisKey)).thenReturn(cachedJson);

      // Setup reflection signature
      Method dummyMethod = this.getClass().getMethod("dummyResponseEntityMethod");
      when(methodSignature.getMethod()).thenReturn(dummyMethod);

      // Execute
      Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation);

      // Assertions
      assertNotNull(result);
      assertTrue(result instanceof ResponseEntity);
      ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
      assertEquals(202, responseEntity.getStatusCode().value());
      assertEquals("Cached Data", responseEntity.getBody());

      verify(joinPoint, never()).proceed();
   }

   @Test
   void shouldThrowProcessingExceptionWhenRequestIsAlreadyProcessing() throws Throwable {
      String key = "test-key-3";
      UUID userId = UUID.randomUUID();
      when(request.getHeader("X-Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");
      mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

      String expectedRedisKey = "idempotency:POST:/api/test:" + userId + ":" + key;

      when(valueOperations.setIfAbsent(eq(expectedRedisKey), eq("PROCESSING"), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);
      when(valueOperations.get(expectedRedisKey)).thenReturn("PROCESSING");

      AppException exception = assertThrows(AppException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      assertEquals(ErrorCode.IDEMPOTENCY_KEY_PROCESSING.getCode(), exception.getErrorCode());
      verify(joinPoint, never()).proceed();
   }

   @Test
   void shouldDeleteKeyAndThrowWhenBusinessLogicFails() throws Throwable {
      String key = "test-key-4";
      UUID userId = UUID.randomUUID();
      when(request.getHeader("X-Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");
      mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

      String expectedRedisKey = "idempotency:POST:/api/test:" + userId + ":" + key;

      when(valueOperations.setIfAbsent(eq(expectedRedisKey), eq("PROCESSING"), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

      RuntimeException exception = new RuntimeException("DB Connection Failed");
      when(joinPoint.proceed()).thenThrow(exception);

      // Execute
      RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      assertEquals("DB Connection Failed", thrown.getMessage());
      verify(redisTemplate).delete(expectedRedisKey);
   }

   // Helper methods for reflection tests
   public ResponseEntity<String> dummyResponseEntityMethod() {
      return null;
   }
}
