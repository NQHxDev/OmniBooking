package com.omnibooking.services.booking.impl;

import com.omnibooking.model.Coupon;
import com.omnibooking.model.Property;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.DiscountType;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.pricing.CouponReservationService;
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
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class CouponRaceConditionTest {

   @Autowired
   private CouponReservationService couponReservationService;

   @Autowired
   private CouponRepository couponRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private BookingStatusLogRepository bookingStatusLogRepository;

   @Autowired
   private InventoryOperationRepository inventoryOperationRepository;

   @Autowired
   private BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   @Autowired
   private BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

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

   private Property testProperty;

   private User testUser;

   private Coupon testCoupon;

   private void cleanDatabase() {
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
            .username("coupon_usr_" + UUID.randomUUID().toString().substring(0, 8))
            .email("coupon_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Coupon Hotel")
            .address("101 Coupon Road")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testCoupon = Coupon.builder()
            .code("RACE50")
            .discountType(DiscountType.FIXED_AMOUNT)
            .discountValue(BigDecimal.valueOf(50.00))
            .minBookingAmount(BigDecimal.valueOf(100.00))
            .validFrom(Instant.now().minusSeconds(3600))
            .validUntil(Instant.now().plusSeconds(3600))
            .usageLimit(1) // Usage limit of 1
            .usedCount(0)
            .reservedCount(0)
            .isActive(true)
            .property(testProperty)
            .build();
      testCoupon = couponRepository.save(testCoupon);
   }

   @Test
   public void testConcurrentCouponReservation_ShouldOnlySucceedForOne() throws InterruptedException {
      int threadCount = 5;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threadCount);

      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
         String sessionId = "session_" + i + "_" + UUID.randomUUID();
         executor.submit(() -> {
            try {
               latch.await();
               couponReservationService.reserveCoupon(
                     testCoupon.getId(),
                     sessionId,
                     testUser.getId(),
                     testProperty.getId());
               successCount.incrementAndGet();
            } catch (Exception e) {
               failCount.incrementAndGet();
            } finally {
               doneLatch.countDown();
            }
         });
      }

      latch.countDown();
      doneLatch.await();
      executor.shutdown();

      assertEquals(1, successCount.get(), "Only 1 reservation should succeed");
      assertEquals(4, failCount.get(), "4 reservations should fail");

      Coupon reloadedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
      assertEquals(1, reloadedCoupon.getReservedCount(), "Reserved count should be 1");
   }

}
