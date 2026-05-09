package com.omnibooking.services;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.event.MediaUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProducer {

   private final KafkaTemplate<String, MediaUploadEvent> kafkaTemplate;

   public void sendUploadEvent(MediaUploadEvent event) {
      log.info("[Kafka Producer] Sending media upload event for file: {} to folder: {}", 
               event.getFileName(), event.getFolder());
      kafkaTemplate.send(KafkaConfig.MEDIA_TOPIC, Objects.requireNonNull(event.getCorrelationId()), event);
   }

}
