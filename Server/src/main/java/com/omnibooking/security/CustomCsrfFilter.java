package com.omnibooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class CustomCsrfFilter extends OncePerRequestFilter {

   private final ObjectMapper objectMapper;

   private final String allowedOrigins;

   private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

   private static final List<String> STATE_CHANGING_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

   private static final org.springframework.util.AntPathMatcher PATH_MATCHER = new org.springframework.util.AntPathMatcher();

   private static final List<String> CSRF_BYPASS_PATTERNS = Arrays.asList(
         "/auth/login", "/auth/login/**",
         "/**/auth/login", "/**/auth/login/**",
         "/auth/register", "/auth/register/**",
         "/**/auth/register", "/**/auth/register/**",
         "/auth/refresh", "/auth/refresh/**",
         "/**/auth/refresh", "/**/auth/refresh/**",
         "/auth/logout", "/auth/logout/**",
         "/**/auth/logout", "/**/auth/logout/**",
         "/auth/2fa/login", "/auth/2fa/login/**",
         "/**/auth/2fa/login", "/**/auth/2fa/login/**",
         "/auth/forgot-password", "/auth/forgot-password/**",
         "/**/auth/forgot-password", "/**/auth/forgot-password/**",
         "/auth/reset-password", "/auth/reset-password/**",
         "/**/auth/reset-password", "/**/auth/reset-password/**",
         "/auth/activate-guest", "/auth/activate-guest/**",
         "/**/auth/activate-guest", "/**/auth/activate-guest/**",
         "/auth/finalize-registration", "/auth/finalize-registration/**",
         "/**/auth/finalize-registration", "/**/auth/finalize-registration/**",
         "/bookings", "/bookings/**",
         "/**/bookings", "/**/bookings/**",
         "/payments/*/create", "/**/payments/*/create",
         "/payments/*/callback", "/**/payments/*/callback");

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

         // Origin Header validation for browser state-changing requests
         String origin = request.getHeader("Origin");
         if (origin != null && !origin.isBlank()) {
            boolean matched = false;
            if (allowedOrigins != null && !allowedOrigins.isBlank()) {
               matched = Arrays.stream(allowedOrigins.split(","))
                     .map(String::trim)
                     .anyMatch(allowed -> allowed.equalsIgnoreCase(origin));
            }
            if (!matched) {
               handleError(request, response, ErrorCode.CSRF_ORIGIN_INVALID);
               return;
            }
         }

         String csrfCookie = getCookieValue(request, "csrf_token");
         String csrfHeader = request.getHeader("X-CSRF-Token");

         if (csrfCookie == null || csrfHeader == null || !csrfCookie.equals(csrfHeader)) {
            handleError(request, response, ErrorCode.CSRF_TOKEN_INVALID);
            return;
         }
      }

      filterChain.doFilter(request, response);
   }

   private boolean isCsrfBypassEndpoint(@org.springframework.lang.NonNull HttpServletRequest request) {
      String path = request.getRequestURI();
      return CSRF_BYPASS_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
   }

   private void handleError(HttpServletRequest request, HttpServletResponse response, ErrorCode error)
         throws IOException {
      String requestId = (String) request.getAttribute("requestId");

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
