package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.PartnerLegalProfileResponse;
import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.dto.IncompleteUploadResponse;
import com.omnibooking.services.property.PropertyService;
import com.omnibooking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.omnibooking.dto.PropertyDetailResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/partner/properties")
@RequiredArgsConstructor
@Tag(name = "Property Management", description = "Endpoints for Partners to manage their properties")
@Slf4j
public class PropertyController {

   private final PropertyService propertyService;

   @PostMapping
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Register a new property (Partner Only)")
   public ApiResponse<PropertyResponse> createProperty(@Valid @RequestBody PropertyRequest request) {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Creating property for user: {}", userId);
      PropertyResponse response = propertyService.createProperty(request, userId);

      return ApiResponse.success(response);
   }

   @GetMapping("/mine")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get all properties owned by current partner")
   public ApiResponse<List<PropertyResponse>> getMyProperties() {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Fetching properties for user: {}", userId);
      List<PropertyResponse> response = propertyService.getPropertiesByOwner(userId);

      return ApiResponse.success(response);
   }

   @GetMapping("/legal-profiles")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get active legal profiles for current partner")
   public ApiResponse<List<PartnerLegalProfileResponse>> getLegalProfiles() {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Fetching legal profiles for partner: {}", userId);
      List<PartnerLegalProfileResponse> response = propertyService.getPartnerLegalProfiles(userId);

      return ApiResponse.success(response);
   }

   @GetMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get detailed property by ID (Partner Only)")
   public ApiResponse<PropertyDetailResponse> getPropertyDetail(@PathVariable UUID id) {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Fetching detailed property: {} for user: {}", id, userId);
      PropertyDetailResponse response = propertyService.getPropertyDetailForPartner(id, userId);

      return ApiResponse.success(response);
   }

   @GetMapping("/incomplete-uploads")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get properties with incomplete image uploads for current partner")
   public ApiResponse<List<IncompleteUploadResponse>> getIncompleteUploads() {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Fetching incomplete uploads for partner: {}", userId);
      List<IncompleteUploadResponse> response = propertyService.getIncompleteUploads(userId);

      return ApiResponse.success(response);
   }

   @PatchMapping("/{propertyId}/dismiss-incomplete")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Dismiss incomplete image upload warning for a property")
   public ApiResponse<Void> dismissIncompleteUpload(@PathVariable UUID propertyId) {
      UUID userId = SecurityUtils.getCurrentUserId();
      log.info("Controller: Dismissing incomplete uploads warning for property: {} by partner: {}", propertyId, userId);
      propertyService.dismissIncompleteUpload(propertyId, userId);

      return ApiResponse.success(null);
   }

}
