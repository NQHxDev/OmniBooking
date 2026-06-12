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

   @Bean
   public Counter idempotencyHitCounter(MeterRegistry r) {
      return Counter.builder("idempotency.hit")
            .description("Total idempotency cache hits").register(r);
   }

   @Bean
   public Counter idempotencyMissCounter(MeterRegistry r) {
      return Counter.builder("idempotency.miss")
            .description("Total idempotency cache misses").register(r);
   }

   @Bean
   public Counter idempotencyConflictCounter(MeterRegistry r) {
      return Counter.builder("idempotency.conflict")
            .description("Total idempotency conflicts detected").register(r);
   }

   @Bean
   public Counter idempotencyProcessingCounter(MeterRegistry r) {
      return Counter.builder("idempotency.processing")
            .description("Total idempotency keys currently processing").register(r);
   }

   @Bean
   public Counter idempotencyReclaimedCounter(MeterRegistry r) {
      return Counter.builder("idempotency.reclaimed")
            .description("Total idempotency keys reclaimed from stale or failed status").register(r);
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

   @Bean
   public Counter reconciliationCouponLeakCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.reconciliation.coupon_leak.total")
            .description("Total coupon leak anomalies detected").register(r);
   }

   @Bean
   public Counter couponReleaseRetryCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.coupon.release.retry.total")
            .description("Total coupon release retry attempts").register(r);
   }

   @Bean
   public Counter couponReleaseRetrySuccessCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.coupon.release.retry.success.total")
            .description("Total successful coupon release retries").register(r);
   }

   @Bean
   public Counter couponReleaseRetryFailureCounter(MeterRegistry r) {
      return Counter.builder("omnibooking.coupon.release.retry.failure.total")
            .description("Total failed coupon release retries").register(r);
   }

   @Bean
   public MeterBinder couponReleasePendingBinder(com.omnibooking.repository.pricing.CouponReleaseRetryRepository couponReleaseRetryRepository) {
      return registry -> Gauge.builder("omnibooking.coupon.release.pending.total", () -> {
         try {
            return couponReleaseRetryRepository.countByStatus("PENDING");
         } catch (Exception e) {
            return 0;
         }
      })
            .description("Number of pending coupon release retries")
            .register(registry);
   }

   @Bean
   public Counter propertyCreatedCounter(MeterRegistry r) {
      return Counter.builder("property_created_total")
            .description("Total number of properties created").register(r);
   }

   @Bean
   public Counter availabilityGenerationSuccessCounter(MeterRegistry r) {
      return Counter.builder("availability_generation_success_total")
            .description("Total number of successful availability generations").register(r);
   }

   @Bean
   public Counter availabilityGenerationFailedCounter(MeterRegistry r) {
      return Counter.builder("availability_generation_failed_total")
            .description("Total number of failed availability generations").register(r);
   }

   @Bean
   public Counter availabilityRegenerationCounter(MeterRegistry r) {
      return Counter.builder("availability_regeneration_total")
            .description("Total number of availability regenerations via reconciliation").register(r);
   }

   @Bean
   public Timer availabilityGenerationDurationTimer(MeterRegistry r) {
      return Timer.builder("availability_generation_duration")
            .description("Duration of availability generation execution")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(r);
   }

   @Bean
   public Counter propertySetupSuccessCounter(MeterRegistry r) {
      return Counter.builder("property_setup_success_total")
            .description("Total number of properties successfully set up").register(r);
   }

   @Bean
   public Counter propertySetupFailedCounter(MeterRegistry r) {
      return Counter.builder("property_setup_failed_total")
            .description("Total number of property setup failures (retry attempts)").register(r);
   }

   @Bean
   public Counter propertySetupDeadCounter(MeterRegistry r) {
      return Counter.builder("property_setup_dead_total")
            .description("Total number of property setup dead/final failures").register(r);
   }

   @Bean
   public Timer propertySetupDurationTimer(MeterRegistry r) {
      return Timer.builder("property_setup_duration_seconds")
            .description("Duration of property setup execution in seconds")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(r);
   }

}
