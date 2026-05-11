package com.omnibooking.services.impl;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.services.PropertySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertySearchServiceImpl implements PropertySearchService {

   private final ElasticsearchOperations elasticsearchOperations;

   @Override
   public Page<PropertyDocument> searchProperties(String query, Double minPrice, Double maxPrice, Integer stars,
         String propertyType, List<String> amenities, Double minRating, Pageable pageable) {

      Criteria criteria = new Criteria();

      // 1. Text Search (Fuzzy/Multi-match equivalent)
      if (query != null && !query.isBlank()) {
         Criteria textCriteria = new Criteria("name").fuzzy(query)
               .or(new Criteria("city").fuzzy(query))
               .or(new Criteria("address").fuzzy(query))
               .or(new Criteria("description").fuzzy(query));
         criteria.and(textCriteria);
      }

      // 2. Price Filter
      if (minPrice != null) {
         criteria.and(new Criteria("minPrice").greaterThanEqual(minPrice));
      }
      if (maxPrice != null) {
         criteria.and(new Criteria("minPrice").lessThanEqual(maxPrice));
      }

      // 3. Star Rating Filter
      if (stars != null) {
         criteria.and(new Criteria("starRating").is(stars));
      }

      // 4. Property Type Filter
      if (propertyType != null && !propertyType.isBlank()) {
         criteria.and(new Criteria("propertyType").is(propertyType));
      }

      // 5. Amenities Filter (Matches ALL specified amenities)
      if (amenities != null && !amenities.isEmpty()) {
         for (String amenity : amenities) {
            criteria.and(new Criteria("amenities").is(amenity));
         }
      }

      // 6. Rating Filter
      if (minRating != null) {
         criteria.and(new Criteria("averageRating").greaterThanEqual(minRating));
      }

      CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
      criteriaQuery.setPageable(pageable);

      SearchHits<PropertyDocument> searchHits = elasticsearchOperations.search(criteriaQuery, PropertyDocument.class);

      List<PropertyDocument> results = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList();

      return new PageImpl<>(results, pageable, searchHits.getTotalHits());
   }
}
