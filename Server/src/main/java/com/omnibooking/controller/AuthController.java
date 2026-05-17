package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.security.Anonymous;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.ForgotPasswordRequest;
import com.omnibooking.dto.ResetPasswordRequest;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.OAuth2ServiceFactory;
import com.omnibooking.services.user.RegistrationQueueService;
import com.omnibooking.services.communication.SseNotificationService;
import com.omnibooking.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.omnibooking.annotation.Idempotent;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

   private final AuthService authService;

   private final OAuth2ServiceFactory oAuth2ServiceFactory;

   private final AppProperties appProperties;

   private final RegistrationQueueService registrationQueueService;

   private final SseNotificationService sseNotificationService;

   @Anonymous
   @Idempotent
   @PostMapping("/register")
   public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      request.setRequestId(requestId);

      // Push to Redis Queue for Batch Processing
      registrationQueueService.pushToQueue(request);

      return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(null, "Registration request received and is being processed.", requestId));
   }

   @Anonymous
   @PostMapping("/finalize-registration")
   public ResponseEntity<ApiResponse<AuthResponse>> finalizeRegistration(
         @RequestBody Map<String, String> body,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String accessToken = body.get("accessToken");
      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.finalizeRegistration(accessToken, ip, userAgent, httpResponse);
      return ResponseEntity.ok(ApiResponse.success(authResponse, "Session synchronized successfully", requestId));
   }

   @Anonymous
   @GetMapping(value = "/subscribe/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public SseEmitter subscribe(@PathVariable String requestId) {
      return sseNotificationService.subscribe(requestId);
   }

   @Anonymous
   @PostMapping("/login")
   public ResponseEntity<ApiResponse<AuthResponse>> login(
         @Valid @RequestBody LoginRequest request,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.login(request, ip, userAgent, httpResponse);
      return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful", requestId));
   }

   @Anonymous
   @PostMapping("/refresh")
   public ResponseEntity<ApiResponse<AuthResponse>> refresh(
         @CookieValue(name = "session_id", required = false) String sessionId,
         @CookieValue(name = "refresh_token", required = false) String refreshToken,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      if (sessionId == null || refreshToken == null) {
         log.warn("Missing session_id or refresh_token in cookies for refresh request");
         authService.clearAllCookies(httpResponse);
         throw new com.omnibooking.exception.AppException(com.omnibooking.exception.ErrorCode.INVALID_SESSION);
      }

      AuthResponse authResponse = authService.refresh(sessionId, refreshToken, ip, userAgent, httpResponse);
      return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully", requestId));
   }

   @Anonymous
   @PostMapping("/logout")
   public ResponseEntity<ApiResponse<Void>> logout(
         @CookieValue(name = "session_id", required = false) String sessionId,
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String requestId = (String) httpRequest.getAttribute("requestId");

      if (principal != null && sessionId != null) {
         authService.logout(UUID.fromString(sessionId), principal.getId(), httpResponse);
      } else {
         // Even if principal is null, we should try to clear cookies
         authService.clearAllCookies(httpResponse);
      }

      return ResponseEntity.ok(ApiResponse.success(null, "Logout successful", requestId));
   }

   @Anonymous
   @GetMapping("/verify")
   public ResponseEntity<ApiResponse<Void>> verify(
         @RequestParam String token,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.verifyEmail(token);
      return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully", requestId));
   }

   @PostMapping("/resend-verification")
   public ResponseEntity<ApiResponse<Void>> resendVerification(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.resendVerification(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(null, "Verification email resent successfully", requestId));
   }

   @Anonymous
   @PostMapping("/forgot-password")
   public ResponseEntity<ApiResponse<Void>> forgotPassword(
         @Valid @RequestBody ForgotPasswordRequest request,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.forgotPassword(request.getEmail());
      return ResponseEntity
            .ok(ApiResponse.success(null, "If an account exists, a reset link has been sent", requestId));
   }

   @Anonymous
   @PostMapping("/reset-password")
   public ResponseEntity<ApiResponse<Void>> resetPassword(
         @Valid @RequestBody ResetPasswordRequest request,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.resetPassword(request.getToken(), request.getNewPassword(), request.isLogoutAll());
      return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully", requestId));
   }

   @Anonymous
   @GetMapping("/{provider}/url")
   public ResponseEntity<ApiResponse<String>> getOAuthUrl(
         @PathVariable String provider,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      String url = oAuth2ServiceFactory.getService(provider).generateAuthUrl();
      return ResponseEntity.ok(ApiResponse.success(url, provider + " auth URL generated", requestId));
   }

   @Anonymous
   @GetMapping("/{provider}/callback")
   public void oauthCallback(
         @PathVariable String provider,
         @RequestParam String code,
         @RequestParam String state,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) throws java.io.IOException {

      try {
         OAuth2UserInfo userInfo = oAuth2ServiceFactory.getService(provider).exchangeCodeForUserInfo(code, state);
         String ip = getClientIp(httpRequest);
         String userAgent = httpRequest.getHeader("User-Agent");

         authService.loginWithOAuth2(provider, userInfo, ip, userAgent, httpResponse, false);

         // Redirect to frontend
         httpResponse.sendRedirect(appProperties.getOauth2().getGoogle().getFrontendCallbackUrl());
      } catch (Exception e) {
         log.error(provider + " login failed", e);
         // Redirect to frontend with error
         httpResponse
               .sendRedirect(appProperties.getOauth2().getGoogle().getFrontendCallbackUrl() + "?error=auth_failed");
      }
   }

   private String getClientIp(HttpServletRequest request) {
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
         return xForwardedFor.split(",")[0];
      }
      return request.getRemoteAddr();
   }

}
