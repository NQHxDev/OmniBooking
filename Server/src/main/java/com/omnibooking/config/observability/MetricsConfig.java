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
                  "SELECT COUNT(*) FROM outbox_events WHERE status IN ('PENDING', 'PROCESSING')", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Number of pending events in transactional outbox")
      .register(registry);
   }

   @Bean
   public io.micrometer.core.instrument.binder.MeterBinder outboxPendingCountBinder(JdbcTemplate jdbcTemplate) {
      return registry -> Gauge.builder("omnibooking.outbox.pending.count", () -> {
         try {
            return jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING'", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Number of pending events in transactional outbox")
      .register(registry);
   }

   @Bean
   public io.micrometer.core.instrument.binder.MeterBinder outboxProcessingCountBinder(JdbcTemplate jdbcTemplate) {
      return registry -> Gauge.builder("omnibooking.outbox.processing.count", () -> {
         try {
            return jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM outbox_events WHERE status = 'PROCESSING'", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Number of processing events in transactional outbox")
      .register(registry);
   }

   @Bean
   public io.micrometer.core.instrument.binder.MeterBinder outboxDeadCountBinder(JdbcTemplate jdbcTemplate) {
      return registry -> Gauge.builder("omnibooking.outbox.dead.count", () -> {
         try {
            return jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM outbox_events WHERE status = 'DEAD'", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Number of dead/failed events in transactional outbox")
      .register(registry);
   }

   @Bean
   public io.micrometer.core.instrument.binder.MeterBinder outboxRetryCountBinder(JdbcTemplate jdbcTemplate) {
      return registry -> Gauge.builder("omnibooking.outbox.retry.count", () -> {
         try {
            return jdbcTemplate.queryForObject(
                  "SELECT COALESCE(SUM(retry_count), 0) FROM outbox_events", Integer.class);
         } catch (Exception e) {
            return 0;
         }
      })
      .description("Total retry attempts across all events in transactional outbox")
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

   @Bean
   public Counter kafkaConsumerDuplicateCounter(MeterRegistry registry) {
      return Counter.builder("omnibooking.kafka.consumer.duplicate")
            .description("Total number of duplicate Kafka events detected")
            .register(registry);
   }

   @Bean
   public Counter kafkaConsumerSkippedCounter(MeterRegistry registry) {
      return Counter.builder("omnibooking.kafka.consumer.skipped")
            .description("Total number of skipped Kafka events due to idempotency")
            .register(registry);
   }
}
