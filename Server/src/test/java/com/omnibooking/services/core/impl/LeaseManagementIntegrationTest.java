package com.omnibooking.services.core.impl;

import com.omnibooking.exception.AppException;
import com.omnibooking.model.ProcessedEvent;
import com.omnibooking.repository.infra.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaseManagementIntegrationTest {

   private IdempotencyServiceImpl idempotencyService;

   @Mock
   private ProcessedEventRepository processedEventRepository;

   private MeterRegistry meterRegistry;

   @BeforeEach
   void setUp() {
      meterRegistry = new SimpleMeterRegistry();
      idempotencyService = new IdempotencyServiceImpl(processedEventRepository, meterRegistry);
   }

   @Test
   void testClaimEventSuccessOnNewEvent() {
      UUID eventId = UUID.randomUUID();
      String consumerGroup = "test-group";

      when(processedEventRepository.findByIdForWrite(eventId, consumerGroup)).thenReturn(Optional.empty());

      boolean claimed = idempotencyService.claimEvent(eventId, consumerGroup);

      assertTrue(claimed);
      verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
   }

   @Test
   void testClaimEventDuplicateOnCompletedEvent() {
      UUID eventId = UUID.randomUUID();
      String consumerGroup = "test-group";

      ProcessedEvent event = ProcessedEvent.builder()
            .eventId(eventId)
            .consumerGroup(consumerGroup)
            .status("COMPLETED")
            .build();

      when(processedEventRepository.findByIdForWrite(eventId, consumerGroup)).thenReturn(Optional.of(event));

      boolean claimed = idempotencyService.claimEvent(eventId, consumerGroup);

      assertFalse(claimed);
      assertEquals(1.0, meterRegistry.counter("omnibooking.event.duplicate").count());
   }

   @Test
   void testClaimEventThrowsOnActiveProcessing() {
      UUID eventId = UUID.randomUUID();
      String consumerGroup = "test-group";

      ProcessedEvent event = ProcessedEvent.builder()
            .eventId(eventId)
            .consumerGroup(consumerGroup)
            .status("PROCESSING")
            .leaseUntil(Instant.now().plus(Duration.ofMinutes(2)))
            .build();

      when(processedEventRepository.findByIdForWrite(eventId, consumerGroup)).thenReturn(Optional.of(event));

      assertThrows(AppException.class, () -> {
         idempotencyService.claimEvent(eventId, consumerGroup);
      });
   }

   @Test
   void testClaimEventSucceedsOnExpiredLeaseTakeover() {
      UUID eventId = UUID.randomUUID();
      String consumerGroup = "test-group";

      ProcessedEvent event = ProcessedEvent.builder()
            .eventId(eventId)
            .consumerGroup(consumerGroup)
            .status("PROCESSING")
            .leaseUntil(Instant.now().minus(Duration.ofMinutes(1)))
            .build();

      when(processedEventRepository.findByIdForWrite(eventId, consumerGroup)).thenReturn(Optional.of(event));

      boolean claimed = idempotencyService.claimEvent(eventId, consumerGroup);

      assertTrue(claimed);
      assertEquals(1.0, meterRegistry.counter("omnibooking.lease.takeover").count());
      verify(processedEventRepository).saveAndFlush(event);
   }

   @Test
   void testClaimEventHandlesConcurrentIntegrityViolation() {
      UUID eventId = UUID.randomUUID();
      String consumerGroup = "test-group";

      when(processedEventRepository.findByIdForWrite(eventId, consumerGroup)).thenReturn(Optional.empty());
      when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
            .thenThrow(new DataIntegrityViolationException("Unique key constraint"));

      boolean claimed = idempotencyService.claimEvent(eventId, consumerGroup);

      assertFalse(claimed);
      assertEquals(1.0, meterRegistry.counter("omnibooking.event.duplicate").count());
   }

}
