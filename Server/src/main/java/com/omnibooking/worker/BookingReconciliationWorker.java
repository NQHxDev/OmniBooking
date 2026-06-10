package com.omnibooking.worker;

import com.omnibooking.model.Booking;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.OperationType;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.omnibooking.model.CouponReservation;
import com.omnibooking.model.enums.ReservationStatus;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingReconciliationWorker {

   private final BookingRepository bookingRepository;

   private final InventoryOperationRepository inventoryOperationRepository;

   private final TransactionRepository transactionRepository;

   private final Counter reconciliationAnomalyCounter;

   private final Counter reconciliationInventoryLeakCounter;

   private final Counter reconciliationPaymentMismatchCounter;

   private final Counter reconciliationStuckBookingCounter;

   private final CouponReservationRepository couponReservationRepository;

   private final CouponRepository couponRepository;

   private final Counter reconciliationCouponLeakCounter;

   /**
    * Runs every 30 minutes. Detects and reports/repairs discrepancies.
    */
   @Scheduled(cron = "0 */30 * * * *")
   @SchedulerLock(name = "bookingReconciliation", lockAtMostFor = "PT25M", lockAtLeastFor = "PT5M")
   public void reconcile() {
      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("booking-reconciliation-worker", CheckInStatus.IN_PROGRESS));

      try {
         log.info("=== Booking Reconciliation Started ===");

         // 1. Inventory Reconciliation
         reconcileInventory();

         // 2. Booking Status Reconciliation
         reconcileStuckBookings();

         // 3. Payment Reconciliation
         reconcilePayments();

         // 4. Coupon Reconciliation
         reconcileCoupons();

         log.info("=== Booking Reconciliation Completed ===");
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "booking-reconciliation-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Error in BookingReconciliationWorker", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "booking-reconciliation-worker", CheckInStatus.ERROR));
      }
   }

   private void reconcileInventory() {
      // Query: bookings with terminal status (EXPIRED, CANCELLED)
      // that have RESERVE ledger entries but no RELEASE ledger entries
      // → These represent inventory leaks that need manual investigation
      List<Object[]> leaks = inventoryOperationRepository
            .findBookingsWithReserveButNoRelease(
                  OperationType.RESERVE,
                  OperationType.RELEASE,
                  List.of(BookingStatus.EXPIRED, BookingStatus.CANCELLED));

      if (!leaks.isEmpty()) {
         log.error("[RECONCILIATION] Found {} bookings with unreleased inventory:", leaks.size());
         for (Object[] row : leaks) {
            log.error("  Booking: {}, RoomType: {}, Date: {}",
                  row[0], row[1], row[2]);
         }
         reconciliationInventoryLeakCounter.increment(leaks.size());
         reconciliationAnomalyCounter.increment(leaks.size());
      }
   }

   private void reconcileStuckBookings() {
      // Bookings stuck in PENDING/PENDING_PAYMENT with expires_at > 1 hour past
      Instant stuckThreshold = Instant.now().minus(1, ChronoUnit.HOURS);
      List<Booking> stuck = bookingRepository.findStuckBookings(
            List.of(BookingStatus.PENDING_PAYMENT),
            stuckThreshold);

      if (!stuck.isEmpty()) {
         log.error("[RECONCILIATION] Found {} stuck PENDING bookings " +
               "(expired > 1 hour ago, worker may have failed):", stuck.size());
         stuck.forEach(b -> log.error("  Booking: {}, expiresAt: {}",
               b.getId(), b.getExpiresAt()));
         reconciliationStuckBookingCounter.increment(stuck.size());
         reconciliationAnomalyCounter.increment(stuck.size());
      }
   }

   private void reconcilePayments() {
      // Transactions with SUCCESS status but booking still PENDING_PAYMENT
      List<Object[]> orphans = transactionRepository
            .findSuccessTransactionsWithNonConfirmedBookings();

      if (!orphans.isEmpty()) {
         log.error("[RECONCILIATION] Found {} paid but unconfirmed bookings:",
               orphans.size());
         for (Object[] row : orphans) {
            log.error("  Booking: {}, Transaction: {}, ProviderTxId: {}",
                  row[0], row[1], row[2]);
         }
         reconciliationPaymentMismatchCounter.increment(orphans.size());
         reconciliationAnomalyCounter.increment(orphans.size());
      }
   }

   private void reconcileCoupons() {
      List<CouponReservation> leaked = couponReservationRepository.findLeakedCouponReservations();

      if (!leaked.isEmpty()) {
         log.error("[RECONCILIATION] Found {} leaked coupon reservations:", leaked.size());
         for (CouponReservation cr : leaked) {
            log.error("  Reservation: {}, Coupon: {}, Customer: {}",
                  cr.getId(), cr.getCoupon().getCode(), cr.getCustomer().getEmail());

            try {
               int rowsAffected = couponReservationRepository.transitionStatus(
                     cr.getId(), ReservationStatus.CONSUMED, ReservationStatus.EXPIRED);
               if (rowsAffected > 0) {
                  couponRepository.refundCouponUsageAtomically(cr.getCoupon().getId());
                  log.info("[RECONCILIATION] Successfully auto-repaired leaked coupon reservation: {}", cr.getId());
               }
            } catch (Exception e) {
               log.error("[RECONCILIATION] Failed to auto-repair leaked coupon reservation: {}", cr.getId(), e);
            }
         }
         reconciliationCouponLeakCounter.increment(leaked.size());
         reconciliationAnomalyCounter.increment(leaked.size());
      }
   }

}
