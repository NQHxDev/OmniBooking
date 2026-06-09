package com.omnibooking.services.media;

import com.omnibooking.dto.MediaProgress;

import java.util.Optional;
import java.util.UUID;

/**
 * Central service managing media upload progress lifecycle in Redis.
 * All progress calculations (percentage, status transitions) are performed
 * atomically via Redis Lua scripts — the backend is the single source of truth.
 */
public interface MediaProgressService {

   /**
    * Initialize progress tracking for a new property upload batch.
    * Creates the Redis hash with initial counters and adds to ZSET index.
    */
   void initProgress(UUID propertyId, UUID ownerId, int totalImages);

   /**
    * Idempotent: mark an image as queued to Kafka.
    *
    * @return true if state changed, false if duplicate (correlationId already seen)
    */
   boolean markQueued(UUID propertyId, String correlationId);

   /**
    * Idempotent: mark an image as successfully processed by Cloudinary.
    *
    * @return true if state changed, false if duplicate
    */
   boolean markProcessed(UUID propertyId, String correlationId);

   /**
    * Idempotent: mark an image as failed during processing.
    *
    * @return true if state changed, false if duplicate
    */
   boolean markFailed(UUID propertyId, String correlationId);

   /**
    * Get current progress snapshot from Redis.
    */
   Optional<MediaProgress> getProgress(UUID propertyId);

   /**
    * Verify that the given user owns the property's progress data.
    *
    * @return true if ownerId matches, false otherwise
    */
   boolean verifyOwnership(UUID propertyId, UUID userId);

   /**
    * Cleanup progress data: delete hash, sets, and remove from ZSET index.
    */
   void cleanup(UUID propertyId);

}
