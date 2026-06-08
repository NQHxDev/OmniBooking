package com.omnibooking.services.pricing;

import java.util.UUID;

public interface AuditLogService {

   void logChange(String entityType, UUID entityId, String operationType, UUID actorId, Object oldValues,
         Object newValues);

}
