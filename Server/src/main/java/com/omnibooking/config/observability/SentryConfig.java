package com.omnibooking.config.observability;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import com.omnibooking.exception.AppException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SentryConfig {

   @Bean
   public Sentry.OptionsConfiguration<SentryOptions> sentryOptionsConfiguration() {
      return options -> {
         // Ignore business exceptions to reduce noise
         options.addIgnoredExceptionForType(AppException.class);
         options.addIgnoredExceptionForType(MethodArgumentNotValidException.class);
         options.addIgnoredExceptionForType(MissingRequestCookieException.class);
         options.addIgnoredExceptionForType(AccessDeniedException.class);

         // Add static tag
         options.setTag("service", "omnibooking-server");

         // PII Sanitization & Event Flood Protection (beforeSend)
         options.setBeforeSend((event, hint) -> {
            // Mask Request Headers and Cookies
            if (event.getRequest() != null) {
               io.sentry.protocol.Request request = event.getRequest();
               if (request.getHeaders() != null) {
                  Map<String, String> sanitizedHeaders = new HashMap<>();
                  request.getHeaders().forEach((k, v) -> {
                     sanitizedHeaders.put(k, SentryPiiSanitizer.sanitizeString(v));
                  });
                  request.setHeaders(sanitizedHeaders);
               }
               if (request.getCookies() != null) {
                  request.setCookies(SentryPiiSanitizer.sanitizeString(request.getCookies()));
               }
               if (request.getData() != null && request.getData() instanceof String) {
                  request.setData(SentryPiiSanitizer.sanitizeString((String) request.getData()));
               }
            }

            // Mask User Information
            if (event.getUser() != null) {
               io.sentry.protocol.User user = event.getUser();
               if (user.getEmail() != null) {
                  user.setEmail("[MASKED_EMAIL]");
               }
            }

            // Fingerprinting: Deduplicate exception by type and message
            if (event.getThrowable() != null) {
               Throwable throwable = event.getThrowable();
               event.setFingerprints(java.util.List.of(
                     throwable.getClass().getName(),
                     throwable.getMessage() != null ? throwable.getMessage() : "no-message"));
            }

            return event;
         });

         // Sanitization for Breadcrumbs
         options.setBeforeBreadcrumb((breadcrumb, hint) -> {
            if (breadcrumb.getMessage() != null) {
               breadcrumb.setMessage(SentryPiiSanitizer.sanitizeString(breadcrumb.getMessage()));
            }
            if (breadcrumb.getData() != null) {
               Map<String, Object> sanitizedData = SentryPiiSanitizer.sanitizeMap(breadcrumb.getData());
               breadcrumb.getData().clear();
               breadcrumb.getData().putAll(sanitizedData);
            }
            return breadcrumb;
         });

         // Dynamic Traces Sampler based on environment
         options.setTracesSampler(context -> {
            String env = options.getEnvironment();
            if ("production".equalsIgnoreCase(env)) {
               return 0.2; // 20% in production
            } else if ("staging".equalsIgnoreCase(env)) {
               return 1.0; // 100% in staging
            } else {
               return 1.0; // 100% in development/local
            }
         });
      };
   }
}
