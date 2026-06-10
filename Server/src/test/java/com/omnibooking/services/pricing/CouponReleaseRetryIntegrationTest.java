package com.omnibooking.services.pricing;

import com.omnibooking.model.Coupon;
import com.omnibooking.model.CouponReleaseRetry;
import com.omnibooking.model.CouponReservation;
import com.omnibooking.model.Property;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.DiscountType;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.pricing.CouponReleaseRetryRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CouponReleaseRetryIntegrationTest {

   @Autowired
   private CouponReleaseRetryService couponReleaseRetryService;

   @Autowired
   private CouponReleaseRetryRepository couponReleaseRetryRepository;

   @Autowired
   private CouponReservationService couponReservationService;

   @Autowired
   private CouponRepository couponRepository;

   @Autowired
   private CouponReservationRepository couponReservationRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private ReviewRepository reviewRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

   @Autowired
   private BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   @Autowired
   private BookingStatusLogRepository bookingStatusLogRepository;

   @Autowired
   private TransactionRepository transactionRepository;

   @Autowired
   private InventoryOperationRepository inventoryOperationRepository;

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

   @BeforeEach
   public void setUp() {
      // Clear tables in dependency order
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

      testUser = User.builder()
            .username("retry_guest_" + UUID.randomUUID().toString().substring(0, 8))
            .email("retry_guest_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
            .password("encoded_pass")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Retry Palace")
            .address("789 Retry road")
            .city("Da Nang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testCoupon = Coupon.builder()
            .code("RETRY50_" + UUID.randomUUID().toString().substring(0, 8))
            .discountType(DiscountType.FIXED_AMOUNT)
            .discountValue(BigDecimal.valueOf(50.00))
            .minBookingAmount(BigDecimal.valueOf(100.00))
            .validFrom(Instant.now().minusSeconds(3600))
            .validUntil(Instant.now().plusSeconds(3600))
            .usageLimit(5)
            .usedCount(0)
            .reservedCount(0)
            .isActive(true)
            .property(testProperty)
            .build();
      testCoupon = couponRepository.save(testCoupon);
   }

   @Test
   public void shouldCreateUniquePendingRetryAndProcessSuccessfully() {
      UUID bookingId = UUID.randomUUID();
      UUID couponId = testCoupon.getId();
      UUID userId = testUser.getId();

      // Create a reservation and consume it to get into CONSUMED status
      String sessionId = "session_" + UUID.randomUUID();
      CouponReservation reservation = couponReservationService.reserveCoupon(
            couponId, sessionId, userId, testProperty.getId());
      couponReservationService.consumeReservation(reservation.getReservationToken());

      Coupon couponPre = couponRepository.findById(couponId).orElseThrow();
      assertEquals(1, couponPre.getUsedCount());

      // Try creating retry records
      couponReleaseRetryService.createRetry(bookingId, couponId, userId);
      couponReleaseRetryService.createRetry(bookingId, couponId, userId); // Duplicate attempt

      // Verify only one PENDING retry record exists
      long count = couponReleaseRetryRepository.countByStatus("PENDING");
      assertEquals(1, count);

      Optional<CouponReleaseRetry> retryOpt = couponReleaseRetryRepository
            .findByBookingIdAndCouponIdAndStatus(bookingId, couponId, "PENDING");
      assertTrue(retryOpt.isPresent());
      CouponReleaseRetry retry = retryOpt.get();
      assertEquals(0, retry.getAttemptCount());

      // Set nextAttemptAt to past to allow worker processing
      retry.setNextAttemptAt(Instant.now().minusSeconds(10));
      couponReleaseRetryRepository.saveAndFlush(retry);

      // Trigger worker execution
      couponReleaseRetryService.processPendingRetries();

      // Verify successful processing
      CouponReleaseRetry processed = couponReleaseRetryRepository.findById(retry.getId()).orElseThrow();
      assertEquals("SUCCESS", processed.getStatus());
      assertEquals(1, processed.getAttemptCount());

      Coupon couponPost = couponRepository.findById(couponId).orElseThrow();
      assertEquals(0, couponPost.getUsedCount()); // Coupon count refunded correctly!

      // Test idempotency: call refundReservation again
      couponReservationService.refundReservation(couponId, userId);
      Coupon couponIdempotent = couponRepository.findById(couponId).orElseThrow();
      assertEquals(0, couponIdempotent.getUsedCount()); // Usage count NOT decremented again!
   }

   @Test
   public void shouldPurgeRetryRecordsBasedOnRetentionPolicy() {
      UUID bookingId1 = UUID.randomUUID();
      UUID bookingId2 = UUID.randomUUID();
      UUID couponId = testCoupon.getId();
      UUID userId = testUser.getId();

      // Insert older SUCCESS retry record
      CouponReleaseRetry successRetry = CouponReleaseRetry.builder()
            .bookingId(bookingId1)
            .couponId(couponId)
            .userId(userId)
            .status("SUCCESS")
            .attemptCount(1)
            .lastAttemptAt(Instant.now().minus(31, ChronoUnit.DAYS))
            .nextAttemptAt(Instant.now())
            .build();
      couponReleaseRetryRepository.saveAndFlush(successRetry);

      // Insert older FAILED retry record
      CouponReleaseRetry failedRetry = CouponReleaseRetry.builder()
            .bookingId(bookingId2)
            .couponId(couponId)
            .userId(userId)
            .status("FAILED")
            .attemptCount(5)
            .lastAttemptAt(Instant.now().minus(181, ChronoUnit.DAYS))
            .nextAttemptAt(Instant.now())
            .build();
      couponReleaseRetryRepository.saveAndFlush(failedRetry);

      // Run cleanup
      couponReleaseRetryService.purgeOldRetryRecords();

      // Verify both are purged
      assertFalse(couponReleaseRetryRepository.findById(successRetry.getId()).isPresent());
      assertFalse(couponReleaseRetryRepository.findById(failedRetry.getId()).isPresent());
   }
}
