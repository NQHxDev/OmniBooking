package com.omnibooking.services.communication;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducer {

   private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

   public void sendEmailEvent(EmailEvent event) {
      log.info("[Kafka Producer] Sending email event to {} for subject: {}", event.getTo(), event.getSubject());
      kafkaTemplate.send(KafkaConfig.MAIL_TOPIC, event);
   }

}
