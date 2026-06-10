package com.omnibooking.model;

import com.omnibooking.model.enums.TransactionStatus;
import com.omnibooking.model.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "transactions", uniqueConstraints = {
   @UniqueConstraint(name = "uq_transactions_provider_order_id", columnNames = {"payment_method", "provider_order_id"}),
   @UniqueConstraint(name = "uq_transactions_provider_tx_id", columnNames = {"payment_method", "provider_transaction_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Transaction extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "booking_id", nullable = false)
   private Booking booking;

   @Column(nullable = false)
   private BigDecimal amount;

   @Enumerated(EnumType.STRING)
   @Column(name = "transaction_type", nullable = false, length = 20)
   private TransactionType transactionType;

   @Column(name = "payment_method", length = 50)
   private String paymentMethod;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private TransactionStatus status = TransactionStatus.PENDING;

   @Column(name = "provider_order_id")
   private String providerOrderId;

   @Column(name = "provider_transaction_id")
   private String providerTransactionId;

   @Column(name = "local_amount", precision = 19, scale = 4)
   private BigDecimal localAmount;

   @Column(name = "local_currency", length = 3)
   private String localCurrency;

   @JdbcTypeCode(SqlTypes.JSON)
   @Column(columnDefinition = "jsonb")
   private String metadata;

}
