package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerStatsResponse {

   private String monthlyRevenue;
   private String monthlyRevenueChange;
   private boolean monthlyRevenueUp;

   private String totalBookings;
   private String totalBookingsChange;
   private boolean totalBookingsUp;

   private String newCustomers;
   private String newCustomersChange;
   private boolean newCustomersUp;

   private Double ratingScore;
   private String ratingScoreChange;
   private boolean ratingScoreUp;

}
