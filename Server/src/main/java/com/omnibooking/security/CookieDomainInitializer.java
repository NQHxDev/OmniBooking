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
         String clientUrl = appProperties.getClientUrl();
         if (clientUrl == null || clientUrl.isBlank()) {
            throw new IllegalStateException("CLIENT_URL (or app.client-url) must be configured in production profile!");
         }
         try {
            java.net.URI uri = new java.net.URI(clientUrl);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
               throw new IllegalStateException("CLIENT_URL '" + clientUrl + "' is malformed. It must have a valid host.");
            }
            if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1")) {
               throw new IllegalStateException("CLIENT_URL '" + clientUrl + "' is invalid for production. Localhost / loopback addresses are not allowed.");
            }
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
               throw new IllegalStateException("CLIENT_URL '" + clientUrl + "' has an invalid protocol. It must be http or https.");
            }
         } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("CLIENT_URL '" + clientUrl + "' is malformed: " + e.getMessage());
         }

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
