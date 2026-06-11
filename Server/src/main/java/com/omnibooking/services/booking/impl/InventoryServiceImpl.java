package com.omnibooking.services.booking.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.exception.AppException;
import com.omnibooking.model.Booking;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.enums.OperationType;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.services.booking.InventoryService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

   private final RoomAvailabilityRepository roomAvailabilityRepository;

   private final InventoryOperationRepository inventoryOperationRepository;

   private final Counter inventoryReservationCounter;

   private final Counter inventoryReleaseCounter;

   // @formatter:off
   /**
    * Get or pre-create a RoomAvailability record within the main transaction.
    *
    * Note: Previously, this used REQUIRES_NEW propagation to isolate initialization.
    * However, under concurrent load, REQUIRES_NEW causes connection pool deadlocks
    * (threads holding a connection from the main transaction while waiting for another
    * connection from the pool) and transaction suspension anomalies in tests.
    *
    * In production, availability records are pre-populated, so dynamic insertion
    * is a fallback and does not cause key conflicts in practice.
    */
   // @formatter:on
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
   public void deductInventoryOnly(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int numRooms) {
      LocalDate date = checkIn;
      while (date.isBefore(checkOut)) {
         getOrCreateAvailability(roomType, date);
         int rowsUpdated = roomAvailabilityRepository.deductAvailabilityAtomically(roomType.getId(), date, numRooms);
         if (rowsUpdated == 0) {
            throw new AppException("BOOKING_001",
                  "Room not available for date: " + date, HttpStatus.BAD_REQUEST);
         }
         date = date.plusDays(1);
      }
   }

   @Override
   @Transactional
   public void writeReserveAuditLog(Booking booking, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
         int numRooms) {
      LocalDate date = checkIn;
      while (date.isBefore(checkOut)) {
         inventoryOperationRepository.insertOperationStandard(
               UuidCreator.getTimeOrderedEpoch(),
               booking.getId(),
               roomType.getId(),
               date,
               OperationType.RESERVE.name(),
               numRooms);
         date = date.plusDays(1);
      }
      inventoryReservationCounter.increment();
   }

   @Override
   @Transactional
   public void reserveInventory(Booking booking, RoomType roomType,
         LocalDate checkIn, LocalDate checkOut, int numRooms) {
      deductInventoryOnly(roomType, checkIn, checkOut, numRooms);
      writeReserveAuditLog(booking, roomType, checkIn, checkOut, numRooms);
   }

   @Override
   @Transactional
   public void releaseInventory(Booking booking) {
      LocalDate date = booking.getCheckInDate();
      while (date.isBefore(booking.getCheckOutDate())) {
         int inserted = inventoryOperationRepository.insertOperationIdempotently(
               UuidCreator.getTimeOrderedEpoch(),
               booking.getId(),
               booking.getRoomType().getId(),
               date,
               OperationType.RELEASE.name(),
               booking.getNumRooms());

         if (inserted == 1) {
            roomAvailabilityRepository.incrementAvailability(
                  booking.getRoomType().getId(), date, booking.getNumRooms());
         } else {
            log.info("Duplicate inventory release detected and ignored for booking {} and date {}", booking.getId(),
                  date);
         }
         date = date.plusDays(1);
      }
      inventoryReleaseCounter.increment();
   }

}
