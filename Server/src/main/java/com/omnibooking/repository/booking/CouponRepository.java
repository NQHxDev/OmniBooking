package com.omnibooking.repository.booking;

import com.omnibooking.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

   Optional<Coupon> findByCodeIgnoreCase(String code);

   @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code) AND c.isActive = true AND (c.property.id = :propertyId OR c.property IS NULL)")
   Optional<Coupon> findActiveCouponByCodeAndProperty(@Param("code") String code, @Param("propertyId") UUID propertyId);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE Coupon c SET c.reservedCount = c.reservedCount + 1 " +
         "WHERE c.id = :id AND c.isActive = true AND (c.usageLimit IS NULL OR c.usedCount + c.reservedCount < c.usageLimit)")
   int incrementReservedCountAtomically(@Param("id") UUID id);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE Coupon c SET c.reservedCount = c.reservedCount - 1 " +
         "WHERE c.id = :id AND c.reservedCount > 0")
   int decrementReservedCountAtomically(@Param("id") UUID id);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1, c.reservedCount = c.reservedCount - 1 " +
         "WHERE c.id = :id AND c.reservedCount > 0")
   int consumeReservedCouponAtomically(@Param("id") UUID id);

   List<Coupon> findByPropertyId(UUID propertyId);

}
