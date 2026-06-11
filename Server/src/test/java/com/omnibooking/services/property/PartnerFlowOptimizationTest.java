package com.omnibooking.services.property;

import com.omnibooking.dto.PartnerStatsResponse;
import com.omnibooking.dto.PropertyDetailResponse;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.partner.PartnerService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
      "spring.cache.type=redis",
      "spring.data.redis.host=localhost",
      "spring.data.redis.port=6380"
})
public class PartnerFlowOptimizationTest {

   @Autowired
   private PropertyService propertyService;

   @Autowired
   private PartnerService partnerService;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private EntityManagerFactory entityManagerFactory;

   @Autowired
   private EntityManager entityManager;

   @Autowired
   private CacheManager cacheManager;

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

   private User testUser;
   private Property testProperty;

   @BeforeEach
   public void setUp() {
      // Clear Redis cache before testing
      var statsCache = cacheManager.getCache("partner_stats");
      if (statsCache != null) {
         statsCache.clear();
      }
      var bookingsCache = cacheManager.getCache("partner_bookings");
      if (bookingsCache != null) {
         bookingsCache.clear();
      }

      testUser = User.builder()
            .username("p_owner_" + UUID.randomUUID().toString().substring(0, 8))
            .email("owner_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Partner Optimization Grand Hotel")
            .address("456 Optimization Way")
            .city("Danang")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);
   }

   @Test
   @Transactional
   public void testPropertyDetailQueryComplexityIsConstant() {
      // 1. Thêm 1 room type và đo lường số lượng SQL statements
      RoomType rt1 = RoomType.builder()
            .property(testProperty)
            .name("Suite Room 1")
            .basePrice(BigDecimal.valueOf(150.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(5)
            .build();
      roomTypeRepository.save(rt1);

      // Warm up caches (Redis images cache, Hibernate metadata)
      propertyService.getPropertyDetail(testProperty.getId());

      SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      Statistics stats = sessionFactory.getStatistics();
      stats.setStatisticsEnabled(true);

      entityManager.flush();
      entityManager.clear();
      stats.clear();
      PropertyDetailResponse detail1 = propertyService.getPropertyDetail(testProperty.getId());
      long queryCountWith1RoomType = stats.getPrepareStatementCount();
      assertNotNull(detail1);
      assertEquals(1, detail1.getRoomTypes().size());

      // 2. Thêm tiếp 2 room types nữa (tổng cộng 3 room types) và đo lường lại số
      // lượng SQL statements
      RoomType rt2 = RoomType.builder()
            .property(testProperty)
            .name("Suite Room 2")
            .basePrice(BigDecimal.valueOf(200.00))
            .capacityAdults(2)
            .capacityChildren(1)
            .totalRooms(3)
            .build();
      RoomType rt3 = RoomType.builder()
            .property(testProperty)
            .name("Suite Room 3")
            .basePrice(BigDecimal.valueOf(250.00))
            .capacityAdults(3)
            .capacityChildren(1)
            .totalRooms(2)
            .build();
      roomTypeRepository.save(rt2);
      roomTypeRepository.save(rt3);

      entityManager.flush();
      entityManager.clear();
      stats.clear();
      PropertyDetailResponse detail3 = propertyService.getPropertyDetail(testProperty.getId());
      long queryCountWith3RoomTypes = stats.getPrepareStatementCount();
      assertNotNull(detail3);
      assertEquals(3, detail3.getRoomTypes().size());

      // 3. Xác minh tính phức tạp không thay đổi (Constant complexity O(1))
      // Số lượng câu lệnh SQL truy cập DB khi có 1 và 3 room types phải hoàn toàn như
      // nhau!
      System.out.println("====== SQL Query Count Verification ======");
      System.out.println("Query count with 1 room type: " + queryCountWith1RoomType);
      System.out.println("Query count with 3 room types: " + queryCountWith3RoomTypes);
      System.out.println("==========================================");

      assertEquals(queryCountWith1RoomType, queryCountWith3RoomTypes,
            "Query count must remain constant (O(1)) and not grow with the number of room types (N+1 query fixed).");
   }

   @Test
   public void testCacheStampedeProtectionWithSyncTrue() throws Exception {
      ExecutorService executor = Executors.newFixedThreadPool(10);
      try {
         List<CompletableFuture<PartnerStatsResponse>> futures = new ArrayList<>();
         for (int i = 0; i < 10; i++) {
            futures
                  .add(CompletableFuture.supplyAsync(() -> partnerService.getPartnerStats(testUser.getId()), executor));
         }

         CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

         for (var future : futures) {
            assertNotNull(future.get());
         }

         // Xác minh cache đã được lưu
         String expectedKey = testUser.getId().toString() + ":" + java.time.YearMonth.now().toString();
         var cache = cacheManager.getCache("partner_stats");
         assertNotNull(cache);
         assertNotNull(cache.get(expectedKey));
      } finally {
         executor.shutdown();
      }
   }

}
