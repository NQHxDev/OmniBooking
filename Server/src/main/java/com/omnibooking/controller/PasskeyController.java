package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.auth.passkey.PasskeyRegistrationOptionsResponse;
import com.omnibooking.dto.auth.passkey.PasskeyRegistrationVerifyRequest;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.PasskeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.omnibooking.exception.ErrorCode;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import java.util.UUID;
import java.util.List;
import com.omnibooking.dto.auth.passkey.PasskeyResponse;

@RestController
@RequestMapping("/auth/passkey")
@RequiredArgsConstructor
public class PasskeyController {

   private final PasskeyService passkeyService;
   private final com.omnibooking.services.SecurityVerificationService securityVerificationService;

   @PostMapping("/register/options")
   public ResponseEntity<ApiResponse<PasskeyRegistrationOptionsResponse>> getRegistrationOptions(
         @AuthenticationPrincipal UserPrincipal principal,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      if (principal == null)
         throw new InsufficientAuthenticationException("User must be authenticated");

      String requestId = (String) httpRequest.getAttribute("requestId");

      // Kiểm tra xem phiên có được tin tưởng (xác thực OTP trong 30p) không
      if (!securityVerificationService.isSessionTrusted(principal.getId())) {
         return ResponseEntity.status(ErrorCode.SECURITY_VERIFICATION_REQUIRED.getStatus())
               .body(ApiResponse.error(ErrorCode.SECURITY_VERIFICATION_REQUIRED.getMessage(),
                     ErrorCode.SECURITY_VERIFICATION_REQUIRED.getCode(), null, requestId));
      }

      PasskeyRegistrationOptionsResponse options = passkeyService.generateRegistrationOptions(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(options, "Registration options generated", requestId));
   }

   @PostMapping("/register/verify")
   public ResponseEntity<ApiResponse<Void>> verifyRegistration(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody PasskeyRegistrationVerifyRequest request,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      if (principal == null)
         throw new InsufficientAuthenticationException("User must be authenticated");

      String requestId = (String) httpRequest.getAttribute("requestId");
      passkeyService.verifyRegistration(principal.getId(), request);
      return ResponseEntity.ok(ApiResponse.success(null, "Passkey registered successfully", requestId));
   }

   @GetMapping("/status")
   public ResponseEntity<ApiResponse<Boolean>> checkPasskeyStatus(
         @AuthenticationPrincipal UserPrincipal principal,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      if (principal == null)
         throw new InsufficientAuthenticationException("User must be authenticated");

      String requestId = (String) httpRequest.getAttribute("requestId");
      boolean hasPasskeys = passkeyService.hasPasskeys(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(hasPasskeys, "Passkey status fetched", requestId));
   }

   @GetMapping
   public ResponseEntity<ApiResponse<List<PasskeyResponse>>> listPasskeys(
         @AuthenticationPrincipal UserPrincipal principal,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      if (principal == null)
         throw new InsufficientAuthenticationException("User must be authenticated");

      String requestId = (String) httpRequest.getAttribute("requestId");
      List<PasskeyResponse> passkeys = passkeyService.listPasskeys(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(passkeys, "Passkeys fetched successfully", requestId));
   }

   @DeleteMapping("/{passkeyId}")
   public ResponseEntity<ApiResponse<Void>> deletePasskey(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID passkeyId,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      if (principal == null)
         throw new InsufficientAuthenticationException("User must be authenticated");

      String requestId = (String) httpRequest.getAttribute("requestId");

      if (!securityVerificationService.isSessionTrusted(principal.getId())) {
         return ResponseEntity.status(ErrorCode.SECURITY_VERIFICATION_REQUIRED.getStatus())
               .body(ApiResponse.error(ErrorCode.SECURITY_VERIFICATION_REQUIRED.getMessage(),
                     ErrorCode.SECURITY_VERIFICATION_REQUIRED.getCode(), null, requestId));
      }

      passkeyService.deletePasskey(principal.getId(), passkeyId);
      return ResponseEntity.ok(ApiResponse.success(null, "Passkey deleted successfully", requestId));
   }

}
