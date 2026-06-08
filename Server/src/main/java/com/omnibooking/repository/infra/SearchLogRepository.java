package com.omnibooking.repository.infra;

import com.omnibooking.model.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {

   @Query("SELECT sl.queryText FROM SearchLog sl " +
         "WHERE sl.createdAt >= :since " +
         "AND (:countryCode IS NULL OR sl.countryCode = :countryCode) " +
         "AND sl.deletedAt IS NULL " +
         "GROUP BY sl.queryText " +
         "ORDER BY MAX(CASE WHEN sl.isBoosted = true THEN 1 ELSE 0 END) DESC, COUNT(sl.id) DESC")
   List<String> findTopQueries(@Param("since") Instant since,
         @Param("countryCode") String countryCode,
         Pageable pageable);

}
