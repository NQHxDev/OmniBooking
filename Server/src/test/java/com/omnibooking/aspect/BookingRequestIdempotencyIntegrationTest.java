package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingRequestIdempotencyIntegrationTest {

   private interface MockValueOperations extends ValueOperations<String, String> {
   }

   private interface MockHashOperations extends HashOperations<String, Object, Object> {
   }

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private IdempotencyKeyRepository idempotencyKeyRepository;

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
   private ObjectMapper objectMapper;

   @Autowired
   private JWTService jwtService;

   private String accessToken;

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
   private LocalDate testDate;

   private String sessionId;
   private String csrfToken;

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
      idempotencyKeyRepository.deleteAll();
      userRepository.deleteAll();
   }

   @BeforeEach
   public void setUp() {
      cleanDatabase();

      testDate = LocalDate.now().plusDays(2);
      sessionId = UUID.randomUUID().toString();
      csrfToken = CookieUtils.calculateCsrfToken(sessionId, CookieUtils.csrfSecret);

      testUser = User.builder()
            .username("book_idemp_user_" + UUID.randomUUID().toString().substring(0, 8))
            .email("book_idemp_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      UUID userId = testUser.getId();
      UUID sessionIdUuid = UUID.fromString(sessionId);
      String fingerprint = "fingerprint";
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);
      accessToken = jwtService.generateAccessToken(
            userId,
            Collections.singletonList(SecurityConstants.Roles.USER),
            sessionIdUuid,
            fgpHash);

      ValueOperations<String, String> valueOps = Mockito.mock(MockValueOperations.class);
      Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

      HashOperations<String, Object, Object> hashOps = Mockito.mock(MockHashOperations.class);
      Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

      Map<Object, Object> sessionMap = new HashMap<>();
      sessionMap.put("userId", userId.toString());
      sessionMap.put("roles", "[\"ROLE_USER\"]");
      sessionMap.put("active", "true");
      Mockito.when(hashOps.entries(Mockito.anyString())).thenReturn(sessionMap);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Idemp Booking Palace")
            .address("777 Idemp Street")
            .city("Da Nang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Idemp Booking Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(5)
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);

      // Save availability
      RoomAvailability availability = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(testDate)
            .availableCount(5)
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(availability);

      RoomAvailability availabilityNextDay = RoomAvailability.builder()
            .roomType(testRoomType)
            .availabilityDate(testDate.plusDays(1))
            .availableCount(5)
            .isClosed(false)
            .build();
      roomAvailabilityRepository.save(availabilityNextDay);
   }

   @Test
   public void testBasicBookingIdempotencyFlow() throws Exception {
      String key = UUID.randomUUID().toString();
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(testRoomType.getId())
            .checkInDate(testDate)
            .checkOutDate(testDate.plusDays(1))
            .numRooms(1)
            .guestName("Idemp Guest")
            .guestEmail("idemp_guest@example.com")
            .currency("USD")
            .paymentMethod("cash")
            .build();

      String content = objectMapper.writeValueAsString(request);

      // First Request
      MvcResult firstResult = mockMvc.perform(post("/bookings")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, accessToken))
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.FINGERPRINT, "fingerprint"))
            .header("x-fgp", "fingerprint")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Booking created successfully"))
            .andReturn();

      String firstResponse = firstResult.getResponse().getContentAsString();

      // Second Request (Duplicate Key) -> Should replay the cached response
      MvcResult secondResult = mockMvc.perform(post("/bookings")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, accessToken))
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.FINGERPRINT, "fingerprint"))
            .header("x-fgp", "fingerprint")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andReturn();

      String secondResponse = secondResult.getResponse().getContentAsString();

      assertEquals(firstResponse, secondResponse, "Replayed response must match original response exactly");
   }

   @Test
   public void testConcurrentBookingRequestsIdempotency() throws Exception {
      final int concurrentRequests = 10;
      final String key = UUID.randomUUID().toString();
      final CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(testRoomType.getId())
            .checkInDate(testDate)
            .checkOutDate(testDate.plusDays(1))
            .numRooms(1)
            .guestName("Concurrent Idemp Guest")
            .guestEmail("concurrent_idemp_guest@example.com")
            .currency("USD")
            .paymentMethod("cash")
            .build();

      final String content = objectMapper.writeValueAsString(request);

      ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
      CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger conflictCount = new AtomicInteger(0);
      final AtomicInteger processingCount = new AtomicInteger(0);
      final AtomicInteger otherCount = new AtomicInteger(0);

      for (int i = 0; i < concurrentRequests; i++) {
         executor.submit(() -> {
            readyLatch.countDown();
            try {
               startLatch.await();
               MvcResult result = mockMvc.perform(post("/bookings")
                     .header("Origin", "http://localhost:3000")
                     .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, accessToken))
                     .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
                     .cookie(new Cookie(CookieUtils.FINGERPRINT, "fingerprint"))
                     .header("x-fgp", "fingerprint")
                     .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
                     .header("X-CSRF-Token", csrfToken)
                     .header("Idempotency-Key", key)
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(content))
                     .andReturn();

               int status = result.getResponse().getStatus();
               if (status == 200) {
                  successCount.incrementAndGet();
               } else if (status == 409) {
                  String responseBody = result.getResponse().getContentAsString();
                  if (responseBody.contains("IDEM_002")) {
                     processingCount.incrementAndGet();
                  } else {
                     conflictCount.incrementAndGet();
                  }
               } else {
                  otherCount.incrementAndGet();
               }
            } catch (Exception e) {
               e.printStackTrace();
            } finally {
               finishLatch.countDown();
            }
         });
      }

      assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
      startLatch.countDown();

      assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
      executor.shutdown();

      assertEquals(1, successCount.get(), "Only one booking request must succeed");
      assertEquals(concurrentRequests - 1, processingCount.get(),
            "All other concurrent requests must receive 409 Conflict (PROCESSING status)");
      assertEquals(0, conflictCount.get());
      assertEquals(0, otherCount.get());
   }

}
