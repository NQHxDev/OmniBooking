package com.omnibooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
   @NotNull(message = "Room Type ID cannot be null")
   private UUID roomTypeId;

   @NotNull(message = "Check-in date cannot be null")
   private LocalDate checkInDate;

   @NotNull(message = "Check-out date cannot be null")
   private LocalDate checkOutDate;

   @Min(value = 1, message = "Must book at least 1 room")
   @NotNull(message = "Number of rooms cannot be null")
   private Integer numRooms;

   @NotBlank(message = "Guest name cannot be blank")
   private String guestName;

   @NotBlank(message = "Guest email cannot be blank")
   @Email(message = "Invalid email format")
   private String guestEmail;

   private String guestPhone; // Optional, will be encrypted
   private String specialRequests;
   private UUID couponId;
   private String reservationToken;
   private String paymentMethod;
   private Integer guestCount;

   @Builder.Default
   private String currency = "USD";
}
