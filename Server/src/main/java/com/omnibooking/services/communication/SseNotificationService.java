package com.omnibooking.services.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseNotificationService {

   // Map to store emitters by requestId
   private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

   private final MeterRegistry meterRegistry;

   private final ObjectMapper objectMapper;

   private final StringRedisTemplate redisTemplate;

   public SseEmitter subscribe(String requestId) {
      MDC.put("requestId", requestId);
      try {
         // Check if we already have a cached registration result (race condition fallback)
         String resultKey = "registration_result:" + requestId;
         String cachedResult = redisTemplate.opsForValue().get(resultKey);
         if (cachedResult != null && cachedResult.contains("SUCCESS")) {
            String tokenKey = "registration_token:" + requestId;
            String accessToken = redisTemplate.opsForValue().get(tokenKey);
            if (accessToken != null) {
               SseEmitter emitter = new SseEmitter(120_000L);
               try {
                  Map<String, String> responseMap = new HashMap<>();
                  responseMap.put("accessToken", accessToken);
                  String responseJson = objectMapper.writeValueAsString(responseMap);

                  emitter.send(SseEmitter.event()
                        .name("REGISTRATION_COMPLETE")
                        .data(responseJson));
                  emitter.complete();
                  logJson("registration_sse_sent_cached", requestId, null, "Cached registration notification sent via SSE successfully");
                  return emitter;
               } catch (IOException e) {
                  logJson("registration_sse_failed_cached", requestId, null, "Failed to send cached SSE: " + e.getMessage());
               }
            }
         }

         // Timeout after 2 minutes of waiting
         SseEmitter emitter = new SseEmitter(120_000L);

         emitters.put(requestId, emitter);

         emitter.onCompletion(() -> {
            MDC.put("requestId", requestId);
            try {
               emitters.remove(requestId);
               logJson("sse_connection_completed", requestId, null, "SSE connection completed");
            } finally {
               MDC.remove("requestId");
            }
         });
         emitter.onTimeout(() -> {
            MDC.put("requestId", requestId);
            try {
               emitters.remove(requestId);
               logJson("sse_connection_timeout", requestId, null, "SSE connection timed out");
            } finally {
               MDC.remove("requestId");
            }
         });
         emitter.onError((e) -> {
            MDC.put("requestId", requestId);
            try {
               emitters.remove(requestId);
               logJson("sse_connection_error", requestId, null, "SSE connection error: " + e.getMessage());
            } finally {
               MDC.remove("requestId");
            }
         });

         logJson("sse_subscribed", requestId, null, "New SSE subscription registered");

         // Send an initial "connected" event
         try {
            emitter.send(SseEmitter.event()
                  .name("CONNECTED")
                  .data("Connected to registration status stream"));
         } catch (IOException e) {
            emitters.remove(requestId);
         }

         return emitter;
      } finally {
         MDC.remove("requestId");
      }
   }

   public void sendNotification(String requestId, Object data) {
      MDC.put("requestId", requestId);
      try {
         SseEmitter emitter = emitters.get(requestId);
         if (emitter != null) {
            try {
               emitter.send(SseEmitter.event()
                     .name("REGISTRATION_COMPLETE")
                     .data(data));
               emitter.complete();
               meterRegistry.counter("registration_sse_success_total").increment();
               logJson("registration_sse_sent", requestId, null, "Notification sent via SSE successfully");
            } catch (IOException e) {
               logJson("registration_sse_failed", requestId, null, "Failed to send SSE: " + e.getMessage());
               emitter.completeWithError(e);
            } finally {
               emitters.remove(requestId);
            }
         } else {
            logJson("registration_sse_skipped", requestId, null, "No active SSE emitter found for client notification");
         }
      } finally {
         MDC.remove("requestId");
      }
   }

   private void logJson(String event, String requestId, String email, String message) {
      try {
         Map<String, Object> logPayload = new HashMap<>();
         logPayload.put("requestId", requestId);
         logPayload.put("event", event);
         if (email != null) {
            logPayload.put("email", email);
         }
         logPayload.put("message", message);
         logPayload.put("timestamp", Instant.now().toString());
         log.info(objectMapper.writeValueAsString(logPayload));
      } catch (Exception e) {
         log.error("Failed to write JSON log", e);
      }
   }

}
