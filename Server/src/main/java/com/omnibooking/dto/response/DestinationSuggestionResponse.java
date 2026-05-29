package com.omnibooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationSuggestionResponse {

   private String id;

   private String name;

   private String type; // CITY, LANDMARK, HOTEL

   private String country;

   private String countryCode;

   private LocationDto location;

   private String displayName;

   private String imageUrl;

   private Long propertyCount;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public static class LocationDto {
      private double lat;
      private double lon;
   }

}
