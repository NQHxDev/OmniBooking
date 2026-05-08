package com.omnibooking.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecurityUtils {

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
      if (input == null)
         return null;
      MessageDigest digest = SHA_256_DIGEST.get();
      digest.reset();
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
   }

}
