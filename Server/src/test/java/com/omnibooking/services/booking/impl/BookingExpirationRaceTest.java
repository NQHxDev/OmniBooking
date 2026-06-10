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
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.services.booking.BookingService;
import com.omnibooking.worker.BookingExpirationWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class BookingExpirationRaceTest {

   @Autowired
   private BookingService bookingService;

   @Autowired
   private BookingExpirationWorker bookingExpirationWorker;

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

      Mockito.doReturn(CompletableFuture.completedFuture(null))
            .when(kafkaTemplate).send(
                  ArgumentMatchers.anyString(),
                  ArgumentMatchers.any(),
                  ArgumentMatchers.any());

      Mockito.doReturn(CompletableFuture.completedFuture(null))
            .when(kafkaTemplate).send(
                  ArgumentMatchers.anyString(),
                  ArgumentMatchers.any());

      testUser = User.builder()
            .username("race_usr_" + UUID.randomUUID().toString().substring(0, 8))
            .email("race_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .roles(Collections.emptySet())
            .build();
      LoggerFactory.getLogger(BookingExpirationRaceTest.class)
            .info("BookingExpirationRaceTest using kafkaTemplate: {}", System.identityHashCode(kafkaTemplate));
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Race Palace")
            .address("789 Race Road")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Race Room")
            .basePrice(BigDecimal.valueOf(200.00))
            .capacityAdults(2)
            .capacityChildren(0)
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
   public void testConfirmationAndExpirationRace_ShouldOnlyOneSucceed() throws InterruptedException {
      LocalDate checkIn = LocalDate.now().plusDays(6);
      LocalDate checkOut = checkIn.plusDays(1);

      // Create a booking requesting momo deposit payment (initial status:
      // PENDING_PAYMENT)
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(testRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("Race Guest")
            .guestEmail("race_guest@example.com")
            .guestPhone("0987654321")
            .currency("USD")
            .paymentMethod("momo")
            .build();

      BookingResponse response = bookingService.createBooking(request,
            com.omnibooking.security.UserPrincipal.create(testUser));
      UUID bookingId = response.getId();

      // Check initial state
      BookingStatus initialStatus = bookingRepository.findById(bookingId).orElseThrow().getStatus();
      assertEquals(BookingStatus.PENDING_PAYMENT, initialStatus);

      // RoomAvailability count should be 4
      RoomAvailability initialAvail = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), checkIn)
            .orElseThrow();
      assertEquals(4, initialAvail.getAvailableCount());

      ExecutorService executor = Executors.newFixedThreadPool(2);
      CountDownLatch latch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(2);

      AtomicBoolean confirmSucceeded = new AtomicBoolean(false);
      AtomicBoolean expireSucceeded = new AtomicBoolean(false);

      // Thread 1: confirm booking
      executor.submit(() -> {
         try {
            latch.await();
            bookingService.confirmBooking(bookingId, "momo", "provider_tx_race_123", "{}");
            confirmSucceeded.set(true);
         } catch (Exception e) {
            LoggerFactory.getLogger(BookingExpirationRaceTest.class)
                  .error("Confirm Booking thread failed", e);
         } finally {
            doneLatch.countDown();
         }
      });

      // Thread 2: expire booking (pass an Instant in the future so that it is past
      // booking's expiry)
      executor.submit(() -> {
         try {
            latch.await();
            bookingExpirationWorker.processExpiration(bookingId, Instant.now().plus(2, ChronoUnit.DAYS));
            expireSucceeded.set(true);
         } catch (Exception e) {
            LoggerFactory.getLogger(BookingExpirationRaceTest.class)
                  .error("Expire Booking thread failed", e);
         } finally {
            doneLatch.countDown();
         }
      });

      latch.countDown(); // Start both racing threads
      doneLatch.await();
      executor.shutdown();

      // Check final status of the booking
      BookingStatus finalStatus = bookingRepository.findById(bookingId).orElseThrow().getStatus();
      assertTrue(finalStatus == BookingStatus.CONFIRMED || finalStatus == BookingStatus.EXPIRED);

      // Verify side-effects match the final status
      RoomAvailability finalAvail = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), checkIn)
            .orElseThrow();

      if (finalStatus == BookingStatus.CONFIRMED) {
         // Confirmed: transaction saved, outbox saved, inventory still reserved (count =
         // 4)
         long txCount = transactionRepository.findAll().stream()
               .filter(t -> t.getBooking().getId().equals(bookingId))
               .count();
         assertEquals(1, txCount);

         long outboxCount = outboxEventRepository.findAll().stream()
               .filter(e -> e.getAggregateId().equals(bookingId))
               .count();
         assertEquals(1, outboxCount);

         assertEquals(4, finalAvail.getAvailableCount());
      } else {
         // Expired: transaction not saved, outbox not saved, inventory released (count =
         // 5)
         long txCount = transactionRepository.findAll().stream()
               .filter(t -> t.getBooking().getId().equals(bookingId))
               .count();
         assertEquals(0, txCount);

         long outboxCount = outboxEventRepository.findAll().stream()
               .filter(e -> e.getAggregateId().equals(bookingId))
               .count();
         assertEquals(0, outboxCount);

         assertEquals(5, finalAvail.getAvailableCount());
      }
   }

}
