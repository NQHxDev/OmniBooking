package com.omnibooking.worker;

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
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.Sentry;
import io.sentry.SentryLevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("test")
public class ReconciliationJobIntegrationTest {

   @Autowired
   private BookingReconciliationWorker reconciliationWorker;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private UserRepository userRepository;

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
   private InventoryOperationRepository inventoryOperationRepository;

   @Autowired
   private TransactionRepository transactionRepository;

   @Autowired
   private OutboxEventRepository outboxEventRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private CouponRepository couponRepository;

   @Autowired
   private MeterRegistry meterRegistry;

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
            .username("recon_user_" + UUID.randomUUID().toString().substring(0, 8))
            .email("recon_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Recon Palace")
            .address("123 Recon Road")
            .city("Da Nang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Recon Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(10) // capacity limit is 10 rooms
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);
   }

   @Test
   public void testReconcileRoomAvailability_WithOverfilledAvailability_ShouldAlertAndNotFix() {
      // Create overfilled availability: availableCount (15) > totalRooms (10)
      LocalDate testDate = LocalDate.now().plusDays(2);
      RoomAvailability overfilledAvailability = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(testDate)
            .availableCount(15) // Overfilled!
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(overfilledAvailability);

      // Get initial counter value
      double initialAnomalyCount = meterRegistry.counter("omnibooking.reconciliation.anomaly.total").count();

      // Run reconciliation using try-with-resources for mockStatic to avoid leaking
      // mocked static Sentry across threads
      try (MockedStatic<Sentry> mockedSentry = Mockito.mockStatic(Sentry.class)) {
         reconciliationWorker.reconcile();

         // Verify Sentry alert was captured
         mockedSentry.verify(() -> Sentry.captureMessage(anyString(), any(SentryLevel.class)), times(1));
      }

      // Verify availableCount is still 15 (Alert-Only approach, no auto-repair)
      RoomAvailability afterReconcile = roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .orElseThrow();
      assertEquals(15, afterReconcile.getAvailableCount(), "Overfilled room count must not be modified in database");

      // Verify micrometer metrics incremented
      double finalAnomalyCount = meterRegistry.counter("omnibooking.reconciliation.anomaly.total").count();
      assertEquals(initialAnomalyCount + 1, finalAnomalyCount,
            "omnibooking.reconciliation.anomaly.total metric should increment by 1");
   }

}
