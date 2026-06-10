package com.omnibooking.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.services.auth.JWTService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWTService Unit Tests")
class JWTServiceTest {

   private JWTService jwtService;
   private final String secret = "8de4da3b84a9c41be141d312211360274f5264607945464f05e46d108ff7bd69";
   private final long expiration = 3600000; // 1 hour

   @BeforeEach
   void setUp() {
      jwtService = new JWTService();
      ReflectionTestUtils.setField(jwtService, "secret", secret);
      ReflectionTestUtils.setField(Objects.requireNonNull(jwtService), "expiration", expiration);
   }

   @Test
   @DisplayName("Should generate a valid access token and extract claims correctly")
   void shouldGenerateAndExtractToken() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      List<String> roles = List.of(SecurityConstants.Roles.USER, SecurityConstants.Roles.PARTNER);
      String fingerprintHash = "sample_hash";

      // Act
      String token = jwtService.generateAccessToken(userId, roles, sessionId, fingerprintHash);

      // Assert
      assertThat(token).isNotBlank();

      Claims claims = jwtService.extractAllClaims(token);
      assertThat(claims.getSubject()).isEqualTo(userId.toString());
      assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
      assertThat(jwtService.extractSessionId(token)).isEqualTo(sessionId);
      assertThat(jwtService.extractFingerprintHash(token)).isEqualTo(fingerprintHash);

      Object rolesObj = claims.get("roles");
      List<String> extractedRoles = new ArrayList<>();

      if (rolesObj instanceof List<?>) {
         for (Object item : (List<?>) rolesObj) {
            if (item instanceof String) {
               extractedRoles.add((String) item);
            }
         }
      }
      assertThat(extractedRoles).containsExactlyInAnyOrder(SecurityConstants.Roles.USER,
            SecurityConstants.Roles.PARTNER);
   }

   @Test
   @DisplayName("Should throw exception when parsing invalid token")
   void shouldThrow_WhenTokenInvalid() {
      String invalidToken = "invalid.token.string";
      assertThatThrownBy(() -> jwtService.extractAllClaims(invalidToken))
            .isInstanceOf(Exception.class);
   }

   @Test
   @DisplayName("Should throw exception when token is expired")
   void shouldThrow_WhenTokenExpired() throws InterruptedException {
      // Arrange: set short expiration
      ReflectionTestUtils.setField(Objects.requireNonNull(jwtService), "expiration", 1L); // 1ms
      UUID userId = UUID.randomUUID();
      String token = jwtService.generateAccessToken(userId, List.of(), UUID.randomUUID(), "hash");

      Thread.sleep(10); // Wait for expiration

      // Act & Assert
      assertThatThrownBy(() -> jwtService.extractAllClaims(token))
            .isInstanceOf(ExpiredJwtException.class);
   }

   @Test
   @DisplayName("Should generate token with tokenVersion and extract it correctly")
   void shouldGenerateAndExtractTokenVersion() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      List<String> roles = List.of(SecurityConstants.Roles.USER);
      String fingerprintHash = "sample_hash";
      Integer tokenVersion = 3;

      // Act
      String token = jwtService.generateAccessToken(userId, roles, sessionId, fingerprintHash, tokenVersion);

      // Assert
      assertThat(token).isNotBlank();
      assertThat(jwtService.extractTokenVersion(token)).isEqualTo(tokenVersion);
   }

}
