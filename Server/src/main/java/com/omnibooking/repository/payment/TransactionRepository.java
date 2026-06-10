package com.omnibooking.repository.payment;

import com.omnibooking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

   @Query("SELECT t.booking.id, t.id, t.providerTransactionId " +
         "FROM Transaction t " +
         "WHERE t.status = com.omnibooking.model.enums.TransactionStatus.SUCCESS " +
         "AND t.booking.status = com.omnibooking.model.enums.BookingStatus.PENDING_PAYMENT")
   List<Object[]> findSuccessTransactionsWithNonConfirmedBookings();

}
