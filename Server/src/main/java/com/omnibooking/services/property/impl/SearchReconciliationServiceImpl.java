package com.omnibooking.services.property.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.model.Property;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.property.SearchReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchReconciliationServiceImpl implements SearchReconciliationService {

   private final PropertyRepository propertyRepository;

   private final PropertyElasticsearchRepository propertyElasticsearchRepository;

   private final OutboxService outboxService;

   @Override
   @Transactional
   public void reconcileProperties() {
      log.info("Starting Search Index Reconciliation (PostgreSQL vs Elasticsearch)...");
      List<Property> properties = propertyRepository.findAll();
      int driftCount = 0;

      for (Property property : properties) {
         String docId = property.getId().toString();
         boolean existsInEs = false;
         try {
            existsInEs = propertyElasticsearchRepository.existsById(docId);
         } catch (Exception e) {
            log.error("Failed to check status in Elasticsearch for property ID: {}", docId, e);
            continue;
         }

         if (!existsInEs) {
            log.warn("Drift detected: Property {} is missing in Elasticsearch. Queueing sync...", property.getId());
            outboxService.saveEvent(
                  property.getId(),
                  "PROPERTY",
                  EventConstants.PROPERTY_SYNC,
                  PropertySyncEvent.builder()
                        .propertyId(property.getId())
                        .operation("CREATE")
                        .build());
            driftCount++;
         }
      }

      if (driftCount > 0) {
         log.info("Search Reconciliation finished. Found and queued repairs for {} mismatched properties.", driftCount);
      } else {
         log.info("Search Reconciliation finished. No drift detected between Database and Elasticsearch.");
      }
   }

}
