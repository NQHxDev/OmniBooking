package com.omnibooking.exception;

import com.omnibooking.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(AppException.class)
   public ResponseEntity<ApiResponse<Object>> handleAppException(
         AppException ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      return ResponseEntity.status(Objects.requireNonNull(ex.getStatus()))
            .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode(), null, requestId));
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
         MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      Map<String, String> errors = new HashMap<>();
      ex.getBindingResult().getAllErrors().forEach((error) -> {
         String fieldName = ((FieldError) error).getField();
         String errorMessage = error.getDefaultMessage();
         errors.put(fieldName, errorMessage);
      });
      return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation failed", ErrorCode.INVALID_KEY.getCode(), errors, requestId));
   }

   @ExceptionHandler(org.springframework.web.bind.MissingRequestCookieException.class)
   public ResponseEntity<ApiResponse<Object>> handleMissingCookieException(
         org.springframework.web.bind.MissingRequestCookieException ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INVALID_SESSION;
      return ResponseEntity.status(error.getStatus())
            .body(ApiResponse.error("Session expired or invalid. Please login again.", error.getCode(), null, requestId));
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ApiResponse<Object>> handleAllExceptions(
         Exception ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;
      return ResponseEntity.status(Objects.requireNonNull(error.getStatus()))
            .body(ApiResponse.error(error.getMessage(), error.getCode(), ex.getMessage(),
                  requestId));
   }

}
