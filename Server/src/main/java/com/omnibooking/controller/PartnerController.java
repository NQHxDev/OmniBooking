package com.omnibooking.controller;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.omnibooking.services.auth.AuthService;
import com.omnibooking.services.partner.PartnerService;
import com.omnibooking.dto.PartnerStatsResponse;
import com.omnibooking.dto.PartnerBookingResponse;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.util.OtpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import java.util.Objects;
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

   private final OutboxService outboxService;

   private final PartnerService partnerService;

   @GetMapping("/stats")
   public ResponseEntity<ApiResponse<PartnerStatsResponse>> getStats(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest request) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      UUID userId = Objects.requireNonNull(principal.getId(), "User ID cannot be null");
      PartnerStatsResponse stats = partnerService.getPartnerStats(userId);

      return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê đối tác thành công", requestId));
   }

   @Transactional
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
            .map(profile -> profile.getDisplayName())
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
      String otpCode = OtpUtils.generateAlphanumericOtp();

      // Store in Redis (valid for 10 minutes)
      String redisKey = "otp:partner:" + userId;

      // Use requireNonNull to satisfy strict null safety checks
      Objects.requireNonNull(redisKey, "Redis key cannot be null");
      Objects.requireNonNull(otpCode, "OTP code cannot be null");

      redisTemplate.opsForValue().set(redisKey, otpCode, 10, TimeUnit.MINUTES);

      // Set cooldown lock (30 seconds)
      redisTemplate.opsForValue().set(lockKey, "locked", 30, TimeUnit.SECONDS);

      // Record in Outbox instead of sending directly
      com.omnibooking.dto.event.EmailEvent emailEvent = mailService.buildPartnerOtpEmailEvent(email, fullName, otpCode);
      outboxService.saveEvent(
            userId,
            "PARTNER",
            EventConstants.PARTNER_OTP_SEND,
            emailEvent);

      log.info("Partner OTP recorded in outbox for email: {} (RequestId: {})", email, requestId);

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
         @CookieValue(name = "session_id", required = false) String sessionId,
         HttpServletRequest request,
         HttpServletResponse response) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      String ip = request.getRemoteAddr();
      String userAgent = request.getHeader("User-Agent");

      // Inherit rememberMe from current session if possible
      boolean rememberMe = false;
      if (sessionId != null) {
         try {
            com.omnibooking.security.RedisSessionInfo sessionInfo = authService.getSessionInfo(sessionId);
            if (sessionInfo != null) {
               rememberMe = sessionInfo.isRememberMe();
            }
         } catch (Exception e) {
            log.warn("Failed to fetch session info for rememberMe inheritance: {}", e.getMessage());
         }
      }

      AuthResponse authResponse = authService.upgradeToPartner(principal.getId(), ip, userAgent, response, rememberMe);

      log.info("User {} upgraded to partner (RequestId: {})", principal.getId(), requestId);

      return ResponseEntity
            .ok(ApiResponse.success(authResponse, "Chúc mừng! Bạn đã trở thành đối tác của OmniBooking", requestId));
   }

   @GetMapping("/bookings")
   public ResponseEntity<ApiResponse<List<PartnerBookingResponse>>> getBookings(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest request) {

      String requestIdAttr = (String) request.getAttribute("requestId");
      String requestId = requestIdAttr != null ? requestIdAttr : "N/A";

      UUID userId = Objects.requireNonNull(principal.getId(), "User ID cannot be null");
      List<PartnerBookingResponse> bookings = partnerService.getPartnerBookings(userId);

      return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách đặt phòng đối tác thành công", requestId));
   }

}
