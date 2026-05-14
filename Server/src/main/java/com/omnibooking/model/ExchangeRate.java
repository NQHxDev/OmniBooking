package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExchangeRate extends BaseEntity {

   @Column(name = "from_currency", nullable = false, length = 3)
   private String fromCurrency; // Always USD in this setup

   @Column(name = "to_currency", nullable = false, length = 3)
   private String toCurrency;

   @Column(nullable = false, precision = 18, scale = 6)
   private BigDecimal rate;

   @Column(length = 50)
   private String provider;

   @Column(name = "fetched_at", nullable = false)
   private Instant fetchedAt;
}
