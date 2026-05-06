package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.ZonedDateTime;
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

   @Version
   private Long version;

   @CreatedDate
   @Column(name = "created_at", nullable = false, updatable = false)
   private ZonedDateTime createdAt;

   @LastModifiedDate
   @Column(name = "updated_at", nullable = false)
   private ZonedDateTime updatedAt;

   @Column(name = "deleted_at")
   private ZonedDateTime deleted_at;

}
