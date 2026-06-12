package com.omnibooking.worker;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.dto.event.PropertySyncEvent;
import com.omnibooking.model.PropertyCreatedOutbox;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.model.enums.PropertyStatus;
import com.omnibooking.repository.infra.PropertyCreatedOutboxRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.property.PropertyService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyCreatedOutboxWorker {

   private final PropertyCreatedOutboxRepository propertyCreatedOutboxRepository;

   private final PropertyRepository propertyRepository;

   private final RoomTypeRepository roomTypeRepository;

   private final PropertyService propertyService;

   private final OutboxService outboxService;

   private final MeterRegistry meterRegistry;

   @Value("${omnibooking.property.setup.lease-duration:5m}")
   private Duration leaseDuration;

   @Autowired
   @Lazy
   private PropertyCreatedOutboxWorker self;

   private Counter successCounter;
   private Counter failedCounter;
   private Counter deadCounter;
   private Timer durationTimer;

   @PostConstruct
   public void initMetrics() {
      this.successCounter = meterRegistry.counter("property_setup_success_total");
      this.failedCounter = meterRegistry.counter("property_setup_failed_total");
      this.deadCounter = meterRegistry.counter("property_setup_dead_total");
      this.durationTimer = Timer.builder("property_setup_duration_seconds")
            .description("Duration of property setup execution in seconds")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
   }

   @Scheduled(fixedDelayString = "${omnibooking.property.setup.interval-ms:5000}")
   @SchedulerLock(name = "propertySetupOutbox", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
   public void processSetupJobs() {
      try {
         log.info("processSetupJobs called");
         Pageable pageable = PageRequest.of(0, 10);
         List<PropertyCreatedOutbox> batch = self.lockAndFetchEventsToProcess(pageable);
         log.info("lockAndFetchEventsToProcess returned {} events", batch.size());
         if (batch.isEmpty()) {
            return;
         }

         log.info("Processing {} property setup outbox records", batch.size());
         for (PropertyCreatedOutbox record : batch) {
            try {
               processSingleSetup(record);
            } catch (Exception e) {
               log.error("Unexpected error processing setup record: {}", record.getId(), e);
            }
         }
      } catch (Exception e) {
         log.error("Error in PropertyCreatedOutboxWorker execution cycle", e);
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public List<PropertyCreatedOutbox> lockAndFetchEventsToProcess(Pageable pageable) {
      Instant now = Instant.now();
      log.info("lockAndFetchEventsToProcess: querying with now={}", now);
      List<PropertyCreatedOutbox> events = propertyCreatedOutboxRepository.findEventsToProcess(now, pageable);
      log.info("lockAndFetchEventsToProcess: found {} events in query", events.size());
      if (events.isEmpty()) {
         return List.of();
      }

      Instant leaseExpiry = now.plus(leaseDuration);
      for (PropertyCreatedOutbox event : events) {
         event.setStatus(OutboxStatus.PROCESSING);
         event.setLeaseUntil(leaseExpiry);
      }

      return propertyCreatedOutboxRepository.saveAllAndFlush(events);
   }

   public void processSingleSetup(PropertyCreatedOutbox record) {
      log.info("Starting property setup process for outbox record: {} (propertyId: {})", record.getId(),
            record.getPropertyId());
      Instant start = Instant.now();
      try {
         // Get room types associated with the property
         List<RoomType> roomTypes = roomTypeRepository.findByPropertyId(record.getPropertyId());
         List<UUID> roomTypeIds = roomTypes.stream().map(RoomType::getId).toList();

         if (!roomTypeIds.isEmpty()) {
            propertyService.initializeRoomAvailability(roomTypeIds);
         }

         self.markAsProcessed(record.getId(), record.getPropertyId());
         durationTimer.record(Duration.between(start, Instant.now()));
         log.info("Completed property setup successfully for property: {}", record.getPropertyId());
      } catch (Exception e) {
         log.error("Failed to setup property availability: {}", record.getPropertyId(), e);
         self.handleFailure(record.getId(), record.getPropertyId(), e);
      }
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void markAsProcessed(UUID recordId, UUID propertyId) {
      propertyCreatedOutboxRepository.findById(recordId).ifPresent(record -> {
         record.setStatus(OutboxStatus.PROCESSED);
         propertyCreatedOutboxRepository.saveAndFlush(record);
      });

      propertyRepository.findById(propertyId).ifPresent(property -> {
         property.setStatus(PropertyStatus.ACTIVE);
         propertyRepository.saveAndFlush(property);

         // Save PROPERTY_SYNC event to outbox for reliable Elasticsearch sync
         PropertySyncEvent syncEvent = PropertySyncEvent.builder()
               .propertyId(propertyId)
               .operation("UPDATE")
               .build();
         outboxService.saveEvent(propertyId, "PROPERTY", EventConstants.PROPERTY_SYNC, syncEvent);
         log.info("Saved PROPERTY_SYNC event for active property: {}", propertyId);

         // Evict public properties cache
         propertyService.evictPublicPropertiesCache();
      });

      successCounter.increment();
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void handleFailure(UUID recordId, UUID propertyId, Exception ex) {
      propertyCreatedOutboxRepository.findById(recordId).ifPresent(record -> {
         int nextRetryCount = record.getRetryCount() + 1;
         record.setRetryCount(nextRetryCount);

         String errorMsg = ex.getClass().getName() + ": " + ex.getMessage();
         if (errorMsg.length() > 1000) {
            errorMsg = errorMsg.substring(0, 1000);
         }
         record.setLastError(errorMsg);

         if (nextRetryCount >= 5) {
            record.setStatus(OutboxStatus.DEAD);
            record.setLeaseUntil(null);
            propertyCreatedOutboxRepository.saveAndFlush(record);

            // Update property status to SETUP_FAILED
            propertyRepository.findById(propertyId).ifPresent(property -> {
               property.setStatus(PropertyStatus.SETUP_FAILED);
               propertyRepository.saveAndFlush(property);
            });

            deadCounter.increment();
            log.warn("Property setup outbox record {} reached max retries. Property {} marked as SETUP_FAILED.",
                  recordId, propertyId);
         } else {
            record.setStatus(OutboxStatus.PENDING);
            record.setLeaseUntil(null);
            // Exponential backoff: 1, 2, 4, 8 minutes
            long backoffMinutes = 1L << (nextRetryCount - 1);
            record.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(backoffMinutes)));
            propertyCreatedOutboxRepository.saveAndFlush(record);

            failedCounter.increment();
            log.info("Property setup outbox record {} rescheduled for retry in {} minutes (attempt {}/5).", recordId,
                  backoffMinutes, nextRetryCount);
         }
      });
   }

   @Scheduled(cron = "0 0 3 * * *")
   @SchedulerLock(name = "propertySetupCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
   @Transactional
   public void purgeOldProcessedJobs() {
      Instant threshold = Instant.now().minus(90, ChronoUnit.DAYS);
      int deleted = propertyCreatedOutboxRepository.deleteProcessedEventsBefore(threshold);
      if (deleted > 0) {
         log.info("Purged {} processed property setup outbox records older than 90 days", deleted);
      }
   }

}
