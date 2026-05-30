package com.omnibooking.services.core;

import java.util.Map;
import java.util.UUID;

public interface EventMetadataProvider {

   boolean supports(Object payload);

   void setEventId(Object payload, UUID eventId);

   Map<String, Object> extractMetadata(Object payload);

}
