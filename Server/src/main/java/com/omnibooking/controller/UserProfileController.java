package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.profile.UpdateProfileRequest;
import com.omnibooking.dto.profile.UserProfileResponse;
import com.omnibooking.dto.profile.ChangePasswordRequest;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

   private final UserProfileService userProfileService;

   @GetMapping("/me")
   public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
         @AuthenticationPrincipal UserPrincipal principal) {
      UserProfileResponse response = userProfileService.getProfile(principal.getId());
      return ResponseEntity.ok(ApiResponse.success(response, "Profile fetched successfully", null));
   }

   @PatchMapping("/me")
   public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
         @AuthenticationPrincipal UserPrincipal principal,
         @RequestBody UpdateProfileRequest request) {
      UserProfileResponse response = userProfileService.updateProfile(principal.getId(), request);
      return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully", null));
   }

   @PostMapping("/password")
   public ResponseEntity<ApiResponse<Void>> changePassword(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody ChangePasswordRequest request) {
      userProfileService.changePassword(principal.getId(), request);
      return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully", null));
   }

}

