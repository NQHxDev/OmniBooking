package com.omnibooking.services.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.user.RegistrationQueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationQueueServiceImpl implements RegistrationQueueService {

   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final RegistrationInboxRepository registrationInboxRepository;
   private final EncryptionService encryptionService;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   @Value("${omnibooking.kafka.registration.topic-name:registration-request-topic}")
   private String topicName;

   @Transactional
   @Override
   public void pushToQueue(RegisterRequest request) {
      UUID reqId = UUID.fromString(request.getRequestId());
      MDC.put("requestId", request.getRequestId());
      try {
         // Check if already exists in inbox to prevent duplicate ingestion
         if (registrationInboxRepository.existsById(reqId)) {
            log.warn("Registration request with ID {} already exists in inbox, skipping push", reqId);
            return;
         }

         // Initial Redis Registration Result to PENDING (10 minutes)
         String resultKey = "registration_result:" + request.getRequestId();
         redisTemplate.opsForValue().set(resultKey, "PENDING", 10, TimeUnit.MINUTES);

         // Save raw request in PostgreSQL inbox (durability check)
         String payload = objectMapper.writeValueAsString(request);
         RegistrationInbox inbox = RegistrationInbox.builder()
               .requestId(reqId)
               .payload(payload)
               .status(RegistrationInboxStatus.PENDING)
               .build();
         registrationInboxRepository.save(inbox);

         logJson("registration_queued_inbox", request.getRequestId(), request.getEmail(),
               "Saved pending registration request to inbox");

         // Register after-commit hook to publish to Kafka
         TransactionSynchronizationManager.registerSynchronization(
               new TransactionSynchronization() {
                  @Override
                  public void afterCommit() {
                     publishToKafkaAsync(request, reqId);
                  }
               });

      } catch (Exception e) {
         log.error("Failed to process registration request", e);
         throw new RuntimeException("System is busy, please try again later", e);
      } finally {
         MDC.remove("requestId");
      }
   }

   public void publishToKafkaAsync(RegisterRequest request, UUID reqId) {
      MDC.put("requestId", request.getRequestId());
      try {
         // Encrypt password using AES-256-GCM with active key
         String activeKeyId = encryptionService.getActiveKeyId();
         String encryptedPassword = encryptionService.encrypt(request.getPassword(), activeKeyId);

         // Build Kafka message
         RegistrationMessage message = RegistrationMessage.builder()
               .requestId(request.getRequestId())
               .email(request.getEmail())
               .fullName(request.getFullName())
               .keyId(activeKeyId)
               .encryptedPassword(encryptedPassword)
               .build();

         // Send to Kafka (key = email to guarantee partition ordering)
         kafkaTemplate.send(topicName, request.getEmail(), message)
               .whenComplete((result, ex) -> {
                  MDC.put("requestId", request.getRequestId());
                  try {
                     if (ex != null) {
                        logJson("registration_queue_failed", request.getRequestId(), request.getEmail(),
                              "Failed to publish registration request to Kafka: " + ex.getMessage());
                        handleIngressFailure(reqId, ex);
                     } else {
                        logJson("registration_queued", request.getRequestId(), request.getEmail(),
                              "Successfully published registration request to Kafka");
                        updateInboxStatusToSent(reqId);
                     }
                  } finally {
                     MDC.remove("requestId");
                  }
               });
      } catch (Exception e) {
         logJson("registration_queue_failed", request.getRequestId(), request.getEmail(),
               "Error preparing/publishing registration request to Kafka: " + e.getMessage());
         handleIngressFailure(reqId, e);
      } finally {
         MDC.remove("requestId");
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void updateInboxStatusToSent(UUID reqId) {
      registrationInboxRepository.findById(reqId).ifPresent(inbox -> {
         inbox.setStatus(RegistrationInboxStatus.SENT);
         inbox.setPublishedAt(Instant.now());
         registrationInboxRepository.save(inbox);
      });
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void handleIngressFailure(UUID reqId, Throwable ex) {
      MDC.put("requestId", reqId.toString());
      try {
         registrationInboxRepository.findById(reqId).ifPresent(inbox -> {
            int nextRetryCount = inbox.getRetryCount() + 1;
            inbox.setRetryCount(nextRetryCount);
            inbox.setLastError(ex != null ? ex.getMessage() : "Unknown Kafka publish failure");

            if (nextRetryCount > 10) {
               inbox.setStatus(RegistrationInboxStatus.FAILED_PERMANENT);
               inbox.setProcessedAt(Instant.now());
               redisTemplate.opsForValue().set("registration_result:" + reqId, "FAILED_PERMANENT", 10,
                     TimeUnit.MINUTES);
               meterRegistry.counter("omnibooking.registration.failed_permanent.count").increment();
               meterRegistry.counter("registration_failed_total").increment();
               log.warn("Ingress request {} exceeded max retries. Marked as FAILED_PERMANENT.", reqId);
            } else {
               int backoffSec = getBackoffSeconds(nextRetryCount);
               inbox.setNextRetryAt(Instant.now().plusSeconds(backoffSec));
               meterRegistry.counter("omnibooking.registration.retry.count").increment();
               log.info("Ingress request {} failed, retryCount={}, nextRetryIn={}s", reqId, nextRetryCount, backoffSec);
            }
            registrationInboxRepository.save(inbox);
         });
      } finally {
         MDC.remove("requestId");
      }
   }

   private int getBackoffSeconds(int retryCount) {
      return switch (retryCount) {
         case 1 -> 30;
         case 2 -> 60;
         case 3 -> 120;
         case 4 -> 300;
         default -> 600;
      };
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

}
