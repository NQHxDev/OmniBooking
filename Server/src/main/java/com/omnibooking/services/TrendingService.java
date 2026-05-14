package com.omnibooking.services;

import java.util.List;

public interface TrendingService {
   List<String> getTrendingDestinations(String countryCode, int limit);
}
