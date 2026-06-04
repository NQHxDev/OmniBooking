package com.omnibooking.services.search.impl;

import com.omnibooking.model.SearchLog;
import com.omnibooking.repository.SearchLogRepository;
import com.omnibooking.services.search.SearchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogServiceImpl implements SearchLogService {

   private final SearchLogRepository searchLogRepository;

   @Async
   @Override
   @Transactional
   public void logSearch(String query, String countryCode, String userId) {
      if (query == null || query.trim().isEmpty()) {
         return;
      }

      UUID userUuid = null;
      if (userId != null && !userId.trim().isEmpty()) {
         try {
            userUuid = UUID.fromString(userId.trim());
         } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for userId in logSearch: {}", userId, e);
         }
      }

      SearchLog logEntry = SearchLog.builder()
            .queryText(query.trim())
            .countryCode(countryCode)
            .userId(userUuid)
            .isBoosted(false)
            .build();

      try {
         searchLogRepository.save(logEntry);
      } catch (Exception e) {
         log.error("Failed to log search query: {}", query, e);
      }
   }

}
