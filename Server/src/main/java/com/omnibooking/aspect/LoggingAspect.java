package com.omnibooking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.omnibooking.exception.AppException;

import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

   private static final Logger requestSuccessLogger = LoggerFactory.getLogger("com.omnibooking.request.success");
   private static final Logger requestErrorLogger = LoggerFactory.getLogger("com.omnibooking.request.error");

   @Pointcut("within(com.omnibooking.controller..*)")
   public void controllerPointcut() {
   }

   @Around("controllerPointcut()")
   public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
      long start = System.currentTimeMillis();
      String className = joinPoint.getSignature().getDeclaringTypeName();
      String methodName = joinPoint.getSignature().getName();

      String requestId = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(r -> (String) r.getAttribute("requestId"))
            .orElse("unknown");

      log.debug("[{}] Entering: {}.{}() with arguments = {}", requestId, className, methodName,
            Arrays.toString(joinPoint.getArgs()));

      try {
         Object result = joinPoint.proceed();
         long duration = System.currentTimeMillis() - start;

         requestSuccessLogger.info("[{}] SUCCESS [{}ms]: {}.{}() - result = {}",
               requestId, duration, className, methodName, result);

         return result;
      } catch (Throwable e) {
         long duration = System.currentTimeMillis() - start;

         if (e instanceof AppException ||
               e instanceof MethodArgumentNotValidException ||
               e instanceof MissingRequestCookieException) {
            requestErrorLogger.warn("[{}] FAILED [{}ms]: {}.{}() - Exception: {} - Message: {}",
                  requestId, duration, className, methodName, e.getClass().getSimpleName(), e.getMessage());
         } else {
            requestErrorLogger.error("[{}] FAILED [{}ms]: {}.{}() - Exception: {} - Message: {}",
                  requestId, duration, className, methodName, e.getClass().getSimpleName(), e.getMessage());
         }

         throw e;
      }
   }
}
