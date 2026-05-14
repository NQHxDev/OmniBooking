package com.omnibooking.repository;

import com.omnibooking.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

   @Query(value = "SELECT * FROM outbox_events WHERE processed = false ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", nativeQuery = true)
   List<OutboxEvent> findUnprocessedForUpdate(Pageable pageable);

}
