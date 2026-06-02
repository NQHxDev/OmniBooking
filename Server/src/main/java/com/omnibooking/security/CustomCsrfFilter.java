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
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RequiredArgsConstructor
public class CustomCsrfFilter extends OncePerRequestFilter {

   private final ObjectMapper objectMapper;

   private final RequestMappingHandlerMapping requestMappingHandlerMapping;

   private final String allowedOrigins;

   private static final List<String> STATE_CHANGING_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

   @Override
   protected void doFilterInternal(
         @org.springframework.lang.NonNull HttpServletRequest request,
         @org.springframework.lang.NonNull HttpServletResponse response,
         @org.springframework.lang.NonNull FilterChain filterChain)
         throws ServletException, IOException {

      if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
         filterChain.doFilter(request, response);
         return;
      }

      String method = request.getMethod();
      if (STATE_CHANGING_METHODS.contains(method)) {
         if (isPublicEndpoint(request)) {
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

   private boolean isPublicEndpoint(@org.springframework.lang.NonNull HttpServletRequest request) {
      try {
         HandlerExecutionChain handlerChain = requestMappingHandlerMapping.getHandler(request);
         if (handlerChain == null) {
            return false;
         }

         Object handler = handlerChain.getHandler();
         if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.hasMethodAnnotation(Anonymous.class) ||
                  handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class);
         }
      } catch (Exception e) {
         return false;
      }
      return false;
   }

   private void handleError(HttpServletRequest request, HttpServletResponse response, ErrorCode error) throws IOException {
      String requestId = (String) request.getAttribute("requestId");
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
