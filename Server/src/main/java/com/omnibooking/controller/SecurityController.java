package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.auth.SecurityCheckResponse;
import com.omnibooking.dto.auth.SecurityOtpVerifyRequest;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.SecurityVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.omnibooking.exception.ErrorCode;

import java.util.concurrent.TimeUnit;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/auth/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityController {

   private final SecurityVerificationService securityVerificationService;
   private final StringRedisTemplate redisTemplate;

   @PostMapping("/otp/request")
   public ResponseEntity<ApiResponse<Void>> requestOtp(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      securityVerificationService.sendSecurityOTP(principal.getId(), principal.getEmail());

      return ResponseEntity.ok(ApiResponse.success(null, "Mã OTP đã được gửi tới email của bạn", requestId));
   }

   @PostMapping("/otp/verify")
   public ResponseEntity<ApiResponse<Void>> verifyOtp(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody SecurityOtpVerifyRequest request,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      boolean isVerified = securityVerificationService.verifySecurityOTP(principal.getId(), request.getOtp());

      if (isVerified) {
         return ResponseEntity.ok(ApiResponse.success(null,
               "Xác thực thành công. Bạn có 30 phút để thực hiện các thao tác bảo mật.", requestId));
      } else {
         return ResponseEntity.status(ErrorCode.INVALID_OTP.getStatus())
               .body(ApiResponse.error(ErrorCode.INVALID_OTP.getMessage(), ErrorCode.INVALID_OTP.getCode(), requestId));
      }
   }

   @GetMapping("/status")
   public ResponseEntity<ApiResponse<SecurityCheckResponse>> getSecurityStatus(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      boolean isTrusted = securityVerificationService.isSessionTrusted(principal.getId());

      long remainingTime = 0;
      if (isTrusted) {
         String trustedKey = "TRUSTED_SESSION:" + principal.getId();
         remainingTime = redisTemplate.getExpire(trustedKey, TimeUnit.SECONDS);
      }

      SecurityCheckResponse response = SecurityCheckResponse.builder()
            .isTrusted(isTrusted)
            .remainingTimeSeconds(remainingTime > 0 ? remainingTime : 0)
            .build();

      return ResponseEntity.ok(ApiResponse.success(response, "Lấy trạng thái bảo mật thành công", requestId));
   }
}
