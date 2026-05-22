package com.omnibooking.services.media;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.CloudinaryResponse;
import com.omnibooking.dto.event.MediaUploadEvent;
import com.omnibooking.model.Media;
import com.omnibooking.repository.MediaRepository;
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

   @KafkaListener(topics = KafkaConfig.MEDIA_TOPIC, groupId = "omnibooking-media-group")
   public void consumeUploadEvent(MediaUploadEvent event) {
      log.info("[Kafka Consumer] Processing media upload for entity: {} ({})", event.getEntityId(),
            event.getEntityType());

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

         // Evict 'properties' cache so the home page updates immediately once the main image is ready
         if (event.isMain() && "PROPERTY".equals(event.getEntityType())) {
            org.springframework.cache.Cache propertiesCache = cacheManager.getCache("properties");
            if (propertiesCache != null) {
               propertiesCache.clear();
               log.info("[Kafka Consumer] Evicted 'properties' cache because main image for property {} is ready", event.getEntityId());
            }
         }

      } catch (Exception e) {
         log.error("[Kafka Consumer] Error processing media for correlationId: {}. Error: {}",
               event.getCorrelationId(), e.getMessage());
      }
   }
}
