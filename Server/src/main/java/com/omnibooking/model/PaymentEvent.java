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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentEvent extends BaseEntity {

   @Column(name = "transaction_id")
   private UUID transactionId;

   @Column(name = "booking_id")
   private UUID bookingId;

   @Column(name = "event_type", nullable = false, length = 50)
   private String eventType;

   @Builder.Default
   @Column(name = "event_timestamp", nullable = false)
   private Instant eventTimestamp = Instant.now();

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(columnDefinition = "jsonb")
   private String metadata;

}
