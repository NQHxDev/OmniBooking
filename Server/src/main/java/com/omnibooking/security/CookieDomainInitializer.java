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

   @PostConstruct
   public void init() {
      String domain = appProperties.getSecurity().getCookieDomain();
      if (domain != null && !domain.isBlank()) {
         log.info("Initializing CookieUtils with domain: {}", domain);
         CookieUtils.cookieDomain = domain;
      } else {
         log.info("No cookie domain specified, cookies will default to current host");
         CookieUtils.cookieDomain = null;
      }
   }
}
