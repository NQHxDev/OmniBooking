package com.omnibooking.controller;

import com.omnibooking.annotation.Idempotent;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.security.Anonymous;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class MockEndpointsController {

   @Anonymous
   @Idempotent(expiration = 48, timeUnit = TimeUnit.HOURS)
   @PostMapping("/payments")
   public ResponseEntity<ApiResponse<Map<String, Object>>> mockCreatePayment(
         @RequestBody Map<String, Object> request,
         HttpServletRequest httpRequest) {
      if (request.containsKey("delayMs")) {
         try {
            Thread.sleep(((Number) request.get("delayMs")).longValue());
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
      String requestId = (String) httpRequest.getAttribute("requestId");
      Map<String, Object> response = new HashMap<>();
      response.put("paymentId", "pay-123456");
      response.put("amount", request.get("amount"));
      response.put("status", "INITIALIZED");

      return ResponseEntity.ok(ApiResponse.success(response, "Payment initialized successfully", requestId));
   }

   @Anonymous
   @Idempotent(expiration = 7, timeUnit = TimeUnit.DAYS)
   @PostMapping("/refunds")
   public ResponseEntity<ApiResponse<Map<String, Object>>> mockCreateRefund(
         @RequestBody Map<String, Object> request,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      Map<String, Object> response = new HashMap<>();
      response.put("refundId", "ref-789012");
      response.put("amount", request.get("amount"));
      response.put("status", "SUCCESS");

      return ResponseEntity.ok(ApiResponse.success(response, "Refund request created successfully", requestId));
   }

   @Anonymous
   @Idempotent(expiration = 24, timeUnit = TimeUnit.HOURS)
   @PostMapping("/coupon/redeem")
   public ResponseEntity<ApiResponse<Map<String, Object>>> mockRedeemCoupon(
         @RequestBody Map<String, Object> request,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      Map<String, Object> response = new HashMap<>();
      response.put("couponId", "coupon-abcdef");
      response.put("code", request.get("code"));
      response.put("discountAmount", 15.0);
      response.put("status", "REDEEMED");

      return ResponseEntity.ok(ApiResponse.success(response, "Coupon redeemed successfully", requestId));
   }

}
