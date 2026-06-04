package com.omnibooking.services.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.worker.RegistrationBatchWorker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.omnibooking.services.user.RegistrationQueueService;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationQueueServiceImpl implements RegistrationQueueService {

   private final StringRedisTemplate redisTemplate;

   private final ObjectMapper objectMapper;

   private final RegistrationBatchWorker registrationBatchWorker;

   private static final String REGISTRATION_QUEUE_KEY = "registration_queue";

   @Override
   public void pushToQueue(RegisterRequest request) {
      try {
         String json = objectMapper.writeValueAsString(request);
         redisTemplate.opsForList().rightPush(REGISTRATION_QUEUE_KEY, json);
         log.info("Pushed registration request to queue for email: {}", request.getEmail());

         // Wake up the worker immediately
         registrationBatchWorker.trigger();
      } catch (Exception e) {
         log.error("Failed to push registration request to queue", e);
         throw new RuntimeException("System is busy, please try again later");
      }
   }

}
