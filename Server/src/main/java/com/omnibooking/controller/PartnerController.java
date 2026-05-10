package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.MailService;
import com.omnibooking.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.omnibooking.services.AuthService;
import com.omnibooking.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/partner")
@RequiredArgsConstructor
@Slf4j
public class PartnerController {

   private final MailService mailService;
   private final StringRedisTemplate redisTemplate;
   private final UserProfileRepository userProfileRepository;
   private final AuthService authService;

   @PostMapping("/send-otp")
   public ResponseEntity<ApiResponse<Void>> sendOtp(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest request) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      String email = principal.getEmail();
      UUID userId = Objects.requireNonNull(principal.getId(), "User ID cannot be null");

      // Fetch full name from profile
      String fullName = userProfileRepository.findById(userId)
            .map(profile -> profile.getFirstName() + " " + profile.getLastName())
            .orElse(principal.getUsername());

      // Cooldown check (prevent sending multiple emails within 30 seconds)
      String lockKey = "otp:lock:" + userId;
      Boolean isLocked = redisTemplate.hasKey(lockKey);
      if (Boolean.TRUE.equals(isLocked)) {
         log.warn("Partner OTP request ignored due to cooldown for user: {}", userId);
         return ResponseEntity
               .ok(ApiResponse.success(null, "Mã xác thực đã được gửi, vui lòng kiểm tra email", requestId));
      }

      // Generate Alphanumeric OTP (A0A000 pattern)
      String otpCode = generateAlphanumericOtp();

      // Store in Redis (valid for 10 minutes)
      String redisKey = "otp:partner:" + userId;

      // Use requireNonNull to satisfy strict null safety checks
      Objects.requireNonNull(redisKey, "Redis key cannot be null");
      Objects.requireNonNull(otpCode, "OTP code cannot be null");

      redisTemplate.opsForValue().set(redisKey, otpCode, 10, TimeUnit.MINUTES);

      // Set cooldown lock (30 seconds)
      redisTemplate.opsForValue().set(lockKey, "locked", 30, TimeUnit.SECONDS);

      // Send email via Kafka
      mailService.sendPartnerOtpEmail(email, fullName, otpCode);

      log.info("Partner OTP sent to email: {} (RequestId: {})", email, requestId);

      return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực đã được gửi đến email của bạn", requestId));
   }

   @PostMapping("/verify-otp")
   public ResponseEntity<ApiResponse<Void>> verifyOtp(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestParam String code,
         HttpServletRequest request) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      UUID userId = Objects.requireNonNull(principal.getId(), "User ID cannot be null");
      String redisKey = "otp:partner:" + userId;
      String storedCode = redisTemplate.opsForValue().get(redisKey);

      if (storedCode == null || !storedCode.equals(code)) {
         return ResponseEntity.badRequest()
               .body(ApiResponse.error("Mã xác thực không chính xác hoặc đã hết hạn", "INVALID_OTP", null, requestId));
      }

      // Remove OTP after successful verification
      redisTemplate.delete(redisKey);

      log.info("Partner OTP verified for user: {} (RequestId: {})", userId, requestId);

      return ResponseEntity.ok(ApiResponse.success(null, "Xác thực thành công", requestId));
   }

   @PostMapping("/complete")
   public ResponseEntity<ApiResponse<AuthResponse>> completeRegistration(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest request,
         HttpServletResponse response) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      String ip = request.getRemoteAddr();
      String userAgent = request.getHeader("User-Agent");

      AuthResponse authResponse = authService.upgradeToPartner(principal.getId(), ip, userAgent, response);

      log.info("User {} upgraded to partner (RequestId: {})", principal.getId(), requestId);

      return ResponseEntity
            .ok(ApiResponse.success(authResponse, "Chúc mừng! Bạn đã trở thành đối tác của OmniBooking", requestId));
   }

   private String generateAlphanumericOtp() {
      Random random = new Random();
      String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      String digits = "0123456789";

      return String.valueOf(chars.charAt(random.nextInt(chars.length()))) +
            digits.charAt(random.nextInt(digits.length())) +
            chars.charAt(random.nextInt(chars.length())) +
            digits.charAt(random.nextInt(digits.length())) +
            digits.charAt(random.nextInt(digits.length())) +
            digits.charAt(random.nextInt(digits.length()));
   }
}
