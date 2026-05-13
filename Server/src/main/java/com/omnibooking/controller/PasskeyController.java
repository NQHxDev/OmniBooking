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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import java.util.UUID;
import java.util.List;
import com.omnibooking.dto.auth.passkey.PasskeyResponse;

@RestController
@RequestMapping("/auth/passkey")
@RequiredArgsConstructor
public class PasskeyController {

   private final PasskeyService passkeyService;

   @PostMapping("/register/options")
   public ResponseEntity<ApiResponse<PasskeyRegistrationOptionsResponse>> getRegistrationOptions(
         @AuthenticationPrincipal UserPrincipal principal) {
      if (principal == null) throw new InsufficientAuthenticationException("User must be authenticated");
      PasskeyRegistrationOptionsResponse options = passkeyService.generateRegistrationOptions(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(options, "Registration options generated", null));
   }

   @PostMapping("/register/verify")
   public ResponseEntity<ApiResponse<Void>> verifyRegistration(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody PasskeyRegistrationVerifyRequest request) {
      if (principal == null) throw new InsufficientAuthenticationException("User must be authenticated");
      passkeyService.verifyRegistration(principal.getId(), request);
      return ResponseEntity.ok(ApiResponse.success(null, "Passkey registered successfully", null));
   }

   @GetMapping("/status")
   public ResponseEntity<ApiResponse<Boolean>> checkPasskeyStatus(
         @AuthenticationPrincipal UserPrincipal principal) {
      if (principal == null) throw new InsufficientAuthenticationException("User must be authenticated");
      boolean hasPasskeys = passkeyService.hasPasskeys(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(hasPasskeys, "Passkey status fetched", null));
   }

   @GetMapping
   public ResponseEntity<ApiResponse<List<PasskeyResponse>>> listPasskeys(
         @AuthenticationPrincipal UserPrincipal principal) {
      if (principal == null) throw new InsufficientAuthenticationException("User must be authenticated");
      List<PasskeyResponse> passkeys = passkeyService.listPasskeys(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(passkeys, "Passkeys fetched successfully", null));
   }

   @DeleteMapping("/{passkeyId}")
   public ResponseEntity<ApiResponse<Void>> deletePasskey(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID passkeyId) {
      if (principal == null) throw new InsufficientAuthenticationException("User must be authenticated");
      passkeyService.deletePasskey(principal.getId(), passkeyId);
      return ResponseEntity.ok(ApiResponse.success(null, "Passkey deleted successfully", null));
   }

}
