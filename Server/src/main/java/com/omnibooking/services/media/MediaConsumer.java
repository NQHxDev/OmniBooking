package com.omnibooking.services.media;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.CloudinaryResponse;
import com.omnibooking.dto.event.MediaUploadEvent;
import com.omnibooking.model.Media;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.services.property.PropertyService;
import com.omnibooking.services.property.PropertyImagesCacheService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.core.IdempotencyService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import com.omnibooking.services.core.LeaseRenewer;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaConsumer {

   private final CloudinaryService cloudinaryService;

   private final MediaRepository mediaRepository;

   private final PropertyRepository propertyRepository;

   private final PropertyService propertyService;

   private final PropertyImagesCacheService propertyImagesCacheService;

   private final OutboxService outboxService;

   private final IdempotencyService idempotencyService;

   private final MeterRegistry meterRegistry;

   @Transactional
   @KafkaListener(topics = KafkaConfig.MEDIA_TOPIC, groupId = "omnibooking-media-group")
   public void consumeUploadEvent(MediaUploadEvent event) {
      String consumerGroup = "omnibooking-media-group";
      if (event.getEventId() != null) {
         boolean claimed = idempotencyService.claimEvent(event.getEventId(), consumerGroup);
         if (!claimed) {
            log.warn("[Kafka Consumer] Duplicate media upload event detected and skipped: eventId={}, entityId={}",
                  event.getEventId(), event.getEntityId());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            meterRegistry.counter("omnibooking.kafka.consumer.skipped").increment();
            return;
         }
      }

      log.info("[Kafka Consumer] Processing media upload for entity: {} ({}) (eventId: {})", event.getEntityId(),
            event.getEntityType(), event.getEventId());

      CloudinaryResponse response = null;
      try (LeaseRenewer ignored = new LeaseRenewer(idempotencyService, event.getEventId(), consumerGroup)) {
         // Upload to Cloudinary
         response = cloudinaryService.upload(event.getFileBytes(), event.getFolder());
         log.info("[Kafka Consumer] Uploaded to Cloudinary. URL: {}", response.url());

         // Persist to Database
         Media media = Media.builder()
               .url(response.secureUrl())
               .publicId(response.publicId())
               .format(response.format())
               .resourceType(response.resourceType())
               .bytes(response.bytes())
               .entityId(UUID.fromString(event.getEntityId()))
               .entityType(event.getEntityType())
               .isMain(event.isMain())
               .build();

         mediaRepository.save(Objects.requireNonNull(media));
         log.info("[Kafka Consumer] Successfully persisted media record for correlationId: {}",
               event.getCorrelationId());

         // Evict property images cache
         if ("PROPERTY".equals(event.getEntityType())) {
            try {
               UUID propertyId = UUID.fromString(event.getEntityId());
               propertyImagesCacheService.evict(propertyId);
               log.info("[Kafka Consumer] Evicted property images cache for property: {}", propertyId);
            } catch (Exception e) {
               log.error("[Kafka Consumer] Failed to evict property images cache for property: {}", event.getEntityId(),
                     e);
            }
         }

         // Evict 'properties' cache so the home page updates immediately once the main
         // image is ready
         if (event.isMain() && "PROPERTY".equals(event.getEntityType())) {
            // Evict public homepage cache region via PropertyService to avoid proxy
            // limitations
            try {
               propertyService.evictPublicPropertiesCache();
            } catch (Exception e) {
               log.error("[Kafka Consumer] Failed to evict public properties cache", e);
            }

            // Evict the partner_properties cache for the owner
            try {
               UUID propertyId = UUID.fromString(event.getEntityId());
               propertyRepository.findById(propertyId).ifPresent(property -> {
                  propertyService.evictPartnerPropertiesCache(property.getOwner().getId());
               });
            } catch (Exception e) {
               log.error("[Kafka Consumer] Failed to evict partner properties cache", e);
            }

            // Sync to Elasticsearch via Transactional Outbox to guarantee delivery
            UUID propertyId = UUID.fromString(event.getEntityId());
            outboxService.saveEvent(
                  propertyId,
                  "PROPERTY",
                  "PROPERTY_SYNC",
                  PropertySyncEvent.builder()
                        .propertyId(propertyId)
                        .operation("CREATE")
                        .build());
            log.info(
                  "[Kafka Consumer] Recorded Elasticsearch sync event in outbox for property: {} after main image upload completed",
                  propertyId);
         }

         if (event.getEventId() != null) {
            idempotencyService.completeEvent(event.getEventId(), consumerGroup);
         }
      } catch (Exception e) {
         log.error("[Kafka Consumer] Error processing media for correlationId: {}. Error: {}",
               event.getCorrelationId(), e.getMessage());

         // Rollback compensation for Cloudinary resource leakage prevention
         if (response != null && response.publicId() != null) {
            try {
               log.warn(
                     "[Kafka Consumer] Database persistence failed. Performing rollback compensation: deleting uploaded asset {} from Cloudinary",
                     response.publicId());
               cloudinaryService.delete(response.publicId());
               meterRegistry.counter("omnibooking.media.orphaned.cleanup").increment();
            } catch (Exception deleteEx) {
               log.error("[Kafka Consumer] Failed to delete orphaned Cloudinary asset: {}", response.publicId(),
                     deleteEx);
            }
         }

         // Release claim to allow retry
         if (event.getEventId() != null) {
            try {
               idempotencyService.releaseClaim(event.getEventId(), consumerGroup);
            } catch (Exception releaseEx) {
               log.error("[Kafka Consumer] Failed to release claim: {}", event.getEventId(), releaseEx);
            }
         }

         // Propagate exception to trigger transaction rollback and Kafka retry
         throw new RuntimeException("Media processing failed, rolled back changes", e);
      }
   }

}
