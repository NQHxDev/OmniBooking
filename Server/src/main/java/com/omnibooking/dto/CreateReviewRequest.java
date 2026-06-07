package com.omnibooking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

   @NotNull(message = "Booking ID is required")
   private UUID bookingId;

   @NotNull(message = "Rating is required")
   @Min(value = 1, message = "Rating must be at least 1")
   @Max(value = 5, message = "Rating must be at most 5")
   private Integer rating;

   private String comment;

}
