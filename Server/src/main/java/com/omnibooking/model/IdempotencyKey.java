package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.omnibooking.model.enums.IdempotencyStatus;
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
@Table(name = "idempotency_keys", uniqueConstraints = {
      @UniqueConstraint(name = "uq_endpoint_idempotency_key", columnNames = { "endpoint", "idempotency_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

   @Id
   private UUID id;

   @Column(name = "idempotency_key", nullable = false)
   private String idempotencyKey;

   @Column(nullable = false)
   private String endpoint;

   @Column(name = "request_hash", nullable = false)
   private String requestHash;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(name = "response_payload", columnDefinition = "jsonb")
   private String responsePayload;

   @Column(name = "response_status")
   private Integer responseStatus;

   @Enumerated(EnumType.STRING)
   @Column(name = "processing_status", nullable = false, length = 50)
   private IdempotencyStatus processingStatus;

   @Column(name = "response_cached", nullable = false)
   @Builder.Default
   private boolean responseCached = true;

   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;

   @Column(name = "processing_started_at", nullable = false)
   private Instant processingStartedAt;

}
