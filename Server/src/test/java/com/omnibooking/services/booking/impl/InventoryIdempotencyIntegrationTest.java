package com.omnibooking.services.booking.impl;

import com.omnibooking.model.Booking;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.services.booking.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryIdempotencyIntegrationTest {

   @Autowired
   private InventoryService inventoryService;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private InventoryOperationRepository inventoryOperationRepository;

   @Autowired
   private ReviewRepository reviewRepository;

   @Autowired
   private CouponReservationRepository couponReservationRepository;

   @Autowired
   private BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   @Autowired
   private BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

   @Autowired
   private BookingStatusLogRepository bookingStatusLogRepository;

   @Autowired
   private TransactionRepository transactionRepository;

   @Autowired
   private CouponRepository couponRepository;

   @MockitoBean
   private ElasticsearchOperations elasticsearchOperations;

   @MockitoBean
   private PropertyElasticsearchRepository propertyElasticsearchRepository;

   @MockitoBean
   private DestinationElasticsearchRepository destinationElasticsearchRepository;

   @MockitoBean
   private KafkaTemplate<String, Object> kafkaTemplate;

   @MockitoBean
   private KafkaAdmin kafkaAdmin;

   @MockitoBean
   private StringRedisTemplate stringRedisTemplate;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   private Property testProperty;

   private RoomType testRoomType;

   private User testUser;

   private LocalDate testDate;

   @BeforeEach
   public void setUp() {
      reviewRepository.deleteAll();
      couponReservationRepository.deleteAll();
      bookingAppliedRuleVersionRepository.deleteAll();
      bookingPriceBreakdownRepository.deleteAll();
      bookingStatusLogRepository.deleteAll();
      inventoryOperationRepository.deleteAll();
      transactionRepository.deleteAll();
      bookingRepository.deleteAll();
      roomAvailabilityRepository.deleteAll();
      roomTypeRepository.deleteAll();
      couponRepository.deleteAll();
      propertyRepository.deleteAll();
      userRepository.deleteAll();

      testDate = LocalDate.now().plusDays(5);

      String rand = UUID.randomUUID().toString().substring(0, 8);
      testUser = User.builder()
            .username("idemp_usr_" + rand)
            .email("idemp_" + rand + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Idemp Grand Palace")
            .address("999 Idemp Ave")
            .city("Da Nang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Idemp Room")
            .basePrice(BigDecimal.valueOf(150.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(20)
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);

      // Pre-populate availability for testDate
      RoomAvailability availability = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(testDate)
            .availableCount(10) // 10 rooms available
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(availability);
   }

   @Test
   public void testDuplicateRelease_ShouldBeIdempotent() {
      Booking booking = Booking.builder()
            .user(testUser)
            .roomType(testRoomType)
            .checkInDate(testDate)
            .checkOutDate(testDate.plusDays(1))
            .numRooms(2)
            .status(BookingStatus.CANCELLED)
            .totalPrice(BigDecimal.valueOf(300.00))
            .finalPrice(BigDecimal.valueOf(300.00))
            .guestName("Idemp Guest")
            .guestEmail("guest@example.com")
            .build();
      booking = bookingRepository.save(booking);

      // Initially deduct inventory
      inventoryService.deductInventoryOnly(testRoomType, testDate, testDate.plusDays(1), 2);
      inventoryService.writeReserveAuditLog(booking, testRoomType, testDate, testDate.plusDays(1), 2);

      RoomAvailability beforeRelease = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(8, beforeRelease.getAvailableCount(), "Available count should be 8 after reserve");

      // First release call
      inventoryService.releaseInventory(booking);

      RoomAvailability afterFirstRelease = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(10, afterFirstRelease.getAvailableCount(), "Available count should be restored to 10");

      // Second release call (duplicate)
      inventoryService.releaseInventory(booking);

      RoomAvailability afterSecondRelease = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(10, afterSecondRelease.getAvailableCount(), "Available count should remain 10 (idempotent)");
   }

}
