package com.omnibooking.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registry and router for event schema migrations.
 * Scans all registered implementations of {@link EventUpcasterStrategy} and executes them sequentially.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventUpcaster {

   private final List<EventUpcasterStrategy> strategies;

   /**
    * Sequentially migrates an event payload from currentVersion to targetVersion.
    */
   public JsonNode upcast(String eventType, JsonNode payload, int currentVersion, int targetVersion) {
      if (currentVersion >= targetVersion) {
         return payload;
      }

      JsonNode upcasted = payload;
      for (int v = currentVersion; v < targetVersion; v++) {
         boolean strategyApplied = false;
         for (EventUpcasterStrategy strategy : strategies) {
            if (strategy.canUpcast(eventType, v)) {
               upcasted = strategy.upcast(upcasted);
               strategyApplied = true;
               log.info("Successfully applied upcast strategy for event: {}, from v{} to v{}", 
                     eventType, v, v + 1);
               break;
            }
         }
         if (!strategyApplied) {
            log.warn("No upcast strategy found for event: {}, source version: v{}. Aborting chain.", 
                  eventType, v);
            break; // Stop migration if any intermediate step is missing
         }
      }
      return upcasted;
   }

}
