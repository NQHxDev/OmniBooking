package com.omnibooking.services.communication;

import com.omnibooking.config.KafkaConfig;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.services.core.IdempotencyService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

   private final ResendEmailService resendEmailService;
   private final IdempotencyService idempotencyService;
   private final MeterRegistry meterRegistry;

   @KafkaListener(topics = KafkaConfig.MAIL_TOPIC, groupId = "omnibooking-mail-group")
   public void consumeEmailEvent(EmailEvent event) {
      String consumerGroup = "omnibooking-mail-group";
      if (event.getEventId() != null) {
         if (idempotencyService.isProcessed(event.getEventId(), consumerGroup)) {
            log.warn("[Kafka Consumer] Duplicate email event detected and skipped: eventId={}, to={}", 
                  event.getEventId(), event.getTo());
            meterRegistry.counter("omnibooking.kafka.consumer.duplicate").increment();
            meterRegistry.counter("omnibooking.kafka.consumer.skipped").increment();
            return;
         }
      }

      log.info("[Kafka Consumer] Processing email event for {} (eventId: {})", event.getTo(), event.getEventId());
      resendEmailService.sendHtmlEmail(event.getTo(), event.getSubject(), event.getContent());

      if (event.getEventId() != null) {
         idempotencyService.markProcessed(event.getEventId(), consumerGroup);
      }
   }

}
