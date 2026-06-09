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

}
