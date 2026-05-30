package com.omnibooking.services.core;

import java.util.UUID;

public interface IdempotencyService {

   boolean isProcessed(UUID eventId, String consumerGroup);

   void markProcessed(UUID eventId, String consumerGroup);

}
