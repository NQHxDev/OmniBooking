package com.omnibooking.services.booking;

import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.security.UserPrincipal;

public interface BookingService {

   BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal);

   void confirmBooking(java.util.UUID bookingId, String paymentMethod, String providerTransactionId, String metadata);

   BookingResponse getBookingById(java.util.UUID bookingId);

}

