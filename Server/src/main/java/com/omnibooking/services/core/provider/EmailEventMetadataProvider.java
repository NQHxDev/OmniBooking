package com.omnibooking.services.core.provider;

import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.services.core.EventMetadataProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EmailEventMetadataProvider implements EventMetadataProvider {

   @Override
   public boolean supports(Object payload) {
      return payload instanceof EmailEvent;
   }

   @Override
   public void setEventId(Object payload, UUID eventId) {
      if (payload instanceof EmailEvent) {
         ((EmailEvent) payload).setEventId(eventId);
      }
   }

   @Override
   public Map<String, Object> extractMetadata(Object payload) {
      Map<String, Object> metadata = new HashMap<>();
      if (payload instanceof EmailEvent) {
         EmailEvent event = (EmailEvent) payload;
         metadata.put("eventId", event.getEventId() != null ? event.getEventId().toString() : "");
         metadata.put("to", event.getTo() != null ? event.getTo() : "");
         metadata.put("subject", event.getSubject() != null ? event.getSubject() : "");
      }
      return metadata;
   }

}
