package com.omnibooking.repository;

import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegistrationInboxRepository extends JpaRepository<RegistrationInbox, UUID> {

   @Query(value = "SELECT * FROM registration_inbox WHERE status = 'PENDING' AND created_at <= :threshold ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", nativeQuery = true)
   List<RegistrationInbox> findPendingToProcess(@Param("threshold") Instant threshold, Pageable pageable);

   @Query(value = "SELECT * FROM registration_inbox WHERE status = 'PROCESSING' AND created_at <= :threshold ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", nativeQuery = true)
   List<RegistrationInbox> findStaleProcessingToRecover(@Param("threshold") Instant threshold, Pageable pageable);

   @Modifying
   @Query("DELETE FROM RegistrationInbox r WHERE r.status = :status AND r.createdAt < :threshold")
   int deleteByStatusAndCreatedAtBefore(@Param("status") RegistrationInboxStatus status, @Param("threshold") Instant threshold);

}
