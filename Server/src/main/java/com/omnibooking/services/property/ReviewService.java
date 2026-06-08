package com.omnibooking.services.property;

import com.omnibooking.dto.CreateReviewRequest;
import com.omnibooking.dto.PageResponse;
import com.omnibooking.dto.ReviewReplyRequest;
import com.omnibooking.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

   ReviewResponse createReview(UUID currentUserId, CreateReviewRequest request);

   ReviewResponse replyToReview(UUID currentUserId, UUID reviewId, ReviewReplyRequest request);

   void deleteReview(UUID currentUserId, UUID reviewId, String reason);

   ReviewResponse hideReview(UUID currentUserId, UUID reviewId, String reason);

   ReviewResponse restoreReview(UUID currentUserId, UUID reviewId);

   PageResponse<ReviewResponse> getPropertyReviews(UUID propertyId, Pageable pageable);

   PageResponse<ReviewResponse> getUserReviews(UUID userId, Pageable pageable);

   PageResponse<ReviewResponse> getPartnerReviews(UUID partnerId, Pageable pageable);

   void rebuildPropertyRatings(UUID propertyId);

   void rebuildAllRatings();

}
