package com.omnibooking.services.impl;

import com.omnibooking.repository.SearchLogRepository;
import com.omnibooking.services.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

   private final SearchLogRepository searchLogRepository;
   private static final int TRENDING_DAYS = 21;

   @Override
   public List<String> getTrendingDestinations(String countryCode, int limit) {
      Instant since = Instant.now().minus(TRENDING_DAYS, ChronoUnit.DAYS);
      return searchLogRepository.findTopQueries(since, countryCode, PageRequest.of(0, limit));
   }
}
