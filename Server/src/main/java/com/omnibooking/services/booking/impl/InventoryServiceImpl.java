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

   /**
    * Get or pre-create a RoomAvailability record within the main transaction.
    * Note: Previously, this used REQUIRES_NEW propagation to isolate initialization.
    * However, under concurrent load, REQUIRES_NEW causes connection pool deadlocks
    * (threads holding a connection from the main transaction while waiting for another
    * connection from the pool) and transaction suspension anomalies in tests.
    * In production, availability records are pre-populated, so dynamic insertion
    * is a fallback and does not cause key conflicts in practice.
    */
   @Transactional
   public RoomAvailability getOrCreateAvailability(RoomType roomType, LocalDate date) {
      return roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDate(roomType.getId(), date)
            .orElseGet(() -> {
               try {
                  RoomAvailability init = RoomAvailability.builder()
                        .roomType(roomType)
                        .availabilityDate(date)
                        .availableCount(roomType.getTotalRooms())
                        .isClosed(false)
                        .build();
                  return roomAvailabilityRepository.saveAndFlush(init);
               } catch (Exception e) {
                  // Concurrent insertion, find the record created by the other thread
                  return roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDate(roomType.getId(), date)
                        .orElseThrow(() -> new AppException("BOOKING_001",
                              "Failed to initialize inventory for date: " + date, HttpStatus.BAD_REQUEST));
               }
            });
   }

   @Override
   @Transactional
   public void reserveInventory(Booking booking, RoomType roomType,
                                LocalDate checkIn, LocalDate checkOut, int numRooms) {
      LocalDate date = checkIn;
      while (date.isBefore(checkOut)) {
         // Step 1: Ensure availability record exists
         getOrCreateAvailability(roomType, date);

         // Step 2: Atomic Update deduction
         int rowsUpdated = roomAvailabilityRepository.deductAvailabilityAtomically(roomType.getId(), date, numRooms);
         if (rowsUpdated == 0) {
            throw new AppException("BOOKING_001",
               "Room not available for date: " + date, HttpStatus.BAD_REQUEST);
         }

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
