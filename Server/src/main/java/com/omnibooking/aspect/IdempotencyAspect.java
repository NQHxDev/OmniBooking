package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.annotation.Idempotent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.web.bind.annotation.RequestBody;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

   private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

   private static final String REDIS_PREFIX = "idempotency:";

   private static final String PROCESSING_VALUE = "PROCESSING";

   private final StringRedisTemplate redisTemplate;

   private final ObjectMapper objectMapper;

   @Around("@annotation(idempotent)")
   public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
      HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
      String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
      if (key == null || key.isBlank()) {
         key = request.getHeader("Idempotency-Key");
      }

      if (key == null || key.isBlank()) {
         log.warn("Missing Idempotency-Key or X-Idempotency-Key for idempotent endpoint: {}", request.getRequestURI());
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
      }

      String redisKey = getRedisKey(request, key);

      // Compute deterministic hash of request body
      String requestBodyHash = computeRequestHash(joinPoint);

      // Đánh dấu đang xử lý (Lock)
      Boolean isNewKey = redisTemplate.opsForValue().setIfAbsent(redisKey, PROCESSING_VALUE, 5, TimeUnit.MINUTES);

      if (Boolean.FALSE.equals(isNewKey)) {
         // Key đã tồn tại, kiểm tra xem đang xử lý hay đã có cache
         String cachedResponse = redisTemplate.opsForValue().get(redisKey);

         if (cachedResponse != null) {
            if (PROCESSING_VALUE.equals(cachedResponse)) {
               log.warn("Request with key {} is already being processed", key);
               throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
            }

            // Trả về kết quả từ cache
            log.info("Returning cached response for idempotency key: {}", key);
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            CacheValue cacheValue = objectMapper.readValue(cachedResponse, CacheValue.class);

            // Same key + different payload → 409 Conflict
            if (cacheValue.getRequestHash() != null
                  && !cacheValue.getRequestHash().equals(requestBodyHash)) {
               throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }

            if (cacheValue.isResponseEntity()) {
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
               if (cacheValue.getBody() != null) {
                  JavaType type = objectMapper.getTypeFactory().constructType(bodyType);
                  body = objectMapper.readValue(cacheValue.getBody(), type);
               }
               return ResponseEntity.status(cacheValue.getStatusCode()).body(body);
            } else {
               if (cacheValue.getBody() == null) {
                  return null;
               }
               JavaType type = objectMapper.getTypeFactory().constructType(signature.getReturnType());
               return objectMapper.readValue(cacheValue.getBody(), type);
            }
         }

         // Dự phòng trong trường hợp key vừa hết hạn giữa setIfAbsent và get
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      }

      try {
         // Thực thi nghiệp vụ
         Object result = joinPoint.proceed();

         // Lưu kết quả vào Redis
         CacheValue cacheValue;
         if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            String bodyJson = responseEntity.getBody() != null
                  ? objectMapper.writeValueAsString(responseEntity.getBody())
                  : null;
            cacheValue = new CacheValue(responseEntity.getStatusCode().value(), bodyJson, true, requestBodyHash);
         } else {
            String bodyJson = result != null ? objectMapper.writeValueAsString(result) : null;
            cacheValue = new CacheValue(200, bodyJson, false, requestBodyHash);
         }

         String jsonResponse = objectMapper.writeValueAsString(cacheValue);
         redisTemplate.opsForValue().set(redisKey, jsonResponse, idempotent.expiration(),
               Objects.requireNonNull(idempotent.timeUnit()));

         return result;
      } catch (Throwable e) {
         // Bắt Throwable thay vì Exception để tránh rò rỉ khóa khi có Error nghiêm trọng
         log.error("Error occurred while processing idempotent request, releasing lock key: {}", key, e);
         redisTemplate.delete(redisKey);
         throw e;
      }
   }

   private String computeRequestHash(ProceedingJoinPoint joinPoint) {
      try {
         Object[] args = joinPoint.getArgs();
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
         return null;  // Graceful degradation — skip hash validation
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

   private String getRedisKey(HttpServletRequest request, String key) {
      UUID userId = SecurityUtils.getCurrentUserId();
      String userIdStr = (userId != null) ? userId.toString() : "anonymous";
      return REDIS_PREFIX + request.getMethod() + ":" + request.getRequestURI() + ":" + userIdStr + ":" + key;
   }

   /**
    * Wrapper class to store cached response metadata and serialized body.
    */
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   private static class CacheValue {
      private int statusCode;
      private String body;
      private boolean isResponseEntity;
      private String requestHash;  // SHA-256 of request body
   }

}
