package com.omnibooking.services.payment.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.MomoConfig;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Booking;
import com.omnibooking.model.Transaction;
import com.omnibooking.model.PaymentEvent;
import com.omnibooking.model.enums.TransactionStatus;
import com.omnibooking.model.enums.PaymentGatewayStatus;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.payment.PaymentEventRepository;
import com.omnibooking.services.booking.BookingService;
import com.omnibooking.services.core.CurrencyService;
import com.omnibooking.services.payment.MomoPaymentService;
import com.omnibooking.services.payment.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentServiceImpl implements MomoPaymentService {

   private final MomoConfig momoConfig;

   private final BookingRepository bookingRepository;

   private final BookingService bookingService;

   private final CurrencyService currencyService;

   private final RestTemplate restTemplate;

   private final TransactionRepository transactionRepository;

   private final PaymentStateMachine paymentStateMachine;

   private final PaymentEventRepository paymentEventRepository;

   @Override
   public String getProviderName() {
      return "MOMO";
   }

   @Override
   public String createPaymentLink(UUID bookingId) {
      log.info("Generating MoMo payment UAT link for booking: {}", bookingId);
      Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      if (!booking.getRequiresDeposit()) {
         throw new AppException("PAYMENT_001", "Deposit is not required for this booking",
               HttpStatus.BAD_REQUEST);
      }

      // Convert USD amount to VND
      BigDecimal amountVnd = currencyService.convertFromBase(booking.getDepositAmount(), "VND");
      long amount = amountVnd.longValue();

      String orderId = bookingId.toString() + "_" + System.currentTimeMillis();
      String requestId = UUID.randomUUID().toString();
      String orderInfo = "Thanh toan dat coc OmniBooking cho booking "
            + booking.getId().toString().substring(0, 8).toUpperCase();
      String requestType = "VISA".equalsIgnoreCase(booking.getPaymentMethod()) ? "payWithCC" : "captureWallet";
      String extraData = "";

      // Signature Raw String:
      // accessKey=$accessKey&amount=$amount&extraData=$extraData&ipnUrl=$ipnUrl&orderId=$orderId&orderInfo=$orderInfo&partnerCode=$partnerCode&redirectUrl=$redirectUrl&requestId=$requestId&requestType=$requestType
      String rawSignature = String.format(
            "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
            momoConfig.getAccessKey(),
            amount,
            extraData,
            momoConfig.getNotifyUrl(),
            orderId,
            orderInfo,
            momoConfig.getPartnerCode(),
            momoConfig.getReturnUrl(),
            requestId,
            requestType);

      String signature = hmacSha256(rawSignature, momoConfig.getSecretKey());

      // Save PENDING transaction to DB
      Transaction pendingTx = Transaction.builder()
            .booking(booking)
            .amount(booking.getDepositAmount()) // USD amount
            .localAmount(BigDecimal.valueOf(amount)) // exact VND amount sent to gateway
            .localCurrency("VND")
            .paymentMethod(getProviderName())
            .status(TransactionStatus.PENDING)
            .providerOrderId(orderId)
            .build();
      transactionRepository.save(pendingTx);

      // Record audit event
      PaymentEvent event = PaymentEvent.builder()
            .transactionId(pendingTx.getId())
            .bookingId(bookingId)
            .eventType("PAYMENT_CREATED")
            .metadata(
                  String.format("{\"orderId\":\"%s\",\"requestId\":\"%s\",\"amount\":%d}", orderId, requestId, amount))
            .build();
      paymentEventRepository.save(event);

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("partnerCode", momoConfig.getPartnerCode());
      requestBody.put("partnerName", "OmniBooking");
      requestBody.put("storeId", "OmniBooking");
      requestBody.put("requestId", requestId);
      requestBody.put("amount", amount);
      requestBody.put("orderId", orderId);
      requestBody.put("orderInfo", orderInfo);
      requestBody.put("redirectUrl", momoConfig.getReturnUrl());
      requestBody.put("ipnUrl", momoConfig.getNotifyUrl());
      requestBody.put("lang", "vi");
      requestBody.put("extraData", extraData);
      requestBody.put("requestType", requestType);
      requestBody.put("signature", signature);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

      try {
         log.info("Sending create UAT payment request to MoMo UAT endpoint: {}", momoConfig.getApiUrl());
         ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
               momoConfig.getApiUrl(),
               HttpMethod.POST,
               entity,
               new ParameterizedTypeReference<Map<String, Object>>() {
               });
         Map<String, Object> response = responseEntity.getBody();

         if (response != null && response.containsKey("resultCode")) {
            int resultCode = ((Number) response.get("resultCode")).intValue();
            if (resultCode == 0) {
               String payUrl = (String) response.get("payUrl");
               log.info("MoMo payment UAT link generated successfully: {}", payUrl);
               return payUrl;
            } else {
               String message = (String) response.get("message");
               log.error("MoMo payment UAT link generation failed. Code: {}, Message: {}", resultCode, message);
               throw new AppException("PAYMENT_002", "MoMo API error: " + message,
                     org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            }
         }
         throw new AppException("PAYMENT_003", "Empty response from MoMo API",
               org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (AppException e) {
         throw e;
      } catch (Exception e) {
         log.error("Failed to contact MoMo UAT payment gateway API: {}", e.getMessage(), e);
         throw new AppException("PAYMENT_004", "Failed to connect to MoMo UAT payment gateway: " + e.getMessage(),
               org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
      }
   }

   @Override
   public void processPaymentCallback(Map<String, String> params) {
      log.info("Processing MoMo UAT callback: {}", params);

      String partnerCode = params.get("partnerCode");
      String orderId = params.get("orderId");
      String requestId = params.get("requestId");
      String amountStr = params.get("amount");
      String orderInfo = params.get("orderInfo");
      String orderType = params.get("orderType");
      String transId = params.get("transId");
      String resultCodeStr = params.get("resultCode");
      String message = params.get("message");
      String payType = params.get("payType");
      String responseTime = params.get("responseTime");
      String extraData = params.get("extraData");
      String receivedSignature = params.get("signature");

      String extraDataVal = extraData != null ? extraData : "";
      String orderTypeVal = orderType != null ? orderType : "";
      String payTypeVal = payType != null ? payType : "";
      String messageVal = message != null ? message : "";

      // 1. Resolve bookingId and transactionId
      Optional<Transaction> pendingTxOpt = transactionRepository.findByProviderOrderId(orderId);
      UUID bookingId = null;
      UUID transactionId = null;
      if (pendingTxOpt.isPresent()) {
         bookingId = pendingTxOpt.get().getBooking().getId();
         transactionId = pendingTxOpt.get().getId();
      } else {
         String actualBookingIdStr = orderId;
         if (orderId != null && orderId.contains("_")) {
            actualBookingIdStr = orderId.split("_")[0];
         }
         try {
            bookingId = UUID.fromString(actualBookingIdStr);
         } catch (Exception e) {
            log.warn("Failed to parse booking ID from order ID: {}", orderId);
         }
      }

      // Record CALLBACK_RECEIVED event
      PaymentEvent callbackEvent = PaymentEvent.builder()
            .transactionId(transactionId)
            .bookingId(bookingId)
            .eventType("CALLBACK_RECEIVED")
            .metadata(toJson(params))
            .build();
      paymentEventRepository.save(callbackEvent);

      // Check fields and calculate signature
      // Format:
      // accessKey=$accessKey&amount=$amount&extraData=$extraData&message=$message&orderId=$orderId&orderInfo=$orderInfo&orderType=$orderType&partnerCode=$partnerCode&payType=$payType&requestId=$requestId&responseTime=$responseTime&resultCode=$resultCode&transId=$transId
      String rawSignature = String.format(
            "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
            momoConfig.getAccessKey(),
            amountStr,
            extraDataVal,
            messageVal,
            orderId,
            orderInfo,
            orderTypeVal,
            partnerCode,
            payTypeVal,
            requestId,
            responseTime,
            resultCodeStr,
            transId);

      String calculatedSignature = hmacSha256(rawSignature, momoConfig.getSecretKey());

      boolean isLocalhost = momoConfig.getNotifyUrl() != null && momoConfig.getNotifyUrl().contains("localhost");
      boolean isMock = isLocalhost && "mock_signature".equalsIgnoreCase(receivedSignature);

      if (!isMock && !calculatedSignature.equalsIgnoreCase(receivedSignature)) {
         log.error("MoMo callback signature validation failed! Expected: {}, Calculated: {}", receivedSignature,
               calculatedSignature);

         PaymentEvent sigFailEvent = PaymentEvent.builder()
               .transactionId(transactionId)
               .bookingId(bookingId)
               .eventType("PAYMENT_FAILED")
               .metadata(String.format(
                     "{\"reason\":\"Signature verification failed\",\"received\":\"%s\",\"calculated\":\"%s\"}",
                     receivedSignature, calculatedSignature))
               .build();
         paymentEventRepository.save(sigFailEvent);

         throw new AppException("PAYMENT_005", "Signature verification failed",
               org.springframework.http.HttpStatus.BAD_REQUEST);
      }

      log.info("MoMo UAT callback signature verified successfully.");

      PaymentEvent sigEvent = PaymentEvent.builder()
            .transactionId(transactionId)
            .bookingId(bookingId)
            .eventType("SIGNATURE_VERIFIED")
            .metadata(String.format("{\"orderId\":\"%s\"}", orderId))
            .build();
      paymentEventRepository.save(sigEvent);

      // 2. Scenario A & C Check (Replay/Collision checks)
      if (transId != null) {
         Optional<Transaction> existingTxOpt = transactionRepository.findByPaymentMethodAndProviderTransactionId("MOMO",
               transId);
         if (existingTxOpt.isPresent()) {
            Transaction existingTx = existingTxOpt.get();
            if (existingTx.getBooking().getId().equals(bookingId)) {
               // Scenario A: Same booking, same transId -> Duplicate callback, ignore (no-op)
               log.info("Duplicate callback detected for booking {} and transId {}. Ignoring.", bookingId, transId);
               return;
            } else {
               // Scenario C: Replay attack/collision -> transId belongs to different booking!
               log.error(
                     "[SECURITY_ALERT] Replay attack or transaction ID reuse detected! transId {} belongs to booking {} but callback claims booking {}",
                     transId, existingTx.getBooking().getId(), bookingId);

               PaymentEvent securityAlert = PaymentEvent.builder()
                     .transactionId(existingTx.getId())
                     .bookingId(bookingId)
                     .eventType("SECURITY_ALERT")
                     .metadata(String.format("{\"receivedTransId\":\"%s\",\"existingBookingId\":\"%s\"}", transId,
                           existingTx.getBooking().getId()))
                     .build();
               paymentEventRepository.save(securityAlert);

               throw new AppException("PAYMENT_007", "Transaction ID already associated with another booking",
                     HttpStatus.CONFLICT);
            }
         }
      }

      // 3. Scenario B Check (Overpayment check)
      if (pendingTxOpt.isPresent()) {
         Transaction pendingTx = pendingTxOpt.get();
         if (pendingTx.getStatus() == TransactionStatus.SUCCESS) {
            // Scenario B: Booking already paid with another transId (Overpayment)
            log.error("Overpayment detected! orderId {} was already paid. transId received: {}.", orderId, transId);

            PaymentEvent duplicatePaymentEvent = PaymentEvent.builder()
                  .transactionId(pendingTx.getId())
                  .bookingId(bookingId)
                  .eventType("DUPLICATE_PAYMENT_DETECTED")
                  .metadata(String.format("{\"newTransId\":\"%s\",\"existingTransId\":\"%s\"}", transId,
                        pendingTx.getProviderTransactionId()))
                  .build();
            paymentEventRepository.save(duplicatePaymentEvent);

            return; // Ignore, return success to gateway (no-op)
         }
      }

      // 4. Amount Verification
      if (pendingTxOpt.isPresent()) {
         Transaction pendingTx = pendingTxOpt.get();
         BigDecimal callbackAmount = new BigDecimal(amountStr);
         if (callbackAmount.compareTo(pendingTx.getLocalAmount()) != 0) {
            log.error("Payment amount mismatch! Expected: {}, Received: {}, BookingId: {}",
                  pendingTx.getLocalAmount(), callbackAmount, bookingId);

            PaymentEvent amountMismatchEvent = PaymentEvent.builder()
                  .transactionId(pendingTx.getId())
                  .bookingId(bookingId)
                  .eventType("PAYMENT_FAILED")
                  .metadata(
                        String.format("{\"expectedAmount\":%s,\"receivedAmount\":%s,\"reason\":\"Amount mismatch\"}",
                              pendingTx.getLocalAmount(), callbackAmount))
                  .build();
            paymentEventRepository.save(amountMismatchEvent);

            throw new AppException("PAYMENT_006", "Payment amount mismatch", HttpStatus.BAD_REQUEST);
         }
      }

      int resultCode = Integer.parseInt(resultCodeStr);

      if (resultCode == 0) {
         log.info("MoMo payment was successful for booking: {}", bookingId);

         PaymentEvent amtVerifiedEvent = PaymentEvent.builder()
               .transactionId(transactionId)
               .bookingId(bookingId)
               .eventType("AMOUNT_VERIFIED")
               .metadata(String.format("{\"amount\":%s}", amountStr))
               .build();
         paymentEventRepository.save(amtVerifiedEvent);

         String metadata = String.format(
               "{\"orderId\":\"%s\",\"partnerCode\":\"%s\",\"requestId\":\"%s\",\"transId\":\"%s\",\"payType\":\"%s\",\"responseTime\":\"%s\"}",
               orderId, partnerCode, requestId, transId, payTypeVal, responseTime);
         bookingService.confirmBooking(bookingId, "MOMO", transId, metadata);
      } else {
         log.warn("MoMo payment UAT callback failed with resultCode: {} for booking: {}", resultCode, bookingId);
         if (pendingTxOpt.isPresent()) {
            Transaction pendingTx = pendingTxOpt.get();
            paymentStateMachine.transition(pendingTx, TransactionStatus.FAILED);
            transactionRepository.save(pendingTx);
         }

         PaymentEvent failEvent = PaymentEvent.builder()
               .transactionId(transactionId)
               .bookingId(bookingId)
               .eventType("PAYMENT_FAILED")
               .metadata(String.format("{\"resultCode\":%d,\"message\":\"%s\"}", resultCode, messageVal))
               .build();
         paymentEventRepository.save(failEvent);
      }
   }

   @Override
   @CircuitBreaker(name = "momoGateway", fallbackMethod = "queryPaymentStatusFallback")
   public PaymentGatewayStatus queryPaymentStatus(Transaction transaction) {
      log.info("Querying MoMo transaction status for Transaction: {}", transaction.getId());

      String orderId = transaction.getProviderOrderId();
      String requestId = UUID.randomUUID().toString();

      String rawSignature = String.format(
            "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
            momoConfig.getAccessKey(),
            orderId,
            momoConfig.getPartnerCode(),
            requestId);

      String signature = hmacSha256(rawSignature, momoConfig.getSecretKey());

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("partnerCode", momoConfig.getPartnerCode());
      requestBody.put("requestId", requestId);
      requestBody.put("orderId", orderId);
      requestBody.put("signature", signature);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

      String queryUrl = momoConfig.getApiUrl().replace("/create", "/query");

      try {
         ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
               queryUrl,
               HttpMethod.POST,
               entity,
               new ParameterizedTypeReference<Map<String, Object>>() {
               });
         Map<String, Object> response = responseEntity.getBody();

         if (response != null && response.containsKey("resultCode")) {
            int resultCode = ((Number) response.get("resultCode")).intValue();
            log.info("MoMo query response for orderId {}: resultCode={}", orderId, resultCode);

            if (resultCode == 0) {
               Object transIdObj = response.get("transId");
               String transId = transIdObj != null ? String.valueOf(transIdObj) : orderId;

               log.info("[RECONCILIATION] Payment query succeeded. Auto-confirming booking {} with transId {}",
                     transaction.getBooking().getId(), transId);

               bookingService.confirmBooking(transaction.getBooking().getId(), "MOMO", transId, toJson(response));
               return PaymentGatewayStatus.SUCCESS;
            } else if (resultCode == 9000 || resultCode == 1000 || resultCode == 8000) {
               return PaymentGatewayStatus.PENDING;
            } else {
               return PaymentGatewayStatus.FAILED;
            }
         }
         return PaymentGatewayStatus.PENDING;
      } catch (Exception e) {
         log.error("Failed to query MoMo gateway: {}", e.getMessage(), e);
         throw e;
      }
   }

   public PaymentGatewayStatus queryPaymentStatusFallback(Transaction transaction, Throwable t) {
      log.error(
            "[GATEWAY_ALERT] Gateway MoMo is unavailable. Circuit breaker fallback active. Transaction: {}, Error: {}",
            transaction.getId(), t.getMessage());

      try {
         PaymentEvent event = PaymentEvent.builder()
               .transactionId(transaction.getId())
               .bookingId(transaction.getBooking().getId())
               .eventType("GATEWAY_UNAVAILABLE")
               .metadata(String.format("{\"error\":\"%s\",\"providerOrderId\":\"%s\"}",
                     t.getMessage().replaceAll("\"", "\\\""), transaction.getProviderOrderId()))
               .build();
         paymentEventRepository.save(event);
      } catch (Exception e) {
         log.error("Failed to save GATEWAY_UNAVAILABLE payment event", e);
      }

      return PaymentGatewayStatus.PENDING;
   }

   private String hmacSha256(String data, String key) {
      try {
         SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
         Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(secretKeySpec);
         byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

         StringBuilder hexString = new StringBuilder();
         for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
               hexString.append('0');
            }
            hexString.append(hex);
         }
         return hexString.toString();
      } catch (Exception e) {
         log.error("Failed to calculate HMAC-SHA256 signature", e);
         throw new RuntimeException("Failed to calculate HMAC-SHA256 signature", e);
      }
   }

   private String toJson(Object obj) {
      try {
         return new ObjectMapper().writeValueAsString(obj);
      } catch (Exception e) {
         return "{}";
      }
   }

}
