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
import com.omnibooking.services.pricing.PriceCalculationService;

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
import org.springframework.transaction.annotation.Propagation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.model.PropertyCreatedOutbox;
import com.omnibooking.repository.infra.PropertyCreatedOutboxRepository;
import com.omnibooking.model.enums.PropertyStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.text.Normalizer;

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

   private final PriceCalculationService priceCalculationService;

   private final AmenityHelper amenityHelper;

   private final Counter propertyCreatedCounter;

   private final Timer availabilityGenerationDurationTimer;

   private final PropertyCreatedOutboxRepository propertyCreatedOutboxRepository;

   private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

   @Override
   @Transactional
   @Caching(evict = {
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

      // 1. Resolve Amenities first (to avoid saving the Property entity twice)
      Set<Amenity> amenitySet = null;
      if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
         amenitySet = resolveAndSaveAmenities(request.getAmenities());
      }

      // Safe Enum Parsing
      PropertyType propertyType;
      try {
         propertyType = PropertyType.valueOf(request.getPropertyType().toUpperCase());
      } catch (IllegalArgumentException e) {
         throw new AppException(ErrorCode.INVALID_KEY, "Loại cơ sở lưu trú không hợp lệ: " + request.getPropertyType());
      }

      // 2. Build and save Property with amenities set directly
      Property property = Property.builder()
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .propertyType(propertyType)
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
            .status(PropertyStatus.PENDING_SETUP)
            .amenities(amenitySet)
            .build();

      Property saved = propertyRepository.save(Objects.requireNonNull(property));

      // 3. Save/reactivate partner legal profile
      savePartnerLegalProfile(owner, request.getBusinessRegistrationNumber(), request.getTaxCode(),
            request.getLegalOwnerName());

      // 4. Save Room Types and gather room type IDs (do NOT generate RoomAvailability
      // here)
      List<UUID> roomTypeIds = new ArrayList<>();
      if (request.getRoomTypes() != null && !request.getRoomTypes().isEmpty()) {
         List<RoomType> roomTypesToSave = new ArrayList<>();
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
            roomTypesToSave.add(roomType);
         }
         List<RoomType> savedRoomTypes = roomTypeRepository.saveAll(roomTypesToSave);
         roomTypeIds = savedRoomTypes.stream().map(RoomType::getId).toList();
      }

      // 5. Save setup job to transactional outbox
      if (!roomTypeIds.isEmpty()) {
         PropertyCreatedOutbox outbox = PropertyCreatedOutbox.builder()
               .propertyId(saved.getId())
               .status(OutboxStatus.PENDING)
               .build();
         propertyCreatedOutboxRepository.save(outbox);
         log.info("Saved property setup outbox record for property: {}", saved.getId());
      }

      // Increment property created metric
      propertyCreatedCounter.increment();

      return PropertyResponse.builder()
            .id(saved.getId())
            .name(saved.getName())
            .propertyType(saved.getPropertyType().name())
            .city(saved.getCity())
            .country(saved.getCountry())
            .imageUrl(getMainImageUrl(saved.getId()))
            .averageRating(saved.getAverageRating())
            .reviewCount(saved.getReviewCount())
            .status(saved.getStatus().name())
            .build();
   }

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void initializeRoomAvailability(List<UUID> roomTypeIds) {
      if (roomTypeIds == null || roomTypeIds.isEmpty()) {
         return;
      }

      availabilityGenerationDurationTimer.record(() -> {
         log.info("Initializing room availability for {} room types in bulk", roomTypeIds.size());
         List<RoomAvailability> availabilities = new ArrayList<>();
         LocalDate startDate = LocalDate.now();
         LocalDate endDate = startDate.plusDays(90);

         // Batch load RoomTypes
         List<RoomType> roomTypes = roomTypeRepository.findAllById(roomTypeIds);

         // Batch load existing availability dates
         List<Object[]> existingDatesRaw = roomAvailabilityRepository
               .findAvailabilityDatesByRoomTypeIdsAndDateRange(roomTypeIds, startDate, endDate);

         Map<UUID, Set<LocalDate>> existingMap = new HashMap<>();
         for (Object[] row : existingDatesRaw) {
            UUID rtId = (UUID) row[0];
            LocalDate date = (LocalDate) row[1];
            existingMap.computeIfAbsent(rtId, k -> new HashSet<>()).add(date);
         }

         for (RoomType roomType : roomTypes) {
            Set<LocalDate> existingSet = existingMap.getOrDefault(roomType.getId(), Set.of());

            for (int i = 0; i < 90; i++) {
               LocalDate date = startDate.plusDays(i);
               if (existingSet.contains(date)) {
                  continue; // Skip if record already exists
               }
               RoomAvailability availability = RoomAvailability.builder()
                     .roomType(roomType)
                     .availabilityDate(date)
                     .availableCount(roomType.getTotalRooms())
                     .isClosed(false)
                     .build();
               availabilities.add(availability);
            }
         }

         if (!availabilities.isEmpty()) {
            roomAvailabilityRepository.saveAll(availabilities);
            log.info("Successfully persisted {} availability records in batch", availabilities.size());
         }
      });
   }

   private Set<Amenity> resolveAndSaveAmenities(List<String> names) {
      if (names == null || names.isEmpty()) {
         return Set.of();
      }

      Set<String> cleanNames = names.stream()
            .filter(Objects::nonNull)
            .map(this::normalizeAmenityName)
            .filter(name -> !name.isEmpty())
            .collect(Collectors.toSet());

      if (cleanNames.isEmpty()) {
         return Set.of();
      }

      // Single lookup query for all names
      List<Amenity> existing = amenityRepository.findByNameInIgnoreCase(cleanNames);
      Map<String, Amenity> existingMap = existing.stream()
            .collect(Collectors.toMap(
                  a -> a.getName().toLowerCase(Locale.ROOT),
                  a -> a,
                  (a1, a2) -> a1));

      Set<Amenity> result = new HashSet<>(existing);
      List<Amenity> toSave = new ArrayList<>();

      for (String name : cleanNames) {
         String key = name.toLowerCase(Locale.ROOT);
         if (!existingMap.containsKey(key)) {
            toSave.add(Amenity.builder()
                  .name(name)
                  .category(AmenityCategory.GENERAL)
                  .build());
         }
      }

      if (!toSave.isEmpty()) {
         try {
            // Normal path: save all new amenities in a single REQUIRES_NEW transaction
            List<Amenity> saved = amenityHelper.saveAllInNewTransaction(toSave);
            result.addAll(saved);
         } catch (Exception e) {
            log.warn("Failed to batch save new amenities in one transaction. Resolving individually: {}",
                  e.getMessage());
            // Fallback path: save or lookup individually to handle concurrent insertions
            for (Amenity amenity : toSave) {
               try {
                  Amenity saved = amenityHelper.saveInNewTransaction(amenity);
                  result.add(saved);
               } catch (Exception ex) {
                  Amenity existingAmenity = amenityRepository.findByNameIgnoreCase(amenity.getName())
                        .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                              "Failed to resolve amenity: " + amenity.getName()));
                  result.add(existingAmenity);
               }
            }
         }
      }

      return result;
   }

   private String normalizeAmenityName(String name) {
      if (name == null) {
         return "";
      }
      String normalized = Normalizer.normalize(name, Normalizer.Form.NFC);
      return normalized
            .replaceAll("\\s+", " ")
            .trim();
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
         return new ArrayList<>();
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
            .collect(Collectors.toCollection(ArrayList::new));
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

   private String normalizeLegalField(String value) {
      if (value == null) {
         return "";
      }
      String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
      return normalized
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
   }

   private void savePartnerLegalProfile(User partner, String regNum, String taxCode,
         String ownerName) {
      if (regNum == null || regNum.isBlank() ||
            taxCode == null || taxCode.isBlank() ||
            ownerName == null || ownerName.isBlank()) {
         return;
      }

      String normalizedRegNum = normalizeLegalField(regNum);
      String normalizedTaxCode = normalizeLegalField(taxCode);
      String normalizedOwnerName = normalizeLegalField(ownerName);
      String plainConcat = normalizedRegNum + "|" + normalizedTaxCode + "|" + normalizedOwnerName;
      String incomingHash = encryptionService.createBlindIndex(plainConcat);

      PartnerLegalProfile matchedProfile = partnerLegalProfileRepository
            .findByPartnerIdAndProfileSearchHash(partner.getId(), incomingHash)
            .orElse(null);

      if (matchedProfile != null) {
         if (Boolean.TRUE.equals(matchedProfile.getIsActive())) {
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
               .profileSearchHash(incomingHash)
               .isActive(true)
               .build();
         partnerLegalProfileRepository.save(newProfile);
      }
   }

   @Override
   @Transactional(readOnly = true)
   public PropertyDetailResponse getPropertyDetailForPartner(UUID propertyId, UUID ownerId) {
      Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

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

      List<RoomType> roomTypeList = roomTypeRepository.findByPropertyId(propertyId);
      List<UUID> roomTypeIds = roomTypeList.stream().map(RoomType::getId).toList();
      Map<UUID, BigDecimal> currentPrices;
      try {
         currentPrices = priceCalculationService.calculateStayPricesForRoomTypes(
               propertyId, roomTypeIds, LocalDate.now(), LocalDate.now().plusDays(1), 2);
      } catch (Exception e) {
         log.error("Failed to calculate batch stay prices for property: {}", propertyId, e);
         currentPrices = Map.of();
      }

      final Map<UUID, BigDecimal> finalPrices = currentPrices;
      List<RoomTypeResponse> roomTypes = roomTypeList.stream()
            .map(r -> {
               BigDecimal currentPrice = finalPrices.getOrDefault(r.getId(), r.getBasePrice());
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
            .status(property.getStatus().name())
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

      List<RoomType> roomTypeList = roomTypeRepository.findByPropertyId(propertyId);
      List<UUID> roomTypeIds = roomTypeList.stream().map(RoomType::getId).toList();
      Map<UUID, BigDecimal> currentPrices;
      try {
         currentPrices = priceCalculationService.calculateStayPricesForRoomTypes(
               propertyId, roomTypeIds, LocalDate.now(), LocalDate.now().plusDays(1), 2);
      } catch (Exception e) {
         log.error("Failed to calculate batch stay prices for property: {}", propertyId, e);
         currentPrices = Map.of();
      }

      final Map<UUID, BigDecimal> finalPrices = currentPrices;
      List<RoomTypeResponse> roomTypes = roomTypeList.stream()
            .map(r -> {
               BigDecimal currentPrice = finalPrices.getOrDefault(r.getId(), r.getBasePrice());
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
            .status(property.getStatus().name())
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
