package com.omnibooking.services.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.services.core.EncryptionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
@DisplayName("RegistrationQueueServiceImpl Unit Tests")
class RegistrationQueueServiceImplTest {

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private ValueOperations<String, String> valueOperations;

   @Mock
   private RegistrationInboxRepository registrationInboxRepository;

   @Mock
   private EncryptionService encryptionService;

   @Mock
   private KafkaTemplate<String, Object> kafkaTemplate;

   private final ObjectMapper objectMapper = new ObjectMapper();
   private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

   private RegistrationQueueServiceImpl service;

   @BeforeEach
   void setUp() {
      service = new RegistrationQueueServiceImpl(
            redisTemplate,
            objectMapper,
            registrationInboxRepository,
            encryptionService,
            kafkaTemplate,
            meterRegistry
      );
      lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
   }

   @Test
   @DisplayName("Test 1: Inbox Encryption - pushToQueue encrypts the password")
   void testInboxEncryption() throws Exception {
      // Arrange
      UUID reqId = UUID.randomUUID();
      RegisterRequest request = RegisterRequest.builder()
            .requestId(reqId.toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password("PlaintextPassword123")
            .build();

      when(registrationInboxRepository.existsById(reqId)).thenReturn(false);
      when(encryptionService.getActiveKeyId()).thenReturn("v3");
      when(encryptionService.encrypt("PlaintextPassword123", "v3")).thenReturn("AbCdEfGhIjKlMn");

      // Act
      service.pushToQueue(request);

      // Assert
      ArgumentCaptor<RegistrationInbox> inboxCaptor = ArgumentCaptor.forClass(RegistrationInbox.class);
      verify(registrationInboxRepository).save(inboxCaptor.capture());

      RegistrationInbox savedInbox = inboxCaptor.getValue();
      assertNotNull(savedInbox);
      assertEquals(reqId, savedInbox.getRequestId());

      RegisterRequest savedRequest = objectMapper.readValue(savedInbox.getPayload(), RegisterRequest.class);
      // Mật khẩu lưu trữ không chứa plaintext
      assertNotEquals("PlaintextPassword123", savedRequest.getPassword());
      // Mật khẩu lưu trữ phải đúng định dạng enc:keyId:ciphertext
      assertEquals("enc:v3:AbCdEfGhIjKlMn", savedRequest.getPassword());
   }

   @Test
   @DisplayName("Test 2: Kafka Republishing - publishToKafkaAsync decrypts and publishes correctly")
   void testKafkaRepublishing() {
      // Arrange
      UUID reqId = UUID.randomUUID();
      RegisterRequest request = RegisterRequest.builder()
            .requestId(reqId.toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password("enc:v3:AbCdEfGhIjKlMn")
            .build();

      when(encryptionService.decrypt("AbCdEfGhIjKlMn", "v3")).thenReturn("PlaintextPassword123");
      when(encryptionService.getActiveKeyId()).thenReturn("v3");
      when(encryptionService.encrypt("PlaintextPassword123", "v3")).thenReturn("NewEncryptedPasswordForKafka");

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
      when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

      // Act
      service.publishToKafkaAsync(request, reqId);

      // Assert
      verify(encryptionService).decrypt("AbCdEfGhIjKlMn", "v3");
      verify(encryptionService).encrypt("PlaintextPassword123", "v3");

      ArgumentCaptor<RegistrationMessage> msgCaptor = ArgumentCaptor.forClass(RegistrationMessage.class);
      verify(kafkaTemplate).send(any(), eq("test@example.com"), msgCaptor.capture());

      RegistrationMessage sentMsg = msgCaptor.getValue();
      assertEquals("NewEncryptedPasswordForKafka", sentMsg.getEncryptedPassword());
      assertEquals("v3", sentMsg.getKeyId());
      // Không gửi plaintext lên Kafka
      assertNotEquals("PlaintextPassword123", sentMsg.getEncryptedPassword());
   }

   @Test
   @DisplayName("Test 3: Malformed Format - Invalid inputs fail and do not call Kafka")
   void testMalformedFormat() {
      String[] malformedPasswords = {
            "enc:",
            "enc:v1",
            "enc:v1:",
            "enc::ciphertext"
      };

      for (String malformedPassword : malformedPasswords) {
         UUID reqId = UUID.randomUUID();
         RegisterRequest request = RegisterRequest.builder()
               .requestId(reqId.toString())
               .email("test@example.com")
               .fullName("John Doe")
               .password(malformedPassword)
               .build();

         // Act & Assert
         assertThrows(Exception.class, () -> {
            service.publishToKafkaAsync(request, reqId);
         }, "Should fail for malformed password: " + malformedPassword);
      }

      verify(kafkaTemplate, never()).send(any(), any(), any());
   }

   @Test
   @DisplayName("Test 4: Unknown Key Version - Unknown keyId throws exception and does not call Kafka")
   void testUnknownKeyVersion() {
      // Arrange
      UUID reqId = UUID.randomUUID();
      RegisterRequest request = RegisterRequest.builder()
            .requestId(reqId.toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password("enc:unknownKey:AbCdEfGhIjKlMn")
            .build();

      doThrow(new RuntimeException("Key not found")).when(encryptionService).decrypt("AbCdEfGhIjKlMn", "unknownKey");

      // Act & Assert
      assertThrows(Exception.class, () -> {
         service.publishToKafkaAsync(request, reqId);
      });

      verify(kafkaTemplate, never()).send(any(), any(), any());
   }

   @Test
   @DisplayName("Test 5: Double Encryption Protection - pushToQueue rejects already encrypted passwords")
   void testDoubleEncryptionProtection() {
      // Arrange
      UUID reqId = UUID.randomUUID();
      RegisterRequest request = RegisterRequest.builder()
            .requestId(reqId.toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password("enc:v3:AbCdEfGhIjKlMn")
            .build();

      when(registrationInboxRepository.existsById(reqId)).thenReturn(false);

      // Act & Assert
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
         service.pushToQueue(request);
      });

      assertEquals("Password is already encrypted", exception.getMessage());
      verify(registrationInboxRepository, never()).save((RegistrationInbox) any());
   }

   @Test
   @DisplayName("Test 6: Full Round-Trip Flow - plaintext matches after encrypting/saving/decrypting/publishing")
   void testFullRoundTripFlow() throws Exception {
      // Arrange
      UUID reqId = UUID.randomUUID();
      String plainTextPassword = "SuperSecurePassword987!";
      RegisterRequest originalRequest = RegisterRequest.builder()
            .requestId(reqId.toString())
            .email("user@example.com")
            .fullName("Alice")
            .password(plainTextPassword)
            .build();

      when(registrationInboxRepository.existsById(reqId)).thenReturn(false);
      when(encryptionService.getActiveKeyId()).thenReturn("v3");
      // Giả lập mã hóa cho Inbox và Kafka theo thứ tự
      when(encryptionService.encrypt(plainTextPassword, "v3")).thenReturn("CipherInbox123", "CipherKafka123");
      // Giả lập giải mã cho Kafka
      when(encryptionService.decrypt("CipherInbox123", "v3")).thenReturn(plainTextPassword);

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
      when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

      // 1. Ghi nhận vào Inbox (pushToQueue)
      service.pushToQueue(originalRequest);

      // Lấy payload đã lưu trong DB
      ArgumentCaptor<RegistrationInbox> inboxCaptor = ArgumentCaptor.forClass(RegistrationInbox.class);
      verify(registrationInboxRepository).save(inboxCaptor.capture());
      RegistrationInbox savedInbox = inboxCaptor.getValue();
      assertNotNull(savedInbox);

      // 2. Đọc payload từ Inbox (được mô phỏng bằng việc đọc JSON)
      RegisterRequest dbRequest = objectMapper.readValue(savedInbox.getPayload(), RegisterRequest.class);
      assertEquals("enc:v3:CipherInbox123", dbRequest.getPassword());

      // 3. Gửi lên Kafka (publishToKafkaAsync)
      service.publishToKafkaAsync(dbRequest, reqId);

      // Xác minh tin nhắn gửi đi
      ArgumentCaptor<RegistrationMessage> msgCaptor = ArgumentCaptor.forClass(RegistrationMessage.class);
      verify(kafkaTemplate).send(any(), eq("user@example.com"), msgCaptor.capture());

      RegistrationMessage sentMsg = msgCaptor.getValue();
      assertEquals("CipherKafka123", sentMsg.getEncryptedPassword());
      assertEquals("v3", sentMsg.getKeyId());

      // Giải mã cuối cùng từ Kafka message để xác minh có khớp với Plaintext gốc
      when(encryptionService.decrypt("CipherKafka123", "v3")).thenReturn(plainTextPassword);
      String finalDecryptedPassword = encryptionService.decrypt(sentMsg.getEncryptedPassword(), sentMsg.getKeyId());
      assertEquals(plainTextPassword, finalDecryptedPassword);
   }

   @Test
   @DisplayName("Test Extra: Validate Null and Empty Passwords in pushToQueue")
   void testNullOrEmptyPasswordValidation() {
      // Test Null
      RegisterRequest nullRequest = RegisterRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password(null)
            .build();

      IllegalArgumentException nullEx = assertThrows(IllegalArgumentException.class, () -> {
         service.pushToQueue(nullRequest);
      });
      assertEquals("Password must not be empty", nullEx.getMessage());

      // Test Empty/Blank
      RegisterRequest emptyRequest = RegisterRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .email("test@example.com")
            .fullName("John Doe")
            .password("   ")
            .build();

      IllegalArgumentException emptyEx = assertThrows(IllegalArgumentException.class, () -> {
         service.pushToQueue(emptyRequest);
      });
      assertEquals("Password must not be empty", emptyEx.getMessage());
   }
}
