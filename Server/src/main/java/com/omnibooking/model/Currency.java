package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Currency extends BaseEntity {

   @Column(nullable = false, unique = true, length = 3)
   private String code; // e.g., USD, VND

   @Column(nullable = false, length = 50)
   private String name;

   @Column(nullable = false, length = 10)
   private String symbol;

   @Builder.Default
   @Column(name = "is_active", nullable = false)
   private boolean isActive = true;

   @Builder.Default
   @Column(name = "is_base", nullable = false)
   private boolean isBase = false;

}
