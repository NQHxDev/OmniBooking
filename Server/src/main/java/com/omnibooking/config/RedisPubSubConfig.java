package com.omnibooking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.services.communication.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubConfig {

   public static final String REGISTRATION_TOPIC = "registration_status_channel";

   @Bean
   public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
         MessageListenerAdapter listenerAdapter) {
      RedisMessageListenerContainer container = new RedisMessageListenerContainer();
      container.setConnectionFactory(connectionFactory);
      container.addMessageListener(listenerAdapter, new PatternTopic(REGISTRATION_TOPIC));
      return container;
   }

   @Bean
   public MessageListenerAdapter listenerAdapter(RedisMessageReceiver receiver) {
      return new MessageListenerAdapter(receiver, "receiveMessage");
   }

   @Component
   @RequiredArgsConstructor
   public static class RedisMessageReceiver {
      private final SseNotificationService sseNotificationService;

      private final ObjectMapper objectMapper;

      public void receiveMessage(String message) {
         // Expected format: "requestId|userDataJson"
         int delimiterIndex = message.indexOf("|");
         if (delimiterIndex > 0) {
            String requestId = message.substring(0, delimiterIndex);
            String dataJson = message.substring(delimiterIndex + 1);
            org.slf4j.MDC.put("requestId", requestId);
            try {
               logJson("registration_pubsub_received", requestId, null,
                     "Received registration pub/sub completion event");
               sseNotificationService.sendNotification(requestId, dataJson);
            } finally {
               org.slf4j.MDC.remove("requestId");
            }
         } else {
            log.warn("Invalid message format received from Redis Pub/Sub: {}", message);
         }
      }

      private void logJson(String event, String requestId, String email, String message) {
         try {
            Map<String, Object> logPayload = new java.util.HashMap<>();
            logPayload.put("requestId", requestId);
            logPayload.put("event", event);
            if (email != null) {
               logPayload.put("email", email);
            }
            logPayload.put("message", message);
            logPayload.put("timestamp", java.time.Instant.now().toString());
            log.info(objectMapper.writeValueAsString(logPayload));
         } catch (Exception e) {
            log.error("Failed to write JSON log", e);
         }
      }
   }

}
