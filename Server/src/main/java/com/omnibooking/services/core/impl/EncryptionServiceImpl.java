package com.omnibooking.services.core.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.services.core.EncryptionService;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionServiceImpl implements EncryptionService {

   private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding";

   private static final int TAG_LENGTH_BIT = 128;

   private static final int IV_LENGTH_BYTE = 12;

   private static final String HMAC_ALGO = "HmacSHA256";

   private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes Cache TTL

   private final AppProperties appProperties;

   private final SecureRandom secureRandom = new SecureRandom();

   // Local cache for SecretKeys to avoid parsing/fetching overhead
   private final ConcurrentMap<String, CachedKey> keyCache = new ConcurrentHashMap<>();

   private static class CachedKey {
      final SecretKeySpec keySpec;
      final long cachedAt;

      CachedKey(SecretKeySpec keySpec) {
         this.keySpec = keySpec;
         this.cachedAt = System.currentTimeMillis();
      }

      boolean isExpired() {
         return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
      }
   }

   @Override
   public String getActiveKeyId() {
      String activeKeyId = appProperties.getSecurity().getActiveKeyId();
      return activeKeyId != null ? activeKeyId : "aes-v1";
   }

   @Override
   public String encrypt(String plainText) {
      return encrypt(plainText, getActiveKeyId());
   }

   @Override
   public String encrypt(String plainText, String keyId) {
      if (plainText == null) {
         return null;
      }

      try {
         byte[] iv = new byte[IV_LENGTH_BYTE];
         secureRandom.nextBytes(iv);

         Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
         GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
         cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(keyId), spec);

         byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

         // Prepend IV to cipherText
         ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
         byteBuffer.put(iv);
         byteBuffer.put(cipherText);

         return Base64.getEncoder().encodeToString(byteBuffer.array());
      } catch (GeneralSecurityException e) {
         log.error("Encryption failed for keyId: {}", keyId, e);
         throw new RuntimeException("Encryption error", e);
      }
   }

   @Override
   public String decrypt(String cipherText) {
      return decrypt(cipherText, getActiveKeyId());
   }

   @Override
   public String decrypt(String cipherText, String keyId) {
      if (cipherText == null) {
         return null;
      }

      try {
         byte[] decode = Base64.getDecoder().decode(cipherText);
         ByteBuffer byteBuffer = ByteBuffer.wrap(decode);

         byte[] iv = new byte[IV_LENGTH_BYTE];
         byteBuffer.get(iv);

         byte[] cipherBytes = new byte[byteBuffer.remaining()];
         byteBuffer.get(cipherBytes);

         Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
         GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
         cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(keyId), spec);

         byte[] plainText = cipher.doFinal(cipherBytes);

         return new String(plainText, StandardCharsets.UTF_8);
      } catch (GeneralSecurityException e) {
         log.error("Decryption failed for keyId: {}", keyId, e);
         throw new RuntimeException("Decryption error", e);
      }
   }

   @Override
   public String createBlindIndex(String input) {
      if (input == null) {
         return null;
      }

      try {
         Mac mac = Mac.getInstance(HMAC_ALGO);
         SecretKeySpec secretKeySpec = new SecretKeySpec(
               appProperties.getSecurity().getHashPepper().getBytes(StandardCharsets.UTF_8),
               HMAC_ALGO);
         mac.init(secretKeySpec);

         byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));

         return Base64.getEncoder().encodeToString(hash);
      } catch (GeneralSecurityException e) {
         log.error("HMAC generation failed", e);
         throw new RuntimeException("Hash error", e);
      }
   }

   private SecretKeySpec getEncryptionKey(String keyId) {
      CachedKey cached = keyCache.get(keyId);
      if (cached != null && !cached.isExpired()) {
         return cached.keySpec;
      }

      // Load key from AppProperties keys map
      String secret = appProperties.getSecurity().getKeys().get(keyId);

      // On-Demand Cache Bypass Refresh
      if (secret == null) {
         log.warn("KeyId '{}' not found in config cache. Attempting on-demand bypass refresh...", keyId);

         // Try loading from environment variables directly for updated keys
         secret = System.getenv("ENCRYPTION_KEY_" + keyId.toUpperCase().replace("-", "_"));

         if (secret == null) {
            // Fallback to default encryptionSecret if keyId matches activeKeyId or is
            // fallback "aes-v1"
            if (keyId.equals(appProperties.getSecurity().getActiveKeyId()) || "aes-v1".equals(keyId)) {
               log.info("Using default encryptionSecret for keyId '{}'", keyId);
               secret = appProperties.getSecurity().getEncryptionSecret();
            } else {
               log.error("Unable to resolve secret key for keyId: {}", keyId);
               throw new RuntimeException("Encryption key not found for keyId: " + keyId);
            }
         }
      }

      SecretKeySpec spec = buildKeySpec(secret);
      keyCache.put(keyId, new CachedKey(spec));
      return spec;
   }

   private SecretKeySpec buildKeySpec(String secret) {
      byte[] keyBytes;
      if (secret.length() == 64) {
         try {
            keyBytes = HexFormat.of().parseHex(secret);
         } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
         }
      } else {
         keyBytes = secret.getBytes(StandardCharsets.UTF_8);
      }

      if (keyBytes.length != 32) {
         log.error("Encryption secret must be exactly 32 bytes (256 bits) for AES-256. Current bytes: {}",
               keyBytes.length);
         throw new RuntimeException("Invalid encryption key length");
      }

      return new SecretKeySpec(keyBytes, "AES");
   }

}
