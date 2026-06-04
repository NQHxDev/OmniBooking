package com.omnibooking.services.core;

import com.omnibooking.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface OutboxService {

   void saveEvent(UUID aggregateId, String aggregateType, String eventType, Object payload);

   void processOutbox();

   void processOutboxAsync();

   void processSingleEvent(OutboxEvent event);

   void markAsProcessed(UUID eventId);

   List<OutboxEvent> lockAndFetchEventsToProcess(Pageable pageable);

   void purgeOldOutboxEvents();

   void rescheduleRetry(UUID eventId);

}
