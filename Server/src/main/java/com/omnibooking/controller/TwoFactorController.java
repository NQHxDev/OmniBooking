package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.TwoFactorLoginRequest;
import com.omnibooking.dto.TwoFactorSetupResponse;
import com.omnibooking.security.Anonymous;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/auth/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

   private final TwoFactorAuthService twoFactorAuthService;

   private final AuthService authService;

   @GetMapping("/status")
   public ResponseEntity<ApiResponse<String>> get2FAStatus(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      String status = twoFactorAuthService.get2FAStatusString(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(status, "2FA status retrieved successfully", requestId));
   }

   @PostMapping("/setup")
   public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2FA(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      TwoFactorSetupResponse response = twoFactorAuthService.initiate2FA(principal.getId());

      return ResponseEntity.ok(ApiResponse.success(response, "2FA setup initiated successfully", requestId));
   }

   @PostMapping("/enable")
   public ResponseEntity<ApiResponse<List<String>>> enable2FA(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody Map<String, String> body,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      String code = body.get("code");
      List<String> backupCodes = twoFactorAuthService.enable2FA(principal.getId(), code);

      return ResponseEntity.ok(ApiResponse.success(backupCodes, "2FA enabled successfully", requestId));
   }

   @PostMapping("/disable")
   public ResponseEntity<ApiResponse<Void>> disable2FA(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody Map<String, String> body,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      String code = body.get("code");
      twoFactorAuthService.disable2FA(principal.getId(), code);

      return ResponseEntity.ok(ApiResponse.success(null, "2FA disabled successfully", requestId));
   }

   @PostMapping("/remove")
   public ResponseEntity<ApiResponse<Void>> remove2FA(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody Map<String, String> body,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      String code = body.get("code");
      twoFactorAuthService.remove2FA(principal.getId(), code);

      return ResponseEntity.ok(ApiResponse.success(null, "2FA removed successfully", requestId));
   }

   @Anonymous
   @PostMapping("/login")
   public ResponseEntity<ApiResponse<AuthResponse>> loginWith2FA(
         @Valid @RequestBody TwoFactorLoginRequest request,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.loginWith2FA(request, ip, userAgent, httpResponse);
      return ResponseEntity.ok(ApiResponse.success(authResponse, "2FA Login successful", requestId));
   }

   private String getClientIp(HttpServletRequest request) {
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
         return xForwardedFor.split(",")[0];
      }
      return request.getRemoteAddr();
   }

}
