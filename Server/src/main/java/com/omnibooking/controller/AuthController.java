package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.security.Anonymous;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.ForgotPasswordRequest;
import com.omnibooking.dto.RegistrationStatusResponse;
import com.omnibooking.dto.ResetPasswordRequest;
import com.omnibooking.dto.oauth.OAuth2UserInfo;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.auth.OAuth2ServiceFactory;
import com.omnibooking.services.auth.TurnstileService;
import com.omnibooking.services.user.RegistrationQueueService;
import com.omnibooking.services.communication.SseNotificationService;
import com.omnibooking.config.AppProperties;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.services.auth.SessionService;
import com.omnibooking.security.RedisSessionInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
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

import com.fasterxml.jackson.databind.ObjectMapper;
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

   private final ObjectMapper objectMapper;

   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   private final RegistrationQueueService registrationQueueService;

   private final SseNotificationService sseNotificationService;

   private final TurnstileService turnstileService;

   private final SessionService sessionService;

   @Anonymous
   @Idempotent
   @PostMapping("/register")
   public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request,
         HttpServletRequest httpRequest) {

      // Verify CAPTCHA
      String ip = getClientIp(httpRequest);
      turnstileService.verifyToken(request.getTurnstileToken(), ip);

      // Fast-fail: check email exists synchronously via Bloom Filter / DB fallback
      if (authService.checkEmail(request.getEmail())) {
         throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }

      String requestId = (String) httpRequest.getAttribute("requestId");
      request.setRequestId(requestId);

      MDC.put("requestId", requestId);
      try {
         logJson("registration_received", requestId, request.getEmail(), "Received registration request");
         // Push to Redis Queue for Batch Processing
         registrationQueueService.pushToQueue(request);
      } finally {
         MDC.remove("requestId");
      }

      return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(null, "Registration request received and is being processed", requestId));
   }

   @Anonymous
   @PostMapping("/finalize-registration")
   public ResponseEntity<ApiResponse<AuthResponse>> finalizeRegistration(
         @RequestBody Map<String, String> body,
         @CookieValue(name = CookieUtils.SESSION_ID, required = false) String oldSessionId,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String accessToken = body.get("accessToken");
      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.finalizeRegistration(accessToken, ip, userAgent, httpResponse,
            oldSessionId);

      return ResponseEntity.ok(ApiResponse.success(authResponse, "Session synchronized successfully", requestId));
   }

   @Anonymous
   @GetMapping(value = "/subscribe/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public SseEmitter subscribe(@PathVariable String requestId) {
      return sseNotificationService.subscribe(requestId);
   }

   @Anonymous
   @GetMapping("/registration-status/{requestId}")
   public ResponseEntity<ApiResponse<RegistrationStatusResponse>> getRegistrationStatus(
         @PathVariable String requestId,
         @RequestParam(required = false, defaultValue = "false") boolean timeout,
         HttpServletRequest httpRequest) {

      String reqId = (String) httpRequest.getAttribute("requestId");
      MDC.put("requestId", requestId);
      try {
         if (timeout) {
            meterRegistry.counter("registration_polling_timeout_total").increment();
            logJson("registration_polling_timeout", requestId, null, "Client reported polling timeout");
         }
         logJson("registration_status_checked", requestId, null, "Checking registration status");
         RegistrationStatusResponse statusResponse = authService.getRegistrationStatus(requestId);

         return ResponseEntity.ok(ApiResponse.success(statusResponse, "Registration status retrieved", reqId));
      } finally {
         MDC.remove("requestId");
      }
   }

   private void logJson(String event, String requestId, String email, String message) {
      try {
         Map<String, Object> logPayload = new HashMap<>();
         logPayload.put("requestId", requestId);
         logPayload.put("event", event);
         if (email != null) {
            logPayload.put("email", email);
         }
         logPayload.put("message", message);
         logPayload.put("timestamp", Instant.now().toString());
         log.info(objectMapper.writeValueAsString(logPayload));
      } catch (Exception e) {
         log.error("Failed to write JSON log", e);
      }
   }

   @Anonymous
   @PostMapping("/login")
   public ResponseEntity<ApiResponse<AuthResponse>> login(
         @Valid @RequestBody LoginRequest request,
         @CookieValue(name = CookieUtils.SESSION_ID, required = false) String oldSessionId,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.login(request, ip, userAgent, httpResponse, oldSessionId);

      return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful", requestId));
   }

   @Anonymous
   @PostMapping("/refresh")
   public ResponseEntity<ApiResponse<AuthResponse>> refresh(
         @CookieValue(name = CookieUtils.SESSION_ID, required = false) String sessionId,
         @CookieValue(name = CookieUtils.REFRESH_TOKEN, required = false) String refreshToken,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      if (sessionId == null || refreshToken == null) {
         log.warn("Missing session_id or refresh_token in cookies for refresh request");
         authService.clearAllCookies(httpResponse);
         throw new AppException(ErrorCode.INVALID_SESSION);
      }

      AuthResponse authResponse = authService.refresh(sessionId, refreshToken, ip, userAgent, httpResponse);

      return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully", requestId));
   }

   @Anonymous
   @PostMapping("/logout")
   public ResponseEntity<ApiResponse<Void>> logout(
         @CookieValue(name = CookieUtils.SESSION_ID, required = false) String sessionId,
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
   @GetMapping("/check-email")
   public ResponseEntity<ApiResponse<Boolean>> checkEmail(
         @RequestParam String email,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      boolean exists = authService.checkEmail(email);

      return ResponseEntity.ok(ApiResponse.success(exists, "Email check completed", requestId));
   }

   @Anonymous
   @PostMapping("/activate-guest")
   public ResponseEntity<ApiResponse<AuthResponse>> activateGuest(
         @RequestBody Map<String, String> body,
         @CookieValue(name = CookieUtils.SESSION_ID, required = false) String oldSessionId,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {
      String token = body.get("token");
      String password = body.get("password");
      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      if (token == null || password == null) {
         throw new AppException(ErrorCode.INVALID_TOKEN,
               "Token and password are required");
      }

      AuthResponse authResponse = authService.activateGuest(token, password, ip, userAgent, httpResponse, oldSessionId);

      return ResponseEntity
            .ok(ApiResponse.success(authResponse, "Account activated and logged in successfully", requestId));
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
         HttpServletResponse httpResponse) throws IOException {

      try {
         OAuth2UserInfo userInfo = oAuth2ServiceFactory.getService(provider).exchangeCodeForUserInfo(code, state);
         String ip = getClientIp(httpRequest);
         String userAgent = httpRequest.getHeader("User-Agent");
         String oldSessionId = CookieUtils.getCookieValue(httpRequest, CookieUtils.SESSION_ID);

         authService.loginWithOAuth2(provider, userInfo, ip, userAgent, httpResponse, false, oldSessionId);

         // Redirect to frontend
         httpResponse.sendRedirect(appProperties.getOauth2().getGoogle().getFrontendCallbackUrl());
      } catch (Exception e) {
         log.error(provider + " login failed", e);
         // Redirect to frontend with error
         httpResponse
               .sendRedirect(appProperties.getOauth2().getGoogle().getFrontendCallbackUrl() + "?error=auth_failed");
      }
   }

   @Anonymous
   @GetMapping("/csrf")
   public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(
         HttpServletRequest request,
         HttpServletResponse response) {

      String csrfCookie = CookieUtils.getCookieValue(request, CookieUtils.CSRF_TOKEN);
      if (csrfCookie == null || csrfCookie.isBlank()) {
         String sessionId = CookieUtils.getCookieValue(request, CookieUtils.SESSION_ID);
         String csrfNonce = null;
         if (sessionId != null && !sessionId.isBlank()) {
            try {
               RedisSessionInfo sessionInfo = sessionService.getSession(UUID.fromString(sessionId));
               if (sessionInfo != null) {
                  csrfNonce = sessionInfo.getCsrfNonce();
               }
            } catch (Exception e) {
               log.error("Failed to load session for CSRF token endpoint", e);
            }
         }
         String secret = appProperties.getSecurity().getCsrfSecret();
         if (secret == null || secret.isBlank()) {
            secret = appProperties.getSecurity().getJwtSecret();
         }
         csrfCookie = CookieUtils.calculateCsrfToken(sessionId, csrfNonce, secret);
         CookieUtils.addCookie(response, CookieUtils.CSRF_TOKEN, csrfCookie, 86400,
               appProperties.getSecurity().isCookieSecure());
      }
      String requestId = (String) request.getAttribute("requestId");

      return ResponseEntity
            .ok(ApiResponse.success(Map.of("csrfToken", csrfCookie), "CSRF token retrieved successfully", requestId));
   }

   private String getClientIp(HttpServletRequest request) {
      return request.getRemoteAddr();
   }

}
