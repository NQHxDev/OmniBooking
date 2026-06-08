package com.omnibooking.repository.pricing;

import com.omnibooking.model.PricingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PricingAuditLogRepository extends JpaRepository<PricingAuditLog, UUID> {

   List<PricingAuditLog> findByCorrelationId(UUID correlationId);

   List<PricingAuditLog> findByEntityId(UUID entityId);

}
