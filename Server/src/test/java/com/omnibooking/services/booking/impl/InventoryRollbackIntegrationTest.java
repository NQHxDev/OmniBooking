package com.omnibooking.services.booking.impl;

import com.omnibooking.exception.AppException;
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
import com.omnibooking.repository.pricing.CouponReleaseRetryRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryRollbackIntegrationTest {

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
   private CouponReleaseRetryRepository couponReleaseRetryRepository;

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

   @Autowired
   private PlatformTransactionManager transactionManager;

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
      couponReleaseRetryRepository.deleteAll();
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
            .username("rollback_usr_" + rand)
            .email("rollback_" + rand + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Rollback Grand Palace")
            .address("999 Rollback Ave")
            .city("Da Nang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Rollback Room")
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
   public void shouldRollbackInventoryDeductionOnDownstreamFailure() {
      Booking booking = Booking.builder()
            .user(testUser)
            .roomType(testRoomType)
            .checkInDate(testDate)
            .checkOutDate(testDate.plusDays(1))
            .numRooms(2)
            .status(BookingStatus.PENDING_PAYMENT)
            .totalPrice(BigDecimal.valueOf(300.00))
            .finalPrice(BigDecimal.valueOf(300.00))
            .build();

      TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

      // Execute transaction that reserves inventory and then throws an exception
      assertThrows(RuntimeException.class, () -> {
         txTemplate.execute(status -> {
            bookingRepository.save(booking);
            // Deduct availability atomically
            inventoryService.reserveInventory(booking, testRoomType, testDate, testDate.plusDays(1), 2);

            // Verify inside transaction that availability count was deducted (10 -> 8)
            RoomAvailability insideAvailability = roomAvailabilityRepository
                  .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
                  .orElseThrow();
            assertEquals(8, insideAvailability.getAvailableCount());

            // Force a downstream failure
            throw new RuntimeException("Forced downstream failure to trigger transaction rollback");
         });
      });

      // Verify outside the transaction that availability count rolled back to
      // original (10)
      RoomAvailability outsideAvailability = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(10, outsideAvailability.getAvailableCount());

      // Verify no reserve operations are persisted in the ledger (should have rolled
      // back)
      long operationCount = inventoryOperationRepository.count();
      assertEquals(0, operationCount);
   }

   @Test
   public void shouldPreventReservationWhenInventoryExceededAndRollbackPartialSteps() {
      Booking booking = Booking.builder()
            .user(testUser)
            .roomType(testRoomType)
            .checkInDate(testDate)
            .checkOutDate(testDate.plusDays(2)) // 2 days booking
            .numRooms(6) // Request 6 rooms. (Day 1: 10 available, Day 2: not created yet so
                         // totalRooms=20)
            .status(BookingStatus.PENDING_PAYMENT)
            .totalPrice(BigDecimal.valueOf(1800.00))
            .finalPrice(BigDecimal.valueOf(1800.00))
            .build();

      // For day 2, we will close availability to force failure on day 2
      TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
      txTemplate.execute(status -> {
         RoomAvailability day2 = RoomAvailability.builder()
               .roomType(testRoomType)
               .availabilityDate(testDate.plusDays(1))
               .availableCount(5) // Only 5 available on day 2 (fails the 6-room request)
               .isClosed(false)
               .build();
         roomAvailabilityRepository.save(day2);
         return null;
      });

      // Attempt to book 6 rooms. Day 1 has 10 (deduction succeeds), Day 2 has 5
      // (deduction fails).
      // Since it is transactional, Day 1 must roll back!
      assertThrows(AppException.class, () -> {
         txTemplate.execute(status -> {
            bookingRepository.save(booking);
            inventoryService.reserveInventory(booking, testRoomType, testDate, testDate.plusDays(2), 6);
            return null;
         });
      });

      // Verify that Day 1 is restored back to 10
      RoomAvailability day1Availability = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(10, day1Availability.getAvailableCount());

      // Verify that Day 2 remains 5
      RoomAvailability day2Availability = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate.plusDays(1))
            .orElseThrow();
      assertEquals(5, day2Availability.getAvailableCount());
   }

}
