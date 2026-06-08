package com.omnibooking.repository.pricing;

import com.omnibooking.model.BookingPriceBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingPriceBreakdownRepository extends JpaRepository<BookingPriceBreakdown, UUID> {

   List<BookingPriceBreakdown> findByBookingId(UUID bookingId);

}
