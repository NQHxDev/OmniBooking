package com.omnibooking.security;

import com.omnibooking.services.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private final JWTService jwtService;
   private final CustomUserDetailsService userDetailsService;
   private final StringRedisTemplate redisTemplate;

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

      try {
         String accessToken = getCookieValue(request, "access_token");
         String sessionIdFromCookie = getCookieValue(request, "session_id");

         if (accessToken != null && sessionIdFromCookie != null) {
            UUID userId = jwtService.extractUserId(accessToken);
            UUID sessionIdFromJwt = jwtService.extractSessionId(accessToken);

            // 1. Verify SessionID consistency
            if (!sessionIdFromJwt.toString().equals(sessionIdFromCookie)) {
               log.warn("Session ID mismatch detected for user: {}", userId);
               filterChain.doFilter(request, response);
               return;
            }

            // 2. Stateful Verification: Check Redis
            String redisKey = "refresh:" + sessionIdFromJwt;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
               log.warn("Session not found in Redis for sessionId: {}", sessionIdFromJwt);
               filterChain.doFilter(request, response);
               return;
            }

            // 3. Set Authentication
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
               UserDetails userDetails = userDetailsService.loadUserById(userId.toString());

               UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                     userDetails, null, userDetails.getAuthorities());
               authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
               SecurityContextHolder.getContext().setAuthentication(authentication);
            }
         }
      } catch (Exception e) {
         log.error("Cannot set user authentication: {}", e.getMessage());
      }

      filterChain.doFilter(request, response);
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
