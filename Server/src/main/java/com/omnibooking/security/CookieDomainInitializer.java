package com.omnibooking.security;

import com.omnibooking.config.AppProperties;
import com.omnibooking.util.CookieUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieDomainInitializer {

   private final AppProperties appProperties;
   private final org.springframework.core.env.Environment env;

   @PostConstruct
   public void init() {
      String domain = appProperties.getSecurity().getCookieDomain();
      
      boolean isProd = java.util.Arrays.asList(env.getActiveProfiles()).contains("prod") ||
                       java.util.Arrays.asList(env.getActiveProfiles()).contains("production") ||
                       "prod".equalsIgnoreCase(env.getProperty("spring.profiles.active")) ||
                       "production".equalsIgnoreCase(env.getProperty("spring.profiles.active"));

      if (isProd) {
         if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("COOKIE_DOMAIN must be configured in production profile!");
         }
         if (domain.startsWith("http://") || domain.startsWith("https://") || domain.contains(":") || domain.contains("/")) {
            throw new IllegalStateException("COOKIE_DOMAIN '" + domain + "' is invalid. It must be a raw domain (e.g., '.yourdomain.com' or 'yourdomain.com') and not contain protocols, ports, or paths.");
         }
      }

      if (domain != null && !domain.isBlank()) {
         log.info("Initializing CookieUtils with domain: {}", domain);
         CookieUtils.cookieDomain = domain;
      } else {
         log.info("No cookie domain specified, cookies will default to current host");
         CookieUtils.cookieDomain = null;
      }
   }
}
