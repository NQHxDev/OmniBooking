package com.omnibooking.dto;

import com.omnibooking.model.enums.BedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRequest {

   private String name;

   private String description;

   private BigDecimal basePrice;

   private Integer capacityAdults;

   private Integer capacityChildren;

   private Integer totalRooms;

   private BigDecimal roomSizeSqm;

   private BedType bedType;

}
