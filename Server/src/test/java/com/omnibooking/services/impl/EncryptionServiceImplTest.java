package com.omnibooking.services.impl;

import com.omnibooking.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionServiceImpl Unit Tests")
class EncryptionServiceImplTest {

   @Mock
   private AppProperties appProperties;

   @Mock
   private AppProperties.Security security;

   @InjectMocks
   private EncryptionServiceImpl encryptionService;

   private static final String ENCRYPTION_SECRET = "12345678901234567890123456789012"; // 32 bytes
   private static final String HASH_PEPPER = "random_pepper";

   @BeforeEach
   @SuppressWarnings("all")
   void setUp() {
      org.mockito.Mockito.lenient().when(appProperties.getSecurity()).thenReturn(security);
      org.mockito.Mockito.lenient().when(security.getEncryptionSecret()).thenReturn(ENCRYPTION_SECRET);
      org.mockito.Mockito.lenient().when(security.getHashPepper()).thenReturn(HASH_PEPPER);
   }

   @Test
   @DisplayName("Should encrypt and decrypt successfully")
   @SuppressWarnings("all")
   void shouldEncryptAndDecrypt_Success() {
      String plainText = "0901234567";

      String cipherText = encryptionService.encrypt(plainText);
      assertThat(cipherText).isNotEqualTo(plainText);
      assertThat(cipherText).isNotEmpty();

      String decryptedText = encryptionService.decrypt(cipherText);
      assertThat(decryptedText).isEqualTo(plainText);
   }

   @Test
   @DisplayName("Should produce same blind index for same input")
   @SuppressWarnings("all")
   void shouldCreateBlindIndex_Deterministic() {
      String input = "0901234567";

      String index1 = encryptionService.createBlindIndex(input);
      String index2 = encryptionService.createBlindIndex(input);

      assertThat(index1).isEqualTo(index2);
      assertThat(index1).isNotEqualTo(input);
   }

   @Test
   @DisplayName("Should return null for null input")
   @SuppressWarnings("all")
   void shouldReturnNull_WhenInputIsNull() {
      assertThat(encryptionService.encrypt(null)).isNull();
      assertThat(encryptionService.decrypt(null)).isNull();
      assertThat(encryptionService.createBlindIndex(null)).isNull();
   }
}
