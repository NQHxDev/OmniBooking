package com.omnibooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class CustomCsrfFilter extends OncePerRequestFilter {

   private final ObjectMapper objectMapper;
   private static final List<String> STATE_CHANGING_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

   @Override
   protected void doFilterInternal(
         @org.springframework.lang.NonNull HttpServletRequest request,
         @org.springframework.lang.NonNull HttpServletResponse response,
         @org.springframework.lang.NonNull FilterChain filterChain)
         throws ServletException, IOException {

      String method = request.getMethod();
      if (STATE_CHANGING_METHODS.contains(method)) {
         String uri = request.getRequestURI();
         // Exception: Register, Login, Refresh and Logout endpoints
         if (uri.endsWith("/auth/register") || uri.endsWith("/auth/login") || uri.endsWith("/auth/refresh")
               || uri.endsWith("/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
         }

         String csrfCookie = getCookieValue(request, "csrf_token");
         String csrfHeader = request.getHeader("X-CSRF-Token");

         if (csrfCookie == null || csrfHeader == null || !csrfCookie.equals(csrfHeader)) {
            handleError(request, response);
            return;
         }
      }

      filterChain.doFilter(request, response);
   }

   private void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
      String requestId = (String) request.getAttribute("requestId");
      ApiResponse<Object> apiResponse = ApiResponse.error(
            "CSRF token mismatch or missing",
            "CSRF_TOKEN_INVALID",
            null,
            requestId);

      response.setStatus(HttpStatus.FORBIDDEN.value());
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
