package com.omnibooking.services.pricing;

import com.omnibooking.model.*;
import com.omnibooking.model.enums.*;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CouponReservationServiceIntegrationTest {

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
      testUser = User.builder()
            .username("guest_user_" + UUID.randomUUID())
            .email("guest_" + UUID.randomUUID() + "@example.com")
            .password("encoded_pass")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Test Grand Hotel")
            .address("456 Property Ave")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testCoupon = Coupon.builder()
            .code("SAVE50_" + UUID.randomUUID().toString().substring(0, 8))
            .discountType(DiscountType.FIXED_AMOUNT)
            .discountValue(BigDecimal.valueOf(50.00))
            .minBookingAmount(BigDecimal.valueOf(100.00))
            .validFrom(Instant.now().minusSeconds(3600))
            .validUntil(Instant.now().plusSeconds(3600))
            .usageLimit(1)
            .usedCount(0)
            .reservedCount(0)
            .isActive(true)
            .property(testProperty)
            .build();
      testCoupon = couponRepository.save(testCoupon);
   }

   @Test
   public void shouldReserveAndConsumeCouponSuccessfully() {
      String sessionId = "session_" + UUID.randomUUID();
      CouponReservation reservation = couponReservationService.reserveCoupon(
            testCoupon.getId(),
            sessionId,
            testUser.getId(),
            testProperty.getId());

      assertNotNull(reservation.getId());
      assertNotNull(reservation.getReservationToken());
      assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());

      Coupon reloadedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
      assertEquals(1, reloadedCoupon.getReservedCount());

      couponReservationService.consumeReservation(reservation.getReservationToken());

      CouponReservation consumedRes = couponReservationRepository.findById(reservation.getId()).orElseThrow();
      assertEquals(ReservationStatus.CONSUMED, consumedRes.getStatus());

      Coupon reloadedCouponAfter = couponRepository.findById(testCoupon.getId()).orElseThrow();
      assertEquals(0, reloadedCouponAfter.getReservedCount());
      assertEquals(1, reloadedCouponAfter.getUsedCount());
   }

   @Test
   public void shouldPreventReservationWhenLimitExceeded() {
      String session1 = "session_1_" + UUID.randomUUID();
      String session2 = "session_2_" + UUID.randomUUID();

      couponReservationService.reserveCoupon(
            testCoupon.getId(),
            session1,
            testUser.getId(),
            testProperty.getId());

      assertThrows(IllegalStateException.class, () -> {
         couponReservationService.reserveCoupon(
               testCoupon.getId(),
               session2,
               testUser.getId(),
               testProperty.getId());
      });
   }

}
