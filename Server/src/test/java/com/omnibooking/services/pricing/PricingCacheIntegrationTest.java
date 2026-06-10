package com.omnibooking.services.pricing;

import com.omnibooking.config.RedisConfig;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.pricing.PriceRuleRepository;
import com.omnibooking.services.pricing.PriceCalculationService.StayPriceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;

@SpringBootTest(properties = {
      "spring.cache.type=redis",
      "spring.data.redis.host=localhost",
      "spring.data.redis.port=6380"
})
public class PricingCacheIntegrationTest {

   @Autowired
   private PriceCalculationService priceCalculationService;

   @Autowired
   private CacheManager cacheManager;

   @MockitoBean
   private RoomTypeRepository roomTypeRepository;

   @MockitoBean
   private PriceRuleRepository priceRuleRepository;

   @MockitoBean
   private RoomAvailabilityRepository roomAvailabilityRepository;

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
   private RedisMessageListenerContainer redisMessageListenerContainer;

   private UUID propertyId;
   private UUID roomTypeId;
   private Property testProperty;
   private RoomType testRoomType;

   @BeforeEach
   public void setUp() {
      // Clear the cache before each test
      var cache = cacheManager.getCache(RedisConfig.PROPERTY_PRICING);
      if (cache != null) {
         cache.clear();
      }

      propertyId = UUID.randomUUID();
      roomTypeId = UUID.randomUUID();

      testProperty = Property.builder()
            .id(propertyId)
            .name("Test Luxury Hotel")
            .city("Hanoi")
            .country("Vietnam")
            .isActive(true)
            .build();

      testRoomType = RoomType.builder()
            .id(roomTypeId)
            .property(testProperty)
            .name("Deluxe Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(5)
            .build();
   }

   @Test
   public void testCacheHitAndTypeSafety() {
      // Stub repositories
      when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(testRoomType));
      when(priceRuleRepository.findByPropertyIdAndIsActiveTrue(propertyId)).thenReturn(List.of());
      when(roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDate(eq(roomTypeId), any(LocalDate.class)))
            .thenReturn(Optional.empty());

      LocalDate checkIn = LocalDate.now().plusDays(2);
      LocalDate checkOut = LocalDate.now().plusDays(5);
      int guestCount = 2;

      // First call: Cache Miss, executes logic, saves to Redis
      StayPriceResult firstResult = priceCalculationService.calculateStayPrice(propertyId, roomTypeId, checkIn,
            checkOut, guestCount);
      assertNotNull(firstResult);

      // Verify repository was called once
      verify(roomTypeRepository, times(1)).findById(roomTypeId);

      // Reset mock invocation count
      clearInvocations(roomTypeRepository);

      // Second call: Cache Hit, retrieves from Redis, does NOT execute logic
      // Under the old configuration, this throws ClassCastException!
      try {
         StayPriceResult secondResult = priceCalculationService.calculateStayPrice(propertyId, roomTypeId, checkIn,
               checkOut, guestCount);
         assertNotNull(secondResult);
         assertEquals(firstResult.totalFinalPrice().setScale(2), secondResult.totalFinalPrice().setScale(2));

         // Verify repository was NOT called during cache hit
         verify(roomTypeRepository, never()).findById(roomTypeId);
      } catch (ClassCastException e) {
         fail("ClassCastException occurred during cache hit: " + e.getMessage());
      }
   }

}
