package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.CreateReviewRequest;
import com.omnibooking.dto.PageResponse;
import com.omnibooking.dto.ReviewReplyRequest;
import com.omnibooking.dto.ReviewResponse;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.property.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews API", description = "Endpoints for guest reviews, partner replies, and moderation")
@Slf4j
public class ReviewController {

   private final ReviewService reviewService;

   @PostMapping
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).USER)")
   @Operation(summary = "Submit a property review")
   public ApiResponse<ReviewResponse> createReview(
         @AuthenticationPrincipal UserPrincipal principal,
         @Valid @RequestBody CreateReviewRequest request) {
      log.info("Request to submit review by user {}: booking={}", principal.getId(), request.getBookingId());
      ReviewResponse response = reviewService.createReview(principal.getId(), request);

      return ApiResponse.success(response, "Review submitted successfully", null);
   }

   @PostMapping("/{id}/reply")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Submit partner reply to a review")
   public ApiResponse<ReviewResponse> replyToReview(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id,
         @Valid @RequestBody ReviewReplyRequest request) {
      log.info("Request by partner {} to reply to review {}", principal.getId(), id);
      ReviewResponse response = reviewService.replyToReview(principal.getId(), id, request);

      return ApiResponse.success(response, "Reply submitted successfully", null);
   }

   @DeleteMapping("/{id}")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).USER) or hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN)")
   @Operation(summary = "Soft delete a review")
   public ApiResponse<Void> deleteReview(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id,
         @RequestParam(required = false, defaultValue = "Deleted by user/admin") String reason) {
      log.info("Request by user {} to delete review {}", principal.getId(), id);
      reviewService.deleteReview(principal.getId(), id, reason);

      return ApiResponse.success(null, "Review deleted successfully", null);
   }

   @PostMapping("/{id}/hide")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN)")
   @Operation(summary = "Hide a review (Moderation)")
   public ApiResponse<ReviewResponse> hideReview(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id,
         @RequestParam String reason) {
      log.info("Request by admin {} to hide review {}", principal.getId(), id);
      ReviewResponse response = reviewService.hideReview(principal.getId(), id, reason);

      return ApiResponse.success(response, "Review hidden successfully", null);
   }

   @PostMapping("/{id}/restore")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN)")
   @Operation(summary = "Restore a review (Moderation)")
   public ApiResponse<ReviewResponse> restoreReview(
         @AuthenticationPrincipal UserPrincipal principal,
         @PathVariable UUID id) {
      log.info("Request by admin {} to restore review {}", principal.getId(), id);
      ReviewResponse response = reviewService.restoreReview(principal.getId(), id);

      return ApiResponse.success(response, "Review restored successfully", null);
   }

   @GetMapping("/properties/{propertyId}")
   @Operation(summary = "Get reviews for a property")
   public ApiResponse<PageResponse<ReviewResponse>> getPropertyReviews(
         @PathVariable UUID propertyId,
         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      log.info("Fetching public reviews for property {}, page={}", propertyId, pageable.getPageNumber());
      PageResponse<ReviewResponse> response = reviewService.getPropertyReviews(propertyId, pageable);

      return ApiResponse.success(response);
   }

   @GetMapping("/me")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).USER)")
   @Operation(summary = "Get logged-in user's reviews")
   public ApiResponse<PageResponse<ReviewResponse>> getUserReviews(
         @AuthenticationPrincipal UserPrincipal principal,
         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      log.info("Fetching reviews for guest {}, page={}", principal.getId(), pageable.getPageNumber());
      PageResponse<ReviewResponse> response = reviewService.getUserReviews(principal.getId(), pageable);

      return ApiResponse.success(response);
   }

   @GetMapping("/partner")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).PARTNER)")
   @Operation(summary = "Get partner's properties reviews")
   public ApiResponse<PageResponse<ReviewResponse>> getPartnerReviews(
         @AuthenticationPrincipal UserPrincipal principal,
         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      log.info("Fetching reviews for partner {}, page={}", principal.getId(), pageable.getPageNumber());
      PageResponse<ReviewResponse> response = reviewService.getPartnerReviews(principal.getId(), pageable);

      return ApiResponse.success(response);
   }

   @PostMapping("/admin/rebuild-ratings")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).ADMIN)")
   @Operation(summary = "Trigger rebuild ratings job")
   public ApiResponse<Void> rebuildRatings(
         @RequestParam(required = false) UUID propertyId) {
      if (propertyId != null) {
         log.info("Triggering rebuild ratings job for property {}", propertyId);
         reviewService.rebuildPropertyRatings(propertyId);
      } else {
         log.info("Triggering rebuild ratings job for all properties");
         reviewService.rebuildAllRatings();
      }

      return ApiResponse.success(null, "Rebuild job triggered successfully", null);
   }

}
