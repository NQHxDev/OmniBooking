package com.omnibooking.services.core;

/**
 * Service for handling sensitive data encryption (AES-256-GCM)
 * and searchable blind indexing (HMAC-SHA256).
 */
public interface EncryptionService {

   /**
    * Encrypts plain text using AES-256-GCM.
    * Uses the active key version.
    *
    * @param plainText The text to encrypt
    * @return Base64 encoded cipher text with IV prepended
    */
   String encrypt(String plainText);

   /**
    * Encrypts plain text using AES-256-GCM and a specific key.
    *
    * @param plainText The text to encrypt
    * @param keyId The identifier of the key to use
    * @return Base64 encoded cipher text with IV prepended
    */
   String encrypt(String plainText, String keyId);

   /**
    * Decrypts cipher text using AES-256-GCM.
    * Uses the active key version.
    *
    * @param cipherText The Base64 encoded cipher text (with IV)
    * @return Original plain text
    */
   String decrypt(String cipherText);

   /**
    * Decrypts cipher text using AES-256-GCM and a specific key.
    *
    * @param cipherText The Base64 encoded cipher text (with IV)
    * @param keyId The identifier of the key to use
    * @return Original plain text
    */
   String decrypt(String cipherText, String keyId);

   /**
    * Creates a deterministic hash (Blind Index) for searching.
    * Uses HMAC-SHA256 with a secret pepper.
    *
    * @param input The text to hash
    * @return Base64 encoded hash
    */
   String createBlindIndex(String input);

   /**
    * Gets the active key ID.
    *
    * @return The active key ID
    */
   String getActiveKeyId();

}
