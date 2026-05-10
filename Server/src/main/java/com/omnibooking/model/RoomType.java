package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomType extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "property_id", nullable = false)
   private Property property;

   @Column(nullable = false, length = 100)
   private String name;

   @Column(columnDefinition = "TEXT")
   private String description;

   @Column(name = "base_price", nullable = false)
   private BigDecimal basePrice;

   @Builder.Default
   @Column(name = "capacity_adults", nullable = false)
   private Integer capacityAdults = 2;

   @Builder.Default
   @Column(name = "capacity_children", nullable = false)
   private Integer capacityChildren = 0;

   @Builder.Default
   @Column(name = "total_rooms", nullable = false)
   private Integer totalRooms = 1;

   @Column(name = "room_size_sqm")
   private BigDecimal roomSizeSqm;

   @Column(name = "bed_type", length = 50)
   private String bedType;

   @ManyToMany
   @JoinTable(
      name = "room_amenities",
      joinColumns = @JoinColumn(name = "room_type_id"),
      inverseJoinColumns = @JoinColumn(name = "amenity_id")
   )
   private Set<Amenity> amenities;

}
