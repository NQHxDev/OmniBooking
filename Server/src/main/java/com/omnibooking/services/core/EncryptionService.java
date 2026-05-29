package com.omnibooking.services.core;

/**
 * Service for handling sensitive data encryption (AES-256-GCM)
 * and searchable blind indexing (HMAC-SHA256).
 */
public interface EncryptionService {

   /**
    * Encrypts plain text using AES-256-GCM.
    * 
    * @param plainText The text to encrypt
    * @return Base64 encoded cipher text with IV prepended
    */
   String encrypt(String plainText);

   /**
    * Decrypts cipher text using AES-256-GCM.
    * 
    * @param cipherText The Base64 encoded cipher text (with IV)
    * @return Original plain text
    */
   String decrypt(String cipherText);

   /**
    * Creates a deterministic hash (Blind Index) for searching.
    * Uses HMAC-SHA256 with a secret pepper.
    * 
    * @param input The text to hash
    * @return Base64 encoded hash
    */
   String createBlindIndex(String input);
}
