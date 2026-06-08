package com.omnibooking.consumer;

import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.user.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.constant.SecurityConstants;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RegistrationKafkaConsumer {

   private final EncryptionService encryptionService;

   private final PasswordEncoder passwordEncoder;

   private final CachedRoleService cachedRoleService;

   private final RegistrationService registrationService;

   private final StringRedisTemplate redisTemplate;

   private final BloomFilterService bloomFilterService;

   private final Executor executor;

   private final ObjectMapper objectMapper;

   public RegistrationKafkaConsumer(
         EncryptionService encryptionService,
         PasswordEncoder passwordEncoder,
         CachedRoleService cachedRoleService,
         RegistrationService registrationService,
         StringRedisTemplate redisTemplate,
         BloomFilterService bloomFilterService,
         @Qualifier("registrationCpuExecutor") Executor executor,
         ObjectMapper objectMapper) {
      this.encryptionService = encryptionService;
      this.passwordEncoder = passwordEncoder;
      this.cachedRoleService = cachedRoleService;
      this.registrationService = registrationService;
      this.redisTemplate = redisTemplate;
      this.bloomFilterService = bloomFilterService;
      this.executor = executor;
      this.objectMapper = objectMapper;
   }

   @KafkaListener(topics = "${omnibooking.kafka.registration.topic-name:registration-request-topic}", groupId = "registration-workers", containerFactory = "registrationListenerContainerFactory")
   public void consumeBatch(List<ConsumerRecord<String, RegistrationMessage>> records, Acknowledgment acknowledgment) {
      if (records == null || records.isEmpty()) {
         acknowledgment.acknowledge();
         return;
      }

      log.info("Kafka Consumer polled batch of {} registration requests", records.size());

      // Delegate all CPU-intensive work and DB writes to the dedicated Worker Pool
      CompletableFuture<Void> batchTask = CompletableFuture.runAsync(() -> {
         processBatch(records);
      }, executor);

      try {
         // Synchronously block the Consumer thread to manage backpressure
         batchTask.get();

         // Acknowledge offset only after successful processing and DB commit
         acknowledgment.acknowledge();
         log.info("Batch of {} requests successfully processed and committed", records.size());
      } catch (Exception e) {
         log.error("Batch processing failed. Offset will not be committed.", e);
         throw new RuntimeException("Kafka batch processing failed", e);
      }
   }

   private void processBatch(List<ConsumerRecord<String, RegistrationMessage>> records) {
      Role userRole;
      try {
         userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);
      } catch (Exception e) {
         log.error("ROLE_USER not found, aborting batch processing", e);
         throw new RuntimeException("Required role not found", e);
      }

      List<User> usersToSave = new ArrayList<>();
      List<UserProfile> profilesToSave = new ArrayList<>();
      List<RegistrationMessage> messagesToSave = new ArrayList<>();

      for (ConsumerRecord<String, RegistrationMessage> record : records) {
         RegistrationMessage msg = record.value();
         if (msg == null)
            continue;

         UUID reqId = UUID.fromString(msg.getRequestId());
         String email = msg.getEmail();

         // Set MDC context for the current record
         org.slf4j.MDC.put("requestId", msg.getRequestId());
         try {
            logJson("registration_consumed", msg.getRequestId(), email, "Kafka consumer started processing request");

            // Idempotency check using Redis
            String idempotencyKey = "registration_idempotency:" + msg.getRequestId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", 24, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(isNew)) {
               log.warn("Duplicate request detected in consumer for requestId: {}. Skipping.", msg.getRequestId());
               continue;
            }

            // Update status to PROCESSING in PG Inbox
            registrationService.updateInboxStatus(reqId, RegistrationInboxStatus.PROCESSING);

            // Bloom Filter optimized exists check (Optimization only)
            if (bloomFilterService.mightContain(email)) {
               if (registrationService.checkEmailExists(email)) {
                  log.warn("Email {} already exists in DB, marking request as FAILED", email);
                  registrationService.updateInboxStatus(reqId, RegistrationInboxStatus.FAILED);
                  redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "FAILED_DUPLICATE_EMAIL",
                        10, TimeUnit.MINUTES);
                  continue;
               }
            }

            // Decrypt password (CPU-bound)
            String decryptedPassword = encryptionService.decrypt(msg.getEncryptedPassword(), msg.getKeyId());

            // Argon2 Hashing (CPU-bound)
            String hashedPassword = passwordEncoder.encode(decryptedPassword);

            // Build Entity instances (ready for fast batch insert)
            User user = User.builder()
                  .id(com.github.f4b6a3.uuid.UuidCreator.getTimeOrderedEpoch()) // BaseEntity prePersist fallback, but
                                                                                // set UUIDv7 here
                  .username(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5))
                  .email(email)
                  .password(hashedPassword)
                  .isActive(true)
                  .roles(Collections.singleton(userRole))
                  .build();
            user.setCreatedAt(java.time.Instant.now());
            user.setUpdatedAt(java.time.Instant.now());

            UserProfile profile = UserProfile.builder()
                  .user(user)
                  .userId(user.getId())
                  .displayName(msg.getFullName())
                  .build();
            profile.setUpdatedAt(java.time.Instant.now());

            usersToSave.add(user);
            profilesToSave.add(profile);
            messagesToSave.add(msg);

         } catch (Exception e) {
            log.error("Failed to prepare registration message for requestId: {}", msg.getRequestId(), e);
            registrationService.handleProcessingFailure(reqId, e);
         } finally {
            org.slf4j.MDC.remove("requestId");
         }
      }

      // 7. Perform batch insert inside a fast database transaction
      if (!usersToSave.isEmpty()) {
         registrationService.saveBatchProcessed(usersToSave, profilesToSave, messagesToSave);
      }
   }

   private void logJson(String event, String requestId, String email, String message) {
      try {
         Map<String, Object> logPayload = new java.util.HashMap<>();
         logPayload.put("requestId", requestId);
         logPayload.put("event", event);
         if (email != null) {
            logPayload.put("email", email);
         }
         logPayload.put("message", message);
         logPayload.put("timestamp", java.time.Instant.now().toString());
         log.info(objectMapper.writeValueAsString(logPayload));
      } catch (Exception e) {
         log.error("Failed to write JSON log", e);
      }
   }

}
