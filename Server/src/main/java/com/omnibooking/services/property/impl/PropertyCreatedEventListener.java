package com.omnibooking.services.property.impl;

import com.omnibooking.dto.event.PropertyCreatedEvent;
import com.omnibooking.services.property.PropertyService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyCreatedEventListener {

   private final PropertyService propertyService;

   private final TaskScheduler taskScheduler;

   private final Counter availabilityGenerationSuccessCounter;

   private final Counter availabilityGenerationFailedCounter;

   @Async("propertyCreationAsyncExecutor")
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void handlePropertyCreated(PropertyCreatedEvent event) {
      log.info("Received PropertyCreatedEvent for property: {} with {} room types",
            event.getPropertyId(), event.getRoomTypeIds().size());
      executeWithRetry(event.getRoomTypeIds(), event.getPropertyId(), 1);
   }

   private void executeWithRetry(List<UUID> roomTypeIds, UUID propertyId, int attempt) {
      try {
         propertyService.initializeRoomAvailability(roomTypeIds);
         availabilityGenerationSuccessCounter.increment();
         log.info("Successfully initialized availability for property {}", propertyId);
      } catch (Exception e) {
         log.warn("Attempt {} to initialize availability failed for property {}: {}",
               attempt, propertyId, e.getMessage());
         if (attempt < 3) {
            long delaySeconds = attempt * 2L; // backoff: 2s, 4s
            taskScheduler.schedule(
                  () -> executeWithRetry(roomTypeIds, propertyId, attempt + 1),
                  Instant.now().plusSeconds(delaySeconds));
         } else {
            log.error("Failed to initialize availability for property {} after 3 attempts. Needs reconciliation.",
                  propertyId);
            availabilityGenerationFailedCounter.increment();
         }
      }
   }

}
