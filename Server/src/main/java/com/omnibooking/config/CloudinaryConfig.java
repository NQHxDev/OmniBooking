package com.omnibooking.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

   private final AppProperties appProperties;

   @Bean
   public Cloudinary cloudinary() {
      return new Cloudinary(ObjectUtils.asMap(
         "cloud_name", appProperties.getCloudinary().getCloudName(),
         "api_key", appProperties.getCloudinary().getApiKey(),
         "api_secret", appProperties.getCloudinary().getApiSecret(),
         "secure", true
      ));
   }
}
