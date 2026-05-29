package com.omnibooking.services.property.impl;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.services.property.PropertySearchService;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertySearchServiceImpl implements PropertySearchService {

   private final ElasticsearchOperations elasticsearchOperations;

   @Override
   public Page<PropertyDocument> searchProperties(String query, Double minPrice, Double maxPrice, Integer stars,
         String propertyType, List<String> amenities, Double minRating, Pageable pageable) {

      BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

      // 1. Text Search using multi_match with cross_fields for best Vietnamese text
      // matching
      if (query != null && !query.isBlank()) {
         String trimmedQuery = query.trim();

         BoolQuery.Builder textBoolBuilder = new BoolQuery.Builder();

         // Primary query: user's original input
         textBoolBuilder.should(buildMultiMatchQuery(trimmedQuery));

         // Alternative query: mapped Vietnamese city name (handles diacritics)
         String altQuery = getAlternativeCityName(trimmedQuery);
         if (altQuery != null) {
            textBoolBuilder.should(buildMultiMatchQuery(altQuery));
         }

         textBoolBuilder.minimumShouldMatch("1");

         boolBuilder.must(new Query.Builder()
               .bool(textBoolBuilder.build())
               .build());
      }

      // 2. Price Filter
      if (minPrice != null) {
         final double min = minPrice;
         boolBuilder.filter(Query.of(q -> q
               .range(r -> r
                     .number(n -> n
                           .field("minPrice")
                           .gte(min)))));
      }
      if (maxPrice != null) {
         final double max = maxPrice;
         boolBuilder.filter(Query.of(q -> q
               .range(r -> r
                     .number(n -> n
                           .field("minPrice")
                           .lte(max)))));
      }

      // 3. Star Rating Filter
      if (stars != null) {
         boolBuilder.filter(new Query.Builder()
               .term(new TermQuery.Builder()
                     .field("starRating")
                     .value(stars)
                     .build())
               .build());
      }

      // 4. Property Type Filter
      if (propertyType != null && !propertyType.isBlank()) {
         boolBuilder.filter(new Query.Builder()
               .term(new TermQuery.Builder()
                     .field("propertyType")
                     .value(propertyType)
                     .build())
               .build());
      }

      // 5. Amenities Filter (Matches ALL specified amenities)
      if (amenities != null && !amenities.isEmpty()) {
         for (String amenity : amenities) {
            boolBuilder.filter(new Query.Builder()
                  .term(new TermQuery.Builder()
                        .field("amenities")
                        .value(amenity)
                        .build())
                  .build());
         }
      }

      // 6. Rating Filter
      if (minRating != null) {
         final double rating = minRating;
         boolBuilder.filter(Query.of(q -> q
               .range(r -> r
                     .number(n -> n
                           .field("averageRating")
                           .gte(rating)))));
      }

      NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(new Query.Builder().bool(boolBuilder.build()).build())
            .withPageable(pageable)
            .build();

      SearchHits<PropertyDocument> searchHits = elasticsearchOperations.search(nativeQuery, PropertyDocument.class);

      List<PropertyDocument> results = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList();

      return new PageImpl<>(results, pageable, searchHits.getTotalHits());
   }

   /**
    * Build a multi_match query that searches across name, city, address,
    * description fields.
    * Uses "best_fields" type which finds the best matching field and uses its
    * score.
    * The vi_analyzer with icu_folding handles Vietnamese diacritics automatically,
    * so "Quang Ninh" will match "Quảng Ninh" and "Tỉnh Quảng Ninh".
    */
   private Query buildMultiMatchQuery(String queryText) {
      return new Query.Builder()
            .multiMatch(new MultiMatchQuery.Builder()
                  .query(queryText)
                  .fields(List.of("name", "city", "address", "description"))
                  .type(TextQueryType.BestFields)
                  .build())
            .build();
   }

   private String getAlternativeCityName(String query) {
      switch (query.toLowerCase()) {
         case "hồ chí minh":
         case "ho chi minh":
         case "ho chi minh city":
            return "Hồ Chí Minh";

         case "hà nội":
         case "hanoi":
            return "Hà Nội";

         case "đà nẵng":
         case "da nang":
            return "Đà Nẵng";

         case "hội an":
         case "hoi an":
            return "Hội An";

         case "phú quốc":
         case "phu quoc":
            return "Phú Quốc";

         case "quảng ninh":
         case "quang ninh":
            return "Quảng Ninh";

         case "hạ long":
         case "ha long":
            return "Hạ Long";

         case "nha trang":
            return "Nha Trang";

         case "đà lạt":
         case "da lat":
            return "Đà Lạt";

         case "vũng tàu":
         case "vung tau":
            return "Vũng Tàu";

         case "sapa":
            return "Sapa";

         case "huế":
         case "hue":
            return "Huế";

         case "hải phòng":
         case "hai phong":
            return "Hải Phòng";

         case "cần thơ":
         case "can tho":
            return "Cần Thơ";

         default:
            return null;
      }
   }
}
