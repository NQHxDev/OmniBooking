package com.omnibooking.repository;

import com.omnibooking.model.Booking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

   List<Booking> findByUserId(UUID userId);

   List<Booking> findByGuestPhoneSearchHash(String phoneSearchHash);

   long countByGuestPhoneSearchHash(String phoneSearchHash);

   @Query("SELECT b FROM Booking b WHERE b.roomType.property.owner.id = :ownerId AND b.deletedAt IS NULL")
   List<Booking> findAllByPartnerId(@Param("ownerId") UUID ownerId);

}
