package com.omnibooking.services.booking.impl;

import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.booking.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class BookingConcurrencyTest {

   @Autowired
   private BookingService bookingService;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   @Autowired
   private BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

   @Autowired
   private BookingStatusLogRepository bookingStatusLogRepository;

   @Autowired
   private InventoryOperationRepository inventoryOperationRepository;

   @Autowired
   private CouponRepository couponRepository;

   @Autowired
   private TransactionRepository transactionRepository;

   @Autowired
   private OutboxEventRepository outboxEventRepository;

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

   @Autowired
   private CouponReservationRepository couponReservationRepository;

   @Autowired
   private ReviewRepository reviewRepository;

   private RoomType testRoomType;

   private User testUser;

   private Property testProperty;

   private void cleanDatabase() {
      reviewRepository.deleteAll();
      couponReservationRepository.deleteAll();
      bookingAppliedRuleVersionRepository.deleteAll();
      bookingPriceBreakdownRepository.deleteAll();
      bookingStatusLogRepository.deleteAll();
      inventoryOperationRepository.deleteAll();
      transactionRepository.deleteAll();
      outboxEventRepository.deleteAll();
      bookingRepository.deleteAll();
      roomAvailabilityRepository.deleteAll();
      roomTypeRepository.deleteAll();
      couponRepository.deleteAll();
      propertyRepository.deleteAll();
      userRepository.deleteAll();
   }

   @BeforeEach
   public void setUp() {
      cleanDatabase();

      testUser = User.builder()
            .username("conc_user_" + UUID.randomUUID().toString().substring(0, 8))
            .email("concurrency_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .roles(java.util.Collections.emptySet())
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Concurrency Palace")
            .address("789 Concurrency Road")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Single Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(1)
            .capacityChildren(0)
            .totalRooms(1) // Only 1 room exists!
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);

      // Initialize availability for tomorrow
      RoomAvailability availability = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(LocalDate.now().plusDays(1))
            .availableCount(1) // 1 room available
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(availability);
   }

   @Test
   public void testConcurrentBookings_ShouldNotOversell() throws InterruptedException {
      int threadCount = 10;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threadCount);

      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failCount = new AtomicInteger(0);

      LocalDate checkIn = LocalDate.now().plusDays(1);
      LocalDate checkOut = checkIn.plusDays(1);

      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(testRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("Concurrency Guest")
            .guestEmail("guest@example.com")
            .currency("USD")
            .paymentMethod("cash") // Cash booking confirmed immediately, no deposit required
            .build();

      UserPrincipal principal = UserPrincipal.create(testUser);

      for (int i = 0; i < threadCount; i++) {
         executor.submit(() -> {
            try {
               latch.await(); // Wait for start signal
               bookingService.createBooking(request, principal);
               successCount.incrementAndGet();
            } catch (Exception e) {
               System.err.println("Booking creation failed: " + e.getMessage());
               e.printStackTrace();
               failCount.incrementAndGet();
            } finally {
               doneLatch.countDown();
            }
         });
      }

      latch.countDown(); // Start all threads concurrently
      doneLatch.await(); // Wait for all threads to complete
      executor.shutdown();

      assertEquals(1, successCount.get(), "Only 1 booking should succeed");
      assertEquals(9, failCount.get(), "9 bookings should fail");

      RoomAvailability availability = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), checkIn)
            .orElseThrow();
      assertEquals(0, availability.getAvailableCount(), "Available count should be 0");
   }

}
