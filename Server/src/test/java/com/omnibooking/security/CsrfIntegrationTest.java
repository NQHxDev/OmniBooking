package com.omnibooking.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
   private RedisMessageListenerContainer redisMessageListenerContainer;

   @Test
   public void shouldAllowAnonymousStateChangingRequestWithoutCsrf() throws Exception {
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
}
