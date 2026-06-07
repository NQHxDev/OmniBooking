package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDetailResponse {

   private UUID id;

   private String name;

   private String description;

   private String propertyType;

   private String address;

   private String city;

   private String country;

   private Integer starRating;

   private String checkInTime;

   private String checkOutTime;

   private String imageUrl;

   private List<String> imageUrls;

   private String businessRegistrationNumber;

   private String taxCode;

   private String legalOwnerName;

   private List<String> amenities;

   private java.math.BigDecimal averageRating;

   private Integer reviewCount;

   private List<RoomTypeResponse> roomTypes;

}
