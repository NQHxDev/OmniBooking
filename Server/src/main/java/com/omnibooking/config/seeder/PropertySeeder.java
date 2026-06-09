package com.omnibooking.config.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.model.enums.BedType;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.constant.MediaConstants;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.config.AppProperties;
import com.omnibooking.model.Amenity;
import com.omnibooking.model.CancellationPolicy;
import com.omnibooking.model.Media;
import com.omnibooking.model.Property;
import com.omnibooking.model.Role;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.repository.infra.MediaRepository;
import com.omnibooking.repository.property.AmenityRepository;
import com.omnibooking.repository.property.CancellationPolicyRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.media.CloudinaryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertySeeder {

   private final UserRepository userRepository;

   private final CachedRoleService cachedRoleService;

   private final AmenityRepository amenityRepository;

   private final PropertyRepository propertyRepository;

   private final RoomTypeRepository roomTypeRepository;

   private final RoomAvailabilityRepository roomAvailabilityRepository;

   private final MediaRepository mediaRepository;

   private final CancellationPolicyRepository cancellationPolicyRepository;

   private final AppProperties appProperties;

   private final ObjectMapper objectMapper;

   private final CloudinaryService cloudinaryService;

   private final org.springframework.cache.CacheManager cacheManager;

   private static final String[] BANNER_FILE_NAMES = {
         "BannerOne.jpg", "BannerTwo.jpg", "BannerThree.jpg", "BannerFour.jpg", "BannerFive.jpg",
         "BannerSix.jpg", "BannerSeven.jpg", "BannerEight.jpg", "BannerNine.jpg", "BannerTen.jpg",
         "BannerEleven.jpg", "BannerTwelve.jpg", "BannerThirteen.jpg", "BannerFourteen.jpg", "BannerFifteen.jpg",
         "BannerSixteen.jpg", "BannerSeventeen.jpg", "BannerEighteen.jpg", "BannerNineteen.jpg", "BannerTwenty.jpg",
         "BannerTwentyOne.jpg", "BannerTwentyTwo.jpg"
   };

   private File locateAssetsDir() {
      File dir = new File("Assets");
      if (dir.exists() && dir.isDirectory()) {
         return dir;
      }
      dir = new File("../Assets");
      if (dir.exists() && dir.isDirectory()) {
         return dir;
      }

      return null;
   }

   @Transactional
   public void cleanUp() {
      log.info("Cleaning up existing properties, rooms, availabilities, media...");
      mediaRepository.deleteByEntityTypeIn(List.of("PROPERTY", "ROOM_TYPE"));
      roomAvailabilityRepository.deleteAllInBatch();
      roomTypeRepository.deleteAllInBatch();
      propertyRepository.deleteAllInBatch();
      cancellationPolicyRepository.deleteAllInBatch();

      // Clear Redis caches to prevent stale data
      if (cacheManager != null) {
         cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
               cache.clear();
            }
         });
         log.info("Successfully evicted all Redis caches...");
      }

      log.info("Successfully cleaned up existing property data...");
   }

   @Transactional
   public void seed(boolean force) {
      long propertyCount = propertyRepository.count();
      if (propertyCount > 0 && !force) {
         return;
      }

      // Fetch the ROLE_PARTNER role
      Role partnerRole;
      try {
         partnerRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.PARTNER);
      } catch (Exception e) {
         log.error("ROLE_PARTNER not found in database! Seeding properties skipped.", e);
         return;
      }

      // Fetch all standard amenities from DB
      List<Amenity> allAmenities = amenityRepository.findAll();
      if (allAmenities.isEmpty()) {
         log.warn("No amenities found in database! Seeded properties will have no amenities.");
      }

      // Read properties from JSON file
      List<MockPropertyDto> properties;
      try (InputStream is = getClass().getResourceAsStream("/mock-properties.json")) {
         if (is == null) {
            log.error("mock-properties.json not found in resources! Seeding aborted...");
            return;
         }
         properties = objectMapper.readValue(is, new TypeReference<List<MockPropertyDto>>() {
         });
      } catch (Exception e) {
         log.error("Failed to read or parse mock-properties.json", e);
         return;
      }

      // Retrieve existing users to act as owner partners
      List<User> existingUsers = userRepository.findByEmailEndingWith("@omnibooking.com");
      if (existingUsers.isEmpty()) {
         existingUsers = userRepository.findAll();
      }

      if (existingUsers.isEmpty()) {
         log.error("No users found in database! Cannot seed properties without owners. Please seed users first...");
         return;
      }

      ThreadLocalRandom random = ThreadLocalRandom.current();

      // Seed Cancellation Policy
      CancellationPolicy policy = CancellationPolicy.builder()
            .name("Flexible - 1 Day")
            .description("Free cancellation up to 24 hours before check-in. Penalty of 100% applies after that.")
            .freeCancellationDays(1)
            .penaltyPercentage(new BigDecimal("100.00"))
            .build();
      policy = cancellationPolicyRepository.save(policy);

      String cloudName = appProperties.getCloudinary().getCloudName();

      String[] commonImageIds = {
            "019e9283-986c-7712-be99-e23e780c8965",
            "019e9283-cbbf-79ea-b2cc-83a53f7358ff",
            "019e9283-e8f6-7d5a-be3f-3a817d377373",
            "019e9284-07d4-7046-9257-e4e0167cdfca",
            "019e9284-2c5a-72b7-8f91-253967ebc1f0",
            "019e9284-4d55-72a5-af6b-b66d731db38b",
            "019e9284-687d-7980-aec7-bf8acd7ddd74",
            "019e9284-7f45-7626-a748-31bd8dc1aec6",
            "019e9284-9e73-76e7-aca6-5dc58ae4230b",
            "019e9284-bd68-7ff9-9d91-d69a9b0d71e9",
            "019e9284-de06-780d-b2da-54916a5975a5",
            "019e9284-fb53-7084-9aff-43fd9e4b352e",
            "019e9285-1507-7656-8617-5f5a7085b9b2",
            "019e9285-316a-746a-8fb7-b330733a0af5",
            "019e9287-41d6-7aca-bf21-4643b8a09268",
            "019e9288-2c86-7e34-b39e-31cef018d859",
            "019e928a-ff21-773b-8e33-eae70e67d438",
            "019e928b-7826-7297-bcde-7fceda3a4e11"
      };

      java.io.File assetsDir = locateAssetsDir();
      if (assetsDir == null) {
         log.warn("Assets directory not found! No images will be uploaded to Cloudinary.");
      } else {
         log.info("Assets directory found at: {}. Uploading common images...", assetsDir.getAbsolutePath());
         for (String imgId : commonImageIds) {
            java.io.File imgFile = new java.io.File(assetsDir, "images/" + imgId + ".jpg");
            if (imgFile.exists()) {
               try {
                  byte[] fileBytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
                  cloudinaryService.upload(fileBytes, MediaConstants.COMMON_FOLDER, imgId);
                  log.info("Uploaded common image: {} successfully", imgId);
               } catch (Exception e) {
                  log.error("Failed to upload common image: {}", imgId, e);
               }
            } else {
               log.warn("Common image file not found: {}", imgFile.getAbsolutePath());
            }
         }
      }

      log.info("Starting to seed {} properties...", properties.size());

      for (int i = 0; i < properties.size(); i++) {
         MockPropertyDto dto = properties.get(i);
         String propertyName = dto.getName();
         String description = dto.getDescription();
         String address = dto.getAddress();
         String city = dto.getCity();
         double lat = dto.getLatitude();
         double lon = dto.getLongitude();

         // Pick a random user from existingUsers and promote them to Partner
         User owner = existingUsers.get(random.nextInt(existingUsers.size()));
         if (!owner.getRoles().contains(partnerRole)) {
            owner.getRoles().add(partnerRole);
            userRepository.save(owner);
         }

         // Determine Property Type
         PropertyType type = PropertyType.values()[random.nextInt(PropertyType.values().length)];

         // Pick random amenities (5 to 10)
         Set<Amenity> propertyAmenities = new java.util.HashSet<>();
         if (!allAmenities.isEmpty()) {
            int numAmenities = 5 + random.nextInt(6); // 5 to 10
            for (int k = 0; k < numAmenities; k++) {
               propertyAmenities.add(allAmenities.get(random.nextInt(allAmenities.size())));
            }
         }

         // Build and persist Property
         Property property = Property.builder()
               .owner(owner)
               .name(propertyName)
               .description(description)
               .propertyType(type)
               .address(address)
               .city(city)
               .country("Việt Nam")
               .latitude(BigDecimal.valueOf(lat))
               .longitude(BigDecimal.valueOf(lon))
               .starRating(3 + random.nextInt(3)) // 3 to 5 stars
               .checkInTime(LocalTime.of(14, 0))
               .checkOutTime(LocalTime.of(12, 0))
               .cancellationPolicy(policy)
               .isActive(true)
               .amenities(propertyAmenities)
               .businessRegistrationNumber("BRN-" + (100000 + random.nextInt(900000)))
               .taxCode("TAX-" + (10000000 + random.nextInt(90000000)))
               .legalOwnerName("Owner of " + propertyName)
               .build();

         property = propertyRepository.save(property);

         // Seed Media for Property (Main banner)
         String bannerImageId = UuidCreator.getTimeOrderedEpoch().toString();
         String bannerPublicId = MediaConstants.getPropertyPublicId(property.getId().toString(), bannerImageId);
         String bannerUrl = MediaConstants.getCloudinaryUrl(cloudName, bannerPublicId, "webp");

         if (assetsDir != null) {
            String bannerFileName = BANNER_FILE_NAMES[i % BANNER_FILE_NAMES.length];
            java.io.File bannerFile = new java.io.File(assetsDir, "banners/" + bannerFileName);
            if (bannerFile.exists()) {
               try {
                  byte[] fileBytes = java.nio.file.Files.readAllBytes(bannerFile.toPath());
                  cloudinaryService.upload(fileBytes, MediaConstants.getPropertyFolder(property.getId().toString()),
                        bannerImageId);
                  log.info("Uploaded banner for property {}: {}", propertyName, bannerFileName);
               } catch (Exception e) {
                  log.error("Failed to upload banner for property {}: {}", propertyName, bannerFileName, e);
               }
            } else {
               log.warn("Banner file not found: {}", bannerFile.getAbsolutePath());
            }
         }

         Media mainMedia = Media.builder()
               .url(bannerUrl)
               .publicId(bannerPublicId)
               .format("webp")
               .resourceType("image")
               .bytes(150000L)
               .entityId(property.getId())
               .entityType(MediaConstants.PROPERTY_TYPE)
               .isMain(true)
               .build();
         mediaRepository.save(mainMedia);

         // Seed Media for Property
         int numSubImages = 6 + random.nextInt(5);
         List<String> selectedCommonIds = new ArrayList<>(Arrays.asList(commonImageIds));
         Collections.shuffle(selectedCommonIds);
         for (int s = 0; s < numSubImages && s < selectedCommonIds.size(); s++) {
            String imgId = selectedCommonIds.get(s);
            String subPublicId = MediaConstants.COMMON_FOLDER + "/" + imgId;
            String subUrl = MediaConstants.getCloudinaryUrl(cloudName, subPublicId, "webp");
            Media subMedia = Media.builder()
                  .url(subUrl)
                  .publicId(subPublicId)
                  .format("webp")
                  .resourceType("image")
                  .bytes(100000L)
                  .entityId(property.getId())
                  .entityType(MediaConstants.PROPERTY_TYPE)
                  .isMain(false)
                  .build();
            mediaRepository.save(subMedia);
         }

         // Seed Room Types with different attributes and increasing prices
         int numRooms = 2 + random.nextInt(3); // 2 to 4 rooms
         BigDecimal basePrice = BigDecimal.valueOf(10 + random.nextInt(40)); // Base price for Room A (10 to 50 USD)

         for (int r = 0; r < numRooms; r++) {
            String roomName;
            BedType bedType;
            int capacityAdults;
            int capacityChildren;
            double size;
            BigDecimal price;

            switch (r) {
               case 0:
                  roomName = "Room A";
                  bedType = BedType.SINGLE;
                  capacityAdults = 1;
                  capacityChildren = 0;
                  size = 20.0;
                  price = basePrice;
                  break;
               case 1:
                  roomName = "Room B";
                  bedType = BedType.DOUBLE;
                  capacityAdults = 2;
                  capacityChildren = 1;
                  size = 35.0;
                  price = basePrice.add(BigDecimal.valueOf(20 + random.nextInt(20))); // Price increase
                  break;
               case 2:
                  roomName = "Room C";
                  bedType = BedType.KING;
                  capacityAdults = 2;
                  capacityChildren = 2;
                  size = 50.0;
                  price = basePrice.add(BigDecimal.valueOf(50 + random.nextInt(20))); // Price increase
                  break;
               case 3:
               default:
                  roomName = "Room D";
                  bedType = BedType.QUEEN;
                  capacityAdults = 4;
                  capacityChildren = 2;
                  size = 75.0;
                  price = basePrice.add(BigDecimal.valueOf(80 + random.nextInt(30))); // Price increase
                  break;
            }

            int totalRoomsCount = 10 - (r * 2); // 10, 8, 6, 4...

            RoomType roomType = RoomType.builder()
                  .property(property)
                  .name(roomName)
                  .description(
                        "This is a premium " + roomName + " featuring " + bedType + " bed at " + property.getName())
                  .basePrice(price)
                  .capacityAdults(capacityAdults)
                  .capacityChildren(capacityChildren)
                  .totalRooms(totalRoomsCount)
                  .roomSizeSqm(BigDecimal.valueOf(size))
                  .bedType(bedType)
                  .build();

            roomType = roomTypeRepository.save(roomType);

            // Seed Room Availability for the next 30 days
            LocalDate today = LocalDate.now();
            List<RoomAvailability> availabilities = new ArrayList<>();
            for (int day = 0; day < 30; day++) {
               LocalDate date = today.plusDays(day);
               RoomAvailability availability = RoomAvailability.builder()
                     .roomType(roomType)
                     .availabilityDate(date)
                     .availableCount(totalRoomsCount)
                     .priceOverride(null) // use base price
                     .isClosed(false)
                     .build();
               availabilities.add(availability);
            }
            roomAvailabilityRepository.saveAll(availabilities);
         }

         if ((i + 1) % 5 == 0) {
            log.info("Seeded {}/{} properties...", i + 1, properties.size());
         }
      }

      log.info("Successfully seeded all {} properties and room types into the database!", properties.size());
   }

   @Data
   public static class MockPropertyDto {
      private String name;
      private String description;
      private String address;
      private String city;
      private double latitude;
      private double longitude;
   }

}
