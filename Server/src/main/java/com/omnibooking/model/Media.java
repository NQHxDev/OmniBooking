package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Media extends BaseEntity {

   @Column(nullable = false, columnDefinition = "TEXT")
   private String url;

   @Column(name = "public_id", nullable = false)
   private String publicId;

   private String format;

   @Column(name = "resource_type")
   private String resourceType;

   private Long bytes;

   @Column(name = "entity_id", nullable = false)
   private UUID entityId;

   @Column(name = "entity_type", nullable = false, length = 50)
   private String entityType;

   @Builder.Default
   @Column(name = "is_main")
   private Boolean isMain = false;

}
