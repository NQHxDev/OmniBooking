package com.omnibooking.repository.infra;

import com.omnibooking.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

   Optional<IdempotencyKey> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);

   @Modifying
   @Transactional
   @Query(value = "INSERT INTO idempotency_keys (id, idempotency_key, endpoint, request_hash, processing_status, created_at, expires_at, processing_started_at, response_cached) "
         +
         "VALUES (:id, :key, :endpoint, :hash, 'PROCESSING', :now, :expiresAt, :now, true)", nativeQuery = true)
   int insertIdempotencyKey(@Param("id") UUID id,
         @Param("key") String key,
         @Param("endpoint") String endpoint,
         @Param("hash") String hash,
         @Param("now") Instant now,
         @Param("expiresAt") Instant expiresAt);

   @Modifying
   @Transactional
   @Query("UPDATE IdempotencyKey i SET i.processingStatus = com.omnibooking.model.enums.IdempotencyStatus.PROCESSING, i.processingStartedAt = :now "
         +
         "WHERE i.idempotencyKey = :key AND i.endpoint = :endpoint AND i.processingStatus = com.omnibooking.model.enums.IdempotencyStatus.PROCESSING AND i.processingStartedAt < :staleTime AND i.requestHash = :hash")
   int reclaimStaleKey(@Param("key") String key, @Param("endpoint") String endpoint, @Param("hash") String hash,
         @Param("now") Instant now, @Param("staleTime") Instant staleTime);

   @Modifying
   @Transactional
   @Query("UPDATE IdempotencyKey i SET i.processingStatus = com.omnibooking.model.enums.IdempotencyStatus.PROCESSING, i.processingStartedAt = :now "
         +
         "WHERE i.idempotencyKey = :key AND i.endpoint = :endpoint AND i.processingStatus = com.omnibooking.model.enums.IdempotencyStatus.FAILED AND i.requestHash = :hash")
   int reclaimFailedKey(@Param("key") String key, @Param("endpoint") String endpoint, @Param("hash") String hash,
         @Param("now") Instant now);

   @Modifying
   @Transactional
   @Query(value = "DELETE FROM idempotency_keys WHERE id IN (" +
         "SELECT k.id FROM idempotency_keys k WHERE k.expires_at < :now ORDER BY k.expires_at ASC LIMIT :batchSize)", nativeQuery = true)
   int deleteExpiredKeysBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

}
