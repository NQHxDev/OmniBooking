package com.omnibooking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation to mark an endpoint as idempotent.
 * Requires the 'X-Idempotency-Key' header from the client.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

   /**
    * Expiration time for the idempotency key in Redis.
    */
   long expiration() default 24;

   /**
    * Time unit for expiration.
    */
   TimeUnit timeUnit() default TimeUnit.HOURS;

}
