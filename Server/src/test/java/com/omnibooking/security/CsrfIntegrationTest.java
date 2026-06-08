package com.omnibooking.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ForwardedHeaderFilter;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.services.auth.JWTService;
import com.omnibooking.util.SecurityUtils;
import com.omnibooking.util.CookieUtils;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
public class CsrfIntegrationTest {

   private MockMvc mockMvc;

   @Autowired
   private WebApplicationContext wac;

   @BeforeEach
   public void setup() {
      this.mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .addFilter(new ForwardedHeaderFilter())
            .apply(springSecurity())
            .build();
   }

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
   private ValueOperations<String, String> valueOps;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   @Test
   public void shouldAllowRequestWithTrustedForwardedHost() throws Exception {
      // Mock Redis opsForValue to return 4 failed attempts to trigger Turnstile
      when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get(ArgumentMatchers.anyString())).thenReturn("4");

      mockMvc.perform(post("/auth/resend-verification")
            .header("X-Forwarded-Host", "localhost")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isUnauthorized()); // Passes CSRF, fails on Auth (unauthorized)
   }

   @Test
   public void shouldBlockRequestWithUntrustedForwardedHost() throws Exception {
      mockMvc.perform(post("/auth/resend-verification")
            .header("X-Forwarded-Host", "attacker.com")
            .header("Origin", "http://attacker.com")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_002")); // Blocked due to untrusted host / CSRF invalid origin
   }

   @Test
   public void shouldBlockRequestWhenBothOriginAndRefererAreMissing() throws Exception {
      mockMvc.perform(post("/auth/resend-verification")
            // No Origin or Referer header
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_002")); // Blocked because both are missing
   }

   @Test
   public void shouldBlock2faSetupWhenCsrfTokenIsMissing() throws Exception {
      // /auth/2fa/setup is a protected state-changing endpoint, should require CSRF
      mockMvc.perform(post("/auth/2fa/setup")
            .header("Origin", "http://localhost:3000"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"));
   }

   @Test
   public void shouldRateLimitRegistrationStatusEndpoint() throws Exception {
      String requestId = UUID.randomUUID().toString();
      when(stringRedisTemplate.execute(
            ArgumentMatchers.any(),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(Object[].class)))
            .thenReturn(0L); // 0L means limit exceeded

      mockMvc.perform(MockMvcRequestBuilders.get("/auth/registration-status/" + requestId))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.errorCode").value("AUTH_011"));
   }

   @Test
   public void shouldTrackTimeoutWhenRegistrationStatusTimesOut() throws Exception {
      String requestId = UUID.randomUUID().toString();
      when(stringRedisTemplate.execute(
            ArgumentMatchers.any(),
            ArgumentMatchers.anyList(),
            ArgumentMatchers.any(Object[].class)))
            .thenReturn(1L); // 1L means checkRateLimit succeeds

      when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
      when(valueOps.get("registration_result:" + requestId)).thenReturn("PENDING");

      mockMvc.perform(MockMvcRequestBuilders.get("/auth/registration-status/" + requestId + "?timeout=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
   }

   @Test
   public void shouldAllowAnonymousStateChangingRequestWithoutCsrf() throws Exception {
      // Mock Redis opsForValue to return 4 failed attempts to trigger Turnstile
      // Captcha
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
      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"))
            .andExpect(jsonPath("$.message").value("CSRF token mismatch or missing"));
   }

   @Test
   public void shouldBlockProtectedStateChangingRequestWhenCsrfTokenIsInvalid() throws Exception {
      // /auth/resend-verification is POST, should require CSRF
      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "valid_cookie_value"))
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
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "my_token"))
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
      String token = jwtService.generateAccessToken(userId, Collections.singletonList(SecurityConstants.Roles.USER),
            sessionId,
            fgpHash);

      HashOperations<String, Object, Object> hashOps = mock(ObjectHashOperations.class);
      when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
      when(hashOps.entries(ArgumentMatchers.anyString())).thenThrow(
            new RedisConnectionFailureException("Redis connection failed"));

      // Calculate the valid HMAC-based CSRF token for the session
      String csrfToken = CookieUtils.calculateCsrfToken(sessionId.toString(),
            CookieUtils.csrfSecret);

      mockMvc.perform(post("/bookings")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.ACCESS_TOKEN, token))
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId.toString()))
            .cookie(new Cookie(CookieUtils.FINGERPRINT, fingerprint))
            .header("x-fgp", fingerprint)
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
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
            .header("Origin", "http://localhost:3000")
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
            .andExpect(MockMvcResultMatchers.cookie().exists(CookieUtils.CSRF_TOKEN));
   }

   @Test
   public void shouldBlockRequestWhenCsrfTokenIsNotBoundToSession() throws Exception {
      UUID sessionId = UUID.randomUUID();
      // Use a random CSRF token instead of the correct session-bound HMAC token
      String wrongCsrfToken = UUID.randomUUID().toString();

      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId.toString()))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, wrongCsrfToken))
            .header("X-CSRF-Token", wrongCsrfToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SEC_001"));
   }

   @Test
   public void shouldAllowRequestWhenCsrfTokenMatchesSessionHmac() throws Exception {
      UUID sessionId = UUID.randomUUID();
      String correctCsrfToken = CookieUtils.calculateCsrfToken(sessionId.toString(),
            CookieUtils.csrfSecret);

      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId.toString()))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, correctCsrfToken))
            .header("X-CSRF-Token", correctCsrfToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_006")); // Passes CSRF, fails on Auth (unauthorized)
   }

   @Test
   public void shouldAllowVaryingOriginFormat() throws Exception {
      // Test matching with trailing slash origin
      mockMvc.perform(post("/auth/resend-verification")
            .header("Origin", "http://localhost:3000/")
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "my_token"))
            .header("X-CSRF-Token", "my_token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_006")); // Passes Origin and CSRF checks
   }

   @Test
   public void shouldAllowAuthCsrfEndpointToReturnToken() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.get("/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.cookie().exists(CookieUtils.CSRF_TOKEN))
            .andExpect(jsonPath("$.data.csrfToken").exists());
   }

   private interface ObjectHashOperations extends HashOperations<String, Object, Object> {
   }

}
