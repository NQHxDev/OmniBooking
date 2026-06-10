package com.omnibooking.model;

import com.omnibooking.model.enums.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inventory_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryOperation extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "booking_id", nullable = false)
   private Booking booking;

   @ManyToOne
   @JoinColumn(name = "room_type_id", nullable = false)
   private RoomType roomType;

   @Column(name = "availability_date", nullable = false)
   private LocalDate availabilityDate;

   @Enumerated(EnumType.STRING)
   @Column(name = "operation_type", nullable = false, length = 10)
   private OperationType operationType; // RESERVE | RELEASE

   @Column(name = "num_rooms", nullable = false)
   private Integer numRooms;

}
