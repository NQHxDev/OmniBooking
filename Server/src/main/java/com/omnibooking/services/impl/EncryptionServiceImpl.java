package com.omnibooking.services.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.services.EncryptionService;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
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

   private final AppProperties appProperties;
   private final SecureRandom secureRandom = new SecureRandom();

   @Override
   public String encrypt(String plainText) {
      if (plainText == null) {
         return null;
      }

      try {
         byte[] iv = new byte[IV_LENGTH_BYTE];
         secureRandom.nextBytes(iv);

         Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
         GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
         cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(), spec);

         byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

         // Prepend IV to cipherText
         ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
         byteBuffer.put(iv);
         byteBuffer.put(cipherText);

         return Base64.getEncoder().encodeToString(byteBuffer.array());
      } catch (GeneralSecurityException e) {
         log.error("Encryption failed", e);
         throw new RuntimeException("Encryption error", e);
      }
   }

   @Override
   public String decrypt(String cipherText) {
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
         cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), spec);

         byte[] plainText = cipher.doFinal(cipherBytes);

         return new String(plainText, StandardCharsets.UTF_8);
      } catch (GeneralSecurityException e) {
         log.error("Decryption failed", e);
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
               HMAC_ALGO
         );
         mac.init(secretKeySpec);

         byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
         return Base64.getEncoder().encodeToString(hash);
      } catch (GeneralSecurityException e) {
         log.error("HMAC generation failed", e);
         throw new RuntimeException("Hash error", e);
      }
   }

   private SecretKeySpec getEncryptionKey() {
      String secret = appProperties.getSecurity().getEncryptionSecret();
      byte[] keyBytes;

      // If the secret is 64 characters, assume it's a Hex string (64 hex chars = 32 bytes)
      if (secret.length() == 64) {
         try {
            keyBytes = java.util.HexFormat.of().parseHex(secret);
         } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
         }
      } else {
         keyBytes = secret.getBytes(StandardCharsets.UTF_8);
      }

      if (keyBytes.length != 32) {
         log.error("Encryption secret must be exactly 32 bytes (256 bits) for AES-256. Current bytes: {}", keyBytes.length);
         throw new RuntimeException("Invalid encryption key length");
      }
      return new SecretKeySpec(keyBytes, "AES");
   }
}
