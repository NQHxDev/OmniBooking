package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerLegalProfileResponse {
   private UUID id;
   private String businessRegistrationNumber;
   private String taxCode;
   private String legalOwnerName;
}
