package com.omnibooking.model;

import com.omnibooking.model.enums.AmenityCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Amenity extends BaseEntity {

   @Column(nullable = false, unique = true, length = 100)
   private String name;

   @Enumerated(EnumType.STRING)
   @Column(length = 50)
   private AmenityCategory category;

   @Column(name = "icon_url")
   private String iconUrl;

}
