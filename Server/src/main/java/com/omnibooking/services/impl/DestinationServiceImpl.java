package com.omnibooking.services.impl;

import com.omnibooking.document.DestinationDocument;
import com.omnibooking.dto.response.DestinationSuggestionResponse;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import com.omnibooking.services.DestinationService;
import com.omnibooking.services.SearchLogService;
import com.omnibooking.services.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

   private final DestinationElasticsearchRepository destinationRepository;
   private final SearchLogService searchLogService;
   private final TrendingService trendingService;

   @Override
   public List<DestinationSuggestionResponse> searchSuggestions(String query, String locale) {
      // 1. Log search (async)
      // Note: countryCode could be passed from controller if needed, or inferred.
      searchLogService.logSearch(query, null, null);

      // 2. Search Elasticsearch
      List<DestinationDocument> documents = destinationRepository.searchByName(query);

      return documents.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
   }

   @Override
   public List<DestinationSuggestionResponse> getTrending(String countryCode) {
      List<String> trendingQueries = trendingService.getTrendingDestinations(countryCode, 8);

      return trendingQueries.stream()
            .map(query -> destinationRepository.searchByName(query))
            .filter(docs -> !docs.isEmpty())
            .map(docs -> docs.get(0))
            .collect(Collectors.toMap(DestinationDocument::getId, doc -> doc, (existing, replacement) -> existing))
            .values()
            .stream()
            .limit(5)
            .map(this::mapToResponse)
            .collect(Collectors.toList());
   }

   private DestinationSuggestionResponse mapToResponse(DestinationDocument doc) {
      return DestinationSuggestionResponse.builder()
            .id(doc.getId())
            .name(doc.getName())
            .type(doc.getType()) // Returns "CITY", "HOTEL", etc.
            .country(doc.getCountryName())
            .countryCode(doc.getCountryCode())
            .location(DestinationSuggestionResponse.LocationDto.builder()
                  .lat(doc.getLocation().getLat())
                  .lon(doc.getLocation().getLon())
                  .build())
            .displayName(doc.getName()) // Let frontend handle ", Country" formatting
            .imageUrl(doc.getImageUrl())
            .build();
   }
}
