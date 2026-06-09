package com.omnibooking.repository.infra;

import com.omnibooking.model.OutboxEvent;
import com.omnibooking.model.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository dedicated to outbox metrics collection.
 * Separated from {@link OutboxEventRepository} to isolate monitoring concerns
 * from business event processing operations.
 */
@Repository
public interface OutboxMetricsRepository extends JpaRepository<OutboxEvent, UUID> {

   long countByStatusIn(List<OutboxStatus> statuses);

   long countByStatus(OutboxStatus status);

   @Query("SELECT COALESCE(SUM(o.retryCount), 0) FROM OutboxEvent o")
   long sumRetryCount();

}
