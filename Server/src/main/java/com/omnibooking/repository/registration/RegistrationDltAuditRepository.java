package com.omnibooking.repository.registration;

import com.omnibooking.model.RegistrationDltAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegistrationDltAuditRepository extends JpaRepository<RegistrationDltAudit, UUID> {
}
