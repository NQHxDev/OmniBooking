package com.omnibooking.services.booking;

import com.omnibooking.dto.CreateBookingRequest;

import java.util.List;
import java.util.UUID;

import com.omnibooking.dto.BookingResponse;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.model.User;

public interface BookingService {

   BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal);

   void confirmBooking(UUID bookingId, String paymentMethod, String providerTransactionId, String metadata);

   BookingResponse getBookingById(UUID bookingId);

   List<BookingResponse> getMyBookings(UUID userId);

   void cancelBooking(UUID bookingId, String reason, User changedBy);

}
