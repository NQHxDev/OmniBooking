package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.security.Anonymous;
import com.omnibooking.services.payment.PaymentProvider;
import com.omnibooking.services.payment.PaymentProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

   private final PaymentProviderFactory paymentProviderFactory;

   @Anonymous
   @PostMapping("/{provider}/create")
   public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
         @PathVariable String provider,
         @RequestBody Map<String, String> request,
         jakarta.servlet.http.HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      String bookingIdStr = request.get("bookingId");
      if (bookingIdStr == null || bookingIdStr.trim().isEmpty()) {
         return ResponseEntity.badRequest()
               .body(ApiResponse.error("Booking ID is required", "BOOKING_ID_REQUIRED", requestId));
      }

      try {
         UUID bookingId = UUID.fromString(bookingIdStr);
         PaymentProvider paymentProvider = paymentProviderFactory.getProvider(provider);
         String payUrl = paymentProvider.createPaymentLink(bookingId);
         Map<String, String> data = new HashMap<>();
         data.put("payUrl", payUrl);
         return ResponseEntity.ok(
               ApiResponse.success(data, provider.toUpperCase() + " payment link generated successfully", requestId));
      } catch (IllegalArgumentException e) {
         return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "INVALID_INPUT", requestId));
      }
   }

   @Anonymous
   @PostMapping("/{provider}/callback")
   public ResponseEntity<Void> paymentCallback(
         @PathVariable String provider,
         @RequestParam Map<String, String> allRequestParams,
         @RequestBody(required = false) Map<String, Object> requestBody) {
      log.info("Received {} payment IPN callback. Params: {}, Body: {}", provider, allRequestParams, requestBody);

      Map<String, String> params = new HashMap<>(allRequestParams);
      if (requestBody != null) {
         for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
            if (entry.getValue() != null) {
               params.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
         }
      }

      PaymentProvider paymentProvider = paymentProviderFactory.getProvider(provider);
      paymentProvider.processPaymentCallback(params);

      return ResponseEntity.noContent().build();
   }

}
