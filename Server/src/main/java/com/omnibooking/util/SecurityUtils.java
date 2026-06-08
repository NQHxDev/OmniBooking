package com.omnibooking.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.omnibooking.security.UserPrincipal;

import java.util.UUID;

public class SecurityUtils {

   /**
    * Get the ID of the currently authenticated user.
    *
    * @return UUID of the current user
    */
   public static UUID getCurrentUserId() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
         return null;
      }

      Object principal = authentication.getPrincipal();
      if (principal instanceof UserPrincipal) {
         return ((UserPrincipal) principal).getId();
      }

      // Fallback for cases where principal is a string or other type
      try {
         return UUID.fromString(authentication.getName());
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static final ThreadLocal<MessageDigest> SHA_256_DIGEST = ThreadLocal.withInitial(() -> {
      try {
         return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("SHA-256 algorithm not found", e);
      }
   });

   /**
    * Computes SHA-256 hash of a string and returns it as Base64.
    * Uses ThreadLocal MessageDigest for thread-safety and performance.
    */
   public static String hashFingerprint(String input) {
      return hashFingerprint(input, null);
   }

   /**
    * Computes SHA-256 hash of a string + pepper and returns it as Base64.
    * Uses ThreadLocal MessageDigest for thread-safety and performance.
    */
   public static String hashFingerprint(String input, String pepper) {
      if (input == null)
         return null;
      String toHash = pepper != null ? input + pepper : input;
      MessageDigest digest = SHA_256_DIGEST.get();
      digest.reset();
      byte[] hash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
   }

}
