package com.omnibooking.model;

import com.omnibooking.model.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OutboxEvent extends BaseEntity {

   @Column(name = "aggregate_id", nullable = false)
   private UUID aggregateId;

   @Column(name = "aggregate_type", nullable = false, length = 50)
   private String aggregateType;

   @Column(name = "event_type", nullable = false, length = 100)
   private String eventType;

   @Column(nullable = false, columnDefinition = "TEXT")
   private String payload;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private OutboxStatus status = OutboxStatus.PENDING;

   @Builder.Default
   @Column(name = "retry_count", nullable = false)
   private Integer retryCount = 0;

   @Builder.Default
   @Column(name = "next_retry_at", nullable = false)
   private Instant nextRetryAt = Instant.now();

   @Column(name = "last_error", columnDefinition = "TEXT")
   private String lastError;

   @Builder.Default
   @Column(name = "event_version", nullable = false)
   private Integer eventVersion = 1;

}
