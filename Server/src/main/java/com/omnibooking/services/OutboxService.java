package com.omnibooking.services;

import java.util.UUID;

public interface OutboxService {

   void saveEvent(UUID aggregateId, String aggregateType, String eventType, Object payload);

   void processOutbox();

   void processSingleEvent(com.omnibooking.model.OutboxEvent event);

}
