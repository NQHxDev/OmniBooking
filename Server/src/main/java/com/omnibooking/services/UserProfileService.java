package com.omnibooking.services;

import com.omnibooking.dto.profile.UpdateProfileRequest;
import com.omnibooking.dto.profile.UserProfileResponse;
import java.util.UUID;

public interface UserProfileService {

   UserProfileResponse getProfile(UUID userId);

   UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

}
