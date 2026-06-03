package com.omnibooking.repository;

import com.omnibooking.model.BookingStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BookingStatusLogRepository extends JpaRepository<BookingStatusLog, UUID> {
}
