package com.omnibooking.config;

import com.omnibooking.services.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

      public void receiveMessage(String message) {
         log.debug("Received message from Redis Pub/Sub: {}", message);
         // Expected format: "requestId|userDataJson"
         int delimiterIndex = message.indexOf("|");
         if (delimiterIndex > 0) {
            String requestId = message.substring(0, delimiterIndex);
            String dataJson = message.substring(delimiterIndex + 1);
            sseNotificationService.sendNotification(requestId, dataJson);
         }
      }
   }

}
