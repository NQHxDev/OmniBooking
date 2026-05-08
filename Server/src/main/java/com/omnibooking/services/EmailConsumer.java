package com.omnibooking.services;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

   private final ResendEmailService resendEmailService;

   @KafkaListener(topics = KafkaConfig.MAIL_TOPIC, groupId = "omnibooking-mail-group")
   public void consumeEmailEvent(EmailEvent event) {
      log.info("[Kafka Consumer] Received email event for {}", event.getTo());
      resendEmailService.sendHtmlEmail(event.getTo(), event.getSubject(), event.getContent());
   }

}
