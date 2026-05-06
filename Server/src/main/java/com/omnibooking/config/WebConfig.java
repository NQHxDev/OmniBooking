package com.omnibooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

   @Value("${app.cors.allowed-origins:http://localhost:3000}")
   private String allowedOrigins;

   @Override
   public void addCorsMappings(@NonNull CorsRegistry registry) {

      registry.addMapping("/**")
            .allowedOrigins(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
   }

}
