package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRequest {
   private String name;
   private String description;
   private String propertyType; // HOTEL, APARTMENT, VILLA, RESORT
   private String address;
   private String city;
   private String country;
   private Integer starRating;
   private LocalTime checkInTime;
   private LocalTime checkOutTime;
   private UUID cancellationPolicyId;
}
