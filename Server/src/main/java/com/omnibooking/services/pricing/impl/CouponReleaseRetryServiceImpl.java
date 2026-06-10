package com.omnibooking.services.pricing.impl;

import com.omnibooking.model.CouponReleaseRetry;
import com.omnibooking.repository.pricing.CouponReleaseRetryRepository;
import com.omnibooking.services.pricing.CouponReleaseRetryService;
import com.omnibooking.services.pricing.CouponReservationService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponReleaseRetryServiceImpl implements CouponReleaseRetryService {

   private final CouponReleaseRetryRepository couponReleaseRetryRepository;
   private final CouponReservationService couponReservationService;
   private final Counter couponReleaseRetryCounter;
   private final Counter couponReleaseRetrySuccessCounter;
   private final Counter couponReleaseRetryFailureCounter;

   private static final long[] RETRY_DELAYS_MINUTES = { 1, 5, 15, 60, 360 }; // 1m, 5m, 15m, 1h, 6h
   private static final int MAX_ATTEMPTS = 5;

   @Override
   @Transactional
   public void createRetry(UUID bookingId, UUID couponId, UUID userId) {
      log.info("Request to create coupon release retry for booking={}, coupon={}, user={}", bookingId, couponId,
            userId);

      // Concurrency check: search for an existing PENDING retry record
      Optional<CouponReleaseRetry> existingPending = couponReleaseRetryRepository
            .findByBookingIdAndCouponIdAndStatus(bookingId, couponId, "PENDING");

      if (existingPending.isPresent()) {
         log.info("Active PENDING retry record already exists for booking {} and coupon {}. Reusing it.", bookingId,
               couponId);
         return;
      }

      // Check if we have historical SUCCESS or FAILED records for this
      // booking/coupon.
      // If we do, we can reactivate them to PENDING state to support admin manual
      // trigger/reconciliation.
      Optional<CouponReleaseRetry> existingSuccess = couponReleaseRetryRepository
            .findByBookingIdAndCouponIdAndStatus(bookingId, couponId, "SUCCESS");
      Optional<CouponReleaseRetry> existingFailed = couponReleaseRetryRepository
            .findByBookingIdAndCouponIdAndStatus(bookingId, couponId, "FAILED");

      Optional<CouponReleaseRetry> existingToReactivate = existingSuccess.isPresent() ? existingSuccess
            : existingFailed;

      if (existingToReactivate.isPresent()) {
         CouponReleaseRetry existing = existingToReactivate.get();
         log.info("Reactivating historical {} retry record {} for booking {} to PENDING status.", existing.getStatus(),
               existing.getId(), bookingId);
         existing.setStatus("PENDING");
         existing.setAttemptCount(0);
         existing.setLastAttemptAt(null);
         existing.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.MINUTES));
         couponReleaseRetryRepository.save(existing);
         return;
      }

      // Insert new PENDING retry record
      CouponReleaseRetry retry = CouponReleaseRetry.builder()
            .bookingId(bookingId)
            .couponId(couponId)
            .userId(userId)
            .status("PENDING")
            .attemptCount(0)
            .nextAttemptAt(Instant.now().plus(1, ChronoUnit.MINUTES))
            .build();

      try {
         couponReleaseRetryRepository.saveAndFlush(retry);
         log.info("Successfully persisted new coupon release retry record for booking {}", bookingId);
      } catch (DataIntegrityViolationException e) {
         log.warn("Concurrent duplicate coupon retry record insertion detected for booking {}. Safe to ignore.",
               bookingId, e);
      }
   }

   @Override
   @Transactional
   public void processPendingRetries() {
      Instant now = Instant.now();
      List<CouponReleaseRetry> pendingList = couponReleaseRetryRepository.findPendingRetries("PENDING", now);
      if (pendingList.isEmpty()) {
         return;
      }

      log.info("Found {} pending coupon release retries to process.", pendingList.size());
      for (CouponReleaseRetry retry : pendingList) {
         couponReleaseRetryCounter.increment();
         int nextAttempt = retry.getAttemptCount() + 1;
         retry.setAttemptCount(nextAttempt);
         retry.setLastAttemptAt(now);

         try {
            log.info("Processing coupon release retry for booking {} (attempt {}/{})", retry.getBookingId(),
                  nextAttempt, MAX_ATTEMPTS);
            couponReservationService.refundReservation(retry.getCouponId(), retry.getUserId());

            // Success!
            retry.setStatus("SUCCESS");
            couponReleaseRetrySuccessCounter.increment();
            couponReleaseRetryRepository.save(retry);
            log.info("Successfully resolved coupon release retry for booking {}", retry.getBookingId());
         } catch (Exception e) {
            log.warn("Coupon release retry attempt {} failed for booking {} - Error: {}", nextAttempt,
                  retry.getBookingId(), e.getMessage());

            if (nextAttempt >= MAX_ATTEMPTS) {
               // Hard failure
               retry.setStatus("FAILED");
               couponReleaseRetryFailureCounter.increment();
               log.error(
                     "Coupon release retry failed for booking {} after reaching maximum attempts ({}). Marked as FAILED.",
                     retry.getBookingId(), MAX_ATTEMPTS, e);
            } else {
               // Schedule next attempt using exponential schedule
               long delayMinutes = RETRY_DELAYS_MINUTES[nextAttempt - 1];
               retry.setNextAttemptAt(now.plus(delayMinutes, ChronoUnit.MINUTES));
            }
            couponReleaseRetryRepository.save(retry);
         }
      }
   }

   @Override
   @Transactional
   public void purgeOldRetryRecords() {
      Instant successCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
      Instant failedCutoff = Instant.now().minus(180, ChronoUnit.DAYS);

      int purgedSuccess = couponReleaseRetryRepository.deleteByStatusAndLastAttemptAtBefore("SUCCESS", successCutoff);
      int purgedFailed = couponReleaseRetryRepository.deleteByStatusAndLastAttemptAtBefore("FAILED", failedCutoff);

      log.info("Purged {} SUCCESS and {} FAILED coupon release retry records from db.", purgedSuccess, purgedFailed);
   }

}
