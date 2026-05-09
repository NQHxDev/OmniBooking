package com.omnibooking.model;

import com.omnibooking.model.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "booking_status_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookingStatusLog extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "booking_id", nullable = false)
   private Booking booking;

   @Enumerated(EnumType.STRING)
   @Column(name = "old_status", length = 20)
   private BookingStatus oldStatus;

   @Enumerated(EnumType.STRING)
   @Column(name = "new_status", nullable = false, length = 20)
   private BookingStatus newStatus;

   @Column(columnDefinition = "TEXT")
   private String reason;

   @ManyToOne
   @JoinColumn(name = "changed_by")
   private User changedBy;

}
