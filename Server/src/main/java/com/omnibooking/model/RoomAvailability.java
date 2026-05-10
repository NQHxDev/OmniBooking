package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "room_availability", uniqueConstraints = {
   @UniqueConstraint(columnNames = {"room_type_id", "availability_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomAvailability extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "room_type_id", nullable = false)
   private RoomType roomType;

   @Column(name = "availability_date", nullable = false)
   private LocalDate availabilityDate;

   @Column(name = "available_count", nullable = false)
   private Integer availableCount;

   @Column(name = "price_override")
   private BigDecimal priceOverride;

   @Builder.Default
   @Column(name = "is_closed")
   private Boolean isClosed = false;

}
