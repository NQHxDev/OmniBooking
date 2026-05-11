package com.omnibooking.services;

import com.omnibooking.dto.event.PropertySyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertySyncProducer {

   private final KafkaTemplate<String, Object> kafkaTemplate;

   @Value("${app.kafka.topics.property-sync}")
   private String topic;

   public void sendSyncEvent(PropertySyncEvent event) {
      log.info("Sending property sync event: {} for property: {}", event.getOperation(), event.getPropertyId());
      kafkaTemplate.send(topic, event.getPropertyId().toString(), event);
   }
}
