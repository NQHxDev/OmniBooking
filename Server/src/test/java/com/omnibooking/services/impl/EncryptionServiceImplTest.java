package com.omnibooking.services.impl;

import com.omnibooking.config.AppProperties;
import com.omnibooking.services.core.impl.EncryptionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionServiceImpl Unit Tests")
class EncryptionServiceImplTest {

   @Mock
   private AppProperties appProperties;

   @Mock
   private AppProperties.Security security;

   @InjectMocks
   private EncryptionServiceImpl encryptionService;

   private static final String ENCRYPTION_SECRET = "3ba3e02fde331c85a69b6b4d778dad8795dd18880e3766fd2b93ec6babcf5e01";
   private static final String HASH_PEPPER = "99eb73cbfbaa0963213c38d2c9bc0bae74ad6e4d4fef9cd667485709c54d5e97";

   @BeforeEach
   void setUp() {
      lenient().when(appProperties.getSecurity()).thenReturn(security);
      lenient().when(security.getEncryptionSecret()).thenReturn(ENCRYPTION_SECRET);
      lenient().when(security.getHashPepper()).thenReturn(HASH_PEPPER);
   }

   @Test
   @DisplayName("Should encrypt and decrypt successfully")
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
   void shouldCreateBlindIndex_Deterministic() {
      String input = "0901234567";

      String index1 = encryptionService.createBlindIndex(input);
      String index2 = encryptionService.createBlindIndex(input);

      assertThat(index1).isEqualTo(index2);
      assertThat(index1).isNotEqualTo(input);
   }

   @Test
   @DisplayName("Should return null for null input")
   void shouldReturnNull_WhenInputIsNull() {
      assertThat(encryptionService.encrypt(null)).isNull();
      assertThat(encryptionService.decrypt(null)).isNull();
      assertThat(encryptionService.createBlindIndex(null)).isNull();
   }

}
