package com.omnibooking.services.booking.impl;

import com.omnibooking.exception.AppException;
import com.omnibooking.model.Booking;
import com.omnibooking.model.InventoryOperation;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.enums.OperationType;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.services.booking.InventoryService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

   private final RoomAvailabilityRepository roomAvailabilityRepository;
   private final InventoryOperationRepository inventoryOperationRepository;
   private final Counter inventoryReservationCounter;
   private final Counter inventoryReleaseCounter;

   @Override
   @Transactional
   public void reserveInventory(Booking booking, RoomType roomType,
                                LocalDate checkIn, LocalDate checkOut, int numRooms) {
      LocalDate date = checkIn;
      while (date.isBefore(checkOut)) {
         final LocalDate finalDate = date;
         RoomAvailability availability = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDateWithLock(roomType.getId(), date)
            .orElseGet(() -> {
               RoomAvailability init = RoomAvailability.builder()
                  .roomType(roomType)
                  .availabilityDate(finalDate)
                  .availableCount(roomType.getTotalRooms())
                  .isClosed(false)
                  .build();
               return roomAvailabilityRepository.save(init);
            });

         if (Boolean.TRUE.equals(availability.getIsClosed())
               || availability.getAvailableCount() < numRooms) {
            throw new AppException("BOOKING_001",
               "Room not available for date: " + date, HttpStatus.BAD_REQUEST);
         }

         availability.setAvailableCount(availability.getAvailableCount() - numRooms);
         roomAvailabilityRepository.save(availability);

         // Audit ledger entry
         inventoryOperationRepository.save(InventoryOperation.builder()
            .booking(booking)
            .roomType(roomType)
            .availabilityDate(date)
            .operationType(OperationType.RESERVE)
            .numRooms(numRooms)
            .build());

         date = date.plusDays(1);
      }

      inventoryReservationCounter.increment();
   }

   @Override
   @Transactional
   public void releaseInventory(Booking booking) {
      // NOTE: Caller guarantees this is only called after a successful
      // atomic status transition (e.g. atomicExpireBooking returned 1).
      // The status transition IS the idempotency mechanism.
      // The ledger is audit-only.

      LocalDate date = booking.getCheckInDate();
      while (date.isBefore(booking.getCheckOutDate())) {
         // Restore availability atomically
         roomAvailabilityRepository.incrementAvailability(
            booking.getRoomType().getId(), date, booking.getNumRooms());

         // Audit ledger entry (record-keeping, not guard)
         inventoryOperationRepository.save(InventoryOperation.builder()
            .booking(booking)
            .roomType(booking.getRoomType())
            .availabilityDate(date)
            .operationType(OperationType.RELEASE)
            .numRooms(booking.getNumRooms())
            .build());

         date = date.plusDays(1);
      }

      inventoryReleaseCounter.increment();
   }
}
