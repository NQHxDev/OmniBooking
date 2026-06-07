package com.omnibooking.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.RegistrationDlt;
import com.omnibooking.model.enums.RegistrationDltStatus;
import com.omnibooking.repository.registration.RegistrationDltRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationDltConsumer {

   private final RegistrationDltRepository dltRepository;

   private final ObjectMapper objectMapper;

   private final MeterRegistry meterRegistry;

   @PostConstruct
   public void initMetrics() {
      // Expose gauge for pending DLT messages
      meterRegistry.gauge("omnibooking.dlt.pending", this,
            self -> self.dltRepository.countByStatus(RegistrationDltStatus.PENDING));
   }

   @KafkaListener(topics = "${omnibooking.kafka.registration.topic-name:registration-request-topic}-dlt", groupId = "registration-dlt-group")
   public void consume(ConsumerRecord<String, RegistrationMessage> record) {
      RegistrationMessage message = record.value();
      if (message == null) {
         log.warn("Received empty DLT message");
         return;
      }

      log.error("Received poison registration message in DLT. Request ID: {}, Partition: {}, Offset: {}",
            message.getRequestId(), record.partition(), record.offset());

      // Extract original exception message from headers if present
      String originalError = "Unknown DLT processing error";
      org.apache.kafka.common.header.Header exceptionHeader = record.headers().lastHeader("x-dlt-exception-message");
      if (exceptionHeader != null) {
         originalError = new String(exceptionHeader.value(), StandardCharsets.UTF_8);
      }

      try {
         UUID reqId = UUID.fromString(message.getRequestId());
         String payload = objectMapper.writeValueAsString(message);

         RegistrationDlt dltRecord = RegistrationDlt.builder()
               .requestId(reqId)
               .email(message.getEmail())
               .payload(payload)
               .partitionId(record.partition())
               .offsetVal(record.offset())
               .originalError(originalError)
               .status(RegistrationDltStatus.PENDING)
               .build();

         dltRepository.save(dltRecord);
         log.info("Persisted DLT message for requestId {} to database", reqId);

      } catch (Exception e) {
         log.error("Failed to persist DLT message to database for requestId: {}", message.getRequestId(), e);
      }
   }

}
