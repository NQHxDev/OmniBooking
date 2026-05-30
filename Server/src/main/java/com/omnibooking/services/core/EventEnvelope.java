package com.omnibooking.services.core;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class EventEnvelope {

   private final UUID eventId;
   private final String eventType;
   private final Object payload;
   private final Map<String, Object> metadata;

   public EventEnvelope(UUID eventId, String eventType, Object payload, EventMetadataProvider provider) {
      this.eventId = eventId;
      this.eventType = eventType;
      this.payload = payload;
      if (provider != null) {
         provider.setEventId(payload, eventId);
         this.metadata = provider.extractMetadata(payload);
      } else {
         this.metadata = Map.of("eventId", eventId.toString());
      }
   }

}
