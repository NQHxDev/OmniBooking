package com.omnibooking.services.property;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.model.PartnerLegalProfile;
import com.omnibooking.model.User;
import com.omnibooking.repository.user.PartnerLegalProfileRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.core.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PartnerLegalProfileNormalizationTest {

   @Autowired
   private PropertyService propertyService;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private PartnerLegalProfileRepository partnerLegalProfileRepository;

   @Autowired
   private EncryptionService encryptionService;

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

   private User testOwner;

   @BeforeEach
   public void setUp() {
      testOwner = User.builder()
            .username("p_owner_" + UUID.randomUUID().toString().substring(0, 8))
            .email("owner_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
            .password("password123")
            .isActive(true)
            .build();
      testOwner = userRepository.save(testOwner);
   }

   @Test
   public void testProfileNormalizationAndBackfill() {
      // 1. Giả lập bản ghi cũ (không có profile_search_hash)
      String rawRegNum = "GP-123456";
      String rawTaxCode = "MST-789012";
      String rawOwnerName = "NGUYỄN VĂN A";

      PartnerLegalProfile oldProfile = PartnerLegalProfile.builder()
            .partner(testOwner)
            .businessRegistrationNumber(encryptionService.encrypt(rawRegNum))
            .taxCode(encryptionService.encrypt(rawTaxCode))
            .legalOwnerName(encryptionService.encrypt(rawOwnerName))
            .profileSearchHash(null) // Bản ghi cũ không có hash
            .isActive(true)
            .build();
      oldProfile = partnerLegalProfileRepository.save(oldProfile);

      assertNull(oldProfile.getProfileSearchHash());

      // 2. Gọi createProperty với thông tin trùng khớp nhưng có biến thể khoảng trắng
      // và chữ hoa/thường
      // "Gp-123456" (chữ hoa/thường), " MST-789012 " (khoảng trắng thừa), "Nguyễn Văn
      // A" (nhiều khoảng trắng liên tiếp)
      PropertyRequest request = PropertyRequest.builder()
            .name("Partner Luxury Villa")
            .propertyType("HOTEL")
            .address("123 Ocean Road")
            .city("Da Nang")
            .country("Vietnam")
            .businessRegistrationNumber("Gp-123456")
            .taxCode(" MST-789012  ")
            .legalOwnerName("Nguyễn  Văn A")
            .expectedImageCount(1)
            .build();

      propertyService.createProperty(request, testOwner.getId());

      // 3. Xác minh rằng bản ghi cũ đã được so khớp và BACKFILL hash thành công
      // (không tạo bản ghi mới)
      List<PartnerLegalProfile> profiles = partnerLegalProfileRepository.findByPartnerId(testOwner.getId());
      assertEquals(1, profiles.size());

      PartnerLegalProfile updatedProfile = profiles.get(0);
      assertEquals(oldProfile.getId(), updatedProfile.getId());
      assertNotNull(updatedProfile.getProfileSearchHash());

      // 4. Xác minh giá trị hash chuẩn hóa trùng khớp với blind index tính toán bằng
      // thuật toán chuẩn hóa
      String expectedConcat = "gp-123456|mst-789012|nguyễn văn a";
      String expectedHash = encryptionService.createBlindIndex(expectedConcat);
      assertEquals(expectedHash, updatedProfile.getProfileSearchHash());
   }

}
