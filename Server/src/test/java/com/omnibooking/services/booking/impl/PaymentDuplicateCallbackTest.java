package com.omnibooking.services.booking.impl;

import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BookingStatus;
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
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.services.booking.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentDuplicateCallbackTest {

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
   private TransactionRepository transactionRepository;

   @Autowired
   private OutboxEventRepository outboxEventRepository;

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
   private CouponReservationRepository couponReservationRepository;

   @Autowired
   private ReviewRepository reviewRepository;

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
            .username("pay_conc_user_" + UUID.randomUUID().toString().substring(0, 8))
            .email("pay_concurrency_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .roles(Collections.emptySet())
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Pay Concurrency Palace")
            .address("789 Pay Concurrency Road")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Deluxe Room")
            .basePrice(BigDecimal.valueOf(150.00))
            .capacityAdults(2)
            .capacityChildren(1)
            .totalRooms(5)
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);

      // Initialize availability for 6 days from now
      RoomAvailability availability = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(LocalDate.now().plusDays(6))
            .availableCount(5)
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(availability);
   }

   @Test
   public void testConcurrentPaymentCallbacks_ShouldOnlyConfirmOnceAndDedupTransactions() throws InterruptedException {
      LocalDate checkIn = LocalDate.now().plusDays(6);
      LocalDate checkOut = checkIn.plusDays(1);

      // Create a booking requesting momo deposit payment (which sets status to
      // PENDING_PAYMENT)
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(testRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("Momo Concurrency Guest")
            .guestEmail("momo_concurrency@example.com")
            .guestPhone("0987654321")
            .currency("USD")
            .paymentMethod("momo")
            .build();

      BookingResponse response = bookingService.createBooking(request, UserPrincipal.create(testUser));
      UUID bookingId = response.getId();

      // Check initial state
      BookingStatus initialStatus = bookingRepository.findById(bookingId).orElseThrow().getStatus();
      assertEquals(BookingStatus.PENDING_PAYMENT, initialStatus);

      int threadCount = 5;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threadCount);

      AtomicInteger callbackAttemptCount = new AtomicInteger(0);
      AtomicInteger callbackFailureCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
         executor.submit(() -> {
            try {
               latch.await(); // Wait for start signal
               bookingService.confirmBooking(bookingId, "momo", "provider_tx_unique_12345", "{}");
               callbackAttemptCount.incrementAndGet();
            } catch (Exception e) {
               callbackFailureCount.incrementAndGet();
            } finally {
               doneLatch.countDown();
            }
         });
      }

      latch.countDown(); // Start all threads concurrently
      doneLatch.await(); // Wait for all threads to complete
      executor.shutdown();

      // Check state after concurrent callbacks
      BookingStatus finalStatus = bookingRepository.findById(bookingId).orElseThrow().getStatus();
      assertEquals(BookingStatus.CONFIRMED, finalStatus);

      // Check that only 1 transaction was saved
      long transactionCount = transactionRepository.findAll().stream()
            .filter(t -> t.getBooking().getId().equals(bookingId))
            .count();
      assertEquals(1, transactionCount, "Exactly 1 transaction should be saved");

      // Check that only 1 outbox event was generated for this booking (from
      // confirmation)
      long outboxCount = outboxEventRepository.findAll().stream()
            .filter(e -> e.getAggregateId().equals(bookingId))
            .count();
      assertEquals(1, outboxCount, "Exactly 1 outbox event should be generated");
   }

}
