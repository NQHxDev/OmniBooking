package com.omnibooking.security;

import com.omnibooking.config.AppProperties;
import com.omnibooking.util.CookieUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieDomainInitializer {

   private final Environment env;

   private final AppProperties appProperties;

   @PostConstruct
   public void init() {
      String domain = appProperties.getSecurity().getCookieDomain();

      boolean isProd = env.acceptsProfiles(Profiles.of("prod", "production"));

      if (isProd) {
         if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("COOKIE_DOMAIN must be configured in production profile!");
         }
         if (domain.contains("://") || domain.contains(":") || domain.contains("/")) {
            throw new IllegalStateException("COOKIE_DOMAIN '" + domain
                  + "' is invalid. It must be a raw domain (e.g., '.yourdomain.com' or 'yourdomain.com') and not contain protocols, ports, or paths.");
         }
      }

      if (domain != null && !domain.isBlank()) {
         String trimmedDomain = domain.trim();
         log.info("Initializing CookieUtils with domain: {}", trimmedDomain);
         CookieUtils.cookieDomain = trimmedDomain;
      } else {
         log.info("No cookie domain specified, cookies will default to current host");
         CookieUtils.cookieDomain = null;
      }

      String csrfSecret = appProperties.getSecurity().getCsrfSecret();
      if (csrfSecret == null || csrfSecret.isBlank()) {
         csrfSecret = appProperties.getSecurity().getJwtSecret();
      }

      CookieUtils.csrfSecret = csrfSecret;
   }

}
