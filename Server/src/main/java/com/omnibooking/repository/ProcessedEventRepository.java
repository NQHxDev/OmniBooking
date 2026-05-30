package com.omnibooking.repository;

import com.omnibooking.model.ProcessedEvent;
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

   List<ProcessedEvent> findByStatusAndUpdatedAtBefore(String status, Instant threshold);

   @Modifying
   @Query("DELETE FROM ProcessedEvent p WHERE p.status IN :statuses AND p.updatedAt < :threshold")
   int deleteOldEvents(@Param("statuses") List<String> statuses, @Param("threshold") Instant threshold);

}
