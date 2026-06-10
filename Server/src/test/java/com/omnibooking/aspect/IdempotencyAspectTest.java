package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.annotation.Idempotent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.exception.IdempotencyConflictException;
import com.omnibooking.exception.IdempotencyResponseNotReplayableException;
import com.omnibooking.model.IdempotencyKey;
import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import com.omnibooking.util.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

public class IdempotencyAspectTest {

   @Mock
   private IdempotencyKeyRepository idempotencyKeyRepository;

   @Mock
   private MeterRegistry meterRegistry;

   @Mock
   private Counter counter;

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

   private Idempotent idempotentAnnotation;

   private MockedStatic<RequestContextHolder> mockedRequestContextHolder;

   private MockedStatic<SecurityUtils> mockedSecurityUtils;

   private AutoCloseable closeable;

   @BeforeEach
   void setUp() {
      closeable = MockitoAnnotations.openMocks(this);
      objectMapper = new ObjectMapper();

      when(meterRegistry.counter(anyString())).thenReturn(counter);

      idempotencyAspect = new IdempotencyAspect(idempotencyKeyRepository, objectMapper, meterRegistry);

      idempotentAnnotation = new Idempotent() {
         @Override
         public Class<? extends Annotation> annotationType() {
            return Idempotent.class;
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
      when(request.getHeader("Idempotency-Key")).thenReturn(null);
      when(request.getHeader("X-Idempotency-Key")).thenReturn(null);

      AppException exception = assertThrows(AppException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      assertEquals(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.getCode(), exception.getErrorCode());
   }

   @Test
   void shouldProceedAndCacheResponseForNewRequestWithResponseEntity() throws Throwable {
      String key = "test-key-1";
      when(request.getHeader("Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");

      // Mock insert succeeds (inserted = 1)
      when(idempotencyKeyRepository.insertIdempotencyKey(any(UUID.class), eq(key), eq("POST /api/test"), anyString(),
            any(Instant.class), any(Instant.class)))
            .thenReturn(1);

      // Business logic returns ResponseEntity
      ResponseEntity<String> businessResult = ResponseEntity.ok("Success Body");
      when(joinPoint.proceed()).thenReturn(businessResult);

      // Execute
      Object result = idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation);

      // Assertions
      assertNotNull(result);
      assertTrue(result instanceof ResponseEntity);
      assertEquals(businessResult, result);

      // Verify stored in DB
      verify(idempotencyKeyRepository).findById(any(UUID.class));
   }

   @Test
   void shouldReturnCachedResponseEntityForDuplicateRequest() throws Throwable {
      String key = "test-key-2";
      when(request.getHeader("Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");

      // Mock insert fails (inserted = 0)
      when(idempotencyKeyRepository.insertIdempotencyKey(any(UUID.class), eq(key), eq("POST /api/test"), anyString(),
            any(Instant.class), any(Instant.class)))
            .thenReturn(0);

      // Hash is match (computeRequestHash will return empty string or "hash-failed"
      // on mock exceptions, let's say "hash-failed")
      IdempotencyKey existingKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /api/test")
            .requestHash("hash-failed")
            .processingStatus("COMPLETED")
            .responseStatus(202)
            .responsePayload("\"Cached Data\"")
            .responseCached(true)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .processingStartedAt(Instant.now())
            .build();

      when(idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, "POST /api/test"))
            .thenReturn(Optional.of(existingKey));

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
      when(request.getHeader("Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");

      when(idempotencyKeyRepository.insertIdempotencyKey(any(UUID.class), eq(key), eq("POST /api/test"), anyString(),
            any(Instant.class), any(Instant.class)))
            .thenReturn(0);

      IdempotencyKey existingKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /api/test")
            .requestHash("hash-failed")
            .processingStatus("PROCESSING")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .processingStartedAt(Instant.now()) // Not stale yet
            .build();

      when(idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, "POST /api/test"))
            .thenReturn(Optional.of(existingKey));

      AppException exception = assertThrows(AppException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      assertEquals(ErrorCode.IDEMPOTENCY_KEY_PROCESSING.getCode(), exception.getErrorCode());
      verify(joinPoint, never()).proceed();
   }

   @Test
   void shouldThrowConflictExceptionWhenRequestHashDiffers() throws Throwable {
      String key = "test-key-4";
      when(request.getHeader("Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");

      when(idempotencyKeyRepository.insertIdempotencyKey(any(UUID.class), eq(key), eq("POST /api/test"), anyString(),
            any(Instant.class), any(Instant.class)))
            .thenReturn(0);

      IdempotencyKey existingKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /api/test")
            .requestHash("different-hash") // Does not match "hash-failed"
            .processingStatus("COMPLETED")
            .build();

      when(idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, "POST /api/test"))
            .thenReturn(Optional.of(existingKey));

      assertThrows(IdempotencyConflictException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      verify(joinPoint, never()).proceed();
   }

   @Test
   void shouldThrowResponseNotReplayableWhenResponseCachedIsFalse() throws Throwable {
      String key = "test-key-5";
      when(request.getHeader("Idempotency-Key")).thenReturn(key);
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");

      when(idempotencyKeyRepository.insertIdempotencyKey(any(UUID.class), eq(key), eq("POST /api/test"), anyString(),
            any(Instant.class), any(Instant.class)))
            .thenReturn(0);

      IdempotencyKey existingKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /api/test")
            .requestHash("hash-failed")
            .processingStatus("COMPLETED")
            .responseCached(false) // Not replayable
            .build();

      when(idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, "POST /api/test"))
            .thenReturn(Optional.of(existingKey));

      assertThrows(IdempotencyResponseNotReplayableException.class,
            () -> idempotencyAspect.handleIdempotency(joinPoint, idempotentAnnotation));

      verify(joinPoint, never()).proceed();
   }

   public ResponseEntity<String> dummyResponseEntityMethod() {
      return null;
   }

}
