package com.omnibooking.repository.property;

import com.omnibooking.model.Property;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

   @EntityGraph(attributePaths = {"amenities", "roomTypes"})
   @Query("SELECT DISTINCT p FROM Property p")
   List<Property> findAllWithAmenitiesAndRoomTypes();

   @EntityGraph(attributePaths = {"amenities", "roomTypes"})
   @Query("SELECT p FROM Property p WHERE p.id = :id")
   Optional<Property> findByIdWithAmenitiesAndRoomTypes(UUID id);

   List<Property> findByOwnerId(UUID ownerId);

   @org.springframework.data.jpa.repository.Query("SELECT p FROM Property p WHERE p.isActive = true " +
         "AND EXISTS (SELECT m FROM com.omnibooking.model.Media m WHERE m.entityId = p.id AND m.entityType = 'PROPERTY' AND m.isMain = true) " +
         "ORDER BY (SELECT COUNT(b) FROM Booking b WHERE b.roomType.property = p " +
         "AND b.status IN (com.omnibooking.model.enums.BookingStatus.CONFIRMED, com.omnibooking.model.enums.BookingStatus.STAYED) " +
         "AND b.createdAt >= :startDate) DESC, random()")
   List<Property> findFeaturedProperties(
         @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate, 
         org.springframework.data.domain.Pageable pageable);

   @org.springframework.data.jpa.repository.Query("SELECT p FROM Property p WHERE p.isActive = true " +
         "AND EXISTS (SELECT m FROM com.omnibooking.model.Media m WHERE m.entityId = p.id AND m.entityType = 'PROPERTY' AND m.isMain = true) " +
         "ORDER BY p.createdAt DESC")
   List<Property> findNewProperties(org.springframework.data.domain.Pageable pageable);

}
