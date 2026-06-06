package com.omnibooking.security;

import com.omnibooking.services.auth.JWTService;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtBenchmarkTest {

   private JWTService jwtService;

   private String token;

   @BeforeEach
   public void setUp() {
      jwtService = new JWTService();
      // Setup secrets for JWT signing/verification
      ReflectionTestUtils.setField(jwtService, "secret",
            "mySuperSecretKeyForOmniBookingJwtValidationBenchmarkMustBeLongEnoughOfAtLeast256Bits");
      ReflectionTestUtils.setField(jwtService, "expiration", 900000L);

      UUID userId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      String fingerprintHash = "sampleFingerprintHashBase64EncodedString=";
      token = jwtService.generateAccessToken(userId, Collections.singleton("ROLE_USER"), sessionId, fingerprintHash, 1);
   }

   @Test
   public void runBenchmark() {
      int warmUpRuns = 1000;
      int testRuns = 20000;

      System.out.println("====== STARTING JWT PARSING BENCHMARK ======");

      // Warm up
      for (int i = 0; i < warmUpRuns; i++) {
         runCurrentFlow();
         runProposedFlow();
      }

      // Benchmark Current Flow
      long startCurrent = System.nanoTime();
      for (int i = 0; i < testRuns; i++) {
         runCurrentFlow();
      }
      long endCurrent = System.nanoTime();
      double durationCurrentMs = (endCurrent - startCurrent) / 1_000_000.0;

      // Benchmark Proposed Flow
      long startProposed = System.nanoTime();
      for (int i = 0; i < testRuns; i++) {
         runProposedFlow();
      }
      long endProposed = System.nanoTime();
      double durationProposedMs = (endProposed - startProposed) / 1_000_000.0;

      double speedUp = durationCurrentMs / durationProposedMs;
      double latencyCurrentUs = (durationCurrentMs * 1000.0) / testRuns;
      double latencyProposedUs = (durationProposedMs * 1000.0) / testRuns;

      System.out.println(String.format("Test Iterations: %d", testRuns));
      System.out.println(String.format("Current Flow (Multiple Parses) Duration: %.2f ms (Latency: %.3f us/op)",
            durationCurrentMs, latencyCurrentUs));
      System.out.println(String.format("Proposed Flow (Single Parse) Duration: %.2f ms (Latency: %.3f us/op)",
            durationProposedMs, latencyProposedUs));
      System.out.println(String.format("Speed-up Factor: %.2fx", speedUp));
   }

   private void runCurrentFlow() {
      UUID userId = jwtService.extractUserId(token);
      UUID sessionId = jwtService.extractSessionId(token);
      String fgpHash = jwtService.extractFingerprintHash(token);
      Integer version = jwtService.extractTokenVersion(token);
      // Suppress compiler unused variable warning
      assert userId != null && sessionId != null && fgpHash != null && version != null;
   }

   private void runProposedFlow() {
      Claims claims = jwtService.extractAllClaims(token);
      UUID userId = UUID.fromString(claims.getSubject());
      UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
      String fgpHash = claims.get("fgh", String.class);
      Integer version = claims.get("tokenVersion", Integer.class);
      // Suppress compiler unused variable warning
      assert userId != null && sessionId != null && fgpHash != null && version != null;
   }
}
