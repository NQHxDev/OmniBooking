package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registration_dlt_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationDltAudit {

   @Id
   private UUID id;

   @PrePersist
   public void prePersist() {
      if (this.id == null) {
         this.id = UuidCreator.getTimeOrderedEpoch();
      }
   }

   @Column(name = "request_id", nullable = false)
   private UUID requestId;

   @Column(name = "replayed_by", nullable = false, length = 255)
   private String replayedBy;

   @Builder.Default
   @Column(name = "replayed_at", nullable = false)
   private Instant replayedAt = Instant.now();

   @Column(name = "original_error")
   private String originalError;

   @Column(name = "replay_result", nullable = false, length = 50)
   private String replayResult;

   @Column(name = "error_message")
   private String errorMessage;

}
