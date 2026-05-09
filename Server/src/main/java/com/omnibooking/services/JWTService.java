package com.omnibooking.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JWTService {

   @Value("${app.security.jwt-secret}")
   private String secret;
 
   @Value("${app.security.jwt-expiration-ms:900000}") // Default 15m
   private long expiration;

   private SecretKey getSigningKey() {
      return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
   }

   /**
    * Generate an Access Token with userId, roles, and sessionId.
    */
   public String generateAccessToken(UUID userId, String role, UUID sessionId, String fingerprintHash) {
      return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("sessionId", sessionId.toString())
            .claim("fgh", fingerprintHash)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
   }

   /**
    * Extract claims from the token.
    */
   public Claims extractAllClaims(String token) {
      return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
   }

   /**
    * Extract SessionID from the token payload.
    */
   public UUID extractSessionId(String token) {
      String sessionIdStr = extractAllClaims(token).get("sessionId", String.class);
      return UUID.fromString(sessionIdStr);
   }

   /**
    * Extract UserID (Subject) from the token payload.
    */
   public UUID extractUserId(String token) {
      return UUID.fromString(extractAllClaims(token).getSubject());
   }

   /**
    * Extract Fingerprint Hash from the token payload.
    */
   public String extractFingerprintHash(String token) {
      return extractAllClaims(token).get("fgh", String.class);
   }

}
