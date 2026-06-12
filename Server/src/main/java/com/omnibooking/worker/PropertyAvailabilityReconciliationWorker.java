package com.omnibooking.worker;

import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.services.property.PropertyService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyAvailabilityReconciliationWorker {

   private final RoomTypeRepository roomTypeRepository;

   private final PropertyService propertyService;

   private final Counter availabilityRegenerationCounter;

   /**
    * Runs every 15 minutes. Detects room types with missing availability and
    * regenerates them.
    */
   @Scheduled(cron = "0 */15 * * * *")
   @SchedulerLock(name = "propertyAvailabilityReconciliation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT2M")
   public void reconcileAvailability() {
      log.info("Starting Property Availability Reconciliation...");
      try {
         List<UUID> roomTypeIds = roomTypeRepository.findRoomTypeIdsWithoutAvailability();
         if (!roomTypeIds.isEmpty()) {
            log.warn("Found {} room types without availability records. Starting regeneration...", roomTypeIds.size());
            propertyService.initializeRoomAvailability(roomTypeIds);
            availabilityRegenerationCounter.increment(roomTypeIds.size());
            log.info("Regenerated availability for {} room types", roomTypeIds.size());
         } else {
            log.info("No room types with missing availability found.");
         }
      } catch (Exception e) {
         log.error("Failed to reconcile property availability", e);
      }
   }

}
