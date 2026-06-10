package com.omnibooking.config.observability;

import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.repository.infra.OutboxMetricsRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MetricsConfig {

   @Bean
   public MeterBinder outboxQueueSizeBinder(OutboxMetricsRepository outboxMetricsRepository) {
      return registry -> Gauge.builder("omnibooking.outbox.queue.size", () -> {
         try {
            return outboxMetricsRepository.countByStatusIn(
                  List.of(OutboxStatus.PENDING, OutboxStatus.PROCESSING));
         } catch (Exception e) {
            return 0;
         }
      })
            .description("Number of pending events in transactional outbox")
            .register(registry);
   }

   @Bean
   public MeterBinder outboxPendingCountBinder(OutboxMetricsRepository outboxMetricsRepository) {
      return registry -> Gauge.builder("omnibooking.outbox.pending.count", () -> {
         try {
            return outboxMetricsRepository.countByStatus(OutboxStatus.PENDING);
         } catch (Exception e) {
            return 0;
         }
      })
            .description("Number of pending events in transactional outbox")
            .register(registry);
   }

   @Bean
   public MeterBinder outboxProcessingCountBinder(OutboxMetricsRepository outboxMetricsRepository) {
      return registry -> Gauge.builder("omnibooking.outbox.processing.count", () -> {
         try {
            return outboxMetricsRepository.countByStatus(OutboxStatus.PROCESSING);
         } catch (Exception e) {
            return 0;
         }
      })
            .description("Number of processing events in transactional outbox")
            .register(registry);
   }

   @Bean
   public MeterBinder outboxDeadCountBinder(OutboxMetricsRepository outboxMetricsRepository) {
      return registry -> Gauge.builder("omnibooking.outbox.dead.count", () -> {
         try {
            return outboxMetricsRepository.countByStatus(OutboxStatus.DEAD);
         } catch (Exception e) {
            return 0;
         }
      })
            .description("Number of dead/failed events in transactional outbox")
            .register(registry);
   }

   @Bean
   public MeterBinder outboxRetryCountBinder(OutboxMetricsRepository outboxMetricsRepository) {
      return registry -> Gauge.builder("omnibooking.outbox.retry.count", () -> {
         try {
            return outboxMetricsRepository.sumRetryCount();
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

   // --- Booking Lifecycle ---
   @Bean
   public Counter bookingCreatedCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.created.total")
            .description("Total bookings created").register(r);
   }

   @Bean
   public Counter bookingConfirmedCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.confirmed.total")
            .description("Total bookings confirmed via payment").register(r);
   }

   @Bean
   public Counter bookingExpiredCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.expired.total")
            .description("Total bookings expired by worker").register(r);
   }

   @Bean
   public Counter bookingCancelledCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.cancelled.total")
            .description("Total bookings cancelled").register(r);
   }

   // --- Inventory ---
   @Bean
   public Counter inventoryReservationCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.inventory.reservation.total")
            .description("Total inventory reservations").register(r);
   }

   @Bean
   public Counter inventoryReleaseCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.inventory.release.total")
            .description("Total inventory releases").register(r);
   }

   // --- Payment ---
   @Bean
   public Counter paymentCallbackCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.payment.callback.total")
            .description("Total payment callbacks received").register(r);
   }

   @Bean
   public Counter paymentDuplicateCallbackCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.payment.callback.duplicate.total")
            .description("Total duplicate payment callbacks detected").register(r);
   }

   // --- Worker Failures ---
   @Bean
   public Counter bookingExpirationFailureCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.expiration.failure.total")
            .description("Total booking expiration failures").register(r);
   }

   // --- Idempotency ---
   @Bean
   public Counter bookingIdempotencyHitCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.booking.idempotency.hit.total")
            .description("Idempotency cache hits for booking creation").register(r);
   }

   // --- Reconciliation ---
   @Bean
   public Counter reconciliationAnomalyCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.reconciliation.anomaly.total")
            .description("Total reconciliation anomalies detected").register(r);
   }

   @Bean
   public Counter reconciliationInventoryLeakCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.reconciliation.inventory_leak.total")
            .description("Total inventory leak anomalies detected").register(r);
   }

   @Bean
   public Counter reconciliationPaymentMismatchCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.reconciliation.payment_mismatch.total")
            .description("Total payment mismatch anomalies detected").register(r);
   }

   @Bean
   public Counter reconciliationStuckBookingCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.reconciliation.stuck_booking.total")
            .description("Total stuck booking anomalies detected").register(r);
   }

}
