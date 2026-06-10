package com.omnibooking.services.booking;

import com.omnibooking.config.BookingConfigProperties;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Booking;
import com.omnibooking.model.BookingStatusLog;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BookingStateMachine {

   private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
         BookingStatus.PENDING_PAYMENT, Set.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.EXPIRED),
         BookingStatus.CONFIRMED,
         Set.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW, BookingStatus.REFUNDED),
         BookingStatus.CHECKED_IN, Set.of(BookingStatus.CHECKED_OUT),
         BookingStatus.CANCELLED, Set.of(BookingStatus.REFUNDED));

   private final BookingConfigProperties config;
   private final BookingStatusLogRepository statusLogRepository;

   /**
    * Validates and executes a booking status transition.
    * This is the ONLY method allowed to change booking status.
    *
    * @throws IllegalStateException if transition is not allowed
    * @throws AppException          if business rule is violated
    */
   public void transition(Booking booking, BookingStatus target, String reason, User changedBy) {
      BookingStatus current = booking.getStatus();

      // 1. Validate transition legality
      Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
      if (!allowed.contains(target)) {
         throw new IllegalStateException(
               "Illegal booking transition: " + current + " → " + target);
      }

      // 2. Validate business rules
      validateBusinessRules(booking, target);

      // 3. Execute (state machine owns setStatus)
      BookingStatus oldStatus = booking.getStatus();
      booking.setStatus(target);

      // 4. Side-effects tied to specific transitions
      if (target == BookingStatus.CONFIRMED) {
         booking.setExpiresAt(null);
      }

      // 5. Audit log
      statusLogRepository.save(BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(oldStatus)
            .newStatus(target)
            .reason(reason)
            .changedBy(changedBy)
            .build());
   }

   private void validateBusinessRules(Booking booking, BookingStatus target) {
      LocalDate today = LocalDate.now();
      Instant now = Instant.now();
      switch (target) {
         case CHECKED_IN -> {
            if (today.isBefore(booking.getCheckInDate()))
               throw new AppException("BOOKING_010",
                     "Cannot check-in before check-in date", HttpStatus.BAD_REQUEST);
         }
         case CHECKED_OUT -> {
            if (today.isBefore(booking.getCheckOutDate()))
               throw new AppException("BOOKING_011",
                     "Cannot check-out before check-out date", HttpStatus.BAD_REQUEST);
         }
         case NO_SHOW -> {
            // Use Instant-based calculation to support arbitrary grace periods
            // (e.g. 2h, 6h, 12h, 36h) without integer division precision loss.
            // check_in_date is LocalDate → convert to start-of-day Instant in system
            // timezone.
            Instant checkInInstant = booking.getCheckInDate()
                  .atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant noShowDeadline = checkInInstant
                  .plus(config.getNoShowGracePeriodHours(), ChronoUnit.HOURS);
            if (!now.isAfter(noShowDeadline))
               throw new AppException("BOOKING_012",
                     "Cannot mark no-show before grace period ends", HttpStatus.BAD_REQUEST);
         }
         default -> {
         }
      }
   }

}
