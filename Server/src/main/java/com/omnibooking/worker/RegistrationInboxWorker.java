package com.omnibooking.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.model.RegistrationInbox;
import com.omnibooking.model.enums.RegistrationInboxStatus;
import com.omnibooking.repository.registration.RegistrationInboxRepository;
import com.omnibooking.services.user.impl.RegistrationQueueServiceImpl;
import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.omnibooking.services.user.RegistrationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationInboxWorker {

   private final RegistrationInboxRepository registrationInboxRepository;
   private final RegistrationQueueServiceImpl registrationQueueService;
   private final ObjectMapper objectMapper;
   private final RegistrationService registrationService;

   @Scheduled(fixedDelay = 30000) // Every 30 seconds
   public void recoverStaleRequests() {
      SentryId checkInId = Sentry.captureCheckIn(
            new CheckIn("registration-inbox-recovery-worker", CheckInStatus.IN_PROGRESS));

      try {
         // Process PENDING records that are older than 15 seconds
         Instant pendingThreshold = Instant.now().minusSeconds(15);
         Instant now = Instant.now();
         List<RegistrationInbox> pendingRecords = registrationInboxRepository.findPendingToProcess(
               pendingThreshold, now, PageRequest.of(0, 50));

         if (!pendingRecords.isEmpty()) {
            log.info("Found {} pending registration requests to republish", pendingRecords.size());
            for (RegistrationInbox record : pendingRecords) {
               try {
                  RegisterRequest request = objectMapper.readValue(record.getPayload(), RegisterRequest.class);
                  // Republish to Kafka (it will automatically update status to SENT upon ACK)
                  registrationQueueService.publishToKafkaAsync(request, record.getRequestId());
               } catch (Exception e) {
                  log.error("Failed to parse and republish registration request: {}", record.getRequestId(), e);
                  registrationQueueService.handleIngressFailure(record.getRequestId(), e);
               }
            }
         }

         // Recover PROCESSING records that are older than 5 minutes
         Instant processingThreshold = Instant.now().minus(5, ChronoUnit.MINUTES);
         List<RegistrationInbox> staleProcessingRecords = registrationInboxRepository.findStaleProcessingToRecover(
               processingThreshold, PageRequest.of(0, 50));

         if (!staleProcessingRecords.isEmpty()) {
            log.info("Found {} stale processing registration requests to reset and retry",
                  staleProcessingRecords.size());
            for (RegistrationInbox record : staleProcessingRecords) {
               registrationService.handleProcessingFailure(record.getRequestId(),
                     new RuntimeException("Stale processing request (worker timeout)"));
            }
         }

         Sentry.captureCheckIn(
               new CheckIn(checkInId, "registration-inbox-recovery-worker", CheckInStatus.OK));
      } catch (Exception e) {
         log.error("Error in RegistrationInboxWorker recovery job", e);
         Sentry.captureCheckIn(
               new CheckIn(checkInId, "registration-inbox-recovery-worker", CheckInStatus.ERROR));
      }
   }

   @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
   @Transactional
   public void purgeOldInboxEvents() {
      log.info("Starting scheduled registration inbox cleanup job...");
      try {
         // Purge SUCCESS records older than 7 days
         Instant successThreshold = Instant.now().minus(7, ChronoUnit.DAYS);
         int deletedSuccess = registrationInboxRepository.deleteByStatusAndCreatedAtBefore(
               RegistrationInboxStatus.SUCCESS, successThreshold);

         // Purge FAILED records older than 30 days
         Instant failedThreshold = Instant.now().minus(30, ChronoUnit.DAYS);
         int deletedFailed = registrationInboxRepository.deleteByStatusAndCreatedAtBefore(
               RegistrationInboxStatus.FAILED, failedThreshold);

         log.info("Registration inbox cleanup finished successfully. Deleted {} SUCCESS and {} FAILED records.",
               deletedSuccess, deletedFailed);
      } catch (Exception e) {
         log.error("Error running registration inbox cleanup job", e);
      }
   }

}
