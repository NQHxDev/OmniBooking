package com.omnibooking.exception;

import com.omnibooking.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
      return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors, requestId));
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ApiResponse<Object>> handleAllExceptions(
         Exception ex, jakarta.servlet.http.HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred", ex.getMessage(), requestId));
   }
}
