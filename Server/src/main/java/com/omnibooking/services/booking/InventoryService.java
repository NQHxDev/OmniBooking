package com.omnibooking.services.booking;

import com.omnibooking.model.Booking;
import com.omnibooking.model.RoomType;
import java.time.LocalDate;

public interface InventoryService {

   /**
    * Deducts inventory for a booking and records RESERVE audit entries.
    *
    * @throws AppException if rooms are not available for any date
    */
   void reserveInventory(Booking booking, RoomType roomType,
         LocalDate checkIn, LocalDate checkOut, int numRooms);

   void deductInventoryOnly(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int numRooms);

   void writeReserveAuditLog(Booking booking, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int numRooms);

   /**
    * Releases inventory for a booking and records RELEASE audit entries.
    * Called ONLY after an atomic status transition has already succeeded.
    * Inventory release itself is NOT the idempotency mechanism —
    * the atomic status transition is.
    */
   void releaseInventory(Booking booking);

}
