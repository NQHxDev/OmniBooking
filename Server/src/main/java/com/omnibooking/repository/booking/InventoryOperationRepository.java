package com.omnibooking.repository.booking;

import com.omnibooking.model.InventoryOperation;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryOperationRepository extends JpaRepository<InventoryOperation, UUID> {

   @Query("SELECT io.booking.id, io.roomType.id, io.availabilityDate " +
         "FROM InventoryOperation io " +
         "WHERE io.operationType = :reserveType " +
         "AND io.booking.status IN :statuses " +
         "AND NOT EXISTS (SELECT 1 FROM InventoryOperation io2 " +
         "WHERE io2.booking.id = io.booking.id " +
         "AND io2.roomType.id = io.roomType.id " +
         "AND io2.availabilityDate = io.availabilityDate " +
         "AND io2.operationType = :releaseType)")
   List<Object[]> findBookingsWithReserveButNoRelease(
         @Param("reserveType") OperationType reserveType,
         @Param("releaseType") OperationType releaseType,
         @Param("statuses") List<BookingStatus> statuses);

}
