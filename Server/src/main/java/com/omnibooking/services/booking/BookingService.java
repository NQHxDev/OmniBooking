package com.omnibooking.services.booking;

import com.omnibooking.dto.CreateBookingRequest;

import java.util.UUID;

import com.omnibooking.dto.BookingResponse;
import com.omnibooking.security.UserPrincipal;

public interface BookingService {

   BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal);

   void confirmBooking(UUID bookingId, String paymentMethod, String providerTransactionId, String metadata);

   BookingResponse getBookingById(UUID bookingId);

}
