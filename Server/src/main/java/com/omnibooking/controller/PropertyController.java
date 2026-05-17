package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.services.property.PropertyService;
import com.omnibooking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
