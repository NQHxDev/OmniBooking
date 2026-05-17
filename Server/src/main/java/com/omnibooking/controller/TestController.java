package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.services.core.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

   private final UserProfileRepository userProfileRepository;
   private final EncryptionService encryptionService;

   @GetMapping("/search-phone")
   public ApiResponse<Map<String, Object>> searchPhone(@RequestParam String phone) {
      // 1. Create Blind Index from input
      String searchHash = encryptionService.createBlindIndex(phone);

      // 2. Search in DB using Hash
      Optional<UserProfile> profileOpt = userProfileRepository.findByPhoneSearchHash(searchHash);

      Map<String, Object> result = new HashMap<>();
      result.put("input_phone", phone);
      result.put("computed_hash", searchHash);

      if (profileOpt.isPresent()) {
         UserProfile profile = profileOpt.get();
         result.put("found", true);
         result.put("user_id", profile.getUserId());
         result.put("display_name", profile.getDisplayName());
         result.put("encrypted_phone_in_db", profile.getPhoneEncrypted());
         result.put("decrypted_phone", encryptionService.decrypt(profile.getPhoneEncrypted()));
      } else {
         result.put("found", false);
         result.put("message", "Không tìm thấy user với số điện thoại này trong DB");
      }

      return ApiResponse.success(result);
   }
}
