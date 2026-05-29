package com.omnibooking.services.property;

import com.omnibooking.document.PropertyDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PropertySearchService {
   Page<PropertyDocument> searchProperties(String query, Double minPrice, Double maxPrice, Integer stars, 
         String propertyType, List<String> amenities, Double minRating, Pageable pageable);
}
