package com.omnibooking.services.property;

import com.omnibooking.dto.response.DestinationSuggestionResponse;

import java.util.List;

public interface DestinationService {

   List<DestinationSuggestionResponse> searchSuggestions(String query, String locale);

   List<DestinationSuggestionResponse> getTrending(String countryCode, String locale);

   void registerDestinationIfNeeded(String cityName, String countryName, Double latitude, Double longitude);

}
