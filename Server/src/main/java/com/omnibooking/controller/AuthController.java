package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.LoginRequest;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

   private final AuthService authService;

   @PostMapping("/register")
   public ResponseEntity<ApiResponse<AuthResponse>> register(
         @Valid @RequestBody RegisterRequest request,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      AuthResponse response = authService.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, requestId));
   }

   @PostMapping("/login")
   public ResponseEntity<ApiResponse<AuthResponse>> login(
         @Valid @RequestBody LoginRequest request,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String ip = getClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      String requestId = (String) httpRequest.getAttribute("requestId");

      AuthResponse authResponse = authService.login(request, ip, userAgent, httpResponse);
      return ResponseEntity.ok(ApiResponse.success(authResponse, requestId));
   }

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
      return ResponseEntity.ok(ApiResponse.success(authResponse, requestId));
   }

   @PostMapping("/logout")
   public ResponseEntity<ApiResponse<Void>> logout(
         @CookieValue(name = "session_id") String sessionId,
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest,
         HttpServletResponse httpResponse) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      authService.logout(UUID.fromString(sessionId), principal.getId(), httpResponse);

      return ResponseEntity.ok(ApiResponse.success(null, requestId));
   }

   private String getClientIp(HttpServletRequest request) {
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
         return xForwardedFor.split(",")[0];
      }
      return request.getRemoteAddr();
   }
}
