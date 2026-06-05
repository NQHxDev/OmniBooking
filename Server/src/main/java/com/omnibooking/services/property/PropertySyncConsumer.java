package com.omnibooking.services.property;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.mapper.PropertyDocumentMapper;
import com.omnibooking.repository.infra.MediaRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.services.core.IdempotencyService;
import com.omnibooking.services.core.LeaseRenewer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertySyncConsumer {

   private final PropertyRepository propertyRepository;

   private final PropertyElasticsearchRepository propertyElasticsearchRepository;

   private final PropertyDocumentMapper propertyDocumentMapper;

   private final MediaRepository mediaRepository;

   private final IdempotencyService idempotencyService;

   private final MeterRegistry meterRegistry;

   private final DestinationService destinationService;

   @KafkaListener(topics = "${app.kafka.topics.property-sync}", groupId = "omnibooking-property-sync-group")
   public void consumePropertySync(PropertySyncEvent event) {
      String consumerGroup = "omnibooking-property-sync-group";
      if (event.getEventId() != null) {
         boolean claimed = idempotencyService.claimEvent(event.getEventId(), consumerGroup);
         if (!claimed) {
            log.warn("[Kafka Consumer] Duplicate PropertySync event detected and skipped: eventId={}, propertyId={}",
                  event.getEventId(), event.getPropertyId());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            meterRegistry.counter("omnibooking.kafka.consumer.skipped").increment();
            return;
         }
      }

      log.info("Received property sync event: {} for property: {} (eventId: {})",
            event.getOperation(), event.getPropertyId(), event.getEventId());

      try (LeaseRenewer ignored = new LeaseRenewer(idempotencyService, event.getEventId(), consumerGroup)) {
         if ("DELETE".equals(event.getOperation())) {
            propertyElasticsearchRepository.deleteById(event.getPropertyId().toString());
            if (event.getEventId() != null) {
               idempotencyService.completeEvent(event.getEventId(), consumerGroup);
            }
            return;
         }

         propertyRepository.findByIdWithAmenitiesAndRoomTypes(event.getPropertyId()).ifPresentOrElse(property -> {
            PropertyDocument document = propertyDocumentMapper.toDocument(property);

            // Set main image url
            mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(property.getId(), "PROPERTY")
                  .ifPresent(media -> document.setMainImageUrl(media.getUrl()));

            propertyElasticsearchRepository.save(document);
            log.info("Successfully synced property to Elasticsearch: {}", property.getId());

            // Register destination if not exists
            try {
               destinationService.registerDestinationIfNeeded(
                     property.getCity(),
                     property.getCountry(),
                     property.getLatitude() != null ? property.getLatitude().doubleValue() : null,
                     property.getLongitude() != null ? property.getLongitude().doubleValue() : null);
            } catch (Exception e) {
               log.error("Failed to register destination for property: {}", property.getId(), e);
            }

            if (event.getEventId() != null) {
               idempotencyService.completeEvent(event.getEventId(), consumerGroup);
            }
         }, () -> {
            log.error("Property not found for sync: {}", event.getPropertyId());
            meterRegistry.counter("omnibooking.search.sync.failure").increment();
            if (event.getEventId() != null) {
               idempotencyService.completeEvent(event.getEventId(), consumerGroup);
            }
         });
      } catch (Exception e) {
         log.error("Failed to process property sync: {}", event.getPropertyId(), e);
         meterRegistry.counter("omnibooking.search.sync.failure").increment();
         if (event.getEventId() != null) {
            try {
               idempotencyService.releaseClaim(event.getEventId(), consumerGroup);
            } catch (Exception releaseEx) {
               log.error("Failed to release claim for event: {}", event.getEventId(), releaseEx);
            }
         }
         throw e; // Propagate exception to trigger Kafka retry
      }
   }

}
