package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.services.PropertyService;
import com.omnibooking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/partner/properties")
@RequiredArgsConstructor
@Tag(name = "Property Management", description = "Endpoints for Partners to manage their properties")
public class PropertyController {

   private final PropertyService propertyService;

   @PostMapping
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Register a new property (Partner Only)")
   public ApiResponse<PropertyResponse> createProperty(@Valid @RequestBody PropertyRequest request) {
      PropertyResponse response = propertyService.createProperty(request, SecurityUtils.getCurrentUserId());
      return ApiResponse.success(response);
   }

   @GetMapping("/mine")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get all properties owned by current partner")
   public ApiResponse<List<PropertyResponse>> getMyProperties() {
      List<PropertyResponse> response = propertyService.getPropertiesByOwner(SecurityUtils.getCurrentUserId());
      return ApiResponse.success(response);
   }
}
