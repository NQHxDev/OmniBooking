package com.omnibooking.exception;

import com.omnibooking.config.observability.ModuleTagResolver;
import com.omnibooking.dto.ApiResponse;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

   @ExceptionHandler(AppException.class)
   public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex, HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");

      return ResponseEntity.status(Objects.requireNonNull(ex.getStatus()))
            .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode(), null, requestId));
   }

   @ExceptionHandler(IdempotencyConflictException.class)
   public ResponseEntity<Map<String, String>> handleIdempotencyConflictException(IdempotencyConflictException ex) {
      Map<String, String> response = new HashMap<>();
      response.put("error", "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
   }

   @ExceptionHandler(IdempotencyResponseNotReplayableException.class)
   public ResponseEntity<Map<String, String>> handleIdempotencyResponseNotReplayableException(
         IdempotencyResponseNotReplayableException ex) {
      Map<String, String> response = new HashMap<>();
      response.put("error", "IDEMPOTENCY_RESPONSE_NOT_REPLAYABLE");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
   }

   private String getLeafFieldName(List<JsonMappingException.Reference> path) {
      if (path == null || path.isEmpty()) {
         return "parameter";
      }
      for (int i = path.size() - 1; i >= 0; i--) {
         String fieldName = path.get(i).getFieldName();
         if (fieldName != null && !fieldName.isEmpty()) {
            return fieldName;
         }
      }
      return "parameter";
   }

   private String getLeafFieldName(String fieldPath) {
      if (fieldPath == null || fieldPath.isEmpty()) {
         return "parameter";
      }
      int lastDot = fieldPath.lastIndexOf('.');
      String leaf = (lastDot != -1) ? fieldPath.substring(lastDot + 1) : fieldPath;
      int bracket = leaf.indexOf('[');
      if (bracket != -1) {
         leaf = leaf.substring(0, bracket);
      }
      return leaf.isEmpty() ? fieldPath : leaf;
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
         MethodArgumentNotValidException ex, HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      Map<String, String> errors = new HashMap<>();
      ex.getBindingResult().getAllErrors().forEach((error) -> {
         String fieldName = ((FieldError) error).getField();
         String errorMessage = error.getDefaultMessage();
         errors.put(getLeafFieldName(fieldName), errorMessage);
      });

      return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation failed", ErrorCode.INVALID_KEY.getCode(), errors, requestId));
   }

   @ExceptionHandler(MissingRequestCookieException.class)
   public ResponseEntity<ApiResponse<Object>> handleMissingCookieException(
         MissingRequestCookieException ex, HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INVALID_SESSION;

      return ResponseEntity.status(Objects.requireNonNull(error.getStatus()))
            .body(ApiResponse.error("Session expired or invalid. Please login again.", error.getCode(), null,
                  requestId));
   }

   @ExceptionHandler(HttpMessageNotReadableException.class)
   public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
         HttpMessageNotReadableException ex, HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      String message = "Invalid JSON payload or format";

      Throwable cause = ex.getCause();
      if (cause instanceof InvalidFormatException) {
         InvalidFormatException ife = (InvalidFormatException) cause;
         String fieldName = getLeafFieldName(ife.getPath());
         message = "Invalid value provided for field '" + fieldName + "'";
      } else if (cause instanceof JsonMappingException) {
         JsonMappingException jme = (JsonMappingException) cause;
         String fieldName = getLeafFieldName(jme.getPath());
         message = "Invalid format for field '" + fieldName + "'";
      }

      return ResponseEntity.badRequest()
            .body(ApiResponse.error(message, ErrorCode.INVALID_KEY.getCode(), null, requestId));
   }

   @ExceptionHandler(AsyncRequestTimeoutException.class)
   public ResponseEntity<Void> handleAsyncRequestTimeoutException() {
      // @formatter:off
      // Async request timeout is a normal lifecycle completion event (e.g. for SSE streams), not an error.
      // Returning 200 OK completes the request cleanly from Spring's perspective.
      // @formatter:on
      return ResponseEntity.ok().build();
   }

   @ExceptionHandler(IOException.class)
   public ResponseEntity<Void> handleIOException(IOException ex) {
      if (ex.getMessage() != null
            && (ex.getMessage().contains("Client disconnect") || ex.getMessage().contains("Broken pipe"))) {
         log.debug("Client disconnected during async request: {}", ex.getMessage());
         return ResponseEntity.ok().build();
      }

      throw new RuntimeException(ex);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {

      String requestId = (String) request.getAttribute("requestId");
      ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;

      log.error("[{}] UNEXPECTED ERROR: ", requestId, ex);

      // Check if Sentry has already captured this exception (via LoggingAspect)
      if (request.getAttribute("sentry_captured") == null) {
         String module = ModuleTagResolver.resolveModule(ex.getClass());
         Sentry.setTag("module", module);
         Sentry.captureException(ex);
         request.setAttribute("sentry_captured", Boolean.TRUE);
      }

      return ResponseEntity.status(Objects.requireNonNull(error.getStatus()))
            .body(ApiResponse.error(error.getMessage(), error.getCode(), ex.getMessage(),
                  requestId));
   }

}
