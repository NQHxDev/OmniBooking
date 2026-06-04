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
         log.warn("Missing X-Idempotency-Key for idempotent endpoint: {}", request.getRequestURI());
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
      }

      String redisKey = getRedisKey(request, key);

      // Đánh dấu đang xử lý (Lock) - sử dụng setIfAbsent trước để tối ưu hóa số lần
      // gọi Redis (1 RTT cho request mới)
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
            cacheValue = new CacheValue(responseEntity.getStatusCode().value(), bodyJson, true);
         } else {
            String bodyJson = result != null ? objectMapper.writeValueAsString(result) : null;
            cacheValue = new CacheValue(200, bodyJson, false);
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
   }

}
