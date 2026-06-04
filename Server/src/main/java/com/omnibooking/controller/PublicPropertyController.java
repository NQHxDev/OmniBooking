package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.dto.PropertyDetailResponse;
import com.omnibooking.services.property.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Property Public API", description = "Public endpoints for viewing properties")
@Slf4j
public class PublicPropertyController {

   private final PropertyService propertyService;

   @GetMapping("/featured")
   @Operation(summary = "Get featured properties for homepage")
   public ApiResponse<List<PropertyResponse>> getFeaturedProperties(
         @RequestParam(defaultValue = "6") int limit) {
      log.info("Public API: Fetching {} featured properties", limit);
      List<PropertyResponse> response = propertyService.getFeaturedProperties(limit);

      return ApiResponse.success(response);
   }

   @GetMapping("/new")
   @Operation(summary = "Get newly added properties for homepage")
   public ApiResponse<List<PropertyResponse>> getNewProperties(
         @RequestParam(defaultValue = "15") int limit) {
      log.info("Public API: Fetching {} new properties", limit);
      List<PropertyResponse> response = propertyService.getNewProperties(limit);

      return ApiResponse.success(response);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get detailed property by ID for public view")
   public ApiResponse<PropertyDetailResponse> getPropertyDetail(@PathVariable UUID id) {
      log.info("Public API: Fetching detailed property: {}", id);
      PropertyDetailResponse response = propertyService.getPropertyDetail(id);

      return ApiResponse.success(response);
   }

}
