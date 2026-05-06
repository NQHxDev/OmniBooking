package com.omnibooking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

   private final String errorCode;
   private final HttpStatus status;

   public AppException(String errorCode, String message, HttpStatus status) {
      super(message);
      this.errorCode = errorCode;
      this.status = status;
   }

}
