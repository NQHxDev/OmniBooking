package com.omnibooking.exception;

public class IdempotencyConflictException extends RuntimeException {

   public IdempotencyConflictException(String message) {
      super(message);
   }

}
