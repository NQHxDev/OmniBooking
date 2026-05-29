package com.omnibooking.services.property.impl;

import com.omnibooking.document.DestinationDocument;
import com.omnibooking.dto.response.DestinationSuggestionResponse;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.services.property.DestinationService;
import com.omnibooking.services.search.SearchLogService;
import com.omnibooking.services.search.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

   private final DestinationElasticsearchRepository destinationRepository;
   private final SearchLogService searchLogService;
   private final TrendingService trendingService;
   private final PropertyElasticsearchRepository propertyElasticsearchRepository;

   @Override
   public List<DestinationSuggestionResponse> searchSuggestions(String query, String locale) {
      // 1. Log search (async)
      // Note: countryCode could be passed from controller if needed, or inferred.
      searchLogService.logSearch(query, null, null);

      // 2. Search Elasticsearch
      List<DestinationDocument> documents = destinationRepository.searchByName(query);

      return documents.stream()
            .map(doc -> mapToResponse(doc, locale))
            .collect(Collectors.toList());
   }

   @Override
   public List<DestinationSuggestionResponse> getTrending(String countryCode, String locale) {
      List<String> trendingQueries = trendingService.getTrendingDestinations(countryCode, 25);

      List<DestinationSuggestionResponse> trending = trendingQueries.stream()
            .map(query -> destinationRepository.searchByName(query))
            .filter(docs -> !docs.isEmpty())
            .map(docs -> docs.get(0))
            .collect(Collectors.toMap(DestinationDocument::getId, doc -> doc, (existing, replacement) -> existing))
            .values()
            .stream()
            .limit(15)
            .map(doc -> mapToResponse(doc, locale))
            .collect(Collectors.toList());

      // Fallback: If no trending search logs exist, fetch top destinations by
      // popularity score
      if (trending.isEmpty()) {
         List<DestinationDocument> docs;
         if (countryCode != null && !countryCode.isBlank()) {
            docs = destinationRepository.findTop15ByCountryCodeOrderByPopularityScoreDesc(countryCode);
            // If the country itself has no destinations seeded, fallback to global top
            // destinations
            if (docs.isEmpty()) {
               docs = destinationRepository.findTop15ByOrderByPopularityScoreDesc();
            }
         } else {
            docs = destinationRepository.findTop15ByOrderByPopularityScoreDesc();
         }
         return docs.stream()
               .map(doc -> mapToResponse(doc, locale))
               .collect(Collectors.toList());
      }

      return trending;
   }

   private String translateDestination(String name, String locale) {
      if ("en".equalsIgnoreCase(locale)) {
         switch (name) {
            case "Hà Nội":
               return "Hanoi";
            case "Hồ Chí Minh":
               return "Ho Chi Minh City";
            case "Đà Nẵng":
               return "Da Nang";
            case "Hội An":
               return "Hoi An";
            case "Phú Quốc":
               return "Phu Quoc";
            case "Quảng Ninh":
               return "Quang Ninh";
            case "Hạ Long":
               return "Ha Long";
            case "Nha Trang":
               return "Nha Trang";
            case "Đà Lạt":
               return "Da Lat";
            case "Vũng Tàu":
               return "Vung Tau";
            case "Sapa":
               return "Sapa";
            case "Huế":
               return "Hue";
            case "Hải Phòng":
               return "Hai Phong";
            case "Cần Thơ":
               return "Can Tho";
            default:
               return name;
         }
      }
      return name;
   }

   private String translateCountry(String countryName, String locale) {
      if ("en".equalsIgnoreCase(locale)) {
         switch (countryName) {
            case "Việt Nam":
               return "Vietnam";
            case "France":
               return "France";
            case "United Kingdom":
               return "United Kingdom";
            case "Japan":
               return "Japan";
            case "United States":
               return "United States";
            case "Thailand":
               return "Thailand";
            default:
               return countryName;
         }
      } else if ("vi".equalsIgnoreCase(locale)) {
         switch (countryName) {
            case "Vietnam":
            case "Việt Nam":
               return "Việt Nam";
            case "France":
               return "Pháp";
            case "United Kingdom":
               return "Vương quốc Anh";
            case "Japan":
               return "Nhật Bản";
            case "United States":
               return "Hoa Kỳ";
            case "Thailand":
               return "Thái Lan";
            default:
               return countryName;
         }
      }
      return countryName;
   }

   private DestinationSuggestionResponse mapToResponse(DestinationDocument doc, String locale) {
      String translatedName = translateDestination(doc.getName(), locale);
      String translatedCountry = translateCountry(doc.getCountryName(), locale);

      long propertyCount = 0;
      try {
         propertyCount = propertyElasticsearchRepository.countByCity(doc.getName());
      } catch (Exception e) {
         log.error("Failed to count properties for city: {}", doc.getName(), e);
      }

      return DestinationSuggestionResponse.builder()
            .id(doc.getId())
            .name(translatedName)
            .type(doc.getType()) // Returns "CITY", "HOTEL", etc.
            .country(translatedCountry)
            .countryCode(doc.getCountryCode())
            .location(DestinationSuggestionResponse.LocationDto.builder()
                  .lat(doc.getLocation().getLat())
                  .lon(doc.getLocation().getLon())
                  .build())
            .displayName(translatedName) // Let frontend handle ", Country" formatting
            .imageUrl(doc.getImageUrl())
            .propertyCount(propertyCount)
            .build();
   }

}
