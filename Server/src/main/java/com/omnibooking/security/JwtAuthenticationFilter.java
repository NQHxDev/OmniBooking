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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
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
            UUID userId = jwtService.extractUserId(accessToken);
            UUID sessionIdFromJwt = jwtService.extractSessionId(accessToken);
            String fgpHashFromJwt = jwtService.extractFingerprintHash(accessToken);
            String fingerprintFromCookie = CookieUtils.getCookieValue(request, CookieUtils.FINGERPRINT);
            if (fingerprintFromCookie == null) {
               fingerprintFromCookie = request.getHeader("x-fgp");
            }

            // Verify SessionID consistency
            if (sessionIdFromCookie == null || !sessionIdFromJwt.toString().equals(sessionIdFromCookie)) {
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
               UserDetails userDetails = userDetailsService.loadUserById(userId.toString());

               if (userDetails instanceof UserPrincipal userPrincipal) {
                  Integer tokenVersionFromJwt = jwtService.extractTokenVersion(accessToken);
                  if (tokenVersionFromJwt == null || !tokenVersionFromJwt.equals(userPrincipal.getTokenVersion())) {
                     log.warn("Token version mismatch for user: {}. Expected {}, got {}", userId,
                           userPrincipal.getTokenVersion(), tokenVersionFromJwt);
                     filterChain.doFilter(request, response);
                     return;
                  }
               }

               UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                     userDetails, null, userDetails.getAuthorities());
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
