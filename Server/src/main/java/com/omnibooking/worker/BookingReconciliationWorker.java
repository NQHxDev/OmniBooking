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
import io.sentry.SentryLevel;
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
import com.omnibooking.services.booking.BookingService;
import com.omnibooking.services.payment.PaymentProviderFactory;
import com.omnibooking.services.payment.PaymentProvider;
import com.omnibooking.services.payment.PaymentStateMachine;
import com.omnibooking.repository.payment.PaymentEventRepository;
import com.omnibooking.model.PaymentEvent;
import com.omnibooking.model.Transaction;
import com.omnibooking.model.enums.TransactionStatus;
import com.omnibooking.model.enums.PaymentGatewayStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.UUID;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.model.RoomAvailability;

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

   private final BookingService bookingService;

   private final PaymentProviderFactory paymentProviderFactory;

   private final PaymentStateMachine paymentStateMachine;

   private final PaymentEventRepository paymentEventRepository;

   private final RoomAvailabilityRepository roomAvailabilityRepository;

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

         // 1b. Room Availability Overfill Reconciliation (Alert-Only)
         reconcileRoomAvailability();

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
      // 1. Auto-confirm legacy orphans (Transaction SUCCESS but Booking
      // PENDING_PAYMENT)
      List<Object[]> orphans = transactionRepository
            .findSuccessTransactionsWithNonConfirmedBookings();

      if (!orphans.isEmpty()) {
         log.error("[RECONCILIATION] Found {} paid but unconfirmed bookings:",
               orphans.size());
         for (Object[] row : orphans) {
            UUID bookingId = (UUID) row[0];
            UUID transactionId = (UUID) row[1];
            String providerTxId = (String) row[2];
            log.error("  Booking: {}, Transaction: {}, ProviderTxId: {}",
                  bookingId, transactionId, providerTxId);

            try {
               log.info("[RECONCILIATION] Auto-confirming orphan booking: {}", bookingId);
               bookingService.confirmBooking(bookingId, "MOMO", providerTxId, "{\"reconciled\":true}");
            } catch (Exception e) {
               log.error("[RECONCILIATION] Failed to auto-confirm orphan booking: {}", bookingId, e);
            }
         }
         reconciliationPaymentMismatchCounter.increment(orphans.size());
         reconciliationAnomalyCounter.increment(orphans.size());
      }

      // 2. Query Gateway for Stale PENDING Transactions
      // stuckThreshold = created at least 10 minutes ago
      Instant stuckThreshold = Instant.now().minus(10, ChronoUnit.MINUTES);
      // maxAgeThreshold = created within 24 hours ago
      Instant maxAgeThreshold = Instant.now().minus(24, ChronoUnit.HOURS);

      PageRequest pageRequest = PageRequest.of(0, 50, Sort.by("updatedAt").ascending());

      List<Transaction> pendingTxs = transactionRepository.findPendingTransactionsForReconciliation(
            TransactionStatus.PENDING, stuckThreshold, maxAgeThreshold, pageRequest);

      if (!pendingTxs.isEmpty()) {
         log.info("[RECONCILIATION] Found {} stale pending transactions to reconcile.", pendingTxs.size());
         for (Transaction tx : pendingTxs) {
            try {
               PaymentProvider provider = paymentProviderFactory.getProvider(tx.getPaymentMethod());
               log.info("[RECONCILIATION] Querying status for transaction: {}, orderId: {}", tx.getId(),
                     tx.getProviderOrderId());
               PaymentGatewayStatus gatewayStatus = provider.queryPaymentStatus(tx);

               if (gatewayStatus == PaymentGatewayStatus.SUCCESS) {
                  log.info("[RECONCILIATION] Transaction {} confirmed SUCCESS by gateway.", tx.getId());

                  // Record RECONCILIATION_FIXED event
                  PaymentEvent fixEvent = PaymentEvent.builder()
                        .transactionId(tx.getId())
                        .bookingId(tx.getBooking().getId())
                        .eventType("RECONCILIATION_FIXED")
                        .metadata(String.format("{\"orderId\":\"%s\",\"status\":\"SUCCESS\"}", tx.getProviderOrderId()))
                        .build();
                  paymentEventRepository.save(fixEvent);
               } else if (gatewayStatus == PaymentGatewayStatus.FAILED) {
                  log.info("[RECONCILIATION] Transaction {} confirmed FAILED by gateway. Transitioning state.",
                        tx.getId());
                  paymentStateMachine.transition(tx, TransactionStatus.FAILED);
                  transactionRepository.save(tx);

                  PaymentEvent failEvent = PaymentEvent.builder()
                        .transactionId(tx.getId())
                        .bookingId(tx.getBooking().getId())
                        .eventType("PAYMENT_FAILED")
                        .metadata(String.format(
                              "{\"orderId\":\"%s\",\"status\":\"FAILED\",\"reason\":\"Gateway reported failed\"}",
                              tx.getProviderOrderId()))
                        .build();
                  paymentEventRepository.save(failEvent);
               } else if (gatewayStatus == PaymentGatewayStatus.PENDING) {
                  // Save transaction to update its updatedAt and send it to the back of the queue
                  tx.setUpdatedAt(Instant.now());
                  transactionRepository.save(tx);
                  log.info("[RECONCILIATION] Transaction {} is still PENDING at gateway. Updated timestamp to defer.",
                        tx.getId());
               }
            } catch (Exception e) {
               log.error("[RECONCILIATION] Failed to reconcile transaction: {}", tx.getId(), e);
            }
         }
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

   private void reconcileRoomAvailability() {
      List<RoomAvailability> overfilled = roomAvailabilityRepository.findOverfilledAvailabilities();
      if (!overfilled.isEmpty()) {
         log.error("[RECONCILIATION] Found {} room availability records exceeding capacity limit:", overfilled.size());
         for (RoomAvailability ra : overfilled) {
            String msg = String.format(
                  "[INVENTORY_ANOMALY] Room availability exceeds capacity for RoomType ID: %s, Date: %s. Available: %d, Total rooms: %d",
                  ra.getRoomType().getId(), ra.getAvailabilityDate(), ra.getAvailableCount(),
                  ra.getRoomType().getTotalRooms());
            log.error(msg);
            Sentry.captureMessage(msg, SentryLevel.ERROR);
            reconciliationAnomalyCounter.increment();
         }
      }
   }

}
