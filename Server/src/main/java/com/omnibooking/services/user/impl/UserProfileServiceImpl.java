package com.omnibooking.services.user.impl;

import com.omnibooking.dto.profile.UpdateProfileRequest;
import com.omnibooking.dto.profile.UserProfileResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

   private final UserProfileRepository userProfileRepository;
   private final EncryptionService encryptionService;

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
      return UserProfileResponse.builder()
            .email(profile.getUser().getEmail())
            .displayName(profile.getDisplayName())
            .dateOfBirth(profile.getDateOfBirth())
            .gender(profile.getGender())
            .address(profile.getAddress())
            .nationality(profile.getNationality())
            .phoneNumber(encryptionService.decrypt(profile.getPhoneEncrypted()))
            .avatarUrl(profile.getAvatarUrl())
            .isVerified(Boolean.TRUE.equals(profile.getIsVerified()))
            .points(profile.getPoints())
            .rankName(profile.getRank() != null ? profile.getRank().getName() : "BRONZE")
            .build();
   }

}
