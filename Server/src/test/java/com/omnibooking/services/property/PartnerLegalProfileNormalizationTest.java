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
   public void testProfileNormalizationAndMatch() {
      // 1. Giả lập bản ghi đã có profile_search_hash (được chuẩn hóa từ trước)
      String rawRegNum = "GP-123456";
      String rawTaxCode = "MST-789012";
      String rawOwnerName = "NGUYỄN VĂN A";

      String expectedConcat = "gp-123456|mst-789012|nguyễn văn a";
      String expectedHash = encryptionService.createBlindIndex(expectedConcat);

      PartnerLegalProfile oldProfile = PartnerLegalProfile.builder()
            .partner(testOwner)
            .businessRegistrationNumber(encryptionService.encrypt(rawRegNum))
            .taxCode(encryptionService.encrypt(rawTaxCode))
            .legalOwnerName(encryptionService.encrypt(rawOwnerName))
            .profileSearchHash(expectedHash) // Đã có hash chuẩn hóa
            .isActive(true)
            .build();
      oldProfile = partnerLegalProfileRepository.save(oldProfile);

      // 2. Gọi createProperty với thông tin trùng khớp nhưng có biến thể khoảng trắng và chữ hoa/thường
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

      // 3. Xác minh rằng bản ghi cũ đã được so khớp thành công bằng hash trực tiếp (không tạo bản ghi mới)
      List<PartnerLegalProfile> profiles = partnerLegalProfileRepository.findByPartnerId(testOwner.getId());
      assertEquals(1, profiles.size());

      PartnerLegalProfile updatedProfile = profiles.get(0);
      assertEquals(oldProfile.getId(), updatedProfile.getId());
      assertEquals(expectedHash, updatedProfile.getProfileSearchHash());
   }

}
