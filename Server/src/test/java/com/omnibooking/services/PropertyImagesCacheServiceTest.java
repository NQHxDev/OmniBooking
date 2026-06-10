package com.omnibooking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.model.Media;
import com.omnibooking.repository.infra.MediaRepository;
import com.omnibooking.services.property.PropertyImagesCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class PropertyImagesCacheServiceTest {

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private ValueOperations<String, String> valueOperations;

   @Mock
   private MediaRepository mediaRepository;

   @Mock
   private ObjectMapper objectMapper;

   @InjectMocks
   private PropertyImagesCacheService propertyImagesCacheService;

   private UUID propertyId;
   private String cacheKey;
   private String lockKey;

   @BeforeEach
   void setUp() {
      propertyId = UUID.randomUUID();
      cacheKey = "property:images:" + propertyId;
      lockKey = "lock:property:images:" + propertyId;
   }

   @Test
   void getPropertyImageUrls_CacheHit_ReturnsCachedUrls() throws Exception {
      String jsonUrls = "[\"http://url1.com\", \"http://url2.com\"]";
      List<String> expectedUrls = List.of("http://url1.com", "http://url2.com");

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(cacheKey)).thenReturn(jsonUrls);
      when(objectMapper.readValue(eq(jsonUrls), ArgumentMatchers.<TypeReference<List<String>>>any()))
            .thenReturn(expectedUrls);

      List<String> actualUrls = propertyImagesCacheService.getPropertyImageUrls(propertyId);

      assertEquals(expectedUrls, actualUrls);
      verify(mediaRepository, never()).findByEntityIdAndEntityType(any(), any());
   }

   @Test
   void getPropertyImageUrls_CacheMissLockAcquired_QueriesDbAndCaches() throws Exception {
      List<String> expectedUrls = List.of("http://url1.com");
      List<Media> mediaList = List.of(Media.builder().url("http://url1.com").build());

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(cacheKey)).thenReturn(null); // cache miss
      when(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true); // lock
                                                                                                                    // acquired

      when(mediaRepository.findByEntityIdAndEntityType(propertyId, "PROPERTY")).thenReturn(mediaList);
      when(objectMapper.writeValueAsString(expectedUrls)).thenReturn("[\"http://url1.com\"]");

      List<String> actualUrls = propertyImagesCacheService.getPropertyImageUrls(propertyId);

      assertEquals(expectedUrls, actualUrls);
      verify(valueOperations).set(eq(cacheKey), eq("[\"http://url1.com\"]"), anyLong(), any(TimeUnit.class));
      verify(redisTemplate).delete(lockKey);
   }

   @Test
   void evict_DeletesKeyFromRedis() {
      propertyImagesCacheService.evict(propertyId);
      verify(redisTemplate).delete(cacheKey);
   }

}
