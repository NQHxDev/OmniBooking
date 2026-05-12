package com.omnibooking.services;

import com.omnibooking.dto.response.DestinationSuggestionResponse;
import java.util.List;

public interface DestinationService {
   List<DestinationSuggestionResponse> searchSuggestions(String query, String locale);
   List<DestinationSuggestionResponse> getTrending(String countryCode);
}
