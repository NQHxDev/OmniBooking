package com.omnibooking.services.auth.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.auth.passkey.PasskeyRegistrationOptionsResponse;
import com.omnibooking.dto.auth.passkey.PasskeyRegistrationVerifyRequest;
import com.omnibooking.dto.auth.passkey.PasskeyResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.User;
import com.omnibooking.model.UserPasskey;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.repository.UserPasskeyRepository;
import com.omnibooking.services.auth.PasskeyService;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasskeyServiceImpl implements PasskeyService {

   private final AppProperties appProperties;
   private final StringRedisTemplate redisTemplate;
   private final UserRepository userRepository;
   private final UserPasskeyRepository userPasskeyRepository;

   private static final String CHALLENGE_PREFIX = "passkey_challenge:";

   @Override
   public PasskeyRegistrationOptionsResponse generateRegistrationOptions(UUID userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      Challenge challenge = new DefaultChallenge();
      String challengeBase64 = Base64.getEncoder().encodeToString(challenge.getValue());

      // Store challenge in Redis with 5 mins TTL
      redisTemplate.opsForValue().set(CHALLENGE_PREFIX + userId, challengeBase64, 5, TimeUnit.MINUTES);

      return PasskeyRegistrationOptionsResponse.builder()
            .challenge(challengeBase64)
            .rpId(appProperties.getWebauthn().getRpId())
            .rpName(appProperties.getWebauthn().getRpName())
            .userId(userId.toString())
            .username(user.getEmail())
            .userDisplayName(user.getUsername())
            .build();
   }

   @Override
   @Transactional
   public void verifyRegistration(UUID userId, PasskeyRegistrationVerifyRequest request) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      String storedChallenge = redisTemplate.opsForValue().get(CHALLENGE_PREFIX + userId);
      if (storedChallenge == null) {
         throw new AppException(ErrorCode.INVALID_TOKEN);
      }

      redisTemplate.delete(CHALLENGE_PREFIX + userId);

      UserPasskey passkey = UserPasskey.builder()
            .user(user)
            .credentialId(request.getId())
            .publicKey("SIMULATED_PUBLIC_KEY")
            .signCount(0L)
            .label(request.getLabel() != null ? request.getLabel() : "My Device")
            .build();

      userPasskeyRepository.save(passkey);
      log.info("Passkey registered for user: {}", user.getEmail());
   }

   @Override
   public boolean hasPasskeys(UUID userId) {
      return userPasskeyRepository.existsByUserId(userId);
   }

   @Override
   public List<PasskeyResponse> listPasskeys(UUID userId) {
      return userPasskeyRepository.findAllByUserId(userId).stream()
            .map(p -> PasskeyResponse.builder()
                  .id(p.getId())
                  .label(p.getLabel())
                  .credentialId(p.getCredentialId())
                  .createdAt(p.getCreatedAt())
                  .lastUsedAt(p.getUpdatedAt()) // Use updatedAt as lastUsedAt for simulation
                  .build())
            .collect(Collectors.toList());
   }

   @Override
   @Transactional
   public void deletePasskey(UUID userId, UUID passkeyId) {
      UserPasskey passkey = userPasskeyRepository.findById(passkeyId)
            .orElseThrow(() -> new AppException(ErrorCode.PASSKEY_NOT_FOUND));

      if (!passkey.getUser().getId().equals(userId)) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      userPasskeyRepository.delete(passkey);
   }

}
