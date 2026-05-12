package com.omnibooking.exception;

import com.omnibooking.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

   private static final org.slf4j.Logger logRequestError = org.slf4j.LoggerFactory.getLogger("com.omnibooking.request.error");

   @ExceptionHandler(AppException.class)
   public ResponseEntity<ApiResponse<Object>> handleAppException(
         AppException ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      logRequestError.warn("[{}] AppException: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
      
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
      
      logRequestError.warn("[{}] Validation failed: {}", requestId, errors);
      
      return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation failed", ErrorCode.INVALID_KEY.getCode(), errors, requestId));
   }

   @ExceptionHandler(org.springframework.web.bind.MissingRequestCookieException.class)
   public ResponseEntity<ApiResponse<Object>> handleMissingCookieException(
         org.springframework.web.bind.MissingRequestCookieException ex,
         jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INVALID_SESSION;
      
      logRequestError.warn("[{}] Missing cookie: {}", requestId, ex.getMessage());
      
      return ResponseEntity.status(Objects.requireNonNull(error.getStatus()))
            .body(ApiResponse.error("Session expired or invalid. Please login again.", error.getCode(), null,
                  requestId));
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ApiResponse<Object>> handleAllExceptions(
         Exception ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;
      
      log.error("[{}] UNEXPECTED ERROR: ", requestId, ex);
      
      return ResponseEntity.status(Objects.requireNonNull(error.getStatus()))
            .body(ApiResponse.error(error.getMessage(), error.getCode(), ex.getMessage(),
                  requestId));
   }

}
