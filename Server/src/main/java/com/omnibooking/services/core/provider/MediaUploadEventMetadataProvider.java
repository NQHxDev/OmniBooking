package com.omnibooking.services.core.provider;

import com.omnibooking.dto.event.MediaUploadEvent;
import com.omnibooking.services.core.EventMetadataProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MediaUploadEventMetadataProvider implements EventMetadataProvider {

   @Override
   public boolean supports(Object payload) {
      return payload instanceof MediaUploadEvent;
   }

   @Override
   public void setEventId(Object payload, UUID eventId) {
      if (payload instanceof MediaUploadEvent) {
         ((MediaUploadEvent) payload).setEventId(eventId);
      }
   }

   @Override
   public Map<String, Object> extractMetadata(Object payload) {
      Map<String, Object> metadata = new HashMap<>();
      if (payload instanceof MediaUploadEvent) {
         MediaUploadEvent event = (MediaUploadEvent) payload;
         metadata.put("eventId", event.getEventId() != null ? event.getEventId().toString() : "");
         metadata.put("correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "");
         metadata.put("entityId", event.getEntityId() != null ? event.getEntityId() : "");
      }
      return metadata;
   }

}
