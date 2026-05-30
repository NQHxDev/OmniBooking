package com.omnibooking.services.core;

import java.util.UUID;

public interface IdempotencyService {

   boolean claimEvent(UUID eventId, String consumerGroup);

   void completeEvent(UUID eventId, String consumerGroup);

   void releaseClaim(UUID eventId, String consumerGroup);

   void renewLease(UUID eventId, String consumerGroup, java.time.Duration extension);

}
