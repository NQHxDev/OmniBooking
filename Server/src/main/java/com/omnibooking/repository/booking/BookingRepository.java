package com.omnibooking.repository.booking;

import com.omnibooking.model.Booking;
import com.omnibooking.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT b FROM Booking b WHERE b.id = :id")
   Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

   List<Booking> findByUserId(UUID userId);

   List<Booking> findByGuestPhoneSearchHash(String phoneSearchHash);

   long countByGuestPhoneSearchHash(String phoneSearchHash);

   /**
    * Fetch bookings along with roomType and property.
    * WARNING: Do NOT add collection fetch joins (e.g. One-to-Many) to this method
    * to avoid Cartesian product explosion and duplicate rows.
    */
   @Query("SELECT b FROM Booking b JOIN FETCH b.roomType rt JOIN FETCH rt.property WHERE rt.property.owner.id = :ownerId AND b.deletedAt IS NULL")
   List<Booking> findAllByPartnerId(@Param("ownerId") UUID ownerId);

   /** Atomic expiration — race-safe against concurrent payment callbacks */
   @Modifying
   @Query("UPDATE Booking b SET b.status = :expiredStatus, b.updatedAt = :now " +
         "WHERE b.id = :bookingId " +
         "AND b.status = :pendingStatus " +
         "AND b.expiresAt < :now")
   int atomicExpireBooking(@Param("bookingId") UUID bookingId,
                           @Param("now") Instant now,
                           @Param("pendingStatus") BookingStatus pendingStatus,
                           @Param("expiredStatus") BookingStatus expiredStatus);

   /** Atomic confirmation — race-safe against concurrent payment callbacks */
   @Modifying
   @Query("UPDATE Booking b SET b.status = :confirmedStatus, b.expiresAt = NULL, b.updatedAt = :now " +
         "WHERE b.id = :bookingId " +
         "AND b.status = :pendingStatus")
   int atomicConfirmBooking(@Param("bookingId") UUID bookingId,
                            @Param("now") Instant now,
                            @Param("pendingStatus") BookingStatus pendingStatus,
                            @Param("confirmedStatus") BookingStatus confirmedStatus);

   /** Paged query for batch expiration processing */
   @Query("SELECT b FROM Booking b " +
         "WHERE b.status IN :statuses AND b.expiresAt < :now " +
         "AND b.deletedAt IS NULL")
   List<Booking> findExpiredBookings(@Param("statuses") List<BookingStatus> statuses,
                                    @Param("now") Instant now,
                                    Pageable pageable);

   @Query("SELECT b FROM Booking b " +
         "WHERE b.status IN :statuses AND b.expiresAt < :threshold " +
         "AND b.deletedAt IS NULL")
   List<Booking> findStuckBookings(@Param("statuses") List<BookingStatus> statuses,
                                   @Param("threshold") Instant threshold);

}
