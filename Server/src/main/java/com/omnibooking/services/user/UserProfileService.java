package com.omnibooking.services.user;

import com.omnibooking.dto.profile.UpdateProfileRequest;
import com.omnibooking.dto.profile.UserProfileResponse;
import com.omnibooking.dto.profile.ChangePasswordRequest;
import java.util.UUID;

public interface UserProfileService {

   UserProfileResponse getProfile(UUID userId);

   UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

   void changePassword(UUID userId, ChangePasswordRequest request);

}
