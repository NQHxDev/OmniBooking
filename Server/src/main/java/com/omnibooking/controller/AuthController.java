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
import com.omnibooking.services.AuthService;
import com.omnibooking.services.OAuth2ServiceFactory;
import com.omnibooking.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

   private final AuthService authService;

   private final OAuth2ServiceFactory oAuth2ServiceFactory;

   private final AppProperties appProperties;

   @Anonymous
   @PostMapping("/register")
   public ResponseEntity<ApiResponse<AuthResponse>> register(
         @Valid @RequestBody RegisterRequest request,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse response = authService.register(request, ip, userAgent, httpResponse);
      return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "User registered successfully", requestId));
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
         @CookieValue(name = "session_id") String sessionId,
         @CookieValue(name = "refresh_token") String refreshToken,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

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
   @org.springframework.web.bind.annotation.GetMapping("/verify")
   public ResponseEntity<ApiResponse<Void>> verify(
         @org.springframework.web.bind.annotation.RequestParam String token,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.verifyEmail(token);
      return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully", requestId));
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
   @org.springframework.web.bind.annotation.GetMapping("/{provider}/url")
   public ResponseEntity<ApiResponse<String>> getOAuthUrl(
         @org.springframework.web.bind.annotation.PathVariable String provider,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      String url = oAuth2ServiceFactory.getService(provider).generateAuthUrl();
      return ResponseEntity.ok(ApiResponse.success(url, provider + " auth URL generated", requestId));
   }

   @Anonymous
   @org.springframework.web.bind.annotation.GetMapping("/{provider}/callback")
   public void oauthCallback(
         @org.springframework.web.bind.annotation.PathVariable String provider,
         @org.springframework.web.bind.annotation.RequestParam String code,
         @org.springframework.web.bind.annotation.RequestParam String state,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) throws java.io.IOException {

      try {
         OAuth2UserInfo userInfo = oAuth2ServiceFactory.getService(provider).exchangeCodeForUserInfo(code, state);
         String ip = getClientIp(httpRequest);
         String userAgent = httpRequest.getHeader("User-Agent");

         authService.loginWithOAuth2(provider, userInfo, ip, userAgent, httpResponse);

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
