package com.omnibooking.repository.infra;

import com.omnibooking.model.PropertyCreatedOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PropertyCreatedOutboxRepository extends JpaRepository<PropertyCreatedOutbox, UUID> {

   @Query(value = "SELECT * FROM property_created_outbox WHERE " +
         "((status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now)) " +
         "OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= :now))) " +
         "ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", nativeQuery = true)
   List<PropertyCreatedOutbox> findEventsToProcess(@Param("now") Instant now, Pageable pageable);

   @Modifying
   @Query("DELETE FROM PropertyCreatedOutbox o WHERE o.status = 'PROCESSED' AND o.updatedAt < :threshold")
   int deleteProcessedEventsBefore(@Param("threshold") Instant threshold);

}
