package com.omnibooking.dto;

import com.omnibooking.model.enums.BookingStatus;
import java.math.BigDecimal;
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
public class BookingResponse {
   private UUID id;
   private String bookingCode;
   private String guestName;
   private String guestEmail;
   private String propertyName;
   private String roomTypeName;
   private LocalDate checkInDate;
   private LocalDate checkOutDate;
   private Integer numRooms;
   private BigDecimal totalPrice;
   private BigDecimal finalPrice;
   private BookingStatus status;
   private String activationToken;
   private String currency;
   private BigDecimal depositAmount;
   private Boolean requiresDeposit;
   private String paymentMethod;
}
