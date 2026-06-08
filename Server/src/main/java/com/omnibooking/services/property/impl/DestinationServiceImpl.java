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
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

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
      // Log search (async)
      // Note: countryCode could be passed from controller if needed, or inferred.
      searchLogService.logSearch(query, null, null);

      // Search Elasticsearch
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
            case "Thành Phố Hồ Chí Minh":
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

   @Override
   public void registerDestinationIfNeeded(String cityName, String countryName, Double latitude, Double longitude) {
      if (cityName == null || cityName.isBlank()) {
         return;
      }
      String normalizedCity = normalizeCityName(cityName);

      try {
         List<DestinationDocument> existing = destinationRepository.searchByName(normalizedCity);
         boolean exists = existing.stream().anyMatch(d -> d.getName().equalsIgnoreCase(normalizedCity));

         if (!exists) {
            log.info("Destination '{}' (original: '{}') does not exist in Elasticsearch. Registering a new one...",
                  normalizedCity, cityName);

            String resolvedCountryCode = "VN";
            String resolvedCountryName = "Việt Nam";

            if (countryName != null && !countryName.isBlank()) {
               String trimmedCountry = countryName.trim();
               if (trimmedCountry.equalsIgnoreCase("Vietnam") || trimmedCountry.equalsIgnoreCase("Việt Nam")) {
                  resolvedCountryCode = "VN";
                  resolvedCountryName = "Việt Nam";
               } else if (trimmedCountry.equalsIgnoreCase("France") || trimmedCountry.equalsIgnoreCase("Pháp")) {
                  resolvedCountryCode = "FR";
                  resolvedCountryName = "Pháp";
               } else if (trimmedCountry.equalsIgnoreCase("United Kingdom")
                     || trimmedCountry.equalsIgnoreCase("Vương quốc Anh")) {
                  resolvedCountryCode = "GB";
                  resolvedCountryName = "Vương quốc Anh";
               } else if (trimmedCountry.equalsIgnoreCase("Japan") || trimmedCountry.equalsIgnoreCase("Nhật Bản")) {
                  resolvedCountryCode = "JP";
                  resolvedCountryName = "Nhật Bản";
               } else if (trimmedCountry.equalsIgnoreCase("United States")
                     || trimmedCountry.equalsIgnoreCase("Hoa Kỳ")) {
                  resolvedCountryCode = "US";
                  resolvedCountryName = "Hoa Kỳ";
               } else if (trimmedCountry.equalsIgnoreCase("Thailand") || trimmedCountry.equalsIgnoreCase("Thái Lan")) {
                  resolvedCountryCode = "TH";
                  resolvedCountryName = "Thái Lan";
               } else {
                  resolvedCountryName = trimmedCountry;
                  resolvedCountryCode = trimmedCountry.length() >= 2 ? trimmedCountry.substring(0, 2).toUpperCase()
                        : "VN";
               }
            }

            double lat = latitude != null ? latitude : 0.0;
            double lon = longitude != null ? longitude : 0.0;

            DestinationDocument newDest = DestinationDocument.builder()
                  .id(UUID.randomUUID().toString())
                  .name(normalizedCity)
                  .type("CITY")
                  .countryCode(resolvedCountryCode)
                  .countryName(resolvedCountryName)
                  .location(new GeoPoint(lat, lon))
                  .popularityScore(1.0)
                  .build();

            destinationRepository.save(newDest);
            log.info("Successfully registered new destination: {}", normalizedCity);
         }
      } catch (Exception e) {
         log.error("Failed to check or register destination for city: {}", normalizedCity, e);
      }
   }

   private String normalizeCityName(String cityName) {
      if (cityName == null)
         return null;
      String trimmed = cityName.trim();
      if (trimmed.equalsIgnoreCase("Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("Thành Phố Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("Thành phố Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("TP Hồ Chí Minh") ||
            trimmed.equalsIgnoreCase("TP. Hồ Chí Minh")) {
         return "Thành Phố Hồ Chí Minh";
      }
      return trimmed.replaceAll("^(?i)(Thành\\s+phố|Tỉnh|TP\\.?)\\s+", "").trim();
   }

}
