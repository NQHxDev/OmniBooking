package com.omnibooking.services.media;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.CloudinaryResponse;
import com.omnibooking.dto.event.MediaUploadEvent;
import com.omnibooking.model.Media;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.services.property.PropertySyncProducer;
import com.omnibooking.services.property.PropertyImagesCacheService;
import com.omnibooking.services.core.IdempotencyService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaConsumer {

   private final CloudinaryService cloudinaryService;
   private final MediaRepository mediaRepository;
   private final CacheManager cacheManager;
   private final PropertySyncProducer propertySyncProducer;
   private final PropertyImagesCacheService propertyImagesCacheService;
   private final IdempotencyService idempotencyService;
   private final MeterRegistry meterRegistry;

   @KafkaListener(topics = KafkaConfig.MEDIA_TOPIC, groupId = "omnibooking-media-group")
   public void consumeUploadEvent(MediaUploadEvent event) {
      String consumerGroup = "omnibooking-media-group";
      if (event.getEventId() != null) {
         if (idempotencyService.isProcessed(event.getEventId(), consumerGroup)) {
            log.warn("[Kafka Consumer] Duplicate media upload event detected and skipped: eventId={}, entityId={}", 
                  event.getEventId(), event.getEntityId());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            meterRegistry.counter("omnibooking.kafka.consumer.skipped").increment();
            return;
         }
      }

      log.info("[Kafka Consumer] Processing media upload for entity: {} ({}) (eventId: {})", event.getEntityId(),
            event.getEntityType(), event.getEventId());

      try {
         // 1. Upload to Cloudinary
         CloudinaryResponse response = cloudinaryService.upload(event.getFileBytes(), event.getFolder());
         log.info("[Kafka Consumer] Uploaded to Cloudinary. URL: {}", response.url());

         // 2. Persist to Database
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
               log.error("[Kafka Consumer] Failed to evict property images cache for property: {}", event.getEntityId(), e);
            }
         }

         // Evict 'properties' cache so the home page updates immediately once the main image is ready
         if (event.isMain() && "PROPERTY".equals(event.getEntityType())) {
            org.springframework.cache.Cache propertiesCache = cacheManager.getCache("properties");
            if (propertiesCache != null) {
               propertiesCache.clear();
               log.info("[Kafka Consumer] Evicted 'properties' cache because main image for property {} is ready", event.getEntityId());
            }

            // Sync to Elasticsearch now that the main image URL is persisted in Postgres and uploaded to Cloudinary
            try {
               UUID propertyId = UUID.fromString(event.getEntityId());
               propertySyncProducer.sendSyncEvent(PropertySyncEvent.builder()
                     .propertyId(propertyId)
                     .operation("CREATE")
                     .build());
               log.info("[Kafka Consumer] Triggered Elasticsearch sync for property: {} after main image upload completed", propertyId);
            } catch (Exception e) {
               log.error("[Kafka Consumer] Failed to trigger Elasticsearch sync for property: {}", event.getEntityId(), e);
            }
         }

         // Mark event as processed successfully
         if (event.getEventId() != null) {
            idempotencyService.markProcessed(event.getEventId(), consumerGroup);
         }

      } catch (Exception e) {
         log.error("[Kafka Consumer] Error processing media for correlationId: {}. Error: {}",
               event.getCorrelationId(), e.getMessage());
      }
   }
}
