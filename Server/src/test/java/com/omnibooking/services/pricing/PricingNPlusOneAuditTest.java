package com.omnibooking.services.pricing;

import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class PricingNPlusOneAuditTest {

   @Autowired
   private PriceCalculationService priceCalculationService;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private EntityManagerFactory entityManagerFactory;

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

   private RoomType testRoomType;

   private User testUser;

   @BeforeEach
   public void setUp() {
      testUser = User.builder()
            .username("audit_user_" + UUID.randomUUID())
            .email("audit_" + UUID.randomUUID() + "@example.com")
            .password("encoded_pass")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Audit Grand Hotel")
            .address("789 Pricing Ave")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Audit Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(10)
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);
   }

   @Test
   public void testPricingQueryCount() {
      SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      Statistics stats = sessionFactory.getStatistics();
      stats.setStatisticsEnabled(true);

      // Scenario 1: 3-night stay
      LocalDate checkIn3 = LocalDate.now().plusDays(1);
      LocalDate checkOut3 = checkIn3.plusDays(4); // 3 nights

      stats.clear();
      priceCalculationService.calculateStayPrice(testProperty.getId(), testRoomType.getId(), checkIn3, checkOut3, 2);
      long queryCount3 = stats.getPrepareStatementCount();

      System.out.println("====== N+1 QUERY AUDIT REPORT ======");
      System.out.println("Query count for 3-night stay: " + queryCount3);

      // Scenario 2: 10-night stay
      LocalDate checkIn10 = LocalDate.now().plusDays(1);
      LocalDate checkOut10 = checkIn10.plusDays(11); // 10 nights

      stats.clear();
      priceCalculationService.calculateStayPrice(testProperty.getId(), testRoomType.getId(), checkIn10, checkOut10, 2);
      long queryCount10 = stats.getPrepareStatementCount();

      System.out.println("Query count for 10-night stay: " + queryCount10);
      System.out.println("Difference (queries for additional 7 days): " + (queryCount10 - queryCount3));
      System.out.println("====================================");

      assertNotNull(testRoomType.getId());
   }
}
