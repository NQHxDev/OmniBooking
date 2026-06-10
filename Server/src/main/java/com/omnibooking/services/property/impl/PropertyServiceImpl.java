package com.omnibooking.services.property.impl;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.dto.IncompleteUploadResponse;
import com.omnibooking.dto.RoomTypeRequest;
import com.omnibooking.dto.PropertyDetailResponse;
import com.omnibooking.dto.RoomTypeResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.Amenity;
import com.omnibooking.model.enums.AmenityCategory;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.infra.MediaRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.AmenityRepository;
import com.omnibooking.services.property.PropertyService;
import com.omnibooking.services.property.PropertyImagesCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.omnibooking.model.Media;
import org.springframework.transaction.annotation.Transactional;
import com.omnibooking.dto.PartnerLegalProfileResponse;
import com.omnibooking.model.PartnerLegalProfile;
import com.omnibooking.repository.user.PartnerLegalProfileRepository;
import com.omnibooking.services.core.EncryptionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

   private final PropertyRepository propertyRepository;

   private final UserRepository userRepository;

   private final MediaRepository mediaRepository;

   private final RoomTypeRepository roomTypeRepository;

   private final RoomAvailabilityRepository roomAvailabilityRepository;

   private final AmenityRepository amenityRepository;

   private final PartnerLegalProfileRepository partnerLegalProfileRepository;

   private final EncryptionService encryptionService;

   private final PropertyImagesCacheService propertyImagesCacheService;

   private final CacheManager cacheManager;

   private final com.omnibooking.services.pricing.PriceCalculationService priceCalculationService;

   private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

   @Override
   @Transactional
   @Caching(evict = {
         @CacheEvict(value = "properties", allEntries = true),
         @CacheEvict(value = "partner_properties", key = "#ownerId")
   })
   public PropertyResponse createProperty(PropertyRequest request, UUID ownerId) {
      User owner = userRepository.findById(Objects.requireNonNull(ownerId))
            .orElseThrow(
                  () -> new AppException(ErrorCode.USER_NOT_FOUND));

      String encryptedRegNum = request.getBusinessRegistrationNumber() != null
            ? encryptionService.encrypt(request.getBusinessRegistrationNumber())
            : null;
      String encryptedTaxCode = request.getTaxCode() != null ? encryptionService.encrypt(request.getTaxCode()) : null;
      String encryptedOwnerName = request.getLegalOwnerName() != null
            ? encryptionService.encrypt(request.getLegalOwnerName())
            : null;

      Property property = Property.builder()
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .propertyType(PropertyType.valueOf(request.getPropertyType()))
            .address(request.getAddress())
            .city(normalizeCityName(request.getCity()))
            .country(request.getCountry())
            .starRating(
                  request.getStarRating() != null && request.getStarRating() == 0 ? null : request.getStarRating())
            .checkInTime(request.getCheckInTime())
            .checkOutTime(request.getCheckOutTime())
            .businessRegistrationNumber(encryptedRegNum)
            .taxCode(encryptedTaxCode)
            .legalOwnerName(encryptedOwnerName)
            .expectedImageCount(request.getExpectedImageCount())
            .isActive(true)
            .build();

      Property saved = propertyRepository.save(Objects.requireNonNull(property));

      // Save/reactivate partner legal profile
      savePartnerLegalProfile(owner, request.getBusinessRegistrationNumber(), request.getTaxCode(),
            request.getLegalOwnerName());

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
         for (RoomTypeRequest roomRequest : request.getRoomTypes()) {
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

      // Immediate sync to Elasticsearch has been removed because property images are
      // processed asynchronously.
      // The property will be indexed in Elasticsearch once the main image upload
      // finishes in MediaConsumer.

      return PropertyResponse.builder()
            .id(saved.getId())
            .name(saved.getName())
            .propertyType(saved.getPropertyType().name())
            .city(saved.getCity())
            .country(saved.getCountry())
            .imageUrl(getMainImageUrl(saved.getId()))
            .averageRating(saved.getAverageRating())
            .reviewCount(saved.getReviewCount())
            .build();
   }

   @Override
   @Cacheable(value = "partner_properties", key = "#ownerId")
   public List<PropertyResponse> getPropertiesByOwner(UUID ownerId) {
      List<Property> properties = propertyRepository.findByOwnerId(ownerId);
      log.info("Found {} properties for owner: {}", properties.size(), ownerId);
      return mapToPropertyResponses(properties);
   }

   @Override
   @CacheEvict(value = "partner_properties", key = "#ownerId")
   public void evictPartnerPropertiesCache(UUID ownerId) {
      log.info("Evicting partner properties cache for owner: {}", ownerId);
   }

   @Override
   @CacheEvict(value = "properties", allEntries = true)
   public void evictPublicPropertiesCache() {
      log.info("Evicting all public properties cache");
   }

   private List<PropertyResponse> getCachedList(Cache cache, String key) {
      if (cache == null)
         return null;
      List<?> rawList = cache.get(key, List.class);
      if (rawList == null || rawList.isEmpty())
         return null;

      List<PropertyResponse> cachedList = new java.util.ArrayList<>();
      for (Object obj : rawList) {
         if (obj instanceof PropertyResponse) {
            cachedList.add((PropertyResponse) obj);
         }
      }
      return cachedList.isEmpty() ? null : cachedList;
   }

   @Override
   public List<PropertyResponse> getFeaturedProperties(int limit) {
      String key = "featured:" + limit;
      Cache cache = cacheManager != null ? cacheManager.getCache("properties") : null;

      List<PropertyResponse> cached = getCachedList(cache, key);
      if (cached != null) {
         return cached;
      }

      Object lock = locks.computeIfAbsent(key, k -> new Object());
      synchronized (lock) {
         cached = getCachedList(cache, key);
         if (cached != null) {
            return cached;
         }

         log.info("Fetching {} featured properties from DB (Cache stampede mitigation)...", limit);
         Instant startDate = Instant.now().minus(30, ChronoUnit.DAYS);
         List<Property> properties = propertyRepository
               .findFeaturedProperties(startDate, PageRequest.of(0, limit));
         List<PropertyResponse> response = mapToPropertyResponses(properties);

         if (cache != null && !response.isEmpty()) {
            cache.put(key, response);
         }

         return response;
      }
   }

   @Override
   public List<PropertyResponse> getNewProperties(int limit) {
      String key = "new:" + limit;
      Cache cache = cacheManager != null ? cacheManager.getCache("properties") : null;

      List<PropertyResponse> cached = getCachedList(cache, key);
      if (cached != null) {
         return cached;
      }

      Object lock = locks.computeIfAbsent(key, k -> new Object());
      synchronized (lock) {
         cached = getCachedList(cache, key);
         if (cached != null) {
            return cached;
         }

         log.info("Fetching {} new properties from DB (Cache stampede mitigation)...", limit);
         List<Property> properties = propertyRepository
               .findNewProperties(PageRequest.of(0, limit));
         List<PropertyResponse> response = mapToPropertyResponses(properties);

         if (cache != null && !response.isEmpty()) {
            cache.put(key, response);
         }

         return response;
      }
   }

   private List<PropertyResponse> mapToPropertyResponses(List<Property> properties) {
      if (properties.isEmpty()) {
         return List.of();
      }

      List<UUID> propertyIds = properties.stream().map(Property::getId).toList();
      List<Media> mainImages = mediaRepository.findMainImagesByEntityIds(propertyIds);

      // Defensive mapping: pick the first main image if duplicates exist
      Map<UUID, Media> mainImageMap = mainImages.stream()
            .collect(Collectors.toMap(
                  Media::getEntityId,
                  m -> m,
                  (existing, replacement) -> existing));

      return properties.stream()
            .map(p -> {
               Media mainMedia = mainImageMap.get(p.getId());
               return PropertyResponse.builder()
                     .id(p.getId())
                     .name(p.getName())
                     .propertyType(p.getPropertyType().name())
                     .city(p.getCity())
                     .country(p.getCountry())
                     .imageUrl(mainMedia != null ? mainMedia.getUrl() : null)
                     .averageRating(p.getAverageRating())
                     .reviewCount(p.getReviewCount())
                     .build();
            })
            .toList();
   }

   private String getMainImageUrl(UUID propertyId) {
      return mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(propertyId, "PROPERTY")
            .map(Media::getUrl)
            .orElse(null);
   }

   private List<String> getAllImageUrls(UUID propertyId) {
      return propertyImagesCacheService.getPropertyImageUrls(propertyId);
   }

   @Override
   public List<PartnerLegalProfileResponse> getPartnerLegalProfiles(UUID partnerId) {
      log.info("Fetching active partner legal profiles for partner: {}", partnerId);
      List<PartnerLegalProfile> profiles = partnerLegalProfileRepository
            .findByPartnerIdAndIsActiveTrueOrderByCreatedAtDesc(partnerId);
      return profiles.stream()
            .map(p -> {
               try {
                  return PartnerLegalProfileResponse.builder()
                        .id(p.getId())
                        .businessRegistrationNumber(encryptionService.decrypt(p.getBusinessRegistrationNumber()))
                        .taxCode(encryptionService.decrypt(p.getTaxCode()))
                        .legalOwnerName(encryptionService.decrypt(p.getLegalOwnerName()))
                        .build();
               } catch (Exception e) {
                  log.error("Failed to decrypt profile response for profile: {}", p.getId(), e);
                  return null;
               }
            })
            .filter(Objects::nonNull)
            .toList();
   }

   private void savePartnerLegalProfile(User partner, String regNum, String taxCode,
         String ownerName) {
      if (regNum == null || regNum.isBlank() ||
            taxCode == null || taxCode.isBlank() ||
            ownerName == null || ownerName.isBlank()) {
         return;
      }

      List<PartnerLegalProfile> allProfiles = partnerLegalProfileRepository.findByPartnerId(partner.getId());
      PartnerLegalProfile matchedProfile = null;

      for (PartnerLegalProfile profile : allProfiles) {
         try {
            String decryptedRegNum = encryptionService.decrypt(profile.getBusinessRegistrationNumber());
            String decryptedTaxCode = encryptionService.decrypt(profile.getTaxCode());
            String decryptedOwnerName = encryptionService.decrypt(profile.getLegalOwnerName());

            if (regNum.trim().equalsIgnoreCase(decryptedRegNum.trim()) &&
                  taxCode.trim().equalsIgnoreCase(decryptedTaxCode.trim()) &&
                  ownerName.trim().equalsIgnoreCase(decryptedOwnerName.trim())) {
               matchedProfile = profile;
               break;
            }
         } catch (Exception e) {
            log.error("Failed to decrypt profile: {}", profile.getId(), e);
         }
      }

      if (matchedProfile != null) {
         if (Boolean.TRUE.equals(matchedProfile.getIsActive())) {
            // Already active, do nothing
            return;
         }
         // Reactivate the matched profile
         List<PartnerLegalProfile> activeProfiles = new java.util.ArrayList<>(
               partnerLegalProfileRepository.findByPartnerIdAndIsActiveTrueOrderByCreatedAtAsc(partner.getId()));
         while (activeProfiles.size() >= 2) {
            PartnerLegalProfile oldest = activeProfiles.remove(0);
            oldest.setIsActive(false);
            partnerLegalProfileRepository.save(oldest);
         }
         matchedProfile.setIsActive(true);
         partnerLegalProfileRepository.save(matchedProfile);
      } else {
         // Create a new one
         List<PartnerLegalProfile> activeProfiles = new java.util.ArrayList<>(
               partnerLegalProfileRepository.findByPartnerIdAndIsActiveTrueOrderByCreatedAtAsc(partner.getId()));
         while (activeProfiles.size() >= 2) {
            PartnerLegalProfile oldest = activeProfiles.remove(0);
            oldest.setIsActive(false);
            partnerLegalProfileRepository.save(oldest);
         }

         PartnerLegalProfile newProfile = PartnerLegalProfile.builder()
               .partner(partner)
               .businessRegistrationNumber(encryptionService.encrypt(regNum.trim()))
               .taxCode(encryptionService.encrypt(taxCode.trim()))
               .legalOwnerName(encryptionService.encrypt(ownerName.trim()))
               .isActive(true)
               .build();
         partnerLegalProfileRepository.save(newProfile);
      }
   }

   @Override
   @Transactional(readOnly = true)
   public PropertyDetailResponse getPropertyDetailForPartner(UUID propertyId, UUID ownerId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)); // Property not found

      if (!property.getOwner().getId().equals(ownerId)) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      String decryptedRegNum = property.getBusinessRegistrationNumber() != null
            ? encryptionService.decrypt(property.getBusinessRegistrationNumber())
            : null;
      String decryptedTaxCode = property.getTaxCode() != null
            ? encryptionService.decrypt(property.getTaxCode())
            : null;
      String decryptedOwnerName = property.getLegalOwnerName() != null
            ? encryptionService.decrypt(property.getLegalOwnerName())
            : null;

      List<String> amenities = property.getAmenities() != null
            ? property.getAmenities().stream().map(Amenity::getName).toList()
            : List.of();

      List<RoomTypeResponse> roomTypes = roomTypeRepository.findByPropertyId(propertyId).stream()
            .map(r -> {
               BigDecimal currentPrice;
               try {
                  var result = priceCalculationService.calculateStayPrice(r.getProperty().getId(), r.getId(),
                        LocalDate.now(), LocalDate.now().plusDays(1), 2);
                  currentPrice = result.totalFinalPrice();
               } catch (Exception e) {
                  currentPrice = r.getBasePrice();
               }
               return RoomTypeResponse.builder()
                     .id(r.getId())
                     .name(r.getName())
                     .description(r.getDescription())
                     .basePrice(r.getBasePrice())
                     .capacityAdults(r.getCapacityAdults())
                     .capacityChildren(r.getCapacityChildren())
                     .totalRooms(r.getTotalRooms())
                     .roomSizeSqm(r.getRoomSizeSqm())
                     .bedType(r.getBedType())
                     .currentPrice(currentPrice)
                     .build();
            })
            .toList();

      return PropertyDetailResponse.builder()
            .id(property.getId())
            .name(property.getName())
            .description(property.getDescription())
            .propertyType(property.getPropertyType().name())
            .address(property.getAddress())
            .city(property.getCity())
            .country(property.getCountry())
            .starRating(property.getStarRating())
            .checkInTime(property.getCheckInTime() != null ? property.getCheckInTime().toString() : null)
            .checkOutTime(property.getCheckOutTime() != null ? property.getCheckOutTime().toString() : null)
            .imageUrl(getMainImageUrl(property.getId()))
            .imageUrls(getAllImageUrls(property.getId()))
            .businessRegistrationNumber(decryptedRegNum)
            .taxCode(decryptedTaxCode)
            .legalOwnerName(decryptedOwnerName)
            .amenities(amenities)
            .averageRating(property.getAverageRating())
            .reviewCount(property.getReviewCount())
            .roomTypes(roomTypes)
            .build();
   }

   @Override
   @Transactional(readOnly = true)
   public PropertyDetailResponse getPropertyDetail(UUID propertyId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      List<String> amenities = property.getAmenities() != null
            ? property.getAmenities().stream().map(Amenity::getName).toList()
            : List.of();

      List<RoomTypeResponse> roomTypes = roomTypeRepository.findByPropertyId(propertyId).stream()
            .map(r -> {
               BigDecimal currentPrice;
               try {
                  var result = priceCalculationService.calculateStayPrice(r.getProperty().getId(), r.getId(),
                        LocalDate.now(), LocalDate.now().plusDays(1), 2);
                  currentPrice = result.totalFinalPrice();
               } catch (Exception e) {
                  currentPrice = r.getBasePrice();
               }
               return RoomTypeResponse.builder()
                     .id(r.getId())
                     .name(r.getName())
                     .description(r.getDescription())
                     .basePrice(r.getBasePrice())
                     .capacityAdults(r.getCapacityAdults())
                     .capacityChildren(r.getCapacityChildren())
                     .totalRooms(r.getTotalRooms())
                     .roomSizeSqm(r.getRoomSizeSqm())
                     .bedType(r.getBedType())
                     .currentPrice(currentPrice)
                     .build();
            })
            .toList();

      return PropertyDetailResponse.builder()
            .id(property.getId())
            .name(property.getName())
            .description(property.getDescription())
            .propertyType(property.getPropertyType().name())
            .address(property.getAddress())
            .city(property.getCity())
            .country(property.getCountry())
            .starRating(property.getStarRating())
            .checkInTime(property.getCheckInTime() != null ? property.getCheckInTime().toString() : null)
            .checkOutTime(property.getCheckOutTime() != null ? property.getCheckOutTime().toString() : null)
            .imageUrl(getMainImageUrl(property.getId()))
            .imageUrls(getAllImageUrls(property.getId()))
            .amenities(amenities)
            .averageRating(property.getAverageRating())
            .reviewCount(property.getReviewCount())
            .roomTypes(roomTypes)
            .build();
   }

   private String normalizeCityName(String cityName) {
      if (cityName == null)
         return null;
      String trimmed = cityName.trim();
      if (trimmed.equalsIgnoreCase("Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("Thành Phố Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("Thành phố Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("TP Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("TP. Hồ Chí Minh")) {
         return "Thành Phố Hồ Chí Minh";
      }

      return trimmed.replaceAll("^(?i)(Thành\\s+phố|Tỉnh|TP\\.?)\\s+", "").trim();
   }

   @Override
   public List<IncompleteUploadResponse> getIncompleteUploads(UUID ownerId) {
      List<Property> properties = propertyRepository.findIncompletePropertiesByOwnerId(ownerId);
      return properties.stream()
            .map(p -> {
               long actualCount = mediaRepository.countByEntityIdAndEntityType(p.getId(), "PROPERTY");
               return IncompleteUploadResponse.builder()
                     .propertyId(p.getId())
                     .propertyName(p.getName())
                     .expectedCount(p.getExpectedImageCount())
                     .actualCount((int) actualCount)
                     .build();
            })
            .collect(Collectors.toList());
   }

   @Override
   @Transactional
   @Caching(evict = {
         @CacheEvict(value = "properties", allEntries = true),
         @CacheEvict(value = "partner_properties", key = "#ownerId")
   })
   public void dismissIncompleteUpload(UUID propertyId, UUID ownerId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

      if (!property.getOwner().getId().equals(ownerId)) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      long actualCount = mediaRepository.countByEntityIdAndEntityType(propertyId, "PROPERTY");
      property.setExpectedImageCount((int) actualCount);
      propertyRepository.save(property);
      log.info(
            "[Recovery] Incomplete upload warning dismissed for property: {} (expected_image_count set to actual: {})",
            propertyId, actualCount);
   }

}
