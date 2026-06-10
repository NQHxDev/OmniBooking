package com.omnibooking.worker;

import com.omnibooking.config.BookingConfigProperties;
import com.omnibooking.model.Booking;
import com.omnibooking.model.BookingStatusLog;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.services.booking.InventoryService;
import com.omnibooking.services.pricing.CouponReservationService;
import com.omnibooking.services.pricing.CouponReleaseRetryService;
import io.micrometer.core.instrument.Counter;
import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationWorker {

   private final BookingRepository bookingRepository;
   private final BookingStatusLogRepository statusLogRepository;
   private final InventoryService inventoryService;
   private final CouponReservationService couponReservationService;
   private final CouponReleaseRetryService couponReleaseRetryService;
   private final BookingConfigProperties config;
   private final Counter bookingExpiredCounter;
   private final Counter bookingExpirationFailureCounter;

   @Autowired
   @Lazy
   private BookingExpirationWorker self;

   @Scheduled(fixedDelayString = "${omnibooking.booking.expiration-check-interval-ms:60000}")
   @SchedulerLock(name = "bookingExpiration", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
   public void expireBookings() {
      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("booking-expiration-worker", CheckInStatus.IN_PROGRESS));

      try {
         int totalProcessed = 0;
         int batchSize = config.getExpirationBatchSize();
         Pageable page = PageRequest.of(0, batchSize);
         Instant now = Instant.now();

         // Loop until no more expired bookings remain
         while (true) {
            List<Booking> batch = bookingRepository.findExpiredBookings(
                  List.of(BookingStatus.PENDING_PAYMENT),
                  now, page);

            if (batch.isEmpty()) {
               break;
            }

            for (Booking booking : batch) {
               try {
                  self.processExpiration(booking.getId(), now);
                  totalProcessed++;
               } catch (Exception e) {
                  log.error("Failed to expire booking {}", booking.getId(), e);
                  bookingExpirationFailureCounter.increment();
               }
            }

            // If batch returned less than batchSize, no more pages
            if (batch.size() < batchSize) {
               break;
            }
         }

         if (totalProcessed > 0) {
            log.info("Expired {} bookings in this cycle", totalProcessed);
         }

         Sentry.captureCheckIn(
               new CheckIn(checkInId, "booking-expiration-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Error in BookingExpirationWorker", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "booking-expiration-worker", CheckInStatus.ERROR));
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void processExpiration(UUID bookingId, Instant now) {
      log.info("Acquiring pessimistic write lock for booking expiration {}", bookingId);
      Booking booking = bookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
         log.info("Booking {} already confirmed/expired, skipping expiration", bookingId);
         return;
      }

      if (booking.getExpiresAt() == null || booking.getExpiresAt().isAfter(now)) {
         log.info("Booking {} has not expired yet, skipping", bookingId);
         return;
      }

      // Transition booking status to EXPIRED atomically
      int rowsUpdated = bookingRepository.atomicExpireBooking(
            bookingId, now, BookingStatus.PENDING_PAYMENT, BookingStatus.EXPIRED);

      if (rowsUpdated == 0) {
         log.info("Booking {} already confirmed/expired or not expired, skipping expiration", bookingId);
         return;
      }

      // Refresh booking state for inventory/coupon release and logging
      booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      // Release inventory
      inventoryService.releaseInventory(booking);

      // Release coupon
      releaseCouponIfPresent(booking);

      // Audit log
      statusLogRepository.save(BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(BookingStatus.PENDING_PAYMENT)
            .newStatus(BookingStatus.EXPIRED)
            .reason("Booking expired: payment not received within time limit")
            .build());

      // Metrics
      bookingExpiredCounter.increment();
   }

   private void releaseCouponIfPresent(Booking booking) {
      if (booking.getCoupon() != null && booking.getUser() != null) {
         UUID couponId = booking.getCoupon().getId();
         UUID userId = booking.getUser().getId();
         try {
            couponReservationService.refundReservation(couponId, userId);
         } catch (Exception e) {
            log.warn("Immediate coupon release failed for booking {}, persisting retry record.", booking.getId(), e);
            couponReleaseRetryService.createRetry(booking.getId(), couponId, userId);
         }
      }
   }

}
