package com.omnibooking.services.communication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseNotificationService {

   // Map to store emitters by requestId
   private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

   public SseEmitter subscribe(String requestId) {
      // Timeout after 2 minutes of waiting
      SseEmitter emitter = new SseEmitter(120_000L);

      emitters.put(requestId, emitter);

      emitter.onCompletion(() -> emitters.remove(requestId));
      emitter.onTimeout(() -> emitters.remove(requestId));
      emitter.onError((e) -> emitters.remove(requestId));

      log.debug("New SSE subscription for requestId: {}", requestId);

      // Send an initial "connected" event
      try {
         emitter.send(SseEmitter.event()
               .name("CONNECTED")
               .data("Connected to registration status stream"));
      } catch (IOException e) {
         emitters.remove(requestId);
      }

      return emitter;
   }

   public void sendNotification(String requestId, Object data) {
      SseEmitter emitter = emitters.get(requestId);
      if (emitter != null) {
         try {
            emitter.send(SseEmitter.event()
                  .name("REGISTRATION_COMPLETE")
                  .data(data));
            emitter.complete();
            log.info("Notification sent via SSE for requestId: {}", requestId);
         } catch (IOException e) {
            log.error("Failed to send SSE for requestId: {}", requestId, e);
            emitter.completeWithError(e);
         } finally {
            emitters.remove(requestId);
         }
      }
   }
}
