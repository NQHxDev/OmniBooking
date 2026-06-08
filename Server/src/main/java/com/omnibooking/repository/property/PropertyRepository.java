package com.omnibooking.repository.property;

import com.omnibooking.model.Property;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

   @EntityGraph(attributePaths = { "amenities", "roomTypes" })
   @Query("SELECT DISTINCT p FROM Property p")
   List<Property> findAllWithAmenitiesAndRoomTypes();

   @EntityGraph(attributePaths = { "amenities", "roomTypes" })
   @Query("SELECT p FROM Property p WHERE p.id = :id")
   Optional<Property> findByIdWithAmenitiesAndRoomTypes(UUID id);

   List<Property> findByOwnerId(UUID ownerId);

   @Query("SELECT p FROM Property p WHERE p.isActive = true " +
         "AND EXISTS (SELECT m FROM com.omnibooking.model.Media m WHERE m.entityId = p.id AND m.entityType = 'PROPERTY' AND m.isMain = true) "
         +
         "ORDER BY (SELECT COUNT(b) FROM Booking b WHERE b.roomType.property = p " +
         "AND b.status IN (com.omnibooking.model.enums.BookingStatus.CONFIRMED, com.omnibooking.model.enums.BookingStatus.STAYED) "
         +
         "AND b.createdAt >= :startDate) DESC, random()")
   List<Property> findFeaturedProperties(@Param("startDate") Instant startDate, Pageable pageable);

   @Query("SELECT p FROM Property p WHERE p.isActive = true " +
         "AND EXISTS (SELECT m FROM com.omnibooking.model.Media m WHERE m.entityId = p.id AND m.entityType = 'PROPERTY' AND m.isMain = true) "
         +
         "ORDER BY p.createdAt DESC")
   List<Property> findNewProperties(Pageable pageable);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @QueryHints({
         @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"),
         @QueryHint(name = "javax.persistence.lock.timeout", value = "3000")
   })
   @Query("SELECT p FROM Property p WHERE p.id = :id")
   Optional<Property> findByIdWithWriteLock(@Param("id") UUID id);

   @Query(value = "SELECT pg_try_advisory_xact_lock(:lockId)", nativeQuery = true)
   boolean tryAdvisoryXactLock(@Param("lockId") long lockId);

   @Modifying
   @Transactional
   @Query(value = "UPDATE properties SET " +
         "rating_sum = COALESCE(rating_sum, 0) + :rating, " +
         "review_count = COALESCE(review_count, 0) + 1, " +
         "average_rating = ROUND((COALESCE(rating_sum, 0) + :rating)::numeric / (COALESCE(review_count, 0) + 1), 2) " +
         "WHERE id = :id", nativeQuery = true)
   int incrementRating(@Param("id") UUID id, @Param("rating") int rating);

   @Modifying
   @Transactional
   @Query(value = "UPDATE properties SET " +
         "rating_sum = GREATEST(0, COALESCE(rating_sum, 0) - :rating), " +
         "review_count = GREATEST(0, COALESCE(review_count, 0) - 1), " +
         "average_rating = CASE WHEN COALESCE(review_count, 0) - 1 > 0 " +
         "THEN ROUND(GREATEST(0, COALESCE(rating_sum, 0) - :rating)::numeric / (COALESCE(review_count, 0) - 1), 2) " +
         "ELSE 0.00 END " +
         "WHERE id = :id", nativeQuery = true)
   int decrementRating(@Param("id") UUID id, @Param("rating") int rating);

}
