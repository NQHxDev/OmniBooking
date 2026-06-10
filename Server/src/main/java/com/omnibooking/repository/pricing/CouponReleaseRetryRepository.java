package com.omnibooking.repository.pricing;

import com.omnibooking.model.CouponReleaseRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponReleaseRetryRepository extends JpaRepository<CouponReleaseRetry, UUID> {

   @Query("SELECT r FROM CouponReleaseRetry r WHERE r.status = :status AND r.nextAttemptAt <= :now")
   List<CouponReleaseRetry> findPendingRetries(@Param("status") String status, @Param("now") Instant now);

   Optional<CouponReleaseRetry> findByBookingIdAndCouponIdAndStatus(UUID bookingId, UUID couponId, String status);

   long countByStatus(String status);

   @Modifying(clearAutomatically = true)
   @Query("DELETE FROM CouponReleaseRetry r WHERE r.status = :status AND r.lastAttemptAt < :cutoff")
   int deleteByStatusAndLastAttemptAtBefore(@Param("status") String status, @Param("cutoff") Instant cutoff);
}
