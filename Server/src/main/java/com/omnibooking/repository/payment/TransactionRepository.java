package com.omnibooking.repository.payment;

import com.omnibooking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

import com.omnibooking.model.enums.TransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

   @Query("SELECT t.booking.id, t.id, t.providerTransactionId " +
         "FROM Transaction t " +
         "WHERE t.status = com.omnibooking.model.enums.TransactionStatus.SUCCESS " +
         "AND t.booking.status = com.omnibooking.model.enums.BookingStatus.PENDING_PAYMENT")
   List<Object[]> findSuccessTransactionsWithNonConfirmedBookings();

   Optional<Transaction> findByProviderOrderId(String providerOrderId);

   Optional<Transaction> findByPaymentMethodAndProviderTransactionId(String paymentMethod, String providerTransactionId);

   List<Transaction> findByBookingIdAndStatus(UUID bookingId, TransactionStatus status);

   @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt < :stuckThreshold AND t.createdAt > :maxAgeThreshold")
   List<Transaction> findPendingTransactionsForReconciliation(
         @Param("status") TransactionStatus status,
         @Param("stuckThreshold") Instant stuckThreshold,
         @Param("maxAgeThreshold") Instant maxAgeThreshold,
         Pageable pageable);

}
