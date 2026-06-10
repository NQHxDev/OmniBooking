package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.PriceRuleRequest;
import com.omnibooking.dto.PriceRuleResponse;
import com.omnibooking.model.PriceRule;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.enums.AdjustmentType;
import com.omnibooking.model.enums.RuleType;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.pricing.PriceRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pricing-rules")
@RequiredArgsConstructor
@Tag(name = "Pricing Rules API", description = "Endpoints for managing dynamic pricing rules")
@Slf4j
public class PriceRuleController {

   private final PriceRuleService priceRuleService;

   private final PropertyRepository propertyRepository;

   private final RoomTypeRepository roomTypeRepository;

   private PriceRuleResponse mapToResponse(PriceRule rule) {
      return new PriceRuleResponse(
            rule.getId(),
            rule.getProperty().getId(),
            rule.getRoomType() != null ? rule.getRoomType().getId() : null,
            rule.getName(),
            rule.getRuleType().name(),
            rule.getStartDate(),
            rule.getEndDate(),
            rule.getAdjustmentType().name(),
            rule.getAdjustmentValue(),
            rule.getOccupancyThreshold(),
            rule.getPriority(),
            rule.getIsActive());
   }

   @PostMapping
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Create a dynamic pricing rule")
   public ApiResponse<PriceRuleResponse> createRule(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody PriceRuleRequest request) {
      log.info("Partner {} creating price rule: {}", principal.getId(), request.name());
      Property property = propertyRepository.findById(request.propertyId())
            .orElseThrow(() -> new IllegalArgumentException("Property not found"));

      if (!property.getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Property does not belong to the partner");
      }

      RoomType roomType = null;
      if (request.roomTypeId() != null) {
         roomType = roomTypeRepository.findById(request.roomTypeId())
               .orElseThrow(() -> new IllegalArgumentException("Room type not found"));
      }

      PriceRule rule = PriceRule.builder()
            .property(property)
            .roomType(roomType)
            .name(request.name())
            .ruleType(RuleType.valueOf(request.ruleType().toUpperCase()))
            .startDate(request.startDate())
            .endDate(request.endDate())
            .adjustmentType(AdjustmentType.valueOf(request.adjustmentType().toUpperCase()))
            .adjustmentValue(request.adjustmentValue())
            .occupancyThreshold(request.occupancyThreshold())
            .priority(request.priority() != null ? request.priority() : 0)
            .isActive(request.isActive() != null ? request.isActive() : true)
            .build();

      PriceRule created = priceRuleService.createRule(rule, principal.getId());
      return ApiResponse.success(mapToResponse(created), "Pricing rule created successfully", null);
   }

   @PutMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Update a pricing rule")
   public ApiResponse<PriceRuleResponse> updateRule(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id,
         @Valid @RequestBody PriceRuleRequest request) {
      log.info("Partner {} updating price rule {}", principal.getId(), id);
      PriceRule existing = priceRuleService.getRuleById(id);
      if (!existing.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Pricing rule does not belong to partner's property");
      }

      RoomType roomType = null;
      if (request.roomTypeId() != null) {
         roomType = roomTypeRepository.findById(request.roomTypeId())
               .orElseThrow(() -> new IllegalArgumentException("Room type not found"));
      }

      PriceRule details = PriceRule.builder()
            .roomType(roomType)
            .name(request.name())
            .ruleType(RuleType.valueOf(request.ruleType().toUpperCase()))
            .startDate(request.startDate())
            .endDate(request.endDate())
            .adjustmentType(AdjustmentType.valueOf(request.adjustmentType().toUpperCase()))
            .adjustmentValue(request.adjustmentValue())
            .occupancyThreshold(request.occupancyThreshold())
            .priority(request.priority() != null ? request.priority() : 0)
            .isActive(request.isActive() != null ? request.isActive() : true)
            .build();

      PriceRule updated = priceRuleService.updateRule(id, details, principal.getId());
      return ApiResponse.success(mapToResponse(updated), "Pricing rule updated successfully", null);
   }

   @DeleteMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Delete a pricing rule")
   public ApiResponse<Void> deleteRule(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id) {
      log.info("Partner {} deleting price rule {}", principal.getId(), id);
      PriceRule existing = priceRuleService.getRuleById(id);
      if (!existing.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Pricing rule does not belong to partner's property");
      }

      priceRuleService.deleteRule(id, principal.getId());
      return ApiResponse.success(null, "Pricing rule deleted successfully", null);
   }

   @GetMapping("/property/{propertyId}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get all pricing rules for a property")
   public ApiResponse<List<PriceRuleResponse>> getRulesByProperty(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID propertyId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new IllegalArgumentException("Property not found"));

      if (!property.getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Property does not belong to the partner");
      }

      List<PriceRuleResponse> responses = priceRuleService.getRulesByProperty(propertyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

      return ApiResponse.success(responses, "Pricing rules retrieved successfully", null);
   }

   @GetMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get price rule details")
   public ApiResponse<PriceRuleResponse> getRuleById(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id) {
      PriceRule rule = priceRuleService.getRuleById(id);
      if (!rule.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Pricing rule does not belong to partner's property");
      }

      return ApiResponse.success(mapToResponse(rule), "Pricing rule retrieved successfully", null);
   }

}
