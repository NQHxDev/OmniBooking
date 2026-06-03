package com.omnibooking.dto;

import com.omnibooking.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerBookingResponse {
   private UUID id;
   private String propertyName;
   private String roomTypeName;
   private LocalDate checkInDate;
   private LocalDate checkOutDate;
   private Integer numRooms;
   private BigDecimal totalPrice;
   private BigDecimal finalPrice;
   private BookingStatus status;
   private String guestName;
   private String guestEmail;
   private String guestPhone;
   private String specialRequests;
   private Instant createdAt;
}
