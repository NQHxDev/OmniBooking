package com.omnibooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.util.CookieUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.omnibooking.services.auth.SessionService;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class CustomCsrfFilter extends OncePerRequestFilter {

   private final ObjectMapper objectMapper;

   private final String allowedOrigins;

   private final MeterRegistry meterRegistry;

   private final List<AntPathRequestMatcher> bypassMatchers;

   private final boolean cookieSecure;

   private final String csrfSecret;

   private final SessionService sessionService;

   private final List<String> trustedHosts;

   private final Counter csrfRejectedCounter;

   private final Counter csrfOriginInvalidCounter;

   private final Counter csrfTokenInvalidCounter;

   private static final List<String> STATE_CHANGING_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

   public CustomCsrfFilter(ObjectMapper objectMapper, String allowedOrigins,
         MeterRegistry meterRegistry, List<String> bypassPatterns, boolean cookieSecure, String csrfSecret,
         SessionService sessionService, List<String> trustedHosts) {
      this.objectMapper = objectMapper;
      this.allowedOrigins = allowedOrigins;
      this.meterRegistry = meterRegistry;
      this.cookieSecure = cookieSecure;
      this.csrfSecret = csrfSecret;
      this.sessionService = sessionService;
      this.trustedHosts = trustedHosts != null ? trustedHosts : List.of();

      this.csrfRejectedCounter = meterRegistry.counter("csrf_rejected_total");
      this.csrfOriginInvalidCounter = meterRegistry.counter("csrf_origin_invalid_total");
      this.csrfTokenInvalidCounter = meterRegistry.counter("csrf_token_invalid_total");

      if (bypassPatterns != null) {
         log.info("Initialized CustomCsrfFilter with bypass patterns: {}", bypassPatterns);
         this.bypassMatchers = bypassPatterns.stream()
               .map(pattern -> new AntPathRequestMatcher(pattern))
               .collect(Collectors.toList());
      } else {
         log.warn("Initialized CustomCsrfFilter with NULL bypass patterns!");
         this.bypassMatchers = List.of();
      }
   }

   @Override
   protected void doFilterInternal(
         @NonNull HttpServletRequest request,
         @NonNull HttpServletResponse response,
         @NonNull FilterChain filterChain)
         throws ServletException, IOException {

      if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
         filterChain.doFilter(request, response);
         return;
      }

      String method = request.getMethod();
      if (STATE_CHANGING_METHODS.contains(method)) {
         if (isCsrfBypassEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
         }

         // Origin/Referer Header validation for browser state-changing requests
         String origin = request.getHeader("Origin");
         String referer = request.getHeader("Referer");

         boolean isOriginValid = false;

         if (origin != null && !origin.isBlank()) {
            isOriginValid = checkOrigin(origin, request);
            if (!isOriginValid) {
               log.warn("CSRF validation failed: Origin '{}' is not allowed for path {}", origin,
                     request.getRequestURI());
               handleError(request, response, ErrorCode.CSRF_ORIGIN_INVALID);
               return;
            }
         } else if (referer != null && !referer.isBlank()) {
            // Fallback to Referer validation
            isOriginValid = checkReferer(referer, request);
            if (!isOriginValid) {
               log.warn("CSRF validation failed: Referer '{}' is not allowed for path {}", referer,
                     request.getRequestURI());
               handleError(request, response, ErrorCode.CSRF_ORIGIN_INVALID);
               return;
            }
         } else {
            // Standard browser state-changing request should have Origin or Referer
            log.warn("CSRF validation failed: Request missing both Origin and Referer headers for path {}",
                  request.getRequestURI());
            handleError(request, response, ErrorCode.CSRF_ORIGIN_INVALID);
            return;
         }

         String csrfCookie = getCookieValue(request, CookieUtils.CSRF_TOKEN);
         String csrfHeader = request.getHeader("X-CSRF-Token");

         // General Double Submit match check
         if (csrfCookie == null || csrfHeader == null || !safeEquals(csrfCookie, csrfHeader)) {
            log.warn("CSRF validation failed for path {}: cookie token = {}, header token = {}",
                  request.getRequestURI(),
                  csrfCookie != null ? "[PRESENT]" : "[MISSING]",
                  csrfHeader != null ? "[PRESENT]" : "[MISSING]");
            handleError(request, response, ErrorCode.CSRF_TOKEN_INVALID);
            return;
         }

         // Session Binding Check
         String sessionId = getCookieValue(request, CookieUtils.SESSION_ID);
         if (sessionId != null && !sessionId.isBlank()) {
            String csrfNonce = null;
            try {
               RedisSessionInfo sessionInfo = (RedisSessionInfo) request.getAttribute("sessionInfo");
               if (sessionInfo == null) {
                  UUID sId = UUID.fromString(sessionId);
                  sessionInfo = sessionService.getSession(sId);
               }
               if (sessionInfo != null) {
                  csrfNonce = sessionInfo.getCsrfNonce();
               }
            } catch (Exception e) {
               log.error("Failed to load session for CSRF validation", e);
            }
            String expectedCsrf = CookieUtils.calculateCsrfToken(sessionId, csrfNonce, csrfSecret);
            if (!safeEquals(csrfCookie, expectedCsrf)) {
               log.warn("CSRF validation failed: Token is not bound to the active session. Path: {}",
                     request.getRequestURI());
               handleError(request, response, ErrorCode.CSRF_TOKEN_INVALID);
               return;
            }
         }
      } else if ("GET".equalsIgnoreCase(method)) {
         // Auto-generate CSRF cookie for GET requests if missing so client SPA can
         // retrieve it
         String csrfCookie = getCookieValue(request, CookieUtils.CSRF_TOKEN);
         if (csrfCookie == null || csrfCookie.isBlank()) {
            String sessionId = getCookieValue(request, CookieUtils.SESSION_ID);
            String csrfNonce = null;
            if (sessionId != null && !sessionId.isBlank()) {
               try {
                  UUID sId = UUID.fromString(sessionId);
                  RedisSessionInfo sessionInfo = sessionService.getSession(sId);
                  if (sessionInfo != null) {
                     csrfNonce = sessionInfo.getCsrfNonce();
                  }
               } catch (Exception e) {
                  log.error("Failed to load session for CSRF cookie generation", e);
               }
            }
            String newCsrfToken = CookieUtils.calculateCsrfToken(sessionId, csrfNonce, csrfSecret);
            CookieUtils.addCookie(response, CookieUtils.CSRF_TOKEN, newCsrfToken, 86400, cookieSecure);
         }
      }

      filterChain.doFilter(request, response);
   }

   private boolean isCsrfBypassEndpoint(@NonNull HttpServletRequest request) {
      boolean matched = bypassMatchers.stream().anyMatch(matcher -> matcher.matches(request));
      log.debug("CSRF bypass matching for path {}: result={}", request.getRequestURI(), matched);
      return matched;
   }

   private boolean checkOrigin(String origin, HttpServletRequest request) {
      if (origin == null || origin.isBlank()) {
         return false;
      }
      try {
         URI originUri = new URI(origin);
         String originScheme = originUri.getScheme();
         String originHost = originUri.getHost();
         int originPort = originUri.getPort();
         if (originHost == null) {
            return false;
         }

         // Dynamic same-origin check against the current request host details
         String requestScheme = request.getScheme();
         String requestHost = request.getServerName();
         int requestPort = request.getServerPort();

         int normalizedOriginPort = originPort == -1 ? ("https".equalsIgnoreCase(originScheme) ? 443 : 80) : originPort;
         int normalizedRequestPort = requestPort == -1 ? ("https".equalsIgnoreCase(requestScheme) ? 443 : 80)
               : requestPort;

         if (originHost.equalsIgnoreCase(requestHost)
               && (originScheme != null && originScheme.equalsIgnoreCase(requestScheme))
               && normalizedOriginPort == normalizedRequestPort) {

            // Host must belong to trusted hosts list
            boolean isTrusted = false;
            if (trustedHosts != null && !trustedHosts.isEmpty()) {
               isTrusted = trustedHosts.stream()
                     .map(String::trim)
                     .anyMatch(host -> host.equalsIgnoreCase(requestHost));
            }
            if (isTrusted) {
               return true;
            } else {
               log.warn("CSRF same-origin validation failed: Request host '{}' is not in trusted-hosts list",
                     requestHost);
            }
         }

         // Fallback to allowedOrigins configuration list
         if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return false;
         }

         return Arrays.stream(allowedOrigins.split(","))
               .map(String::trim)
               .filter(allowed -> !allowed.isBlank())
               .anyMatch(allowed -> {
                  try {
                     URI allowedUri = new URI(allowed);
                     String allowedScheme = allowedUri.getScheme();
                     String allowedHost = allowedUri.getHost();
                     int allowedPort = allowedUri.getPort();

                     // Scheme matching
                     if (allowedScheme != null && !allowedScheme.equalsIgnoreCase(originScheme)) {
                        return false;
                     }
                     // Host matching
                     if (allowedHost != null && !allowedHost.equalsIgnoreCase(originHost)) {
                        return false;
                     }
                     // Port matching (normalized default port logic)
                     int normalizedAllowedPort = allowedPort == -1
                           ? ("https".equalsIgnoreCase(allowedScheme) ? 443 : 80)
                           : allowedPort;

                     return normalizedOriginPort == normalizedAllowedPort;
                  } catch (Exception e) {
                     return allowed.equalsIgnoreCase(origin);
                  }
               });
      } catch (Exception e) {
         log.warn("Failed to parse origin URI: {}", origin, e);
         return false;
      }
   }

   private boolean checkReferer(String referer, HttpServletRequest request) {
      if (referer == null || referer.isBlank()) {
         return false;
      }
      try {
         URI uri = new URI(referer);
         String scheme = uri.getScheme();
         String host = uri.getHost();
         int port = uri.getPort();
         if (host == null) {
            return false;
         }
         String refererOrigin = scheme + "://" + host + (port != -1 ? ":" + port : "");
         return checkOrigin(refererOrigin, request);
      } catch (Exception e) {
         log.debug("Failed to parse referer header: {}", referer, e);
         return false;
      }
   }

   private boolean safeEquals(String a, String b) {
      if (a == null || b == null) {
         return false;
      }
      return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
   }

   private void handleError(HttpServletRequest request, HttpServletResponse response, ErrorCode error)
         throws IOException {
      String requestId = (String) request.getAttribute("requestId");

      csrfRejectedCounter.increment();
      if (error == ErrorCode.CSRF_ORIGIN_INVALID) {
         csrfOriginInvalidCounter.increment();
      } else if (error == ErrorCode.CSRF_TOKEN_INVALID) {
         csrfTokenInvalidCounter.increment();
      }

      meterRegistry.counter("omnibooking.auth.csrf.rejections", "reason", error.name().toLowerCase()).increment();
      meterRegistry.counter("omnibooking.auth.rejections", "reason", "csrf_invalid").increment();

      ApiResponse<Object> apiResponse = ApiResponse.error(
            error.getMessage(),
            error.getCode(),
            null,
            requestId);

      response.setStatus(error.getStatus().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
   }

   private String getCookieValue(HttpServletRequest request, String name) {
      if (request.getCookies() == null)
         return null;
      return Arrays.stream(request.getCookies())
            .filter(cookie -> name.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
   }

}
