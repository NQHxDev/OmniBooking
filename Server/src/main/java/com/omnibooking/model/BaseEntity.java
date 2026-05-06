package com.omnibooking.model;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

   @Id
   private UUID id;

   @PrePersist
   public void prePersist() {
      if (this.id == null) {
         this.id = UuidCreator.getTimeOrderedEpoch();
      }
   }

   @Version
   private Long version;

   @CreatedDate
   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt;

   @LastModifiedDate
   @Column(name = "updated_at", nullable = false)
   private Instant updatedAt;

   @Column(name = "deleted_at")
   private Instant deletedAt;

}
