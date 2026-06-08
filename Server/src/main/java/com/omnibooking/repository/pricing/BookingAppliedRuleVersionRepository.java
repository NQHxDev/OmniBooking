package com.omnibooking.repository.pricing;

import com.omnibooking.model.BookingAppliedRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingAppliedRuleVersionRepository extends JpaRepository<BookingAppliedRuleVersion, UUID> {

   List<BookingAppliedRuleVersion> findByBookingPriceBreakdownId(UUID breakdownId);

}
