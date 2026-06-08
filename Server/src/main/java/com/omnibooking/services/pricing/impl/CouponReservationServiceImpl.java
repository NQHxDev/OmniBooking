package com.omnibooking.services.pricing.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.model.Coupon;
import com.omnibooking.model.CouponReservation;
import com.omnibooking.model.Property;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.ReservationStatus;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.pricing.CouponReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CouponReservationServiceImpl implements CouponReservationService {

   private static final Logger log = LoggerFactory.getLogger(CouponReservationServiceImpl.class);

   private final CouponReservationRepository couponReservationRepository;

   private final CouponRepository couponRepository;

   private final UserRepository userRepository;

   private final PropertyRepository propertyRepository;

   @Autowired
   @Lazy
   private CouponReservationService self;

   public CouponReservationServiceImpl(
         CouponReservationRepository couponReservationRepository,
         CouponRepository couponRepository,
         UserRepository userRepository,
         PropertyRepository propertyRepository) {
      this.couponReservationRepository = couponReservationRepository;
      this.couponRepository = couponRepository;
      this.userRepository = userRepository;
      this.propertyRepository = propertyRepository;
   }

   @Override
   public CouponReservation reserveCoupon(UUID couponId, String bookingSessionId, UUID customerId, UUID propertyId) {
      var existingOpt = couponReservationRepository.findByBookingSessionId(bookingSessionId);
      if (existingOpt.isPresent()) {
         CouponReservation existing = existingOpt.get();
         if (existing.getStatus() == ReservationStatus.ACTIVE) {
            if (existing.getCoupon().getId().equals(couponId)) {
               return existing;
            } else {
               self.releaseReservation(existing.getReservationToken());
            }
         }
      }

      int rowsAffected = couponRepository.incrementReservedCountAtomically(couponId);
      if (rowsAffected == 0) {
         throw new IllegalStateException("Coupon is exhausted or inactive");
      }

      Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
      User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new IllegalArgumentException("Property not found"));

      CouponReservation reservation = CouponReservation.builder()
            .coupon(coupon)
            .bookingSessionId(bookingSessionId)
            .reservationToken(UuidCreator.getTimeOrderedEpoch().toString())
            .customer(customer)
            .property(property)
            .status(ReservationStatus.ACTIVE)
            .reservedAt(Instant.now())
            .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
            .build();

      try {
         return couponReservationRepository.save(reservation);
      } catch (DataIntegrityViolationException ex) {
         couponRepository.decrementReservedCountAtomically(couponId);
         throw new IllegalStateException("Duplicate reservation token or session ID", ex);
      }
   }

   @Override
   @Transactional
   public void consumeReservation(String reservationToken) {
      CouponReservation reservation = couponReservationRepository.findByReservationToken(reservationToken)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

      if (reservation.getStatus() != ReservationStatus.ACTIVE) {
         throw new IllegalStateException(
               "Reservation is not in ACTIVE state. Current status: " + reservation.getStatus());
      }

      int rowsAffected = couponReservationRepository.transitionStatus(reservation.getId(), ReservationStatus.ACTIVE,
            ReservationStatus.CONSUMED);
      if (rowsAffected == 0) {
         throw new IllegalStateException("Reservation was modified by another transaction");
      }

      int couponRows = couponRepository.consumeReservedCouponAtomically(reservation.getCoupon().getId());
      if (couponRows == 0) {
         throw new IllegalStateException("Failed to consume coupon atomically");
      }
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void releaseReservation(String reservationToken) {
      CouponReservation reservation = couponReservationRepository.findByReservationToken(reservationToken)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

      if (reservation.getStatus() != ReservationStatus.ACTIVE) {
         return;
      }

      int rowsAffected = couponReservationRepository.transitionStatus(reservation.getId(), ReservationStatus.ACTIVE,
            ReservationStatus.EXPIRED);
      if (rowsAffected == 0) {
         return;
      }

      couponRepository.decrementReservedCountAtomically(reservation.getCoupon().getId());
   }

   @Scheduled(cron = "0 */1 * * * *")
   @SchedulerLock(name = "cleanExpiredReservations", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
   @Override
   public void cleanExpiredReservations() {
      log.info("Running expired coupon reservations cleanup job...");
      List<CouponReservation> expiredList = couponReservationRepository.findByStatusAndExpiresAtBefore(
            ReservationStatus.ACTIVE, Instant.now());

      int count = 0;
      for (CouponReservation reservation : expiredList) {
         try {
            self.releaseReservation(reservation.getReservationToken());
            count++;
         } catch (Exception e) {
            log.error("Failed to release expired reservation: " + reservation.getId(), e);
         }
      }
      if (count > 0) {
         log.info("Cleaned up {} expired coupon reservations.", count);
      }
   }

}
