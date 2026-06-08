package com.omnibooking.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

   public static final String FEATURED_PROPERTIES = "featured_properties";
   public static final String TRENDING_DESTINATIONS = "trending_destinations";
   public static final String PROPERTY_PRICING = "property_pricing";

   @Bean(name = "cacheManager")
   @Primary
   public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
      ObjectMapper mapper = createObjectMapper();
      GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

      // Default configuration (1 hour TTL)
      RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

      // Specific configurations for different cache regions
      Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

      // Featured properties can stay longer (e.g., 6 hours)
      cacheConfigurations.put(FEATURED_PROPERTIES, defaultConfig.entryTtl(Duration.ofHours(6)));

      // Trending destinations can stay even longer (e.g., 24 hours)
      cacheConfigurations.put(TRENDING_DESTINATIONS, defaultConfig.entryTtl(Duration.ofHours(24)));

      // Dynamic pricing rules can stay for 30 minutes
      cacheConfigurations.put(PROPERTY_PRICING, defaultConfig.entryTtl(Duration.ofMinutes(30)));

      return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware() // Sync with DB transactions
            .build();
   }

   private ObjectMapper createObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      return mapper;
   }

}
