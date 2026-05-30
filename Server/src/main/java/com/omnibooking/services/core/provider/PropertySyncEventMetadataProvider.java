package com.omnibooking.services.core.provider;

import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.services.core.EventMetadataProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PropertySyncEventMetadataProvider implements EventMetadataProvider {

   @Override
   public boolean supports(Object payload) {
      return payload instanceof PropertySyncEvent;
   }

   @Override
   public void setEventId(Object payload, UUID eventId) {
      if (payload instanceof PropertySyncEvent) {
         ((PropertySyncEvent) payload).setEventId(eventId);
      }
   }

   @Override
   public Map<String, Object> extractMetadata(Object payload) {
      Map<String, Object> metadata = new HashMap<>();
      if (payload instanceof PropertySyncEvent) {
         PropertySyncEvent event = (PropertySyncEvent) payload;
         metadata.put("eventId", event.getEventId() != null ? event.getEventId().toString() : "");
         metadata.put("propertyId", event.getPropertyId() != null ? event.getPropertyId().toString() : "");
         metadata.put("operation", event.getOperation() != null ? event.getOperation() : "");
      }
      return metadata;
   }

}
