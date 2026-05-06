package com.omnibooking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

   @Pointcut("within(com.omnibooking.controller..*)")
   public void controllerPointcut() {
   }

   @Before("controllerPointcut()")
   public void logBefore(JoinPoint joinPoint) {
      log.info("Entering: {}.{}() with arguments = {}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            Arrays.toString(joinPoint.getArgs()));
   }

   @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
   public void logAfterReturning(JoinPoint joinPoint, Object result) {
      log.info("Exiting: {}.{}() with result = {}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            result);
   }

}
