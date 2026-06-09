package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for properties with incomplete image uploads.
 * Used in the recovery flow to prompt partners to re-upload missing images.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncompleteUploadResponse {

   private UUID propertyId;

   private String propertyName;

   private int expectedCount;

   private int actualCount;

}
