package com.omnibooking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

   private final String errorCode;

   private final HttpStatus status;

   private final ErrorCode errorEnum;

   public AppException(String errorCode, String message, HttpStatus status) {
      super(message);
      this.errorCode = errorCode;
      this.status = status;
      this.errorEnum = null;
   }

   public AppException(ErrorCode errorCode) {
      super(errorCode.getMessage());
      this.errorCode = errorCode.getCode();
      this.status = errorCode.getStatus();
      this.errorEnum = errorCode;
   }

   public AppException(ErrorCode errorCode, String customMessage) {
      super(customMessage);
      this.errorCode = errorCode.getCode();
      this.status = errorCode.getStatus();
      this.errorEnum = errorCode;
   }

}
