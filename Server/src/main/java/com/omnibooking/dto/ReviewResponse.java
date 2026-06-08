package com.omnibooking.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

   private UUID id;

   private UUID bookingId;

   private UUID propertyId;

   private String propertyName;

   private UUID userId;

   private String userName;

   private String userAvatarUrl;

   private Integer rating;

   private String comment;

   private String reply;

   private String status;

   private Instant replyUpdatedAt;

   private Instant createdAt;

   private Instant updatedAt;

}
