package com.omnibooking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.model.IdempotencyKey;
import com.omnibooking.model.enums.IdempotencyStatus;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.infra.IdempotencyKeyRepository;
import com.omnibooking.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IdempotencyIntegrationTest {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private IdempotencyKeyRepository idempotencyKeyRepository;

   @Autowired
   private ObjectMapper objectMapper;

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

   private String sessionId;
   private String csrfToken;

   @BeforeEach
   void setUp() {
      idempotencyKeyRepository.deleteAll();
      sessionId = UUID.randomUUID().toString();
      csrfToken = CookieUtils.calculateCsrfToken(sessionId, CookieUtils.csrfSecret);
   }

   @Test
   void testBasicIdempotencyFlow() throws Exception {
      String key = UUID.randomUUID().toString();
      Map<String, Object> payload = new HashMap<>();
      payload.put("amount", 150.0);

      String content = objectMapper.writeValueAsString(payload);

      // First Request - Miss
      MvcResult firstResult = mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("INITIALIZED"))
            .andReturn();

      String firstResponse = firstResult.getResponse().getContentAsString();

      // Second Request - Hit
      MvcResult secondResult = mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andReturn();

      String secondResponse = secondResult.getResponse().getContentAsString();

      assertEquals(firstResponse, secondResponse);
   }

   @Test
   void testConflictWithDifferentPayload() throws Exception {
      String key = UUID.randomUUID().toString();
      Map<String, Object> payloadA = new HashMap<>();
      payloadA.put("amount", 100.0);
      String contentA = objectMapper.writeValueAsString(payloadA);

      Map<String, Object> payloadB = new HashMap<>();
      payloadB.put("amount", 200.0);
      String contentB = objectMapper.writeValueAsString(payloadB);

      // First request (Success)
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contentA))
            .andExpect(status().isOk());

      // Second request with different payload (409 Conflict)
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contentB))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST"));
   }

   @Test
   void testFailedStateRecovery() throws Exception {
      String key = UUID.randomUUID().toString();
      Map<String, Object> payload = new HashMap<>();
      payload.put("amount", -50.0);
      String content = objectMapper.writeValueAsString(payload);

      // Seed failed record manually for clean test scenario
      IdempotencyKey failedKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /payments")
            .processingStatus(IdempotencyStatus.FAILED)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .processingStartedAt(Instant.now())
            .build();

      // We will hash it correctly like Aspect does
      String hash = computeSha256(content);
      failedKey.setRequestHash(hash);
      idempotencyKeyRepository.saveAndFlush(failedKey);

      // Retry with DIFFERENT hash -> Conflict
      Map<String, Object> diffPayload = new HashMap<>();
      diffPayload.put("amount", 10.0);
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(diffPayload)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST"));

      // Retry with SAME hash -> Reclaimed and runs again
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("INITIALIZED"));
   }

   @Test
   void testStaleProcessingReclaim() throws Exception {
      String key = UUID.randomUUID().toString();
      Map<String, Object> payload = new HashMap<>();
      payload.put("amount", 100.0);
      String content = objectMapper.writeValueAsString(payload);
      String hash = computeSha256(content);

      // Seed stale PROCESSING key (15 minutes old)
      IdempotencyKey staleKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /payments")
            .requestHash(hash)
            .processingStatus(IdempotencyStatus.PROCESSING)
            .createdAt(Instant.now().minus(15, ChronoUnit.MINUTES))
            .expiresAt(Instant.now().plusSeconds(3600))
            .processingStartedAt(Instant.now().minus(15, ChronoUnit.MINUTES))
            .build();
      idempotencyKeyRepository.saveAndFlush(staleKey);

      // Retry with DIFFERENT hash -> 409 Conflict
      Map<String, Object> diffPayload = new HashMap<>();
      diffPayload.put("amount", 200.0);
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(diffPayload)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST"));

      // Retry with SAME hash -> Reclaims and proceeds
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isOk());
   }

   @Test
   void testOversizedResponseHandling() throws Exception {
      String key = UUID.randomUUID().toString();

      Map<String, Object> payload = new HashMap<>();
      payload.put("amount", 100.0);
      String content = objectMapper.writeValueAsString(payload);
      String hash = computeSha256(content);

      IdempotencyKey uncacheableKey = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .endpoint("POST /payments")
            .requestHash(hash)
            .processingStatus(IdempotencyStatus.COMPLETED)
            .responseStatus(200)
            .responsePayload(null)
            .responseCached(false)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .processingStartedAt(Instant.now())
            .build();
      idempotencyKeyRepository.saveAndFlush(uncacheableKey);

      // Replay must fail with 422
      mockMvc.perform(post("/payments")
            .header("Origin", "http://localhost:3000")
            .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
            .header("X-CSRF-Token", csrfToken)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("IDEMPOTENCY_RESPONSE_NOT_REPLAYABLE"));
   }

   @Test
   void testConcurrentRequestsIdempotency() throws Exception {
      final int concurrentRequests = 50;
      final String key = UUID.randomUUID().toString();
      final Map<String, Object> payload = new HashMap<>();
      payload.put("amount", 250.0);
      payload.put("delayMs", 300);
      final String content = objectMapper.writeValueAsString(payload);

      ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
      CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);

      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger conflictCount = new AtomicInteger(0);
      final AtomicInteger processingCount = new AtomicInteger(0);
      final AtomicInteger otherCount = new AtomicInteger(0);

      for (int i = 0; i < concurrentRequests; i++) {
         executor.submit(() -> {
            readyLatch.countDown();
            try {
               startLatch.await();
               MvcResult result = mockMvc.perform(post("/payments")
                     .header("Origin", "http://localhost:3000")
                     .cookie(new Cookie(CookieUtils.SESSION_ID, sessionId))
                     .cookie(new Cookie(CookieUtils.CSRF_TOKEN, csrfToken))
                     .header("X-CSRF-Token", csrfToken)
                     .header("Idempotency-Key", key)
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(content))
                     .andReturn();

               int status = result.getResponse().getStatus();
               if (status == 200) {
                  successCount.incrementAndGet();
               } else if (status == 409) {
                  String responseBody = result.getResponse().getContentAsString();
                  if (responseBody.contains("IDEM_002")) {
                     processingCount.incrementAndGet();
                  } else {
                     conflictCount.incrementAndGet();
                  }
               } else {
                  otherCount.incrementAndGet();
               }
            } catch (Exception e) {
               e.printStackTrace();
            } finally {
               finishLatch.countDown();
            }
         });
      }

      assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
      startLatch.countDown();

      assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
      executor.shutdown();

      assertEquals(1, successCount.get(), "Only one request must succeed");
      assertEquals(concurrentRequests - 1, processingCount.get(),
            "All other concurrent requests must receive PROCESSING error");
      assertEquals(0, conflictCount.get());
      assertEquals(0, otherCount.get());
   }

   private String computeSha256(String data) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
   }

}
