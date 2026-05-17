package com.omnibooking.controller;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.dto.ApiResponse;
import com.omnibooking.services.property.PropertySearchService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties/search")
@RequiredArgsConstructor
public class PropertySearchController {

   private final PropertySearchService propertySearchService;

   @GetMapping
   public ApiResponse<Page<PropertyDocument>> search(
         @RequestParam(required = false) String ss, // query
         @RequestParam(required = false) Double minPrice,
         @RequestParam(required = false) Double maxPrice,
         @RequestParam(required = false) Integer stars,
         @RequestParam(required = false, name = "group_adults") Integer adults,
         @RequestParam(required = false, name = "group_children") Integer children,
         @RequestParam(required = false, name = "no_rooms") Integer rooms,
         @RequestParam(required = false) String propertyType,
         @RequestParam(required = false) List<String> amenities,
         @RequestParam(required = false) Double minRating,
         Pageable pageable) {

      Page<PropertyDocument> results = propertySearchService.searchProperties(
            ss, minPrice, maxPrice, stars, propertyType, amenities, minRating, pageable);
      return ApiResponse.success(results);
   }

}
