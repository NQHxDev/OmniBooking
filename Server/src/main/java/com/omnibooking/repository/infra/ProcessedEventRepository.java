package com.omnibooking.repository.infra;

import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.model.enums.IdempotencyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT p FROM ProcessedEvent p WHERE p.eventId = :eventId AND p.consumerGroup = :consumerGroup")
   Optional<ProcessedEvent> findByIdForWrite(@Param("eventId") UUID eventId, @Param("consumerGroup") String consumerGroup);

   List<ProcessedEvent> findByStatusAndUpdatedAtBefore(IdempotencyStatus status, Instant threshold);

   List<ProcessedEvent> findByStatusAndLeaseUntilBefore(IdempotencyStatus status, Instant threshold);

   @Modifying
   @Query("DELETE FROM ProcessedEvent p WHERE p.status IN :statuses AND p.updatedAt < :threshold")
   int deleteOldEvents(@Param("statuses") List<IdempotencyStatus> statuses, @Param("threshold") Instant threshold);

   @Modifying
   @Query("UPDATE ProcessedEvent p SET p.leaseUntil = :newLeaseUntil, p.updatedAt = :now WHERE p.eventId = :eventId AND p.consumerGroup = :consumerGroup AND p.status = com.omnibooking.model.enums.IdempotencyStatus.PROCESSING AND p.leaseUntil > :now")
   int renewLeaseOpt(@Param("eventId") UUID eventId, @Param("consumerGroup") String consumerGroup, @Param("newLeaseUntil") Instant newLeaseUntil, @Param("now") Instant now);

   @Modifying
   @Query("UPDATE ProcessedEvent p SET p.status = com.omnibooking.model.enums.IdempotencyStatus.FAILED, p.leaseUntil = :now, p.updatedAt = :now WHERE p.eventId = :eventId AND p.consumerGroup = :consumerGroup AND p.status = com.omnibooking.model.enums.IdempotencyStatus.PROCESSING AND p.leaseUntil < :now")
   int recoverStaleEvent(@Param("eventId") UUID eventId, @Param("consumerGroup") String consumerGroup, @Param("now") Instant now);

}
