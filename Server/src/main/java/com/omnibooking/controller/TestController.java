package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.model.UserProfile;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.services.core.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.omnibooking.repository.PropertyRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.mapper.PropertyDocumentMapper;
import com.omnibooking.repository.MediaRepository;
import com.omnibooking.document.PropertyDocument;
import com.omnibooking.model.Property;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

   private final UserProfileRepository userProfileRepository;
   private final EncryptionService encryptionService;
   private final PropertyRepository propertyRepository;
   private final PropertyElasticsearchRepository propertyElasticsearchRepository;
   private final PropertyDocumentMapper propertyDocumentMapper;
   private final MediaRepository mediaRepository;
   private final ElasticsearchOperations elasticsearchOperations;
 
   @GetMapping("/reindex-properties")
   @Transactional(readOnly = true)
   public ApiResponse<String> reindexProperties() {
      // 1. Explicitly recreate properties index to apply settings.json and vi_analyzer mapping
      IndexOperations indexOps = elasticsearchOperations.indexOps(PropertyDocument.class);
      if (indexOps.exists()) {
         indexOps.delete();
      }
      indexOps.create();
      indexOps.putMapping(indexOps.createMapping(PropertyDocument.class));
 
      // 2. Sync properties from PostgreSQL to Elasticsearch
      List<Property> properties = propertyRepository.findAllWithAmenitiesAndRoomTypes();
      for (Property property : properties) {
         PropertyDocument doc = propertyDocumentMapper.toDocument(property);
         mediaRepository.findFirstByEntityIdAndEntityTypeAndIsMainTrue(property.getId(), "PROPERTY")
            .ifPresent(media -> doc.setMainImageUrl(media.getUrl()));
         propertyElasticsearchRepository.save(doc);
      }
      return ApiResponse.success("Successfully reindexed " + properties.size() + " properties to Elasticsearch.");
   }

   @GetMapping("/search-phone")
   public ApiResponse<Map<String, Object>> searchPhone(@RequestParam String phone) {
      // 1. Create Blind Index from input
      String searchHash = encryptionService.createBlindIndex(phone);

      // 2. Search in DB using Hash
      Optional<UserProfile> profileOpt = userProfileRepository.findByPhoneSearchHash(searchHash);

      Map<String, Object> result = new HashMap<>();
      result.put("input_phone", phone);
      result.put("computed_hash", searchHash);

      if (profileOpt.isPresent()) {
         UserProfile profile = profileOpt.get();
         result.put("found", true);
         result.put("user_id", profile.getUserId());
         result.put("display_name", profile.getDisplayName());
         result.put("encrypted_phone_in_db", profile.getPhoneEncrypted());
         result.put("decrypted_phone", encryptionService.decrypt(profile.getPhoneEncrypted()));
      } else {
         result.put("found", false);
         result.put("message", "Không tìm thấy user với số điện thoại này trong DB");
      }

      return ApiResponse.success(result);
   }
}
