package com.omnibooking.services.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.RoomTypeRequest;
import com.omnibooking.model.Property;
import com.omnibooking.model.PropertyCreatedOutbox;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.BedType;
import com.omnibooking.model.enums.OutboxStatus;
import com.omnibooking.model.enums.PropertyStatus;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import com.omnibooking.repository.infra.PropertyCreatedOutboxRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.worker.PropertyCreatedOutboxWorker;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PropertyIdempotencyOutboxIntegrationTest {

   private interface MockValueOperations extends ValueOperations<String, String> {
   }

   private interface MockHashOperations extends HashOperations<String, Object, Object> {
   }

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private PropertyCreatedOutboxRepository propertyCreatedOutboxRepository;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private IdempotencyKeyRepository idempotencyKeyRepository;

   @Autowired
   private PropertyCreatedOutboxWorker propertyCreatedOutboxWorker;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private JWTService jwtService;

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

   private User testPartner;
   private String partnerToken;
   private String sessionId;
   private String csrfToken;

   private void cleanDatabase() {
      propertyCreatedOutboxRepository.deleteAll();
      roomAvailabilityRepository.deleteAll();
      roomTypeRepository.deleteAll();
      propertyRepository.deleteAll();
      idempotencyKeyRepository.deleteAll();
      userRepository.deleteAll();
   }

   @BeforeEach
   public void setUp() {
      cleanDatabase();

      sessionId = UUID.randomUUID().toString();
      csrfToken = CookieUtils.calculateCsrfToken(sessionId, CookieUtils.csrfSecret);

      testPartner = User.builder()
            .username("prop_partner_" + UUID.randomUUID().toString().substring(0, 8))
            .email("prop_partner_" + UUID.randomUUID() + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testPartner = userRepository.save(testPartner);

      UUID partnerId = testPartner.getId();
      UUID sessionIdUuid = UUID.fromString(sessionId);
      String fingerprint = "fingerprint";
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);

      partnerToken = jwtService.generateAccessToken(
            partnerId,
            Collections.singletonList(SecurityConstants.Roles.PARTNER),
            sessionIdUuid,
            fgpHash);

      ValueOperations<String, String> valueOps = Mockito.mock(MockValueOperations.class);
      Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

      HashOperations<String, Object, Object> hashOps = Mockito.mock(MockHashOperations.class);
      Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

      Map<Object, Object> sessionMap = new HashMap<>();
      sessionMap.put("userId", partnerId.toString());
      sessionMap.put("roles", "[\"ROLE_PARTNER\"]");
      sessionMap.put("active", "true");
      Mockito.when(hashOps.entries(Mockito.anyString())).thenReturn(sessionMap);
   }

   @Test
   public void testPropertyCreationIdempotencyAndOutboxFlow() throws Exception {
      String key = UUID.randomUUID().toString();

      RoomTypeRequest roomRequest = RoomTypeRequest.builder()
            .name("Deluxe Room")
            .basePrice(BigDecimal.valueOf(120.50))
            .capacityAdults(2)
            .capacityChildren(1)
            .totalRooms(10)
            .roomSizeSqm(BigDecimal.valueOf(35.5))
            .bedType(BedType.KING)
            .build();

      PropertyRequest request = PropertyRequest.builder()
            .name("Idemp Luxury Resort")
            .propertyType("RESORT")
            .address("123 Paradise Coast")
            .city("Nha Trang")
            .country("Vietnam")
            .starRating(5)
            .roomTypes(List.of(roomRequest))
            .expectedImageCount(5)
            .build();

      String content = objectMapper.writeValueAsString(request);

      // Request 1: First request to create property
      MvcResult firstResult = mockMvc.perform(post("/partner/properties")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, partnerToken))
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.FINGERPRINT, "fingerprint"))
            .header("x-fgp", "fingerprint")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_SETUP"))
            .andExpect(jsonPath("$.data.name").value("Idemp Luxury Resort"))
            .andReturn();

      String firstResponse = firstResult.getResponse().getContentAsString();

      // Verify DB state for Request 1
      List<Property> properties = propertyRepository.findByOwnerId(testPartner.getId());
      assertEquals(1, properties.size());
      Property createdProperty = properties.get(0);
      assertEquals(PropertyStatus.PENDING_SETUP, createdProperty.getStatus());

      List<PropertyCreatedOutbox> outboxList = propertyCreatedOutboxRepository.findAll();
      assertEquals(1, outboxList.size());
      PropertyCreatedOutbox outboxRecord = outboxList.get(0);
      assertEquals(createdProperty.getId(), outboxRecord.getPropertyId());
      assertEquals(OutboxStatus.PENDING, outboxRecord.getStatus());

      // Request 2: Replay request with same Idempotency-Key
      MvcResult secondResult = mockMvc.perform(post("/partner/properties")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, partnerToken))
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

      // Verify that the response is exact match (replayed) and no new records were
      // created
      assertEquals(firstResponse, secondResponse, "Replayed response must match original response exactly");
      assertEquals(1, propertyRepository.findByOwnerId(testPartner.getId()).size(),
            "No duplicate property should be created");
      assertEquals(1, propertyCreatedOutboxRepository.findAll().size(), "No duplicate outbox record should be created");
   }

   @Test
   public void shouldRecoverExpiredLeaseAndContinueProcessing() {
      // 1. Create a property in PENDING_SETUP state
      Property property = Property.builder()
            .owner(testPartner)
            .name("Lease Recovery Hotel")
            .propertyType(PropertyType.HOTEL)
            .address("456 Setup Road")
            .city("Saigon")
            .country("Vietnam")
            .status(PropertyStatus.PENDING_SETUP)
            .isActive(true)
            .build();
      property = propertyRepository.saveAndFlush(property);

      RoomType roomType = RoomType.builder()
            .property(property)
            .name("Standard Room")
            .basePrice(BigDecimal.valueOf(80.0))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(5)
            .build();
      roomTypeRepository.saveAndFlush(roomType);

      // 2. Insert outbox record mimicking an expired PROCESSING lease
      PropertyCreatedOutbox expiredLeaseRecord = PropertyCreatedOutbox.builder()
            .propertyId(property.getId())
            .status(OutboxStatus.PROCESSING)
            .leaseUntil(Instant.now().minusSeconds(60)) // Expired 1 minute ago
            .build();
      expiredLeaseRecord = propertyCreatedOutboxRepository.saveAndFlush(expiredLeaseRecord);

      System.out.println("=== DB SETUP RECORD ===");
      List<PropertyCreatedOutbox> all = propertyCreatedOutboxRepository.findAll();
      for (PropertyCreatedOutbox o : all) {
         System.out.println("Record: id=" + o.getId() +
               ", propertyId=" + o.getPropertyId() +
               ", status=" + o.getStatus() +
               ", nextRetryAt=" + o.getNextRetryAt() +
               ", leaseUntil=" + o.getLeaseUntil());
      }
      System.out.println("=======================");

      // 3. Trigger worker directly to bypass ShedLock
      List<PropertyCreatedOutbox> batch = propertyCreatedOutboxWorker
            .lockAndFetchEventsToProcess(PageRequest.of(0, 10));
      for (PropertyCreatedOutbox record : batch) {
         propertyCreatedOutboxWorker.processSingleSetup(record);
      }

      // 4. Verify lease recovery and completion
      PropertyCreatedOutbox updatedRecord = propertyCreatedOutboxRepository.findById(expiredLeaseRecord.getId())
            .orElseThrow();
      assertEquals(OutboxStatus.PROCESSED, updatedRecord.getStatus(), "Record should be processed successfully");

      Property updatedProperty = propertyRepository.findById(property.getId()).orElseThrow();
      assertEquals(PropertyStatus.ACTIVE, updatedProperty.getStatus(), "Property status should transition to ACTIVE");

      // Verify availability records created
      long availabilityCount = roomAvailabilityRepository.count();
      assertTrue(availabilityCount > 0, "RoomAvailability records must be created");
   }

}
