package com.omnibooking.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.services.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationBatchWorker {

   private final StringRedisTemplate redisTemplate;
   private final ObjectMapper objectMapper;
   private final RegistrationService registrationService;
   private final AtomicBoolean isProcessing = new AtomicBoolean(false);

   private static final String REGISTRATION_QUEUE_KEY = "registration_queue";
   private static final int BATCH_SIZE = 100;

   /**
    * Manual trigger to wake up the worker.
    */
   public void trigger() {
      if (isProcessing.compareAndSet(false, true)) {
         log.debug("Worker woken up by trigger.");
         Thread.ofVirtual().start(this::processBatchLoop);
      }
   }

   @Scheduled(fixedDelay = 30000) // Fallback every 30 seconds
   public void scheduledFallback() {
      if (isProcessing.compareAndSet(false, true)) {
         log.debug("Worker triggered by scheduled fallback.");
         processBatchLoop();
      }
   }

   private void processBatchLoop() {
      try {
         while (true) {
            List<String> rawRequests = redisTemplate.opsForList().leftPop(REGISTRATION_QUEUE_KEY, BATCH_SIZE);

            if (rawRequests == null || rawRequests.isEmpty()) {
               break;
            }

            log.info("Processing registration batch of size: {}", rawRequests.size());

            List<RegisterRequest> requests = new ArrayList<>();
            for (String raw : rawRequests) {
               try {
                  requests.add(objectMapper.readValue(raw, RegisterRequest.class));
               } catch (Exception e) {
                  log.error("Failed to parse registration request from queue", e);
               }
            }

            if (!requests.isEmpty()) {
               registrationService.saveBatch(requests);
            }

            if (rawRequests.size() < BATCH_SIZE) {
               break;
            }
         }
      } finally {
         isProcessing.set(false);
         log.debug("Worker finished processing and is now sleeping.");
      }
   }

}
