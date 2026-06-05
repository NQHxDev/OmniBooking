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
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.repository.RegistrationInboxRepository;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

   private static final String USER_CDC_TOPIC = "omnibooking-user-cdc";

   @Transactional(readOnly = true)
   public boolean checkEmailExists(String email) {
      return userRepository.existsByEmail(email);
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void updateInboxStatus(UUID requestId, RegistrationInboxStatus status) {
      registrationInboxRepository.findById(requestId).ifPresent(inbox -> {
         inbox.setStatus(status);
         registrationInboxRepository.save(inbox);
         log.debug("Updated pg registration_inbox status for requestId {} to {}", requestId, status);
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

            TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                     @Override
                     public void afterCommit() {
                        // Mark inbox status to SUCCESS
                        updateInboxStatus(UUID.fromString(finalRequestId), RegistrationInboxStatus.SUCCESS);

                        // Cache result in Redis for 24 hours (Durable Result)
                        String resultKey = "registration_result:" + finalRequestId;
                        redisTemplate.opsForValue().set(resultKey, "SUCCESS", 24, TimeUnit.HOURS);

                        // Send real-time notify
                        notifyClient(finalRequestId, finalUser, finalProfile);
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
      try {
         // Double check DB existence to be safe
         if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("Email {} already exists during individual fallback insert. Rejecting.", user.getEmail());
            updateInboxStatus(reqId, RegistrationInboxStatus.FAILED);
            redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "FAILED_DUPLICATE_EMAIL", 24,
                  TimeUnit.HOURS);
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
                     updateInboxStatus(reqId, RegistrationInboxStatus.SUCCESS);
                     redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "SUCCESS", 24,
                           TimeUnit.HOURS);
                     notifyClient(msg.getRequestId(), user, profile);
                  }
               });
      } catch (Exception e) {
         log.error("Failed to save individual user record during fallback for requestId: {}", msg.getRequestId(), e);
         updateInboxStatus(reqId, RegistrationInboxStatus.FAILED);
         redisTemplate.opsForValue().set("registration_result:" + msg.getRequestId(), "FAILED", 24, TimeUnit.HOURS);
      }
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

         String dataJson = objectMapper.writeValueAsString(authResponse);

         // Format: requestId|jsonData
         String message = requestId + "|" + dataJson;
         redisTemplate.convertAndSend(RedisPubSubConfig.REGISTRATION_TOPIC, message);
         log.debug("Published registration completion for requestId: {}", requestId);
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize AuthResponse for SSE notification", e);
      }
   }

}
