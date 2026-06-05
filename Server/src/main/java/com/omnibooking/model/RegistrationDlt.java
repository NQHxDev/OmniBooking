package com.omnibooking.model;

import com.omnibooking.model.enums.RegistrationDltStatus;
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
@Table(name = "registration_dlt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationDlt {

   @Id
   @Column(name = "request_id")
   private UUID requestId;

   @Column(nullable = false, length = 255)
   private String email;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(nullable = false, columnDefinition = "jsonb")
   private String payload;

   @Column(name = "partition_id", nullable = false)
   private Integer partitionId;

   @Column(name = "offset_val", nullable = false)
   private Long offsetVal;

   @Column(name = "original_error")
   private String originalError;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private RegistrationDltStatus status;

   @Builder.Default
   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt = Instant.now();

   @Column(name = "last_replayed_at")
   private Instant lastReplayedAt;

}
