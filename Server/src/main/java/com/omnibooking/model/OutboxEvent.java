package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

   @Column(name = "payload_class", nullable = false)
   private String payloadClass;

   @Builder.Default
   @Column(nullable = false)
   private Boolean processed = false;

   @Column(name = "processed_at")
   private Instant processedAt;

}
