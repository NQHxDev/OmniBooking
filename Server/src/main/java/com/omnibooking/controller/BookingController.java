package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.security.Anonymous;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.booking.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

   private final BookingService bookingService;

   @Anonymous
   @PostMapping
   public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
         @Valid @RequestBody CreateBookingRequest request,
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      log.info("Received booking request. requestId={}, principal={}", requestId, principal != null ? principal.getEmail() : "anonymous");

      BookingResponse response = bookingService.createBooking(request, principal);
      return ResponseEntity.ok(ApiResponse.success(response, "Booking created successfully", requestId));
   }
}
