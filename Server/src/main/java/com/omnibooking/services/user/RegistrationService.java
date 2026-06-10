package com.omnibooking.services.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.config.RedisPubSubConfig;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.dto.event.UserCreatedEvent;
import com.omnibooking.mapper.UserMapper;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.repository.user.UserProfileRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

   private final UserRepository userRepository;

   private final UserProfileRepository userProfileRepository;

   private final RegistrationInboxRepository registrationInboxRepository;

   private final CachedRoleService cachedRoleService;

   private final UserMapper userMapper;

   private final KafkaTemplate<String, Object> kafkaTemplate;

   private final StringRedisTemplate redisTemplate;

   private final ObjectMapper objectMapper;

   private final BloomFilterService bloomFilterService;

   private final JWTService jwtService;

   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   private static final String USER_CDC_TOPIC = "omnibooking-user-cdc";

   @Transactional(readOnly = true)
   public boolean checkEmailExists(String email) {
      return userRepository.existsByEmail(email);
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void updateInboxStatus(UUID requestId, RegistrationInboxStatus status) {
      registrationInboxRepository.findById(requestId).ifPresent(inbox -> {
         inbox.setStatus(status);
         if (status == RegistrationInboxStatus.SUCCESS || status == RegistrationInboxStatus.FAILED
               || status == RegistrationInboxStatus.FAILED_PERMANENT) {
            inbox.setProcessedAt(Instant.now());
         }
         registrationInboxRepository.save(inbox);
         log.debug("Updated pg registration_inbox status for requestId {} to {}", requestId, status);
      });
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public boolean claimInboxForProcessing(UUID requestId) {
      Instant now = Instant.now();
      int updated = registrationInboxRepository.claimRequestForProcessing(requestId, now);
      if (updated > 0) {
         return true;
      }

      return registrationInboxRepository.findById(requestId)
            .map(inbox -> {
               if (inbox.getStatus() == RegistrationInboxStatus.SUCCESS
                     || inbox.getStatus() == RegistrationInboxStatus.FAILED_PERMANENT) {
                  log.info("Request {} already completed/failed permanently, skipping", requestId);
                  meterRegistry.counter("omnibooking.event.duplicate").increment();
               } else {
                  log.info("Request {} is currently in status {}, skipping concurrent execution", requestId,
                        inbox.getStatus());
               }
               return false;
            })
            .orElseGet(() -> {
               try {
                  RegistrationInbox inbox = RegistrationInbox.builder()
                        .requestId(requestId)
                        .payload("{}")
                        .status(RegistrationInboxStatus.PROCESSING)
                        .processingStartedAt(now)
                        .updatedAt(now)
                        .build();
                  registrationInboxRepository.save(inbox);
                  return true;
               } catch (DataIntegrityViolationException e) {
                  log.warn("Concurrent creation of inbox record failed for request {}", requestId);
                  meterRegistry.counter("omnibooking.event.duplicate").increment();
                  return false;
               }
            });
   }

   @Transactional
   public void saveBatchProcessed(List<User> users, List<UserProfile> profiles, List<RegistrationMessage> messages) {
      try {
         // Perform fast batch inserts (Argon2 hashing was offloaded to worker threads
         // outside transaction)
         userRepository.saveAll(users);
         userProfileRepository.saveAll(profiles);

         for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            UserProfile profile = profiles.get(i);
            RegistrationMessage msg = messages.get(i);

            // Add to Bloom Filter
            bloomFilterService.add(user.getEmail());

            // Emit CDC Event (Async)
            UserCreatedEvent event = UserCreatedEvent.builder()
                  .eventId(UuidCreator.getTimeOrderedEpoch())
                  .userId(user.getId())
                  .email(user.getEmail())
                  .fullName(profile.getDisplayName())
                  .build();
            kafkaTemplate.send(USER_CDC_TOPIC, event.getUserId().toString(), event);

            // Notify SSE via Redis PubSub after transaction commit
            final String finalRequestId = msg.getRequestId();
            final User finalUser = user;
            final UserProfile finalProfile = profile;
            final String finalEmail = user.getEmail();

            TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                     @Override
                     public void afterCommit() {
                        MDC.put("requestId", finalRequestId);
                        try {
                           // Mark inbox status to SUCCESS
                           updateInboxStatus(UUID.fromString(finalRequestId), RegistrationInboxStatus.SUCCESS);

                           // Cache result in Redis for 7 days (Durable Result)
                           String resultKey = "registration_result:" + finalRequestId;
                           redisTemplate.opsForValue().set(resultKey, "SUCCESS", 7, TimeUnit.DAYS);

                           // Increment success metric
                           meterRegistry.counter("registration_success_total").increment();

                           // Log JSON success event
                           logJson("registration_db_committed", finalRequestId, finalEmail,
                                 "Registration request successfully saved to database");

                           // Send real-time notify
                           notifyClient(finalRequestId, finalUser, finalProfile);
                        } finally {
                           MDC.remove("requestId");
                        }
                     }
                  });
         }
      } catch (Exception e) {
         log.warn("Batch insert failed (e.g. duplicate constraint or DB error), falling back to individual inserts", e);
         // Fallback: process records individually in new transactions so that a single
         // duplicate doesn't fail the batch
         for (int i = 0; i < users.size(); i++) {
            saveIndividual(users.get(i), profiles.get(i), messages.get(i));
         }
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void saveIndividual(User user, UserProfile profile, RegistrationMessage msg) {
      UUID reqId = UUID.fromString(msg.getRequestId());
      MDC.put("requestId", msg.getRequestId());
      try {
         // Double check DB existence to be safe
         if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Email {} already exists during individual fallback insert. Rejecting.", user.getEmail());
            updateInboxStatus(reqId, RegistrationInboxStatus.FAILED);
            redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "FAILED_DUPLICATE_EMAIL", 10,
                  TimeUnit.MINUTES);
            meterRegistry.counter("registration_failed_total").increment();
            logJson("registration_db_failed_duplicate", msg.getRequestId(), user.getEmail(),
                  "Registration failed: Email already exists during individual insert fallback");
            return;
         }

         userRepository.save(user);
         userProfileRepository.save(profile);
         bloomFilterService.add(user.getEmail());

         UserCreatedEvent event = UserCreatedEvent.builder()
               .eventId(UuidCreator.getTimeOrderedEpoch())
               .userId(user.getId())
               .email(user.getEmail())
               .fullName(profile.getDisplayName())
               .build();
         kafkaTemplate.send(USER_CDC_TOPIC, event.getUserId().toString(), event);

         TransactionSynchronizationManager.registerSynchronization(
               new TransactionSynchronization() {
                  @Override
                  public void afterCommit() {
                     MDC.put("requestId", msg.getRequestId());
                     try {
                        updateInboxStatus(reqId, RegistrationInboxStatus.SUCCESS);
                        redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "SUCCESS", 7,
                              TimeUnit.DAYS);
                        meterRegistry.counter("registration_success_total").increment();
                        logJson("registration_db_committed", msg.getRequestId(), user.getEmail(),
                              "Registration request successfully saved to database (individual fallback)");
                        notifyClient(msg.getRequestId(), user, profile);
                     } finally {
                        MDC.remove("requestId");
                     }
                  }
               });
      } catch (Exception e) {
         log.error("Failed to save individual user record during fallback for requestId: {}", msg.getRequestId(), e);
         handleProcessingFailure(reqId, e);
      } finally {
         MDC.remove("requestId");
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void handleProcessingFailure(UUID reqId, Throwable ex) {
      MDC.put("requestId", reqId.toString());
      try {
         registrationInboxRepository.findById(reqId).ifPresent(inbox -> {
            int nextRetryCount = inbox.getRetryCount() + 1;
            inbox.setRetryCount(nextRetryCount);
            inbox.setLastError(ex != null ? ex.getMessage() : "Unknown consumer processing failure");

            if (nextRetryCount > 10) {
               inbox.setStatus(RegistrationInboxStatus.FAILED_PERMANENT);
               inbox.setProcessedAt(Instant.now());
               redisTemplate.opsForValue().set("registration_result:" + reqId, "FAILED_PERMANENT", 7,
                     TimeUnit.DAYS);
               meterRegistry.counter("omnibooking.registration.failed_permanent.count").increment();
               meterRegistry.counter("registration_failed_total").increment();
               logJson("registration_failed_permanent", reqId.toString(), null,
                     "Consumer processing failed permanently after exceeding max retries");
               log.warn("Processing request {} exceeded max retries. Marked as FAILED_PERMANENT.", reqId);
            } else {
               inbox.setStatus(RegistrationInboxStatus.PENDING);
               int backoffSec = getBackoffSeconds(nextRetryCount);
               inbox.setNextRetryAt(Instant.now().plusSeconds(backoffSec));
               meterRegistry.counter("omnibooking.registration.retry.count").increment();
               logJson("registration_processing_retry", reqId.toString(), null,
                     "Consumer processing failed temporarily, scheduling retry " + nextRetryCount);
               log.info("Processing request {} failed, retryCount={}, nextRetryIn={}s", reqId, nextRetryCount,
                     backoffSec);
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

   /**
    * Legacy batch method left for backward compatibility.
    */
   @Transactional
   public void saveBatch(List<RegisterRequest> requests) {
      Role userRole;
      try {
         userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);
      } catch (Exception e) {
         log.error("ROLE_USER not found, cannot process legacy batch", e);
         return;
      }

      List<User> usersToSave = new ArrayList<>();

      for (RegisterRequest request : requests) {
         if (bloomFilterService.mightContain(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
               continue;
            }
         }

         User user = userMapper.toUser(request);
         // This runs Argon2 sequentially inside the transaction loop (slow, legacy)
         user.setRoles(Collections.singleton(userRole));
         usersToSave.add(user);
      }

      if (usersToSave.isEmpty())
         return;

      List<User> savedUsers = userRepository.saveAll(usersToSave);

      for (int i = 0; i < savedUsers.size(); i++) {
         User user = savedUsers.get(i);
         RegisterRequest req = requests.get(i);

         UserProfile profile = UserProfile.builder()
               .user(user)
               .userId(user.getId())
               .displayName(req.getFullName())
               .build();
         userProfileRepository.save(profile);

         bloomFilterService.add(user.getEmail());

         UserCreatedEvent event = UserCreatedEvent.builder()
               .eventId(UuidCreator.getTimeOrderedEpoch())
               .userId(user.getId())
               .email(user.getEmail())
               .fullName(req.getFullName())
               .build();
         kafkaTemplate.send(USER_CDC_TOPIC, event.getUserId().toString(), event);

         final String finalRequestId = req.getRequestId();
         final User finalUser = user;
         final UserProfile finalProfile = profile;

         TransactionSynchronizationManager.registerSynchronization(
               new TransactionSynchronization() {
                  @Override
                  public void afterCommit() {
                     notifyClient(finalRequestId, finalUser, finalProfile);
                  }
               });
      }
   }

   private void notifyClient(String requestId, User user, UserProfile profile) {
      if (requestId == null)
         return;
      try {
         Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
         AuthResponse authResponse = userMapper.toAuthResponse(user, profile, roleNames);

         // Generate a temporary access token for session finalization
         String accessToken = jwtService.generateAccessToken(user.getId(), roleNames, UUID.randomUUID(),
               "async-registration");
         authResponse.setAccessToken(accessToken);

         // Store the temporary token in Redis with a 10-minute TTL
         String tokenKey = "registration_token:" + requestId;
         redisTemplate.opsForValue().set(tokenKey, accessToken, 10, TimeUnit.MINUTES);

         String dataJson = objectMapper.writeValueAsString(authResponse);

         // Format: requestId|jsonData
         String message = requestId + "|" + dataJson;
         redisTemplate.convertAndSend(RedisPubSubConfig.REGISTRATION_TOPIC, message);
         logJson("registration_published_pubsub", requestId, user.getEmail(),
               "Published registration completion event to Redis Pub/Sub");
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize AuthResponse for SSE notification", e);
      }
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
