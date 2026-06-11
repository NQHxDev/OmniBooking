package com.omnibooking.services.payment;

import com.omnibooking.model.Transaction;
import com.omnibooking.model.enums.TransactionStatus;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

   private static final Map<TransactionStatus, Set<TransactionStatus>> TRANSITIONS = Map.of(
         TransactionStatus.PENDING,
         Set.of(TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.CANCELLED),
         TransactionStatus.SUCCESS, Set.of(TransactionStatus.REFUNDED, TransactionStatus.VOIDED));

   /**
    * Transitions a transaction's status to a target state, validating the legality
    * of the transition.
    *
    * @param transaction the transaction to transition
    * @param target      the target TransactionStatus
    * @throws IllegalStateException if the transition is not allowed
    */
   public void transition(Transaction transaction, TransactionStatus target) {
      TransactionStatus current = transaction.getStatus();

      if (current == target) {
         return;
      }

      Set<TransactionStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
      if (!allowed.contains(target)) {
         throw new IllegalStateException(
               "Illegal payment transaction status transition: " + current + " -> " + target);
      }

      transaction.setStatus(target);
   }

}
