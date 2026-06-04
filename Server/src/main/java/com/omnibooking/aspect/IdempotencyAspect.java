package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.annotation.Idempotent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
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

      String redisKey = REDIS_PREFIX + key;

      // Kiểm tra xem key đã tồn tại chưa
      String cachedResponse = redisTemplate.opsForValue().get(redisKey);

      if (cachedResponse != null) {
         if (PROCESSING_VALUE.equals(cachedResponse)) {
            log.warn("Request with key {} is already being processed", key);
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
         }

         // Trả về kết quả cũ
         log.info("Returning cached response for idempotency key: {}", key);
         MethodSignature signature = (MethodSignature) joinPoint.getSignature();

         // Clean Code: Sử dụng JavaType để tránh cảnh báo Type Safety
         JavaType type = objectMapper.getTypeFactory().constructType(signature.getReturnType());
         return objectMapper.readValue(cachedResponse, type);
      }

      // Đánh dấu đang xử lý (Lock)
      Boolean isNewKey = redisTemplate.opsForValue().setIfAbsent(redisKey, PROCESSING_VALUE, 5, TimeUnit.MINUTES);
      if (Boolean.FALSE.equals(isNewKey)) {
         throw new AppException(ErrorCode.IDEMPOTENCY_KEY_PROCESSING);
      }

      try {
         // Thực thi nghiệp vụ
         Object result = joinPoint.proceed();

         // Lưu kết quả vào Redis
         String jsonResponse = objectMapper.writeValueAsString(result);
         redisTemplate.opsForValue().set(redisKey, Objects.requireNonNull(jsonResponse), idempotent.expiration(),
               Objects.requireNonNull(idempotent.timeUnit()));

         return result;
      } catch (Exception e) {
         // Nếu lỗi, xóa key để có thể retry (hoặc giữ lại tùy business)
         redisTemplate.delete(redisKey);
         throw e;
      }
   }

}
