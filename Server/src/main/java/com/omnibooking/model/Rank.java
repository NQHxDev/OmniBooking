package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ranks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rank extends BaseEntity {

   @Column(nullable = false, unique = true, length = 50)
   private String name;

   @Column(name = "min_points", nullable = false)
   private Integer minPoints;

   @Column(columnDefinition = "TEXT")
   private String benefits;

}
