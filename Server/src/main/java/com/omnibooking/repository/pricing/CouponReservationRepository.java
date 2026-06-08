package com.omnibooking.repository.pricing;

import com.omnibooking.model.CouponReservation;
import com.omnibooking.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponReservationRepository extends JpaRepository<CouponReservation, UUID> {

   Optional<CouponReservation> findByReservationToken(String reservationToken);

   Optional<CouponReservation> findByBookingSessionId(String bookingSessionId);

   List<CouponReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE CouponReservation cr SET cr.status = :newStatus WHERE cr.id = :id AND cr.status = :oldStatus")
   int transitionStatus(@Param("id") UUID id, @Param("oldStatus") ReservationStatus oldStatus,
         @Param("newStatus") ReservationStatus newStatus);

}
