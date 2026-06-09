package com.omnibooking.services.media;

import com.omnibooking.config.AppProperties;
import com.omnibooking.dto.MediaProgress;
import com.omnibooking.dto.event.MediaProgressUpdatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central dispatcher managing all active SSE connections for media progress.
 * Event-driven: pushes updates only when {@link MediaProgressUpdatedEvent} fires.
 * Enforces configurable max-emitters-per-property limit with FIFO eviction.
 */
@Slf4j
@Component
public class SseProgressDispatcher {

   private final AppProperties appProperties;

   private final ConcurrentHashMap<UUID, Set<SseEmitter>> emitterRegistry = new ConcurrentHashMap<>();
   private final AtomicInteger activeEmittersTotal = new AtomicInteger(0);

   public SseProgressDispatcher(AppProperties appProperties, MeterRegistry meterRegistry) {
      this.appProperties = appProperties;
      meterRegistry.gauge("media.progress.active_emitters_total", activeEmittersTotal);
   }

   /**
    * Register a new SSE emitter for a property.
    * Enforces max-emitters-per-property limit with FIFO eviction of oldest emitter.
    */
   public void register(UUID propertyId, SseEmitter emitter) {
      int maxPerProperty = appProperties.getMedia().getProgress().getMaxEmittersPerProperty();

      Set<SseEmitter> emitters = emitterRegistry
            .computeIfAbsent(propertyId, k -> ConcurrentHashMap.newKeySet());

      // Enforce connection limit — evict oldest
      if (emitters.size() >= maxPerProperty) {
         Iterator<SseEmitter> it = emitters.iterator();
         if (it.hasNext()) {
            SseEmitter oldest = it.next();
            try {
               oldest.complete();
            } catch (Exception e) {
               log.debug("[SSE] Error completing oldest emitter during eviction", e);
            }
            it.remove();
            activeEmittersTotal.decrementAndGet();
            log.info("[SSE] Evicted oldest emitter for property: {} (limit: {})",
                  propertyId, maxPerProperty);
         }
      }

      emitters.add(emitter);
      activeEmittersTotal.incrementAndGet();

      // Cleanup on completion/timeout/error
      Runnable cleanup = () -> {
         if (emitters.remove(emitter)) {
            activeEmittersTotal.decrementAndGet();
         }
         if (emitters.isEmpty()) {
            emitterRegistry.remove(propertyId);
         }
      };
      emitter.onCompletion(cleanup);
      emitter.onTimeout(cleanup);
      emitter.onError(e -> cleanup.run());

      log.debug("[SSE] Registered emitter for property: {} (active: {})",
            propertyId, emitters.size());
   }

   /**
    * Event-driven: pushes progress update to all connected emitters for the property.
    * Auto-completes emitters on terminal states.
    */
   @EventListener
   public void onProgressUpdated(MediaProgressUpdatedEvent event) {
      Set<SseEmitter> emitters = emitterRegistry.get(event.getPropertyId());
      if (emitters == null || emitters.isEmpty()) return;

      List<SseEmitter> deadEmitters = new ArrayList<>();
      MediaProgress progress = event.getProgress();

      for (SseEmitter emitter : emitters) {
         try {
            emitter.send(SseEmitter.event()
                  .name("progress")
                  .data(progress));

            // Auto-complete on terminal states
            if (isTerminal(progress.status())) {
               emitter.complete();
            }
         } catch (IOException e) {
            deadEmitters.add(emitter);
         }
      }

      // Cleanup dead emitters
      for (SseEmitter dead : deadEmitters) {
         if (emitters.remove(dead)) {
            activeEmittersTotal.decrementAndGet();
         }
      }
   }

   /**
    * Periodic heartbeat to keep SSE connections alive.
    */
   @Scheduled(fixedRateString = "${app.media.progress.sse-heartbeat-interval:15}000")
   public void sendHeartbeats() {
      emitterRegistry.forEach((propertyId, emitters) -> {
         List<SseEmitter> dead = new ArrayList<>();
         for (SseEmitter emitter : emitters) {
            try {
               emitter.send(SseEmitter.event()
                     .name("heartbeat")
                     .data(""));
            } catch (IOException e) {
               dead.add(emitter);
            }
         }
         for (SseEmitter d : dead) {
            if (emitters.remove(d)) {
               activeEmittersTotal.decrementAndGet();
            }
         }
      });
   }

   private boolean isTerminal(String status) {
      return "COMPLETED".equals(status)
            || "PARTIAL_SUCCESS".equals(status)
            || "FAILED".equals(status);
   }

}
