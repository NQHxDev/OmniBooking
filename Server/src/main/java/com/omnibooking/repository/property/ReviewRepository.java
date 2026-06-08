package com.omnibooking.repository.property;

import com.omnibooking.model.Review;
import com.omnibooking.model.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

   Optional<Review> findByBookingId(UUID bookingId);

   Page<Review> findByPropertyIdAndStatusAndDeletedAtIsNull(UUID propertyId, ReviewStatus status, Pageable pageable);

   Page<Review> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

   Page<Review> findByPropertyOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);

   Page<Review> findByStatusAndDeletedAtIsNull(ReviewStatus status, Pageable pageable);

   @Query("SELECT COUNT(r) FROM Review r WHERE r.property.id = :propertyId AND r.deletedAt IS NULL AND r.status = com.omnibooking.model.enums.ReviewStatus.PUBLISHED")
   long countActiveReviewsByPropertyId(@Param("propertyId") UUID propertyId);

   @Query("SELECT SUM(r.rating) FROM Review r WHERE r.property.id = :propertyId AND r.deletedAt IS NULL AND r.status = com.omnibooking.model.enums.ReviewStatus.PUBLISHED")
   Long sumActiveRatingsByPropertyId(@Param("propertyId") UUID propertyId);

   /**
    * Calculates the average rating score for a partner.
    * Business rules:
    * - Only includes active, published reviews (r.deletedAt IS NULL and status =
    * PUBLISHED).
    * - Excludes reviews belonging to soft-deleted properties (r.property.deletedAt
    * IS NULL).
    * - Includes reviews belonging to disabled/inactive properties
    * (r.property.isActive is ignored) to reflect historical reputation.
    */
   @Query("SELECT AVG(r.rating) FROM Review r WHERE r.property.owner.id = :ownerId AND r.deletedAt IS NULL AND r.property.deletedAt IS NULL AND r.status = com.omnibooking.model.enums.ReviewStatus.PUBLISHED")
   Double getAverageRatingByOwnerId(@Param("ownerId") UUID ownerId);

}
