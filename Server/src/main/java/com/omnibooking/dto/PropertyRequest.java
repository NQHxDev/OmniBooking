package com.omnibooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRequest {

   @NotBlank
   @Size(max = 255)
   private String name;

   private String description;

   @NotBlank
   private String propertyType; // HOTEL, APARTMENT, VILLA, RESORT

   @NotBlank
   private String address;

   @NotBlank
   private String city;

   @NotBlank
   private String country;

   @Min(1)
   @Max(5)
   private Integer starRating;

   private LocalTime checkInTime;

   private LocalTime checkOutTime;

   private UUID cancellationPolicyId;

   private String businessRegistrationNumber;

   private String taxCode;

   private String legalOwnerName;

   private List<String> amenities;

   @NotEmpty
   @Valid
   private List<RoomTypeRequest> roomTypes;

   @Min(0)
   @Max(100)
   private Integer expectedImageCount;

}
