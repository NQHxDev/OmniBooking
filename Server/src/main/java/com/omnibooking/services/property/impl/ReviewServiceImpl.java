package com.omnibooking.services.property.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.dto.CreateReviewRequest;
import com.omnibooking.dto.PageResponse;
import com.omnibooking.dto.ReviewReplyRequest;
import com.omnibooking.dto.ReviewResponse;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Booking;
import com.omnibooking.model.Property;
import com.omnibooking.model.Review;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.ReviewStatus;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.property.ReviewService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

   private final ReviewRepository reviewRepository;

   private final PropertyRepository propertyRepository;

   private final BookingRepository bookingRepository;

   private final UserRepository userRepository;

   private final OutboxService outboxService;

   private final StringRedisTemplate redisTemplate;

   private final MeterRegistry meterRegistry;

   private boolean isRateLimitAllowed(UUID userId) {
      String key = "rate_limit:reviews:" + userId.toString();
      long now = Instant.now().toEpochMilli();
      long oneHourAgo = now - 3600 * 1000;
      try {
         redisTemplate.opsForZSet().removeRangeByScore(key, 0, oneHourAgo);
         Long count = redisTemplate.opsForZSet().zCard(key);
         if (count != null && count >= 5) {
            log.warn("Rate limit exceeded for user: {} on review creation.", userId);
            return false;
         }
         redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
         redisTemplate.expire(key, Duration.ofHours(1));
         return true;
      } catch (Exception e) {
         log.error("Redis rate limit failure, running fail-open policy for user {}", userId, e);
         meterRegistry.counter("redis.rate.limit.failure.total").increment();
         return true; // Fail open
      }
   }

   private Pageable clampPageable(Pageable pageable) {
      int pageSize = pageable.getPageSize();
      if (pageSize < 10) {
         pageSize = 10;
      } else if (pageSize > 50) {
         pageSize = 50;
      }
      return PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
   }

   private ReviewResponse mapToResponse(Review review) {
      String userName = "Anonymous";
      String userAvatarUrl = null;
      if (review.getUser() != null) {
         if (review.getUser().getProfile() != null) {
            userName = review.getUser().getProfile().getDisplayName();
            userAvatarUrl = review.getUser().getProfile().getAvatarUrl();
         } else {
            userName = review.getUser().getUsername();
         }
      }
      return ReviewResponse.builder()
            .id(review.getId())
            .bookingId(review.getBooking().getId())
            .propertyId(review.getProperty().getId())
            .propertyName(review.getProperty().getName())
            .userId(review.getUser() != null ? review.getUser().getId() : null)
            .userName(userName)
            .userAvatarUrl(userAvatarUrl)
            .rating(review.getRating())
            .comment(review.getComment())
            .reply(review.getReply())
            .status(review.getStatus().name())
            .replyUpdatedAt(review.getReplyUpdatedAt())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .build();
   }

   @Override
   @Transactional
   public ReviewResponse createReview(UUID currentUserId, CreateReviewRequest request) {
      if (!isRateLimitAllowed(currentUserId)) {
         throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
      }

      Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      if (!booking.getUser().getId().equals(currentUserId)) {
         throw new AppException(ErrorCode.NOT_BOOKING_OWNER);
      }

      if (booking.getStatus() != BookingStatus.STAYED) {
         throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
      }

      if (!LocalDate.now().isAfter(booking.getCheckOutDate())) {
         throw new AppException(ErrorCode.CHRONOLOGICAL_VALIDATION_FAILED);
      }

      if (reviewRepository.findByBookingId(request.getBookingId()).isPresent()) {
         throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
      }

      String comment = request.getComment();
      if (comment != null && !comment.trim().isEmpty()) {
         int length = comment.length();
         if (length < 10) {
            throw new AppException(ErrorCode.MINIMUM_TEXT_LENGTH_VIOLATION);
         }
         if (length > 1000) {
            throw new AppException(ErrorCode.MAXIMUM_TEXT_LENGTH_VIOLATION);
         }
      } else {
         comment = null; // Normalize empty/blank comments to null
      }

      UUID propertyId = booking.getRoomType().getProperty().getId();
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      Review review = Review.builder()
            .booking(booking)
            .property(property)
            .user(user)
            .rating(request.getRating())
            .comment(comment)
            .status(ReviewStatus.PUBLISHED)
            .build();

      review = reviewRepository.save(review);

      // Perform lockless atomic native update in DB
      propertyRepository.incrementRating(propertyId, request.getRating());

      // Transactional Outbox publish
      outboxService.saveEvent(
            property.getId(),
            "PROPERTY",
            EventConstants.PROPERTY_SYNC,
            PropertySyncEvent.builder()
                  .propertyId(property.getId())
                  .operation("UPDATE")
                  .build());

      meterRegistry.counter("review.creation.total", "status", review.getStatus().name()).increment();

      return mapToResponse(review);
   }

   @Override
   @Transactional
   public ReviewResponse replyToReview(UUID currentUserId, UUID reviewId, ReviewReplyRequest request) {
      Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

      if (!review.getProperty().getOwner().getId().equals(currentUserId)) {
         throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
      }

      review.setReply(request.getReply());
      review.setReplyUpdatedAt(Instant.now());
      review = reviewRepository.save(review);

      log.info("Partner {} replied to review {} of property {}", currentUserId, reviewId, review.getProperty().getId());

      return mapToResponse(review);
   }

   @Override
   @Transactional
   public void deleteReview(UUID currentUserId, UUID reviewId, String reason) {
      Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

      User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      UUID propertyId = review.getProperty().getId();
      if (review.getStatus() == ReviewStatus.PUBLISHED) {
         // Perform lockless atomic native update in DB
         propertyRepository.decrementRating(propertyId, review.getRating());
      }

      review.setDeletedAt(Instant.now());
      review.setDeletedBy(currentUser);
      review.setDeletionReason(reason);
      review.setStatus(ReviewStatus.REMOVED);
      reviewRepository.save(review);

      // Transactional Outbox publish
      outboxService.saveEvent(
            propertyId,
            "PROPERTY",
            EventConstants.PROPERTY_SYNC,
            PropertySyncEvent.builder()
                  .propertyId(propertyId)
                  .operation("UPDATE")
                  .build());

      meterRegistry.counter("review.moderation.total", "transition", "SOFT_DELETE").increment();
   }

   @Override
   @Transactional
   public ReviewResponse hideReview(UUID currentUserId, UUID reviewId, String reason) {
      Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

      User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      UUID propertyId = review.getProperty().getId();
      if (review.getStatus() == ReviewStatus.PUBLISHED) {
         // Perform lockless atomic native update in DB
         propertyRepository.decrementRating(propertyId, review.getRating());
      }

      review.setStatus(ReviewStatus.HIDDEN);
      review.setModeratedAt(Instant.now());
      review.setModeratedBy(currentUser);
      review.setModerationReason(reason);
      review = reviewRepository.save(review);

      // Transactional Outbox publish
      outboxService.saveEvent(
            propertyId,
            "PROPERTY",
            EventConstants.PROPERTY_SYNC,
            PropertySyncEvent.builder()
                  .propertyId(propertyId)
                  .operation("UPDATE")
                  .build());

      meterRegistry.counter("review.moderation.total", "transition", "HIDE").increment();

      return mapToResponse(review);
   }

   @Override
   @Transactional
   public ReviewResponse restoreReview(UUID currentUserId, UUID reviewId) {
      Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

      User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      UUID propertyId = review.getProperty().getId();
      if (review.getStatus() != ReviewStatus.PUBLISHED) {
         // Perform lockless atomic native update in DB
         propertyRepository.incrementRating(propertyId, review.getRating());
      }

      review.setStatus(ReviewStatus.PUBLISHED);
      review.setDeletedAt(null);
      review.setDeletedBy(null);
      review.setDeletionReason(null);
      review.setModeratedAt(Instant.now());
      review.setModeratedBy(currentUser);
      review.setModerationReason("Restored review");
      review = reviewRepository.save(review);

      // Transactional Outbox publish
      outboxService.saveEvent(
            propertyId,
            "PROPERTY",
            EventConstants.PROPERTY_SYNC,
            PropertySyncEvent.builder()
                  .propertyId(propertyId)
                  .operation("UPDATE")
                  .build());

      meterRegistry.counter("review.moderation.total", "transition", "RESTORE").increment();

      return mapToResponse(review);
   }

   @Override
   @Transactional(readOnly = true)
   public PageResponse<ReviewResponse> getPropertyReviews(UUID propertyId, Pageable pageable) {
      Pageable clamped = clampPageable(pageable);
      Page<Review> page = reviewRepository.findByPropertyIdAndStatusAndDeletedAtIsNull(propertyId,
            ReviewStatus.PUBLISHED, clamped);
      return PageResponse.of(
            page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements());
   }

   @Override
   @Transactional(readOnly = true)
   public PageResponse<ReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
      Pageable clamped = clampPageable(pageable);
      Page<Review> page = reviewRepository.findByUserIdAndDeletedAtIsNull(userId, clamped);
      return PageResponse.of(
            page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements());
   }

   @Override
   @Transactional(readOnly = true)
   public PageResponse<ReviewResponse> getPartnerReviews(UUID partnerId, Pageable pageable) {
      Pageable clamped = clampPageable(pageable);
      Page<Review> page = reviewRepository.findByPropertyOwnerIdAndDeletedAtIsNull(partnerId, clamped);
      return PageResponse.of(
            page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements());
   }

   @Override
   @Async
   @Transactional
   public void rebuildPropertyRatings(UUID propertyId) {
      boolean acquired = propertyRepository.tryAdvisoryXactLock(1911L);
      if (!acquired) {
         log.warn("Rebuild ratings job already running or locked. Aborting rebuild for property {}", propertyId);
         return;
      }

      long startTime = System.nanoTime();
      try {
         Property property = propertyRepository.findByIdWithWriteLock(propertyId)
               .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

         long count = reviewRepository.countActiveReviewsByPropertyId(propertyId);
         Long sum = reviewRepository.sumActiveRatingsByPropertyId(propertyId);
         if (sum == null) {
            sum = 0L;
         }

         BigDecimal average = BigDecimal.ZERO;
         if (count > 0) {
            average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
         }

         log.info("Rebuilding ratings for property {}: count={} -> {}, sum={} -> {}, average={} -> {}",
               propertyId, property.getReviewCount(), count, property.getRatingSum(), sum, property.getAverageRating(),
               average);

         property.setReviewCount((int) count);
         property.setRatingSum(sum);
         property.setAverageRating(average);
         propertyRepository.save(property);

         outboxService.saveEvent(
               property.getId(),
               "PROPERTY",
               EventConstants.PROPERTY_SYNC,
               PropertySyncEvent.builder()
                     .propertyId(property.getId())
                     .operation("UPDATE")
                     .build());

      } finally {
         long duration = System.nanoTime() - startTime;
         meterRegistry.timer("review.repair.job.duration.seconds").record(Duration.ofNanos(duration));
      }
   }

   @Override
   @Async
   @Transactional
   public void rebuildAllRatings() {
      boolean acquired = propertyRepository.tryAdvisoryXactLock(1911L);
      if (!acquired) {
         log.warn("Rebuild ratings job already running or locked. Aborting rebuild all.");
         return;
      }

      long startTime = System.nanoTime();
      try {
         log.info("Starting background full rebuild ratings job...");
         List<Property> properties = propertyRepository.findAll();
         for (Property property : properties) {
            long count = reviewRepository.countActiveReviewsByPropertyId(property.getId());
            Long sum = reviewRepository.sumActiveRatingsByPropertyId(property.getId());
            if (sum == null) {
               sum = 0L;
            }

            BigDecimal average = BigDecimal.ZERO;
            if (count > 0) {
               average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }

            property.setReviewCount((int) count);
            property.setRatingSum(sum);
            property.setAverageRating(average);
            propertyRepository.save(property);

            outboxService.saveEvent(
                  property.getId(),
                  "PROPERTY",
                  EventConstants.PROPERTY_SYNC,
                  PropertySyncEvent.builder()
                        .propertyId(property.getId())
                        .operation("UPDATE")
                        .build());
         }
         log.info("Finished rebuilding ratings for all {} properties.", properties.size());
      } finally {
         long duration = System.nanoTime() - startTime;
         meterRegistry.timer("review.repair.job.duration.seconds").record(Duration.ofNanos(duration));
      }
   }

}
