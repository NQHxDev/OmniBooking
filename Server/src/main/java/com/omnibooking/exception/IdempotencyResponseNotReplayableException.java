package com.omnibooking.exception;

public class IdempotencyResponseNotReplayableException extends RuntimeException {

   public IdempotencyResponseNotReplayableException(String message) {
      super(message);
   }

}
