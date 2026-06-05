package com.omnibooking.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import jakarta.servlet.http.Cookie;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
public class CsrfIntegrationTest {

   @Autowired
   private MockMvc mockMvc;

   @MockitoBean
   private ElasticsearchOperations elasticsearchOperations;

   @MockitoBean
   private PropertyElasticsearchRepository propertyElasticsearchRepository;

   @MockitoBean
   private DestinationElasticsearchRepository destinationElasticsearchRepository;

   @MockitoBean
   private KafkaTemplate<String, Object> kafkaTemplate;

   @MockitoBean
   private KafkaAdmin kafkaAdmin;

   @MockitoBean
   private StringRedisTemplate stringRedisTemplate;

   @MockitoBean
   private org.springframework.data.redis.core.ValueOperations<String, String> valueOps;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   @Test
   public void shouldAllowAnonymousStateChangingRequestWithoutCsrf() throws Exception {
      // Mock Redis opsForValue to return 4 failed attempts to trigger Turnstile Captcha
      when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get(ArgumentMatchers.anyString())).thenReturn("4");

      // /auth/login is public (@Anonymous) state-changing endpoint, should bypass
      // CSRF
      mockMvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("{\"email\":\"test@example.com\",\"password\":\"password\",\"turnstileToken\":\"test\"}"))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.errorCode").value("AUTH_018")); // means CSRF did not block it, blocked by Captcha
   }

   @Test
   public void shouldBlockProtectedStateChangingRequestWhenCsrfTokenIsMissing() throws Exception {
      // /auth/resend-verification is POST and is NOT annotated with @Anonymous,
      // should require CSRF
      mockMvc.perform(post("/auth/resend-verification"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"))
            .andExpect(jsonPath("$.message").value("CSRF token mismatch or missing"));
   }

   @Test
   public void shouldBlockProtectedStateChangingRequestWhenCsrfTokenIsInvalid() throws Exception {
      // /auth/resend-verification is POST, should require CSRF
      mockMvc.perform(post("/auth/resend-verification")
            .cookie(new Cookie("csrf_token", "valid_cookie_value"))
            .header("X-CSRF-Token", "invalid_header_value"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"))
            .andExpect(jsonPath("$.message").value("CSRF token mismatch or missing"));
   }

   @Test
   public void shouldPassCsrfFilterWhenCsrfTokenIsValid() throws Exception {
      // When CSRF token is valid, it should pass CSRF filter and go to authentication
      // validation
      // Since we don't supply authentication, it should fail with auth error
      // (TOKEN_EXPIRED / AUTH_006), NOT CSRF_TOKEN_INVALID
      mockMvc.perform(post("/auth/resend-verification")
            .cookie(new Cookie("csrf_token", "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_006")); // CSRF check passed, blocked by JWT filter
   }

   @Autowired
   private JWTService jwtService;

   @Test
   public void shouldReturnServiceUnavailableWhenRedisIsDownDuringAuthentication() throws Exception {
      UUID userId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      String fingerprint = "fingerprint";
      String fgpHash = SecurityUtils.hashFingerprint(fingerprint);
      String token = jwtService.generateAccessToken(userId, Collections.singletonList(SecurityConstants.Roles.USER), sessionId,
            fgpHash);

      when(stringRedisTemplate.hasKey(ArgumentMatchers.anyString())).thenThrow(
            new RedisConnectionFailureException("Redis connection failed"));

      // Calculate the valid HMAC-based CSRF token for the session
      String csrfToken = com.omnibooking.util.CookieUtils.calculateCsrfToken(sessionId.toString(),
            com.omnibooking.util.CookieUtils.csrfSecret);

      mockMvc.perform(post("/bookings")
            .cookie(new Cookie("access_token", token))
            .cookie(new Cookie("session_id", sessionId.toString()))
            .cookie(new Cookie("x_fgp", fingerprint))
            .header("x-fgp", fingerprint)
            .cookie(new Cookie("csrf_token", csrfToken))
            .header("X-CSRF-Token", csrfToken))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
            .andExpect(jsonPath("$.message")
                  .value("Authentication service is temporarily unavailable. Please try again later."));
   }

   @Test
   public void shouldBlockBookingCreationWhenCsrfTokenIsMissing() throws Exception {
      // /bookings is state-changing, should require CSRF even for guests
      mockMvc.perform(post("/bookings")
            .contentType("application/json")
            .content("{\"roomTypeId\":\"" + UUID.randomUUID()
                  + "\",\"guestEmail\":\"test@example.com\",\"guestName\":\"Guest\",\"checkInDate\":\"2026-06-05\",\"checkOutDate\":\"2026-06-08\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"));
   }

   @Test
   public void shouldSetCsrfCookieOnGetRequestWhenMissing() throws Exception {
      // GET request should automatically generate and set csrf_token cookie if it's
      // missing
      mockMvc.perform(MockMvcRequestBuilders.get("/health"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.cookie().exists("csrf_token"));
   }

   @Test
   public void shouldBlockRequestWhenCsrfTokenIsNotBoundToSession() throws Exception {
      UUID sessionId = UUID.randomUUID();
      // Use a random CSRF token instead of the correct session-bound HMAC token
      String wrongCsrfToken = UUID.randomUUID().toString();

      mockMvc.perform(post("/auth/resend-verification")
            .cookie(new Cookie("session_id", sessionId.toString()))
            .cookie(new Cookie("csrf_token", wrongCsrfToken))
            .header("X-CSRF-Token", wrongCsrfToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"));
   }

   @Test
   public void shouldAllowRequestWhenCsrfTokenMatchesSessionHmac() throws Exception {
      UUID sessionId = UUID.randomUUID();
      String correctCsrfToken = com.omnibooking.util.CookieUtils.calculateCsrfToken(sessionId.toString(),
            com.omnibooking.util.CookieUtils.csrfSecret);

      mockMvc.perform(post("/auth/resend-verification")
            .cookie(new Cookie("session_id", sessionId.toString()))
            .cookie(new Cookie("csrf_token", correctCsrfToken))
            .header("X-CSRF-Token", correctCsrfToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_006")); // Passes CSRF, fails on Auth (unauthorized)
   }

   @Test
   public void shouldAllowVaryingOriginFormat() throws Exception {
      // Test matching with trailing slash origin
      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000/")
            .cookie(new Cookie("csrf_token", "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_006")); // Passes Origin and CSRF checks
   }

   @Test
   public void shouldAllowAuthCsrfEndpointToReturnToken() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.get("/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.cookie().exists("csrf_token"))
            .andExpect(jsonPath("$.data.csrfToken").exists());
   }

}
