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
@Table(name = "property_created_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PropertyCreatedOutbox extends BaseEntity {

   @Column(name = "property_id", nullable = false, unique = true)
   private UUID propertyId;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 30)
   private OutboxStatus status = OutboxStatus.PENDING;

   @Builder.Default
   @Column(name = "retry_count", nullable = false)
   private Integer retryCount = 0;

   @Column(name = "last_error", length = 1000)
   private String lastError;

   @Column(name = "next_retry_at")
   private Instant nextRetryAt;

   @Column(name = "lease_until")
   private Instant leaseUntil;

}
