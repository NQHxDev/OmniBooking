package com.omnibooking.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegistrationMessage;
import com.omnibooking.model.Role;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.user.RegistrationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegistrationKafkaConsumerIntegrationTest {

   private RegistrationKafkaConsumer consumer;

   @Mock
   private EncryptionService encryptionService;

   @Mock
   private PasswordEncoder passwordEncoder;

   @Mock
   private CachedRoleService cachedRoleService;

   @Mock
   private RegistrationService registrationService;

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private ValueOperations<String, String> valueOperations;

   @Mock
   private BloomFilterService bloomFilterService;

   @Mock
   private Acknowledgment acknowledgment;

   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private final ObjectMapper objectMapper = new ObjectMapper();
   private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

   @BeforeEach
   void setUp() throws Exception {
      consumer = new RegistrationKafkaConsumer(
            encryptionService,
            passwordEncoder,
            cachedRoleService,
            registrationService,
            redisTemplate,
            bloomFilterService,
            executor,
            objectMapper,
            meterRegistry);

      Role role = new Role();
      role.setName("ROLE_USER");
      lenient().when(cachedRoleService.getRoleByName("ROLE_USER")).thenReturn(role);
      lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
   }

   @Test
   void testSuccessfulProcessingCommitsOffset() {
      UUID reqId = UUID.randomUUID();
      RegistrationMessage msg = RegistrationMessage.builder()
            .requestId(reqId.toString())
            .email("test@omnibooking.com")
            .fullName("Test User")
            .encryptedPassword("encrypted")
            .keyId("key-1")
            .build();

      ConsumerRecord<String, RegistrationMessage> record = new ConsumerRecord<>("topic", 0, 0, "key", msg);

      when(valueOperations.get("registration_result:" + reqId)).thenReturn(null);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
      when(registrationService.claimInboxForProcessing(reqId)).thenReturn(true);
      when(bloomFilterService.mightContain("test@omnibooking.com")).thenReturn(false);
      when(encryptionService.decrypt("encrypted", "key-1")).thenReturn("decrypted");
      when(passwordEncoder.encode("decrypted")).thenReturn("hashed");

      consumer.consumeBatch(Collections.singletonList(record), acknowledgment);

      verify(registrationService).saveBatchProcessed(any(), any(), any());
      verify(acknowledgment).acknowledge();
   }

   @Test
   void testDatabaseOutagePreventsOffsetCommit() {
      UUID reqId = UUID.randomUUID();
      RegistrationMessage msg = RegistrationMessage.builder()
            .requestId(reqId.toString())
            .email("test@omnibooking.com")
            .fullName("Test User")
            .encryptedPassword("encrypted")
            .keyId("key-1")
            .build();

      ConsumerRecord<String, RegistrationMessage> record = new ConsumerRecord<>("topic", 0, 0, "key", msg);

      when(valueOperations.get("registration_result:" + reqId)).thenReturn(null);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

      // Simulate Database Outage
      doThrow(new RuntimeException("Database is down")).when(registrationService).claimInboxForProcessing(reqId);

      assertThrows(RuntimeException.class, () -> {
         consumer.consumeBatch(Collections.singletonList(record), acknowledgment);
      });

      verify(acknowledgment, never()).acknowledge();
   }

   @Test
   void testRedisOutageHandlesResiliently() {
      UUID reqId = UUID.randomUUID();
      RegistrationMessage msg = RegistrationMessage.builder()
            .requestId(reqId.toString())
            .email("test@omnibooking.com")
            .fullName("Test User")
            .encryptedPassword("encrypted")
            .keyId("key-1")
            .build();

      ConsumerRecord<String, RegistrationMessage> record = new ConsumerRecord<>("topic", 0, 0, "key", msg);

      // Simulate Redis Connection Failure
      when(valueOperations.get("registration_result:" + reqId))
            .thenThrow(new RedisConnectionFailureException("Redis is down"));

      // Under Redis failure, processing for this message fails and registers failure,
      // but doesn't crash the entire batch processor thread unless a critical
      // exception escapes.
      // The exception is caught in the loop block, marked in DB, and processing
      // proceeds or fails gracefully.
      consumer.consumeBatch(Collections.singletonList(record), acknowledgment);

      verify(registrationService).handleProcessingFailure(any(UUID.class), any(Exception.class));
      verify(acknowledgment).acknowledge(); // Other healthy messages in the batch would allow acknowledgment, or if the
                                            // batch task returns successfully.
   }

}
