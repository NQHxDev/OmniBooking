package com.omnibooking.services.impl;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.model.Property;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.services.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

   private final PropertyRepository propertyRepository;
   private final com.omnibooking.repository.UserRepository userRepository;
   private final MediaRepository mediaRepository;

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
            .isActive(true)
            .build();

      Property saved = propertyRepository.save(Objects.requireNonNull(property));

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

   private String getMainImageUrl(UUID propertyId) {
      return mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(propertyId, "PROPERTY")
            .map(com.omnibooking.model.Media::getUrl)
            .orElse(null);
   }
}
