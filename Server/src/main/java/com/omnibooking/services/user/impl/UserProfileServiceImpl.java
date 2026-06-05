package com.omnibooking.services.user.impl;

import com.omnibooking.dto.profile.UpdateProfileRequest;
import com.omnibooking.dto.profile.UserProfileResponse;
import com.omnibooking.dto.profile.ChangePasswordRequest;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.user.UserProfileRepository;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

   private final UserProfileRepository userProfileRepository;

   private final EncryptionService encryptionService;

   private final UserRepository userRepository;

   private final PasswordEncoder passwordEncoder;

   @Override
   public UserProfileResponse getProfile(UUID userId) {
      UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      return mapToResponse(profile);
   }

   @Override
   @Transactional
   public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
      UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      if (request.getDisplayName() != null)
         profile.setDisplayName(request.getDisplayName());
      if (request.getDateOfBirth() != null)
         profile.setDateOfBirth(request.getDateOfBirth());
      if (request.getGender() != null)
         profile.setGender(request.getGender());
      if (request.getAddress() != null)
         profile.setAddress(request.getAddress());
      if (request.getNationality() != null)
         profile.setNationality(request.getNationality());
      if (request.getAvatarUrl() != null)
         profile.setAvatarUrl(request.getAvatarUrl());

      if (request.getPhoneNumber() != null) {
         profile.setPhoneEncrypted(encryptionService.encrypt(request.getPhoneNumber()));
         profile.setPhoneSearchHash(encryptionService.createBlindIndex(request.getPhoneNumber()));
      }

      UserProfile savedProfile = userProfileRepository.saveAndFlush(profile);
      log.info("Updated profile for user: {}", userId);

      return mapToResponse(savedProfile);
   }

   private UserProfileResponse mapToResponse(UserProfile profile) {
      boolean hasPassword = profile.getUser().getPassword() != null && !profile.getUser().getPassword().isEmpty();
      String phone = encryptionService.decrypt(profile.getPhoneEncrypted());
      String maskedPhone = maskPhoneNumber(phone);

      return UserProfileResponse.builder()
            .email(profile.getUser().getEmail())
            .displayName(profile.getDisplayName())
            .dateOfBirth(profile.getDateOfBirth())
            .gender(profile.getGender())
            .address(profile.getAddress())
            .nationality(profile.getNationality())
            .phoneNumber(maskedPhone)
            .avatarUrl(profile.getAvatarUrl())
            .isVerified(Boolean.TRUE.equals(profile.getIsVerified()))
            .points(profile.getPoints())
            .rankName(profile.getRank() != null ? profile.getRank().getName() : "BRONZE")
            .hasPassword(hasPassword)
            .build();
   }

   private String maskPhoneNumber(String phone) {
      if (phone == null || phone.trim().isEmpty()) {
         return null;
      }
      String trimmed = phone.trim();
      if (trimmed.length() <= 3) {
         return "*".repeat(trimmed.length());
      }
      String lastThree = trimmed.substring(trimmed.length() - 3);
      return "*".repeat(trimmed.length() - 3) + lastThree;
   }

   @Override
   @Transactional
   public void changePassword(UUID userId, ChangePasswordRequest request) {
      UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
      User user = profile.getUser();

      boolean currentlyHasPassword = user.getPassword() != null && !user.getPassword().isEmpty();

      if (currentlyHasPassword) {
         if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            throw new AppException(ErrorCode.INCORRECT_CURRENT_PASSWORD);
         }
         if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INCORRECT_CURRENT_PASSWORD);
         }
      }

      user.setPassword(passwordEncoder.encode(request.getNewPassword()));
      userRepository.save(user);
      log.info("Password updated successfully for user: {}", userId);
   }

}
