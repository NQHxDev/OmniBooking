package com.omnibooking.model;

import com.omnibooking.model.enums.PropertyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Property extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "owner_id", nullable = false)
   private User owner;

   @Column(nullable = false)
   private String name;

   @Column(columnDefinition = "TEXT")
   private String description;

   @Enumerated(EnumType.STRING)
   @Column(name = "property_type", nullable = false, length = 50)
   private PropertyType propertyType;

   @Column(nullable = false, columnDefinition = "TEXT")
   private String address;

   @Column(nullable = false, length = 100)
   private String city;

   @Column(nullable = false, length = 100)
   private String country;

   private BigDecimal latitude;

   private BigDecimal longitude;

   @Column(name = "star_rating")
   private Integer starRating;

   @Builder.Default
   @Column(name = "check_in_time")
   private LocalTime checkInTime = LocalTime.of(14, 0);

   @Builder.Default
   @Column(name = "check_out_time")
   private LocalTime checkOutTime = LocalTime.of(12, 0);

   @ManyToOne
   @JoinColumn(name = "cancellation_policy_id")
   private CancellationPolicy cancellationPolicy;

   @Builder.Default
   @Column(name = "is_active")
   private Boolean isActive = true;

   @ManyToMany
   @JoinTable(
      name = "property_amenities",
      joinColumns = @JoinColumn(name = "property_id"),
      inverseJoinColumns = @JoinColumn(name = "amenity_id")
   )
   private Set<Amenity> amenities;

   @OneToMany(mappedBy = "property")
   private Set<RoomType> roomTypes;

    @Column(name = "business_registration_number", length = 255)
   private String businessRegistrationNumber;

   @Column(name = "tax_code", length = 255)
   private String taxCode;

   @Column(name = "legal_owner_name", length = 255)
   private String legalOwnerName;

   @Builder.Default
   @Column(name = "average_rating", precision = 4, scale = 2)
   private BigDecimal averageRating = BigDecimal.ZERO;

   @Builder.Default
   @Column(name = "review_count")
   private Integer reviewCount = 0;

   @Builder.Default
   @Column(name = "rating_sum")
   private Long ratingSum = 0L;

}
