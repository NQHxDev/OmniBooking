package com.omnibooking.services.pricing;

import com.omnibooking.model.Property;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.AdjustmentType;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.model.enums.RuleType;
import com.omnibooking.model.PriceRule;
import com.omnibooking.model.PriceRuleVersion;
import com.omnibooking.model.PricingAuditLog;
import com.omnibooking.repository.pricing.PriceRuleVersionRepository;
import com.omnibooking.repository.pricing.PricingAuditLogRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
public class PricingServiceIntegrationTest {

   @Autowired
   private PriceRuleService priceRuleService;

   @Autowired
   private PriceRuleVersionRepository priceRuleVersionRepository;

   @Autowired
   private PricingAuditLogRepository pricingAuditLogRepository;

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

   @BeforeEach
   public void setUp() {
      testUser = User.builder()
            .username("test_partner_" + UUID.randomUUID())
            .email("partner_" + UUID.randomUUID() + "@example.com")
            .password("encoded_pass")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Test Luxury Hotel")
            .address("123 Test St")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);
   }

   @Test
   public void shouldCreatePriceRuleAndGenerateImmutableVersion() {
      PriceRule rule = PriceRule.builder()
            .property(testProperty)
            .name("Winter Holiday Promotion")
            .ruleType(RuleType.SEASONAL)
            .startDate(LocalDate.now().plusDays(2))
            .endDate(LocalDate.now().plusDays(10))
            .adjustmentType(AdjustmentType.PERCENTAGE)
            .adjustmentValue(BigDecimal.valueOf(15.00))
            .priority(1)
            .isActive(true)
            .build();

      PriceRule saved = priceRuleService.createRule(rule, testUser.getId());
      assertNotNull(saved.getId());

      List<PriceRuleVersion> versions = priceRuleVersionRepository.findAll().stream()
            .filter(v -> v.getPriceRule().getId().equals(saved.getId()))
            .toList();

      assertEquals(1, versions.size());
      PriceRuleVersion v1 = versions.get(0);
      assertEquals(1, v1.getVersion());
      assertEquals("Winter Holiday Promotion", v1.getName());
      assertEquals(RuleType.SEASONAL, v1.getRuleType());
      assertEquals(BigDecimal.valueOf(15.00), v1.getAdjustmentValue());
   }

   @Test
   public void shouldPropagateCorrelationIdToAuditLogs() {
      UUID traceId = UUID.randomUUID();
      MDC.put("correlationId", traceId.toString());

      try {
         PriceRule rule = PriceRule.builder()
               .property(testProperty)
               .name("Audit Tracing Test")
               .ruleType(RuleType.WEEKEND)
               .adjustmentType(AdjustmentType.FIXED_AMOUNT)
               .adjustmentValue(BigDecimal.valueOf(50.00))
               .priority(2)
               .isActive(true)
               .build();

         PriceRule saved = priceRuleService.createRule(rule, testUser.getId());

         List<PricingAuditLog> logs = pricingAuditLogRepository.findByCorrelationId(traceId);
         assertFalse(logs.isEmpty());
         PricingAuditLog log = logs.get(0);
         assertEquals(traceId, log.getCorrelationId());
         assertEquals("PRICE_RULE", log.getEntityType());
         assertEquals(saved.getId(), log.getEntityId());
      } finally {
         MDC.clear();
      }
   }

   @Test
   public void shouldRejectInvalidOccupancyRule() {
      PriceRule rule = PriceRule.builder()
            .property(testProperty)
            .name("Invalid Occupancy")
            .ruleType(RuleType.OCCUPANCY)
            .occupancyThreshold(0)
            .adjustmentType(AdjustmentType.FIXED_AMOUNT)
            .adjustmentValue(BigDecimal.valueOf(20.00))
            .priority(0)
            .isActive(true)
            .build();

      assertThrows(IllegalArgumentException.class, () -> {
         priceRuleService.createRule(rule, testUser.getId());
      });
   }

}
