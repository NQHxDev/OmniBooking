package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import com.omnibooking.model.enums.IdempotencyStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEvent.ProcessedEventId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

   @Id
   @Column(name = "event_id", nullable = false)
   private UUID eventId;

   @Id
   @Column(name = "consumer_group", nullable = false, length = 100)
   private String consumerGroup;

   @Column(name = "processed_at", nullable = false)
   @Builder.Default
   private Instant processedAt = Instant.now();

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false, length = 20)
   @Builder.Default
   private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

   @Column(name = "updated_at", nullable = false)
   @Builder.Default
   private Instant updatedAt = Instant.now();

   @Column(name = "lease_until")
   private Instant leaseUntil;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public static class ProcessedEventId implements Serializable {
      private UUID eventId;
      private String consumerGroup;
   }
}
