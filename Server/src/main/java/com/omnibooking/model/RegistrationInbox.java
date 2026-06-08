package com.omnibooking.model;

import com.omnibooking.model.enums.RegistrationInboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "registration_inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationInbox {

   @Id
   @Column(name = "request_id")
   private UUID requestId;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(nullable = false, columnDefinition = "jsonb")
   private String payload;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private RegistrationInboxStatus status;

   @Builder.Default
   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt = Instant.now();

   @Column(name = "published_at")
   private Instant publishedAt;

   @Builder.Default
   @Column(name = "retry_count", nullable = false)
   private Integer retryCount = 0;

   @Column(name = "last_error")
   private String lastError;

   @Column(name = "next_retry_at")
   private Instant nextRetryAt;

   @Column(name = "processed_at")
   private Instant processedAt;

}
