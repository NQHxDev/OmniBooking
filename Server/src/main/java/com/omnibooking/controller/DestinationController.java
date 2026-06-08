package com.omnibooking.controller;

import com.omnibooking.dto.ApiResponse;
import com.omnibooking.dto.response.DestinationSuggestionResponse;
import com.omnibooking.services.property.DestinationService;
import com.omnibooking.services.core.GeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {

   private final DestinationService destinationService;

   private final GeoLocationService geoLocationService;

   @GetMapping("/trending")
   public ApiResponse<List<DestinationSuggestionResponse>> getTrending(
         HttpServletRequest request,
         @RequestParam(required = false, defaultValue = "vi") String locale) {
      String ipAddress = getClientIp(request);
      String countryCode = geoLocationService.getCountryCode(ipAddress);

      // log.info("Fetching trending destinations for IP: {} (Country: {}) with
      // locale: {}", ipAddress, countryCode, locale);

      List<DestinationSuggestionResponse> trending = destinationService.getTrending(countryCode, locale);

      return ApiResponse.success(trending);
   }

   @GetMapping("/search")
   public ApiResponse<List<DestinationSuggestionResponse>> search(
         @RequestParam String query,
         @RequestParam(required = false, defaultValue = "vi") String locale) {

      List<DestinationSuggestionResponse> suggestions = destinationService.searchSuggestions(query, locale);

      return ApiResponse.success(suggestions);
   }

   private String getClientIp(HttpServletRequest request) {
      String remoteAddr = "";

      if (request != null) {
         remoteAddr = request.getHeader("X-FORWARDED-FOR");
         if (remoteAddr == null || "".equals(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
         }
      }

      return remoteAddr;
   }

}
