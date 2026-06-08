package com.omnibooking.services.search;

import java.util.List;

public interface TrendingService {

   List<String> getTrendingDestinations(String countryCode, int limit);

}
