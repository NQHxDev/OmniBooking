package com.omnibooking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
   // Auth Errors
   USER_ALREADY_EXISTS("AUTH_001", "User already exists", HttpStatus.BAD_REQUEST),
   EMAIL_ALREADY_EXISTS("AUTH_002", "Email already exists", HttpStatus.BAD_REQUEST),
   INVALID_CREDENTIALS("AUTH_003", "Invalid username or password", HttpStatus.UNAUTHORIZED),
   ROLE_NOT_FOUND("AUTH_004", "Required role not found", HttpStatus.NOT_FOUND),
   INVALID_SESSION("AUTH_005", "Invalid session or refresh token", HttpStatus.UNAUTHORIZED),
   TOKEN_EXPIRED("AUTH_006", "Access token has expired", HttpStatus.UNAUTHORIZED),
   INVALID_TOKEN("AUTH_007", "Invalid or expired verification token", HttpStatus.BAD_REQUEST),
   USER_NOT_FOUND("AUTH_008", "User not found", HttpStatus.NOT_FOUND),

   // General Errors
   UNCATEGORIZED_EXCEPTION("GEN_999", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
   INVALID_KEY("GEN_001", "Uncategorized error", HttpStatus.BAD_REQUEST),
   UNAUTHORIZED("GEN_002", "You do not have permission", HttpStatus.FORBIDDEN),
   NOT_FOUND("GEN_003", "Resource not found", HttpStatus.NOT_FOUND),
   INTERNAL_SERVER_ERROR("GEN_500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
   CSRF_TOKEN_INVALID("SEC_001", "CSRF token mismatch or missing", HttpStatus.FORBIDDEN),

   // Idempotency Errors
   IDEMPOTENCY_KEY_REQUIRED("IDEM_001", "X-Idempotency-Key header is required", HttpStatus.BAD_REQUEST),
   IDEMPOTENCY_KEY_PROCESSING("IDEM_002", "Request is already being processed", HttpStatus.CONFLICT);

   private final String code;
   private final String message;
   private final HttpStatus status;

   ErrorCode(String code, String message, HttpStatus status) {
      this.code = code;
      this.message = message;
      this.status = status;
   }
}
