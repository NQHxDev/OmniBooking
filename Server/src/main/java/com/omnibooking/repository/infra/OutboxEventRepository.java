package com.omnibooking.repository.infra;

import com.omnibooking.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

   @Query(value = "SELECT * FROM outbox_events WHERE status IN ('PENDING', 'PROCESSING') AND next_retry_at <= :now ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", nativeQuery = true)
   List<OutboxEvent> findEventsToProcess(@Param("now") Instant now, Pageable pageable);

   @Modifying
   @Query("DELETE FROM OutboxEvent o WHERE o.status = com.omnibooking.model.enums.OutboxStatus.PROCESSED AND o.createdAt < :threshold")
   int deleteProcessedEventsBefore(@Param("threshold") Instant threshold);

}
