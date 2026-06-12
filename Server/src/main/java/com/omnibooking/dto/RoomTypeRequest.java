package com.omnibooking.dto;

import com.omnibooking.model.enums.BedType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

   @NotBlank
   @Size(max = 100)
   private String name;

   private String description;

   @NotNull
   @DecimalMin(value = "0.0", inclusive = false)
   @DecimalMax(value = "99999999.99")
   private BigDecimal basePrice;

   @NotNull
   @Min(1)
   @Max(50)
   private Integer capacityAdults;

   @NotNull
   @Min(0)
   @Max(50)
   private Integer capacityChildren;

   @NotNull
   @Min(1)
   @Max(10000)
   private Integer totalRooms;

   @NotNull
   @DecimalMin(value = "0.0", inclusive = false)
   private BigDecimal roomSizeSqm;

   @NotNull
   private BedType bedType;

}
