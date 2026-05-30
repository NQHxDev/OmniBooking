package com.omnibooking.dto.event;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailEvent implements Serializable {

   private UUID eventId;

   private String to;

   private String subject;

   private String content; // HTML content

   private Map<String, Object> templateModel; // Optional for dynamic templates

}
