package com.omnibooking.security;

import com.omnibooking.services.auth.JWTService;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private final JWTService jwtService;

   private final CustomUserDetailsService userDetailsService;

   private final StringRedisTemplate redisTemplate;

   private final MeterRegistry meterRegistry;

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

      try {
         String accessToken = CookieUtils.getCookieValue(request, CookieUtils.ACCESS_TOKEN);
         String sessionIdFromCookie = CookieUtils.getCookieValue(request, CookieUtils.SESSION_ID);

         if (accessToken != null && sessionIdFromCookie != null) {
            Claims claims = jwtService.extractAllClaims(accessToken);
            String subject = claims.getSubject();
            UUID userId = subject != null ? UUID.fromString(subject) : null;
            
            String sessionIdFromJwtStr = claims.get("sessionId", String.class);
            UUID sessionIdFromJwt = sessionIdFromJwtStr != null ? UUID.fromString(sessionIdFromJwtStr) : null;
            
            String fgpHashFromJwt = claims.get("fgh", String.class);
            String fingerprintFromCookie = CookieUtils.getCookieValue(request, CookieUtils.FINGERPRINT);
            if (fingerprintFromCookie == null) {
               fingerprintFromCookie = request.getHeader("x-fgp");
            }

            // Verify SessionID consistency
            if (sessionIdFromCookie == null || sessionIdFromJwt == null || !sessionIdFromJwt.toString().equals(sessionIdFromCookie)) {
               log.warn("Session ID mismatch detected for user: {}", userId);
               filterChain.doFilter(request, response);
               return;
            }

            // Verify Fingerprint consistency
            if (fingerprintFromCookie == null || fgpHashFromJwt == null ||
                  !SecurityUtils.hashFingerprint(fingerprintFromCookie).equals(fgpHashFromJwt)) {
               log.warn("Fingerprint mismatch or missing for user: {}. Possible token theft.", userId);
               filterChain.doFilter(request, response);
               return;
            }

            // Stateful Verification: Check Redis
            String redisKey = "refresh:" + sessionIdFromJwt;
            try {
               if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
                  log.warn("Session not found in Redis for sessionId: {}", sessionIdFromJwt);
                  filterChain.doFilter(request, response);
                  return;
               }
            } catch (Exception ex) {
               log.error("Redis is unavailable during session verification: {}", ex.getMessage());
               handleRedisFailure(request, response, ex);
               return;
            }

            // Set Authentication
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
               String versionKey = "user_token_version:" + userId;
               Integer tokenVersionFromJwt = claims.get("tokenVersion", Integer.class);
               String cachedVersionStr = null;
               try {
                  cachedVersionStr = redisTemplate.opsForValue().get(versionKey);
               } catch (Exception ex) {
                  log.error("Redis is unavailable during token version cache lookup: {}", ex.getMessage());
                  handleRedisFailure(request, response, ex);
                  return;
               }

               Integer tokenVersion;
               if (cachedVersionStr != null) {
                  meterRegistry.counter("omnibooking.auth.token_version.cache.hit").increment();
                  tokenVersion = Integer.parseInt(cachedVersionStr);
               } else {
                  meterRegistry.counter("omnibooking.auth.token_version.cache.miss").increment();
                  // Cache miss: Load from DB and populate cache
                  UserDetails userDetails;
                  try {
                     userDetails = userDetailsService.loadUserById(userId.toString());
                  } catch (Exception ex) {
                     log.error("Failed to load user from DB on token version cache miss: {}", ex.getMessage());
                     filterChain.doFilter(request, response);
                     return;
                  }

                  if (userDetails instanceof UserPrincipal userPrincipal) {
                     tokenVersion = userPrincipal.getTokenVersion();
                     try {
                        redisTemplate.opsForValue().set(versionKey, String.valueOf(tokenVersion), 30, TimeUnit.DAYS);
                     } catch (Exception ex) {
                        log.error("Failed to populate token version cache in Redis: {}", ex.getMessage());
                     }
                  } else {
                     filterChain.doFilter(request, response);
                     return;
                  }
               }

               if (tokenVersionFromJwt == null || !tokenVersionFromJwt.equals(tokenVersion)) {
                  log.warn("Token version mismatch for user: {}. Expected {}, got {}", userId, tokenVersion, tokenVersionFromJwt);
                  filterChain.doFilter(request, response);
                  return;
               }

               // Reconstruct UserPrincipal from JWT claims without DB hit! (Task 3)
               Set<String> roles = java.util.Collections.emptySet();
               Object rolesObj = claims.get("roles");
               if (rolesObj instanceof List<?> rolesList) {
                  roles = rolesList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toSet());
               }
               List<SimpleGrantedAuthority> authorities = roles.stream()
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toList());

               String email = claims.get("email", String.class);
               String username = claims.get("username", String.class);

               UserPrincipal principal = UserPrincipal.builder()
                     .id(userId)
                     .username(username != null && !username.isBlank() ? username : userId.toString())
                     .email(email != null ? email : "")
                     .password("") // Password not needed for session auth
                     .authorities(authorities)
                     .active(true)
                     .tokenVersion(tokenVersion)
                     .build();

               UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                     principal, null, principal.getAuthorities());
               authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
               SecurityContextHolder.getContext().setAuthentication(authentication);
            }
         }
      } catch (ExpiredJwtException e) {
         log.warn("JWT expired for request: {}", request.getRequestURI());
         request.setAttribute("expired_token", "true");
      } catch (Exception e) {
         log.error("Cannot set user authentication: {}", e.getMessage());
      }

      filterChain.doFilter(request, response);
   }

   private void handleRedisFailure(HttpServletRequest request, HttpServletResponse response, Exception ex)
         throws IOException {
      String requestId = (String) request.getAttribute("requestId");

      // Classify Redis exception reason
      String reason = "lookup_failure";
      if (ex instanceof QueryTimeoutException || ex.getMessage().toLowerCase().contains("timeout")) {
         reason = "timeout";
      } else if (ex instanceof RedisConnectionFailureException || ex instanceof DataAccessResourceFailureException) {
         reason = "connection_failure";
      }

      meterRegistry.counter("omnibooking.auth.redis.failures", "reason", reason).increment();
      meterRegistry.counter("omnibooking.auth.rejections", "reason", "redis_unavailable").increment();

      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ApiResponse<Object> apiResponse = ApiResponse.error(
            "Authentication service is temporarily unavailable. Please try again later.",
            "SERVICE_UNAVAILABLE",
            ex.getMessage(),
            requestId);
      response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
   }

}
