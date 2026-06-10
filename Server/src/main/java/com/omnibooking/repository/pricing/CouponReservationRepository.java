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

   @Query("SELECT cr FROM CouponReservation cr WHERE cr.customer.id = :customerId AND cr.coupon.id = :couponId AND cr.status = :status ORDER BY cr.reservedAt DESC")
   List<CouponReservation> findByCustomerIdAndCouponIdAndStatusOrderByReservedAtDesc(
         @Param("customerId") UUID customerId,
         @Param("couponId") UUID couponId,
         @Param("status") ReservationStatus status);

   @Query("SELECT cr FROM CouponReservation cr " +
         "WHERE cr.status = com.omnibooking.model.enums.ReservationStatus.CONSUMED " +
         "AND EXISTS (SELECT b FROM Booking b " +
         "            WHERE b.coupon = cr.coupon " +
         "            AND b.user = cr.customer " +
         "            AND b.status IN (com.omnibooking.model.enums.BookingStatus.CANCELLED, com.omnibooking.model.enums.BookingStatus.EXPIRED))")
   List<CouponReservation> findLeakedCouponReservations();

   @Modifying(clearAutomatically = true)
   @Query("UPDATE CouponReservation cr SET cr.status = :newStatus WHERE cr.id = :id AND cr.status = :oldStatus")
   int transitionStatus(@Param("id") UUID id, @Param("oldStatus") ReservationStatus oldStatus,
         @Param("newStatus") ReservationStatus newStatus);

}
