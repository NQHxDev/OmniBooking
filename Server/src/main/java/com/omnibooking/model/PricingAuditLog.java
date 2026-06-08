package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pricing_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingAuditLog {

   @Id
   private UUID id;

   @Column(name = "entity_type", nullable = false, length = 20)
   private String entityType;

   @Column(name = "entity_id", nullable = false)
   private UUID entityId;

   @Column(name = "operation_type", nullable = false, length = 20)
   private String operationType;

   @Column(name = "actor_id", nullable = false)
   private UUID actorId;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(name = "old_values", columnDefinition = "jsonb")
   private String oldValues;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(name = "new_values", columnDefinition = "jsonb")
   private String newValues;

   @Column(name = "correlation_id", nullable = false)
   private UUID correlationId;

   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @PrePersist
   public void prePersist() {
      if (this.id == null) {
         this.id = UuidCreator.getTimeOrderedEpoch();
      }
      if (this.createdAt == null) {
         this.createdAt = Instant.now();
      }
   }

}
