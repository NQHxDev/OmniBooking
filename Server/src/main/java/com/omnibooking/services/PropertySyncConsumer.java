package com.omnibooking.services;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.mapper.PropertyDocumentMapper;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
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

   @KafkaListener(topics = "${app.kafka.topics.property-sync}", groupId = "omnibooking-property-sync-group")
   public void consumePropertySync(PropertySyncEvent event) {
      log.info("Received property sync event: {} for property: {}", event.getOperation(), event.getPropertyId());

      if ("DELETE".equals(event.getOperation())) {
         propertyElasticsearchRepository.deleteById(event.getPropertyId().toString());
         return;
      }

      propertyRepository.findById(event.getPropertyId()).ifPresent(property -> {
         PropertyDocument document = propertyDocumentMapper.toDocument(property);

         // Set main image url
         mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(property.getId(), "PROPERTY")
               .ifPresent(media -> document.setMainImageUrl(media.getUrl()));

         propertyElasticsearchRepository.save(document);
         log.info("Successfully synced property to Elasticsearch: {}", property.getId());
      });
   }
}
