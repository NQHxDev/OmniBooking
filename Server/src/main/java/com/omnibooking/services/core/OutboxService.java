package com.omnibooking.services.core;

import java.util.UUID;

public interface OutboxService {

   void saveEvent(UUID aggregateId, String aggregateType, String eventType, Object payload);

   void processOutbox();

   void processSingleEvent(com.omnibooking.model.OutboxEvent event);

   void markAsProcessed(UUID eventId);

}
