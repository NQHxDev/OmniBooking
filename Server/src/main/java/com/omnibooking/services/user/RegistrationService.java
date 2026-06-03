package com.omnibooking.services.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.config.RedisPubSubConfig;
import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.event.UserCreatedEvent;
import com.omnibooking.mapper.UserMapper;
import com.omnibooking.model.Role;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.core.BloomFilterService;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

   private final UserRepository userRepository;
   private final UserProfileRepository userProfileRepository;
   private final CachedRoleService cachedRoleService;
   private final PasswordEncoder passwordEncoder;
   private final UserMapper userMapper;
   private final KafkaTemplate<String, Object> kafkaTemplate;
   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final BloomFilterService bloomFilterService;
   private final JWTService jwtService;

   private static final String USER_CDC_TOPIC = "omnibooking-user-cdc";

   @Transactional
   public void saveBatch(List<RegisterRequest> requests) {
      Role userRole;
      try {
         userRole = cachedRoleService.getRoleByName("ROLE_USER");
      } catch (Exception e) {
         log.error("ROLE_USER not found, cannot process batch", e);
         return;
      }

      List<User> usersToSave = new ArrayList<>();

      for (RegisterRequest request : requests) {
         // Check Bloom Filter again inside transaction for consistency
         if (bloomFilterService.mightContain(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
               continue;
            }
         }

         User user = userMapper.toUser(request);
         user.setPassword(passwordEncoder.encode(request.getPassword()));
         user.setRoles(Collections.singleton(userRole));
         usersToSave.add(user);
      }

      if (usersToSave.isEmpty())
         return;

      // saveAll will trigger persist for each new entity
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

         // Update Bloom Filter
         bloomFilterService.add(user.getEmail());

         // 1. Emit Kafka Event for other services (Async)
         UserCreatedEvent event = UserCreatedEvent.builder()
               .eventId(com.github.f4b6a3.uuid.UuidCreator.getTimeOrderedEpoch())
               .userId(user.getId())
               .email(user.getEmail())
               .fullName(req.getFullName())
               .build();
         kafkaTemplate.send(USER_CDC_TOPIC, event.getUserId().toString(), event);

         // 2. Notify SSE via Redis Pub/Sub (Real-time) - ONLY AFTER TRANSACTION COMMITS
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

         // Generate a temporary access token to be used for session finalization
         String accessToken = jwtService.generateAccessToken(user.getId(), roleNames, java.util.UUID.randomUUID(),
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
