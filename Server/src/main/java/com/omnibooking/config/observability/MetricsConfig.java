package com.omnibooking.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MetricsConfig {

   @Bean
   public io.micrometer.core.instrument.binder.MeterBinder outboxQueueSizeBinder(JdbcTemplate jdbcTemplate) {
      return registry -> Gauge.builder("omnibooking.outbox.queue.size", () -> {
         try {
            return jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM outbox_events WHERE processed = false", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Number of pending events in transactional outbox")
      .register(registry);
   }

   @Bean
   public Counter kafkaDlqCounter(MeterRegistry registry) {
      return Counter.builder("omnibooking.kafka.dlq.messages")
            .description("Total number of messages sent to Dead Letter Queue")
            .register(registry);
   }

   @Bean
   public Counter kafkaRetryCounter(MeterRegistry registry) {
      return Counter.builder("omnibooking.kafka.message.retry")
            .description("Total number of retried Kafka messages")
            .register(registry);
   }

   @Bean
   public Timer kafkaProcessingTimer(MeterRegistry registry) {
      return Timer.builder("omnibooking.kafka.message.processing.latency")
            .description("Time taken to process a Kafka message")
            .publishPercentileHistogram()
            .register(registry);
   }
}
