package com.omnibooking.config;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.mapper.PropertyDocumentMapper;
import com.omnibooking.model.Property;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;
import com.omnibooking.services.property.DestinationService;

@Slf4j
@Configuration
@Order(3)
@RequiredArgsConstructor
public class PropertyElasticsearchReindexInitializer implements CommandLineRunner {

   private final PropertyRepository propertyRepository;
   private final PropertyElasticsearchRepository propertyElasticsearchRepository;
   private final PropertyDocumentMapper propertyDocumentMapper;
   private final MediaRepository mediaRepository;
   private final DestinationService destinationService;

   @Override
   public void run(String... args) {
      log.info(
            "Starting property synchronization: force re-syncing all properties from PostgreSQL to Elasticsearch...");
      try {
         List<Property> dbProperties = propertyRepository.findAllWithAmenitiesAndRoomTypes();
         long esCount = propertyElasticsearchRepository.count();

         log.info("PostgreSQL count: {}, Elasticsearch count: {}", dbProperties.size(), esCount);

         // Always force re-sync all properties to ensure data consistency
         // This handles cases where property data was updated but ES was not synced
         int successCount = 0;
         for (Property property : dbProperties) {
            try {
               log.info("Syncing property: id={}, name='{}', city='{}'", property.getId(), property.getName(),
                     property.getCity());
               PropertyDocument document = propertyDocumentMapper.toDocument(property);
               mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(property.getId(), "PROPERTY")
                     .ifPresent(media -> document.setMainImageUrl(media.getUrl()));
               propertyElasticsearchRepository.save(document);

                // Register destination if not exists
                try {
                   destinationService.registerDestinationIfNeeded(
                         property.getCity(),
                         property.getCountry(),
                         property.getLatitude() != null ? property.getLatitude().doubleValue() : null,
                         property.getLongitude() != null ? property.getLongitude().doubleValue() : null
                   );
                } catch (Exception e) {
                   log.error("Failed to register destination on startup: {}", property.getCity(), e);
                }

                successCount++;
            } catch (Exception e) {
               log.error("Failed to sync property: id={}, name='{}', city='{}' - Error: {}",
                     property.getId(), property.getName(), property.getCity(), e.getMessage(), e);
            }
         }
         log.info("Successfully synced {}/{} properties to Elasticsearch.", successCount, dbProperties.size());

         // Clean up orphaned ES documents (exist in ES but not in DB)
         if (esCount > dbProperties.size()) {
            log.info("Detected potential orphaned documents in Elasticsearch. Count difference: {}",
                  esCount - dbProperties.size());
         }
      } catch (Exception e) {
         log.error("Failed to check or sync properties on startup", e);
      }
   }

}
