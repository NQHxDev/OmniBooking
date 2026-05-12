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
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SearchLog extends BaseEntity {

   @Column(name = "query_text", nullable = false)
   private String queryText;

   @Column(name = "country_code", length = 2)
   private String countryCode;

   @Column(name = "user_id")
   private String userId; // Optional, can be null for guests

   @Builder.Default
   @Column(name = "is_boosted", nullable = false)
   private Boolean isBoosted = false; // For manual trending promotion
}
