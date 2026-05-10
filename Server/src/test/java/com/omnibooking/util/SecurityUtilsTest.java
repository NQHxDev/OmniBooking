package com.omnibooking.util;

import com.omnibooking.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUtils Unit Tests")
class SecurityUtilsTest {

   @Test
   @DisplayName("Should return correct User ID from SecurityContext (UserPrincipal case)")
   void shouldGetCurrentUserId_UserPrincipal() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserPrincipal principal = UserPrincipal.builder()
            .id(userId)
            .username("test@example.com")
            .password("pass")
            .authorities(Collections.emptyList())
            .active(true)
            .build();
      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      try {
         // Act
         UUID result = SecurityUtils.getCurrentUserId();

         // Assert
         assertThat(result).isEqualTo(userId);
      } finally {
         SecurityContextHolder.clearContext();
      }
   }

   @Test
   @DisplayName("Should return User ID from name if principal is not UserPrincipal")
   void shouldGetCurrentUserId_FromName() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      try {
         // Act
         UUID result = SecurityUtils.getCurrentUserId();

         // Assert
         assertThat(result).isEqualTo(userId);
      } finally {
         SecurityContextHolder.clearContext();
      }
   }

   @Test
   @DisplayName("Should return null if no authentication")
   void shouldReturnNull_WhenNoAuth() {
      SecurityContextHolder.clearContext();
      assertThat(SecurityUtils.getCurrentUserId()).isNull();
   }

   @Test
   @DisplayName("Should hash fingerprint consistently")
   void shouldHashFingerprint() {
      String input = "random-fingerprint-123";
      String hash1 = SecurityUtils.hashFingerprint(input);
      String hash2 = SecurityUtils.hashFingerprint(input);

      assertThat(hash1).isNotBlank();
      assertThat(hash1).isEqualTo(hash2);
      assertThat(hash1).isNotEqualTo(input);
   }

   @Test
   @DisplayName("Should return null for null input in hashFingerprint")
   void shouldReturnNull_WhenHashInputNull() {
      assertThat(SecurityUtils.hashFingerprint(null)).isNull();
   }
}
