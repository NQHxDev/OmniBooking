package com.omnibooking.services.booking.impl;

import com.omnibooking.config.BookingConfigProperties;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Booking;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.services.booking.BookingStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NoShowGracePeriodTest {

   @Mock
   private BookingConfigProperties config;

   @Mock
   private BookingStatusLogRepository statusLogRepository;

   @InjectMocks
   private BookingStateMachine bookingStateMachine;

   private Booking booking;

   private User user;

   @BeforeEach
   public void setUp() {
      user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
      booking = Booking.builder()
            .id(UUID.randomUUID())
            .status(BookingStatus.CONFIRMED)
            .checkInDate(LocalDate.now())
            .checkOutDate(LocalDate.now().plusDays(2))
            .user(user)
            .build();

      lenient().when(statusLogRepository.save(any())).thenReturn(null);
   }

   @Test
   public void testTransitionToNoShowBeforeGracePeriodEnds_ShouldThrowException() {
      // 24 hours grace period. check-in is start of day today (00:00).
      // If now is check-in + 23 hours, should throw exception.
      when(config.getNoShowGracePeriodHours()).thenReturn(24);

      // @formatter:off
      // Transition fails because now is within the 24-hour grace period from check-in
      // @formatter:on
      AppException exception = assertThrows(AppException.class, () -> {
         bookingStateMachine.transition(booking, BookingStatus.NO_SHOW, "No show test", user);
      });

      assertEquals("BOOKING_012", exception.getErrorCode());
   }

   @Test
   public void testTransitionToNoShowAfterGracePeriodEnds_ShouldSucceed() {
      // 2 hours grace period. check-in was yesterday.
      // If check-in was yesterday, then now (today) is definitely > 2 hours past
      // check-in.
      booking.setCheckInDate(LocalDate.now().minusDays(1));
      when(config.getNoShowGracePeriodHours()).thenReturn(2);

      bookingStateMachine.transition(booking, BookingStatus.NO_SHOW, "No show test", user);

      assertEquals(BookingStatus.NO_SHOW, booking.getStatus());
   }

}
