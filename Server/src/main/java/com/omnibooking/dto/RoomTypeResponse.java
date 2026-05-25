package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeResponse {

   private UUID id;

   private String name;

   private String description;

   private BigDecimal basePrice;

   private Integer capacityAdults;

   private Integer capacityChildren;

   private Integer totalRooms;

   private BigDecimal roomSizeSqm;

   private String bedType;

}
