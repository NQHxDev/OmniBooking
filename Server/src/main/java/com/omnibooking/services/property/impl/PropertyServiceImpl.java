package com.omnibooking.services.property.impl;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.Amenity;
import com.omnibooking.model.enums.AmenityCategory;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.repository.RoomTypeRepository;
import com.omnibooking.repository.RoomAvailabilityRepository;
import com.omnibooking.repository.AmenityRepository;
import com.omnibooking.services.property.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

   private final PropertyRepository propertyRepository;
   private final com.omnibooking.repository.UserRepository userRepository;
   private final MediaRepository mediaRepository;
   private final RoomTypeRepository roomTypeRepository;
   private final RoomAvailabilityRepository roomAvailabilityRepository;
   private final AmenityRepository amenityRepository;
   private final com.omnibooking.services.property.PropertySyncProducer propertySyncProducer;

   @Override
   @Transactional
   public PropertyResponse createProperty(PropertyRequest request, UUID ownerId) {
      log.info("Creating new property: {} for owner: {}", request.getName(), ownerId);

      com.omnibooking.model.User owner = userRepository.findById(Objects.requireNonNull(ownerId))
            .orElseThrow(
                  () -> new com.omnibooking.exception.AppException(com.omnibooking.exception.ErrorCode.USER_NOT_FOUND));

      Property property = Property.builder()
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .propertyType(com.omnibooking.model.enums.PropertyType.valueOf(request.getPropertyType()))
            .address(request.getAddress())
            .city(request.getCity())
            .country(request.getCountry())
            .starRating(
                  request.getStarRating() != null && request.getStarRating() == 0 ? null : request.getStarRating())
            .checkInTime(request.getCheckInTime())
            .checkOutTime(request.getCheckOutTime())
            .businessRegistrationNumber(request.getBusinessRegistrationNumber())
            .taxCode(request.getTaxCode())
            .legalOwnerName(request.getLegalOwnerName())
            .isActive(true)
            .build();

      Property saved = propertyRepository.save(Objects.requireNonNull(property));

      // Save Amenities
      if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
         Set<Amenity> amenitySet = new HashSet<>();
         for (String name : request.getAmenities()) {
            Amenity amenity = amenityRepository.findByNameIgnoreCase(name)
                  .orElseGet(() -> {
                     Amenity newAmenity = Amenity.builder()
                           .name(name)
                           .category(AmenityCategory.GENERAL)
                           .build();
                     return amenityRepository.save(newAmenity);
                  });
            amenitySet.add(amenity);
         }
         saved.setAmenities(amenitySet);
         saved = propertyRepository.save(saved);
      }

      // Save Room Types & Initialize Availability
      if (request.getRoomTypes() != null && !request.getRoomTypes().isEmpty()) {
         for (com.omnibooking.dto.RoomTypeRequest roomRequest : request.getRoomTypes()) {
            RoomType roomType = RoomType.builder()
                  .property(saved)
                  .name(roomRequest.getName())
                  .description(roomRequest.getDescription())
                  .basePrice(roomRequest.getBasePrice())
                  .capacityAdults(roomRequest.getCapacityAdults() != null ? roomRequest.getCapacityAdults() : 2)
                  .capacityChildren(roomRequest.getCapacityChildren() != null ? roomRequest.getCapacityChildren() : 0)
                  .totalRooms(roomRequest.getTotalRooms() != null ? roomRequest.getTotalRooms() : 1)
                  .roomSizeSqm(roomRequest.getRoomSizeSqm())
                  .bedType(roomRequest.getBedType())
                  .build();

            RoomType savedRoom = roomTypeRepository.save(roomType);

            // Initialize Availability for the next 90 days
            LocalDate startDate = LocalDate.now();
            for (int i = 0; i < 90; i++) {
               LocalDate date = startDate.plusDays(i);
               RoomAvailability availability = RoomAvailability.builder()
                     .roomType(savedRoom)
                     .availabilityDate(date)
                     .availableCount(savedRoom.getTotalRooms())
                     .isClosed(false)
                     .build();
               roomAvailabilityRepository.save(availability);
            }
         }
      }

      // Sync to Elasticsearch via Kafka
      propertySyncProducer.sendSyncEvent(com.omnibooking.dto.event.PropertySyncEvent.builder()
            .propertyId(saved.getId())
            .operation("CREATE")
            .build());

      return PropertyResponse.builder()
            .id(saved.getId())
            .name(saved.getName())
            .propertyType(saved.getPropertyType().name())
            .city(saved.getCity())
            .country(saved.getCountry())
            .imageUrl(getMainImageUrl(saved.getId()))
            .build();
   }

   @Override
   public List<PropertyResponse> getPropertiesByOwner(UUID ownerId) {
      List<Property> properties = propertyRepository.findByOwnerId(ownerId);
      log.info("Found {} properties for owner: {}", properties.size(), ownerId);

      return properties.stream()
            .map(p -> PropertyResponse.builder()
                  .id(p.getId())
                  .name(p.getName())
                  .propertyType(p.getPropertyType().name())
                  .city(p.getCity())
                  .country(p.getCountry())
                  .imageUrl(getMainImageUrl(p.getId()))
                  .build())
            .toList();
   }

   @Override
   @Cacheable(value = "properties", key = "'featured:' + #limit")
   public List<PropertyResponse> getFeaturedProperties(int limit) {
      log.info("Fetching {} featured properties", limit);
      List<Property> properties = propertyRepository
            .findFeaturedProperties(org.springframework.data.domain.PageRequest.of(0, limit));

      return properties.stream()
            .map(p -> PropertyResponse.builder()
                  .id(p.getId())
                  .name(p.getName())
                  .propertyType(p.getPropertyType().name())
                  .city(p.getCity())
                  .country(p.getCountry())
                  .imageUrl(getMainImageUrl(p.getId()))
                  .build())
            .toList();
   }

   private String getMainImageUrl(UUID propertyId) {
      return mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(propertyId, "PROPERTY")
            .map(com.omnibooking.model.Media::getUrl)
            .orElse(null);
   }

}
