package com.omnibooking.repository.property;

import com.omnibooking.model.RoomAvailability;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, UUID> {

   Optional<RoomAvailability> findByRoomTypeIdAndAvailabilityDate(UUID roomTypeId, LocalDate availabilityDate);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT r FROM RoomAvailability r WHERE r.roomType.id = :roomTypeId AND r.availabilityDate = :availabilityDate")
   Optional<RoomAvailability> findByRoomTypeIdAndAvailabilityDateWithLock(
         @Param("roomTypeId") UUID roomTypeId,
         @Param("availabilityDate") LocalDate availabilityDate);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE RoomAvailability r SET r.availableCount = r.availableCount + :rooms " +
         "WHERE r.roomType.id = :roomTypeId AND r.availabilityDate = :date")
   int incrementAvailability(@Param("roomTypeId") UUID roomTypeId,
                             @Param("date") LocalDate date,
                             @Param("rooms") int rooms);

   @Modifying(clearAutomatically = true)
   @Query("UPDATE RoomAvailability r SET r.availableCount = r.availableCount - :rooms " +
         "WHERE r.roomType.id = :roomTypeId AND r.availabilityDate = :date " +
         "AND r.availableCount >= :rooms AND r.isClosed = false")
   int deductAvailabilityAtomically(@Param("roomTypeId") UUID roomTypeId,
                                    @Param("date") LocalDate date,
                                    @Param("rooms") int rooms);
}

