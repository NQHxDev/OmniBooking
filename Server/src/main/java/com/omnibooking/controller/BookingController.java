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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

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
      BookingResponse response = bookingService.createBooking(request, principal);

      return ResponseEntity.ok(ApiResponse.success(response, "Booking created successfully", requestId));
   }

   @Anonymous
   @GetMapping("/{id}")
   public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
         @PathVariable UUID id,
         HttpServletRequest httpRequest) {

      String requestId = (String) httpRequest.getAttribute("requestId");
      BookingResponse response = bookingService.getBookingById(id);

      return ResponseEntity.ok(ApiResponse.success(response, "Booking details retrieved successfully", requestId));
   }

   @GetMapping("/mine")
   @PreAuthorize("hasAuthority(T(com.omnibooking.constant.SecurityConstants.Roles).USER)")
   public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
         @AuthenticationPrincipal UserPrincipal principal,
         HttpServletRequest httpRequest) {
      String requestId = (String) httpRequest.getAttribute("requestId");
      List<BookingResponse> response = bookingService.getMyBookings(principal.getId());

      return ResponseEntity.ok(ApiResponse.success(response, "User bookings retrieved successfully", requestId));
   }

}
