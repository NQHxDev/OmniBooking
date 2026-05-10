package com.omnibooking.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadEvent implements Serializable {
   private String correlationId;
   private byte[] fileBytes;
   private String folder;
   private String fileName;
   
   // Target entity info
   private String entityId;
   private String entityType;
   private boolean isMain;
}
