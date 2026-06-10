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

import io.micrometer.core.instrument.MeterRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.constant.SecurityConstants;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

   private final MeterRegistry meterRegistry;

   public RegistrationKafkaConsumer(
         EncryptionService encryptionService,
         PasswordEncoder passwordEncoder,
         CachedRoleService cachedRoleService,
         RegistrationService registrationService,
         StringRedisTemplate redisTemplate,
         BloomFilterService bloomFilterService,
         @Qualifier("registrationCpuExecutor") Executor executor,
         ObjectMapper objectMapper,
         MeterRegistry meterRegistry) {
      this.encryptionService = encryptionService;
      this.passwordEncoder = passwordEncoder;
      this.cachedRoleService = cachedRoleService;
      this.registrationService = registrationService;
      this.redisTemplate = redisTemplate;
      this.bloomFilterService = bloomFilterService;
      this.executor = executor;
      this.objectMapper = objectMapper;
      this.meterRegistry = meterRegistry;
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
      List<String> acquiredLocks = new ArrayList<>();

      try {
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

               // 1. Redis Cache check (Optimization layer)
               String resultKey = "registration_result:" + msg.getRequestId();
               String cachedResult = redisTemplate.opsForValue().get(resultKey);
               if ("SUCCESS".equals(cachedResult) || "FAILED_PERMANENT".equals(cachedResult)) {
                  log.info("Redis cache hit: request {} already completed, skipping", msg.getRequestId());
                  meterRegistry.counter("omnibooking.event.duplicate").increment();
                  continue;
               }

               // 2. Redis Distributed Lock (5 mins TTL to match lease duration)
               String lockKey = "registration_lock:" + msg.getRequestId();
               Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
               if (Boolean.FALSE.equals(locked)) {
                  log.warn("Redis lock exists for request {}, skipping concurrent processing", msg.getRequestId());
                  continue;
               }
               acquiredLocks.add(lockKey);

               // 3. DB Inbox Pattern (Source of Truth)
               if (!registrationService.claimInboxForProcessing(reqId)) {
                  redisTemplate.delete(lockKey);
                  acquiredLocks.remove(lockKey);
                  continue;
               }

               // Bloom Filter optimized exists check (Optimization only)
               if (bloomFilterService.mightContain(email)) {
                  if (registrationService.checkEmailExists(email)) {
                     log.warn("Email {} already exists in DB, marking request as FAILED", email);
                     registrationService.updateInboxStatus(reqId, RegistrationInboxStatus.FAILED);
                     redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(),
                           "FAILED_DUPLICATE_EMAIL",
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
                     // BaseEntity prePersist fallback, set UUIDv7 here
                     .id(UuidCreator.getTimeOrderedEpoch())
                     .username(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5))
                     .email(email)
                     .password(hashedPassword)
                     .isActive(true)
                     .roles(Collections.singleton(userRole))
                     .build();
               user.setCreatedAt(Instant.now());
               user.setUpdatedAt(Instant.now());

               UserProfile profile = UserProfile.builder()
                     .user(user)
                     .userId(user.getId())
                     .displayName(msg.getFullName())
                     .build();
               profile.setUpdatedAt(Instant.now());

               usersToSave.add(user);
               profilesToSave.add(profile);
               messagesToSave.add(msg);

            } catch (Exception e) {
               log.error("Failed to prepare registration message for requestId: {}", msg.getRequestId(), e);
               if (isDatabaseOutage(e)) {
                  throw new RuntimeException("Database outage detected during message preparation", e);
               }
               try {
                  registrationService.handleProcessingFailure(reqId, e);
               } catch (Exception ex) {
                  log.error("Failed to handle processing failure in DB for requestId: {}", msg.getRequestId(), ex);
                  throw new RuntimeException("Database outage detected during failure handling", ex);
               }
            } finally {
               MDC.remove("requestId");
            }
         }

         // Perform batch insert inside a fast database transaction
         if (!usersToSave.isEmpty()) {
            try {
               registrationService.saveBatchProcessed(usersToSave, profilesToSave, messagesToSave);
            } catch (Exception e) {
               log.error("Batch save failed", e);
               throw new RuntimeException("Database outage detected during batch save", e);
            }
         }
      } finally {
         // Guarantee that all locks are cleaned up after the batch processing completes
         // (or fails)
         for (String lockKey : acquiredLocks) {
            try {
               redisTemplate.delete(lockKey);
            } catch (Exception e) {
               log.warn("Failed to clean up Redis lock key: {}", lockKey, e);
            }
         }
      }
   }

   private boolean isDatabaseOutage(Throwable t) {
      if (t == null)
         return false;
      String className = t.getClass().getName();
      if (className.contains("redis") || className.contains("Redis")) {
         return false;
      }
      String msg = t.getMessage();
      if (msg != null
            && (msg.contains("Database is down") || msg.contains("connection") || msg.contains("Connection"))) {
         return true;
      }
      if (t instanceof DataAccessResourceFailureException ||
            t instanceof TransactionException) {
         return true;
      }
      return isDatabaseOutage(t.getCause());
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
