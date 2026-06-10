package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.annotation.Idempotent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.exception.IdempotencyConflictException;
import com.omnibooking.exception.IdempotencyResponseNotReplayableException;
import com.omnibooking.model.IdempotencyKey;
import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

   private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

   private static final String ALT_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

   private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024; // 2 MB

   private final IdempotencyKeyRepository idempotencyKeyRepository;

   private final ObjectMapper objectMapper;

   private final MeterRegistry meterRegistry;

   @Around("@annotation(idempotent)")
   public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
      HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
      String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
      if (key == null || key.isBlank()) {
         key = request.getHeader(ALT_IDEMPOTENCY_KEY_HEADER);
      }

      if (key == null || key.isBlank()) {
         log.warn("Missing Idempotency-Key header for idempotent endpoint: {}", request.getRequestURI());
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
      }

      String endpoint = request.getMethod() + " " + request.getRequestURI();
      String requestBodyHash = computeRequestHash(joinPoint);
      Instant now = Instant.now();
      long expiresMs = idempotent.timeUnit().toMillis(idempotent.expiration());
      Instant expiresAt = now.plusMillis(expiresMs);

      // Try inserting the key with database native INSERT
      UUID id = UuidCreator.getTimeOrderedEpoch();
      int inserted = 0;
      try {
         inserted = idempotencyKeyRepository.insertIdempotencyKey(id, key, endpoint, requestBodyHash, now, expiresAt);
      } catch (DataIntegrityViolationException e) {
         inserted = 0;
      }

      if (inserted == 1) {
         log.info("Successfully acquired new idempotency lock for key: {}, endpoint: {}", key, endpoint);
         return proceedAndSave(joinPoint, id, key, endpoint, requestBodyHash, expiresMs);
      }

      // Key already exists, fetch it and decide what to do
      Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, endpoint);
      if (existingKeyOpt.isEmpty()) {
         // Concurrency race fallback
         meterRegistry.counter("idempotency.processing").increment();
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      }

      IdempotencyKey existingKey = existingKeyOpt.get();

      // Check hash immutability first for all states
      if (!Objects.equals(existingKey.getRequestHash(), requestBodyHash)) {
         log.warn("Idempotency key reused with different request payload. Key: {}, Endpoint: {}", key, endpoint);
         meterRegistry.counter("idempotency.conflict").increment();
         throw new IdempotencyConflictException("Idempotency key reused with different request payload");
      }

      String status = existingKey.getProcessingStatus();
      if ("PROCESSING".equals(status)) {
         Instant staleTime = Instant.now().minus(10, ChronoUnit.MINUTES);
         if (existingKey.getProcessingStartedAt().isBefore(staleTime)) {
            log.info("Detected stale PROCESSING record for key: {}, endpoint: {}. Reclaiming...", key, endpoint);
            int reclaimed = idempotencyKeyRepository.reclaimStaleKey(key, endpoint, requestBodyHash, Instant.now(),
                  staleTime);
            if (reclaimed == 1) {
               meterRegistry.counter("idempotency.reclaimed").increment();
               return proceedAndSave(joinPoint, existingKey.getId(), key, endpoint, requestBodyHash, expiresMs);
            }
         }
         meterRegistry.counter("idempotency.processing").increment();
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      } else if ("FAILED".equals(status)) {
         log.info("Reclaiming failed idempotency record for key: {}, endpoint: {}", key, endpoint);
         int reclaimed = idempotencyKeyRepository.reclaimFailedKey(key, endpoint, requestBodyHash, Instant.now());
         if (reclaimed == 1) {
            meterRegistry.counter("idempotency.reclaimed").increment();
            return proceedAndSave(joinPoint, existingKey.getId(), key, endpoint, requestBodyHash, expiresMs);
         }
         meterRegistry.counter("idempotency.processing").increment();
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      } else if ("COMPLETED".equals(status)) {
         if (!existingKey.isResponseCached()) {
            log.warn("Idempotency response payload was not cached due to size restrictions. Key: {}, Endpoint: {}", key,
                  endpoint);
            throw new IdempotencyResponseNotReplayableException("Idempotency response not replayable");
         }

         log.info("Returning cached response for idempotency key: {}, endpoint: {}", key, endpoint);
         meterRegistry.counter("idempotency.hit").increment();
         return buildReplayedResponse(joinPoint, existingKey);
      }

      meterRegistry.counter("idempotency.processing").increment();
      throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
   }

   private Object proceedAndSave(ProceedingJoinPoint joinPoint, UUID id, String key, String endpoint,
         String requestBodyHash, long expiresMs) throws Throwable {
      Object result;
      try {
         result = joinPoint.proceed();
      } catch (Throwable t) {
         log.error("Error processing business logic for idempotency key: {}, updating to FAILED state.", key, t);
         updateStatus(id, "FAILED", 500, null, false);
         throw t;
      }

      String bodyJson = null;
      boolean responseCached = true;
      int statusCode = 200;

      if (result instanceof ResponseEntity) {
         ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
         statusCode = responseEntity.getStatusCode().value();
         bodyJson = responseEntity.getBody() != null ? objectMapper.writeValueAsString(responseEntity.getBody()) : null;
      } else {
         bodyJson = result != null ? objectMapper.writeValueAsString(result) : null;
      }

      if (bodyJson != null && bodyJson.length() > MAX_RESPONSE_SIZE) {
         log.warn("Response payload size for idempotency key {} exceeded 2MB threshold. Skipping payload cache.", key);
         bodyJson = null;
         responseCached = false;
      }

      updateStatus(id, "COMPLETED", statusCode, bodyJson, responseCached);
      meterRegistry.counter("idempotency.miss").increment();
      return result;
   }

   private void updateStatus(UUID id, String status, Integer responseStatus, String responsePayload,
         boolean responseCached) {
      try {
         idempotencyKeyRepository.findById(id).ifPresent(idempKey -> {
            idempKey.setProcessingStatus(status);
            idempKey.setResponseStatus(responseStatus);
            idempKey.setResponsePayload(responsePayload);
            idempKey.setResponseCached(responseCached);
            idempotencyKeyRepository.saveAndFlush(idempKey);
         });
      } catch (Exception e) {
         log.error("Failed to update idempotency status in database for ID: {}", id, e);
      }
   }

   private Object buildReplayedResponse(ProceedingJoinPoint joinPoint, IdempotencyKey keyRecord) throws Exception {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Class<?> returnType = signature.getMethod().getReturnType();
      if (ResponseEntity.class.isAssignableFrom(returnType)) {
         Type genericReturnType = signature.getMethod().getGenericReturnType();
         Type bodyType = Object.class;
         if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericReturnType;
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class && ResponseEntity.class.isAssignableFrom((Class<?>) rawType)) {
               bodyType = paramType.getActualTypeArguments()[0];
            }
         }
         Object body = null;
         if (keyRecord.getResponsePayload() != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(bodyType);
            body = objectMapper.readValue(keyRecord.getResponsePayload(), type);
         }
         return ResponseEntity.status(keyRecord.getResponseStatus()).body(body);
      } else {
         if (keyRecord.getResponsePayload() == null) {
            return null;
         }
         JavaType type = objectMapper.getTypeFactory().constructType(returnType);
         return objectMapper.readValue(keyRecord.getResponsePayload(), type);
      }
   }

   private String computeRequestHash(ProceedingJoinPoint joinPoint) {
      try {
         Object[] args = joinPoint.getArgs();
         if (args == null) {
            return "hash-failed";
         }
         StringBuilder sb = new StringBuilder();
         for (Object arg : args) {
            if (arg != null && (arg.getClass().isAnnotationPresent(RequestBody.class)
                  || hasRequestBodyAnnotation(joinPoint, arg))) {
               sb.append(objectMapper.writeValueAsString(arg));
            }
         }
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
         return HexFormat.of().formatHex(hash);
      } catch (Exception e) {
         log.warn("Failed to compute request hash, skipping validation", e);
         return "hash-failed";
      }
   }

   private boolean hasRequestBodyAnnotation(ProceedingJoinPoint joinPoint, Object arg) {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      var method = signature.getMethod();
      var annotations = method.getParameterAnnotations();
      Object[] args = joinPoint.getArgs();
      for (int i = 0; i < args.length; i++) {
         if (args[i] == arg) {
            for (var ann : annotations[i]) {
               if (ann.annotationType().equals(RequestBody.class)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

}
