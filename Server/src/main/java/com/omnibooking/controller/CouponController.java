package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.CouponRequest;
import com.omnibooking.dto.CouponResponse;
import com.omnibooking.model.Coupon;
import com.omnibooking.model.CouponReservation;
import com.omnibooking.model.Property;
import com.omnibooking.model.enums.DiscountType;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.pricing.CouponReservationService;
import com.omnibooking.services.pricing.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons API", description = "Endpoints for managing promotions and reserving coupons")
@Slf4j
public class CouponController {

   private final CouponService couponService;

   private final CouponReservationService couponReservationService;

   private final PropertyRepository propertyRepository;

   private CouponResponse mapToResponse(Coupon c) {
      return new CouponResponse(
            c.getId(),
            c.getCode(),
            c.getDiscountType().name(),
            c.getDiscountValue(),
            c.getMinBookingAmount(),
            c.getMaxDiscountAmount(),
            c.getValidFrom(),
            c.getValidUntil(),
            c.getUsageLimit(),
            c.getUsedCount(),
            c.getReservedCount(),
            c.getProperty() != null ? c.getProperty().getId() : null,
            c.getIsActive());
   }

   @PostMapping
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Create a coupon promotion")
   public ApiResponse<CouponResponse> createCoupon(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody CouponRequest request) {
      log.info("Partner {} creating coupon: {}", principal.getId(), request.code());
      Property property = null;
      if (request.propertyId() != null) {
         property = propertyRepository.findById(request.propertyId())
               .orElseThrow(() -> new IllegalArgumentException("Property not found"));
         if (!property.getOwner().getId().equals(principal.getId())) {
            throw new IllegalArgumentException("Property does not belong to partner");
         }
      }

      Coupon coupon = Coupon.builder()
            .code(request.code())
            .discountType(DiscountType.valueOf(request.discountType().toUpperCase()))
            .discountValue(request.discountValue())
            .minBookingAmount(request.minBookingAmount() != null ? request.minBookingAmount() : BigDecimal.ZERO)
            .maxDiscountAmount(request.maxDiscountAmount())
            .validFrom(request.validFrom())
            .validUntil(request.validUntil())
            .usageLimit(request.usageLimit())
            .isActive(request.isActive() != null ? request.isActive() : true)
            .property(property)
            .build();

      Coupon created = couponService.createCoupon(coupon, principal.getId());
      return ApiResponse.success(mapToResponse(created), "Coupon created successfully", null);
   }

   @PutMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Update a coupon promotion")
   public ApiResponse<CouponResponse> updateCoupon(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id,
         @Valid @RequestBody CouponRequest request) {
      log.info("Partner {} updating coupon {}", principal.getId(), id);
      Coupon existing = couponService.getCouponById(id);
      if (existing.getProperty() != null && !existing.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Coupon does not belong to partner's property");
      }

      Property property = null;
      if (request.propertyId() != null) {
         property = propertyRepository.findById(request.propertyId())
               .orElseThrow(() -> new IllegalArgumentException("Property not found"));
         if (!property.getOwner().getId().equals(principal.getId())) {
            throw new IllegalArgumentException("Property does not belong to partner");
         }
      }

      Coupon details = Coupon.builder()
            .code(request.code())
            .discountType(DiscountType.valueOf(request.discountType().toUpperCase()))
            .discountValue(request.discountValue())
            .minBookingAmount(request.minBookingAmount() != null ? request.minBookingAmount() : BigDecimal.ZERO)
            .maxDiscountAmount(request.maxDiscountAmount())
            .validFrom(request.validFrom())
            .validUntil(request.validUntil())
            .usageLimit(request.usageLimit())
            .isActive(request.isActive() != null ? request.isActive() : true)
            .property(property)
            .build();

      Coupon updated = couponService.updateCoupon(id, details, principal.getId());
      return ApiResponse.success(mapToResponse(updated), "Coupon updated successfully", null);
   }

   @DeleteMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Delete a coupon")
   public ApiResponse<Void> deleteCoupon(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id) {
      log.info("Partner {} deleting coupon {}", principal.getId(), id);
      Coupon existing = couponService.getCouponById(id);
      if (existing.getProperty() != null && !existing.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Coupon does not belong to partner's property");
      }

      couponService.deleteCoupon(id, principal.getId());
      return ApiResponse.success(null, "Coupon deleted successfully", null);
   }

   @GetMapping("/property/{propertyId}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get all coupons for a property")
   public ApiResponse<List<CouponResponse>> getCouponsByProperty(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID propertyId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new IllegalArgumentException("Property not found"));

      if (!property.getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Property does not belong to partner");
      }

      List<CouponResponse> responses = couponService.getCouponsByProperty(propertyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

      return ApiResponse.success(responses, "Coupons retrieved successfully", null);
   }

   @GetMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get coupon details")
   public ApiResponse<CouponResponse> getCouponById(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id) {
      Coupon c = couponService.getCouponById(id);
      if (c.getProperty() != null && !c.getProperty().getOwner().getId().equals(principal.getId())) {
         throw new IllegalArgumentException("Coupon does not belong to partner's property");
      }

      return ApiResponse.success(mapToResponse(c), "Coupon retrieved successfully", null);
   }

   public record ReserveCouponRequest(
         @NotNull(message = "Coupon ID is required") UUID couponId,
         @NotBlank(message = "Booking session ID is required") String bookingSessionId,
         @NotNull(message = "Property ID is required") UUID propertyId) {
   }

   public record ReserveCouponResponse(
         UUID reservationId,
         String reservationToken,
         Instant expiresAt) {
   }

   @PostMapping("/reserve")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).USER)")
   @Operation(summary = "Reserve a coupon slot during checkout preparation")
   public ApiResponse<ReserveCouponResponse> reserveCoupon(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody ReserveCouponRequest request) {
      log.info("User {} reserving coupon {} for session {}", principal.getId(), request.couponId(),
            request.bookingSessionId());
      CouponReservation reservation = couponReservationService.reserveCoupon(
            request.couponId(),
            request.bookingSessionId(),
            principal.getId(),
            request.propertyId());

      var response = new ReserveCouponResponse(
            reservation.getId(),
            reservation.getReservationToken(),
            reservation.getExpiresAt());
      return ApiResponse.success(response, "Coupon slot reserved successfully", null);
   }

}
